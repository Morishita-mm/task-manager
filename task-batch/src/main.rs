use anyhow::{Context, Result};
use base64::{engine::general_purpose, Engine as _};
use chrono::{Datelike, Duration, Local, Utc, Weekday};
use dotenv::dotenv;
use reqwest::header;
use serde::{Deserialize, Serialize};
use serde_json::json; // json! マクロを使用するために追加
use std::env;
use std::fmt::Write as FmtWrite; // Stringへの書き込み用
use std::collections::HashMap;
use regex::Regex;

// ==========================================
// Data Structures
// ==========================================

struct GithubContext {
    client: reqwest::blocking::Client,
    owner: String,
    repo: String,
}

#[derive(Debug, Deserialize)]
struct RoutineConfig {
    routines: Vec<Routine>,
}

#[derive(Debug, Deserialize)]
struct Routine {
    title: String,
    schedule: String,
    labels: Vec<String>,
}

#[derive(Debug, Serialize)]
struct CreateIssueRequest {
    title: String,
    labels: Vec<String>,
}

#[derive(Debug, Deserialize)]
struct IssueResponse {
    number: u32,
    #[allow(dead_code)]
    title: String,
}

#[derive(Debug, Deserialize)]
struct Issue {
    number: u32,
    title: String,
    #[allow(dead_code)]
    state: String,
    created_at: chrono::DateTime<chrono::Utc>,
    closed_at: Option<chrono::DateTime<chrono::Utc>>,
    #[allow(dead_code)]
    labels: Vec<Label>,
}

#[derive(Debug, Deserialize)]
struct Label {
    #[allow(dead_code)]
    name: String,
}

#[derive(Debug, Serialize)]
struct IssueUpdate {
    state: String,
}

// ==========================================
// Main Entry Point
// ==========================================

fn main() -> Result<()> {
    dotenv().ok();

    let ctx = init_github_context().context("Failed to initialize GitHub context")?;
    println!("Target Repository: {}/{}", ctx.owner, ctx.repo);

    let args: Vec<String> = env::args().collect();
    let command = if args.len() > 1 {
        args[1].as_str()
    } else {
        "generate"
    };

    match command {
        "generate" => {
            println!("== Running Task Generator ==");
            let config = fetch_routine_config(&ctx).context("Failed to fetch routine config")?;
            run_routine_generator(&ctx, &config).context("Failed to run routine generator")?;
        }
        "cleanup" => {
            println!("== Running Cleanup Job ==");
            run_cleanup_job(&ctx).context("Failed to run cleanup job")?;
        }
        "report" => {
            println!("== Running Report Job ==");
            run_report_job(&ctx).context("Failed to run report job")?;
        }
        _ => {
            eprintln!("Unknown command: {}", command);
            eprintln!("Available commands: generate, cleanup, report");
        }
    }

    Ok(())
}

// ==========================================
// Logic Functions
// ==========================================

fn init_github_context() -> Result<GithubContext> {
    let token = env::var("GITHUB_TOKEN").expect("GITHUB_TOKEN must be set");
    let owner = env::var("GITHUB_OWNER").expect("GITHUB_OWNER must be set");
    let repo = env::var("GITHUB_REPO").expect("GITHUB_REPO must be set");

    let mut headers = header::HeaderMap::new();
    headers.insert(
        header::USER_AGENT,
        header::HeaderValue::from_static("task-batch-cli"),
    );
    headers.insert(
        header::AUTHORIZATION,
        header::HeaderValue::from_str(&format!("token {}", token))?,
    );

    let client = reqwest::blocking::Client::builder()
        .default_headers(headers)
        .build()?;

    Ok(GithubContext {
        client,
        owner,
        repo,
    })
}

fn fetch_routine_config(ctx: &GithubContext) -> Result<RoutineConfig> {
    println!("Fetching config/routines.yaml...");
    let url = format!(
        "https://api.github.com/repos/{}/{}/contents/config/routines.yaml",
        ctx.owner, ctx.repo
    );

    let resp = ctx.client.get(&url).send()?;
    if !resp.status().is_success() {
        println!("Warning: Config file not found.");
        return Ok(RoutineConfig { routines: vec![] });
    }

    let json_resp: serde_json::Value = resp.json()?;
    let content_b64 = match json_resp.get("content") {
        Some(c) => c.as_str().unwrap_or(""),
        None => return Ok(RoutineConfig { routines: vec![] }),
    };

    let clean_content = content_b64.replace("\n", "");
    let decoded_bytes = general_purpose::STANDARD.decode(clean_content)?;
    let yaml_str = String::from_utf8(decoded_bytes)?;

    let config: RoutineConfig = serde_yaml::from_str(&yaml_str)?;
    Ok(config)
}

fn run_routine_generator(ctx: &GithubContext, config: &RoutineConfig) -> Result<()> {
    let now = Local::now();
    let current_weekday = now.weekday();
    println!("Running generator for: {:?}", current_weekday);

    for routine in &config.routines {
        if is_scheduled_today(&routine.schedule, current_weekday) {
            println!(" >> Creating task: {}", routine.title);
            create_issue(ctx, routine)?;
        }
    }
    Ok(())
}

fn run_cleanup_job(ctx: &GithubContext) -> Result<()> {
    let url = format!(
        "https://api.github.com/repos/{}/{}/issues?state=open&labels=routine&per_page=100",
        ctx.owner, ctx.repo
    );

    let resp = ctx.client.get(&url).send()?;
    let issues: Vec<Issue> = resp.json()?;
    let now = Utc::now();
    let threshold_days = 3;

    println!("Checking {} open routine tasks...", issues.len());

    for issue in issues {
        let age = now.signed_duration_since(issue.created_at).num_days();
        if age >= threshold_days {
            println!(
                "Closing expired task: #{} '{}' ({} days old)",
                issue.number, issue.title, age
            );
            close_issue(ctx, issue.number)?;
        }
    }
    Ok(())
}

/// メインのレポートジョブ：各工程を呼び出すオーケストレーター
fn run_report_job(ctx: &GithubContext) -> Result<()> {
    // 1. 今日の完了タスクを取得
    let issues = fetch_todays_closed_issues(ctx)?;
    
    if issues.is_empty() {
        let today_str = Local::now().format("%Y-%m-%d").to_string();
        println!("No tasks completed today ({}).", today_str);
        return Ok(());
    }

    // 2. リッチなMarkdownコンテンツを生成
    let today_str = Local::now().format("%Y-%m-%d").to_string();
    let content = generate_rich_report_content(&today_str, &issues);

    // 3. GitHubへアップロード
    let file_path = format!("reports/{}.md", today_str);
    let message = format!("Add daily report: {}", today_str);
    
    upload_file_to_github(ctx, &file_path, &content, &message)?;
    
    println!("Report generated and pushed to: {}", file_path);
    Ok(())
}

/// 工程1: 今日の完了タスクを取得・フィルタリングする
fn fetch_todays_closed_issues(ctx: &GithubContext) -> Result<Vec<Issue>> {
    // 直近24時間のIssueを取得 (タイムゾーンのズレを考慮して少し広めに取る)
    let since_utc = Utc::now() - Duration::hours(30);
    let since_str = since_utc.format("%Y-%m-%dT%H:%M:%SZ").to_string();

    let url = format!(
        "https://api.github.com/repos/{}/{}/issues?state=closed&since={}&per_page=100",
        ctx.owner, ctx.repo, since_str
    );

    let resp = ctx.client.get(&url).send()?;
    let issues: Vec<Issue> = resp.json()?;

    let local_now = Local::now();
    let today_str = local_now.format("%Y-%m-%d").to_string();

    // 実際に「現地時間の今日」に完了したものをフィルタリング
    let closed_today: Vec<Issue> = issues
        .into_iter()
        .filter(|i| match i.closed_at {
            Some(dt) => {
                let local_dt = dt.with_timezone(&Local);
                local_dt.format("%Y-%m-%d").to_string() == today_str
            }
            None => false,
        })
        .collect();
    
    Ok(closed_today)
}

/// 工程2: 集計を行ってMarkdownテキストを生成する
fn generate_rich_report_content(date: &str, issues: &[Issue]) -> String {
    let mut md = String::new();
    
    // --- 集計処理 ---
    let total_tasks = issues.len();
    let mut total_minutes = 0;
    let mut category_map: HashMap<String, Vec<&Issue>> = HashMap::new();
    let mut highlights: Vec<&Issue> = Vec::new();

    // 表示順序の定義
    let context_order = vec!["Work", "Dev", "Study", "Life", "Health", "Other"];

    for issue in issues {
        // 時間集計 (t:XXm)
        total_minutes += parse_time_from_labels(&issue.labels);

        // カテゴリ分け (c:Context)
        let category = parse_context_from_labels(&issue.labels).unwrap_or("Other".to_string());
        category_map.entry(category).or_default().push(issue);

        // ハイライト判定 (p:critical, p:high)
        if issue.labels.iter().any(|l| l.name == "p:critical" || l.name == "p:high") {
            highlights.push(issue);
        }
    }

    // 時間フォーマット
    let hours = total_minutes / 60;
    let mins = total_minutes % 60;
    let time_str = if hours > 0 {
        format!("{}h {}m", hours, mins)
    } else {
        format!("{}m", mins)
    };

    // フォーカスエリア (最多タスクのカテゴリ)
    let focus_area = category_map.iter()
        .max_by_key(|entry| entry.1.len())
        .map(|(k, _)| k.as_str())
        .unwrap_or("None");

    // --- Markdown構築 ---
    let _ = writeln!(md, "# 📅 Daily Report: {}", date);
    let _ = writeln!(md, "");
    let _ = writeln!(md, "> **Summary**");
    let _ = writeln!(md, "> ✅ Completed: **{} tasks**", total_tasks);
    let _ = writeln!(md, "> ⏱️ Est. Time: **{}**", time_str);
    let _ = writeln!(md, "> 🏆 Focus: **{}**", focus_area);
    let _ = writeln!(md, "");

    // ハイライト
    if !highlights.is_empty() {
        let _ = writeln!(md, "## 🔥 Highlights");
        for issue in &highlights {
            let _ = writeln!(md, "{}", format_issue_line(issue));
        }
        let _ = writeln!(md, "");
    }

    // カテゴリ別表示 (順序指定)
    for ctx in &context_order {
        if let Some(items) = category_map.get(*ctx) {
            let icon = match *ctx {
                "Work" => "🏢", "Dev" => "💻", "Study" => "📚",
                "Life" => "🏠", "Health" => "💪", _ => "📂"
            };
            let _ = writeln!(md, "## {} {}", icon, ctx);
            for issue in items {
                let _ = writeln!(md, "{}", format_issue_line(issue));
            }
            let _ = writeln!(md, "");
        }
    }

    // 定義順以外の残りカテゴリ
    for (ctx, items) in &category_map {
        if !context_order.contains(&ctx.as_str()) {
            let _ = writeln!(md, "## 📂 {}", ctx);
            for issue in items {
                let _ = writeln!(md, "{}", format_issue_line(issue));
            }
            let _ = writeln!(md, "");
        }
    }

    let _ = writeln!(md, "---");
    let _ = write!(md, "*Generated by task-batch*");

    md
}

/// 工程3: ファイルをGitHubにアップロード(作成/更新)する
fn upload_file_to_github(ctx: &GithubContext, path: &str, content: &str, message: &str) -> Result<()> {
    let url = format!(
        "https://api.github.com/repos/{}/{}/contents/{}",
        ctx.owner, ctx.repo, path
    );

    // 既にファイルがあるか確認 (SHA取得)
    let get_resp = ctx.client.get(&url).send()?;
    let sha = if get_resp.status().is_success() {
        let json: serde_json::Value = get_resp.json()?;
        json["sha"].as_str().map(|s| s.to_string())
    } else {
        None
    };

    let encoded_content = general_purpose::STANDARD.encode(content);

    let mut body = json!({
        "message": message,
        "content": encoded_content
    });

    if let Some(s) = sha {
        body.as_object_mut().unwrap().insert("sha".to_string(), json!(s));
    }

    let put_resp = ctx.client.put(&url).json(&body).send()?;

    if !put_resp.status().is_success() {
        // エラー詳細を表示して早期リターン
        let err_text = put_resp.text()?;
        eprintln!("Failed to upload file: {}", err_text);
        return Err(anyhow::anyhow!("GitHub upload failed: {}", err_text));
    }

    Ok(())
}

// ==========================================
// Parsing Helper Functions
// ==========================================

/// ラベル (t:15m, t:1h) から分数を抽出して合計する
fn parse_time_from_labels(labels: &[Label]) -> u32 {
    // 正規表現はコストが高いのでループ外でコンパイルするか、lazy_staticを使いたいところですが、
    // バッチ処理なので都度生成でも許容範囲です。
    let re_min = Regex::new(r"^t:(\d+)m$").unwrap();
    let re_hour = Regex::new(r"^t:(\d+)h$").unwrap();

    let mut minutes = 0;
    for label in labels {
        if let Some(caps) = re_min.captures(&label.name) {
            if let Ok(m) = caps[1].parse::<u32>() {
                minutes += m;
            }
        } else if let Some(caps) = re_hour.captures(&label.name) {
            if let Ok(h) = caps[1].parse::<u32>() {
                minutes += h * 60;
            }
        }
    }
    minutes
}

/// ラベル (c:work) からコンテキスト名 (Work) を抽出する
fn parse_context_from_labels(labels: &[Label]) -> Option<String> {
    for label in labels {
        if label.name.starts_with("c:") {
            let ctx = label.name.trim_start_matches("c:");
            // 先頭を大文字にする
            let mut c = ctx.chars();
            return Some(match c.next() {
                None => String::new(),
                Some(f) => f.to_uppercase().collect::<String>() + c.as_str(),
            });
        }
    }
    None
}

/// タスク行を整形する (- [x] Title `tag`)
fn format_issue_line(issue: &Issue) -> String {
    let tags: Vec<String> = issue.labels.iter()
        .filter(|l| !l.name.starts_with("c:") && l.name != "mobile-entry" && l.name != "routine")
        .map(|l| format!("`{}`", l.name))
        .collect();
    
    let tags_str = if tags.is_empty() { String::new() } else { format!(" {}", tags.join(" ")) };
    
    format!("- [x] #{} {}{}", issue.number, issue.title, tags_str)
}

// ==========================================
// Helper Functions
// ==========================================

fn is_scheduled_today(schedule: &str, today: Weekday) -> bool {
    if schedule == "daily" {
        return true;
    }
    if let Some(day_str) = schedule.strip_prefix("weekly:") {
        let target_day = match day_str.to_lowercase().as_str() {
            "mon" => Weekday::Mon,
            "tue" => Weekday::Tue,
            "wed" => Weekday::Wed,
            "thu" => Weekday::Thu,
            "fri" => Weekday::Fri,
            "sat" => Weekday::Sat,
            "sun" => Weekday::Sun,
            _ => return false,
        };
        return today == target_day;
    }
    false
}

fn create_issue(ctx: &GithubContext, routine: &Routine) -> Result<()> {
    let url = format!(
        "https://api.github.com/repos/{}/{}/issues",
        ctx.owner, ctx.repo
    );
    let body = CreateIssueRequest {
        title: routine.title.clone(),
        labels: routine.labels.clone(),
    };
    let resp = ctx.client.post(&url).json(&body).send()?;
    if resp.status().is_success() {
        let issue: IssueResponse = resp.json()?;
        println!("    [Success] Created Issue #{}", issue.number);
    } else {
        eprintln!("    [Error] Failed to create issue: {:?}", resp.text()?);
    }
    Ok(())
}

fn close_issue(ctx: &GithubContext, issue_number: u32) -> Result<()> {
    let url = format!(
        "https://api.github.com/repos/{}/{}/issues/{}",
        ctx.owner, ctx.repo, issue_number
    );
    let body = IssueUpdate {
        state: "closed".to_string(),
    };
    let resp = ctx.client.patch(&url).json(&body).send()?;
    if resp.status().is_success() {
        println!("    [Success] Closed Issue #{}", issue_number);
    } else {
        eprintln!("    [Error] Failed to close issue: {:?}", resp.text()?);
    }
    Ok(())
}