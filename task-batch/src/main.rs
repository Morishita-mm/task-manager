use anyhow::{Context, Result};
use base64::{engine::general_purpose, Engine as _};
use chrono::{Datelike, Duration, Local, Utc, Weekday}; // Duration, Utcを追加
use dotenv::dotenv;
use reqwest::header;
use serde::{Deserialize, Serialize};
use std::env;
use std::fs::{self, File}; // 追加
use std::io::Write; // 追加
use std::path::Path; // 追加

// ==========================================
// Data Structures
// ==========================================

// アプリケーション全体で共有するGitHub接続情報
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

// Issue生成時のレスポンス用 (最小限)
#[derive(Debug, Deserialize)]
struct IssueResponse {
    number: u32,
    #[allow(dead_code)]
    title: String,
}

// ▼▼▼ 追加: 取得・集計用の詳細なIssue構造体 ▼▼▼
#[derive(Debug, Deserialize)]
struct Issue {
    number: u32,
    title: String,
    state: String,
    created_at: chrono::DateTime<chrono::Utc>,
    closed_at: Option<chrono::DateTime<chrono::Utc>>,
    labels: Vec<Label>,
}

#[derive(Debug, Deserialize)]
struct Label {
    name: String,
}

// Issue更新用 (Close時などに使用)
#[derive(Debug, Serialize)]
struct IssueUpdate {
    state: String,
}

// ==========================================
// Main Entry Point
// ==========================================

fn main() -> Result<()> {
    dotenv().ok();

    // 1. 初期化
    let ctx = init_github_context().context("Failed to initialize GitHub context")?;
    println!("Target Repository: {}/{}", ctx.owner, ctx.repo);

    // 2. コマンドライン引数の判定
    // 使用法:
    //   cargo run -- generate (or 指定なし)
    //   cargo run -- cleanup
    //   cargo run -- report
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

/// 環境変数とHTTPクライアントを初期化してContextを返す
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

/// GitHubからYAML設定ファイルを取得してパースする
fn fetch_routine_config(ctx: &GithubContext) -> Result<RoutineConfig> {
    println!("Fetching config/routines.yaml...");

    let url = format!(
        "https://api.github.com/repos/{}/{}/contents/config/routines.yaml",
        ctx.owner, ctx.repo
    );

    let resp = ctx.client.get(&url).send()?;

    // 404等の場合をハンドリング
    if !resp.status().is_success() {
        println!("Warning: Config file not found or inaccessible.");
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

/// 定期タスク生成ロジック
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

/// ▼▼▼ お掃除機能 (Cleanup) ▼▼▼
/// Openかつroutineラベルが付いた古いタスクをCloseする
fn run_cleanup_job(ctx: &GithubContext) -> Result<()> {
    // 1. 対象のIssueを取得 (Open, label=routine)
    // ページネーションは簡易的に省略していますが、本来は考慮すべきです
    let url = format!(
        "https://api.github.com/repos/{}/{}/issues?state=open&labels=routine&per_page=100",
        ctx.owner, ctx.repo
    );

    let resp = ctx.client.get(&url).send()?;
    let issues: Vec<Issue> = resp.json()?;

    let now = Utc::now();
    let threshold_days = 3; // 3日以上前のものを消す

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

/// ▼▼▼ 日報作成機能 (Report) ▼▼▼
/// 今日Closeされたタスクを集計してMarkdownファイルに出力する
fn run_report_job(ctx: &GithubContext) -> Result<()> {
    // 1. 過去24時間以内に更新されたClosed Issueを取得 (検索範囲を絞る)
    let today_start_utc = Utc::now() - Duration::hours(24);
    let since_str = today_start_utc.format("%Y-%m-%dT%H:%M:%SZ").to_string();

    let url = format!(
        "https://api.github.com/repos/{}/{}/issues?state=closed&since={}&per_page=100",
        ctx.owner, ctx.repo, since_str
    );

    let resp = ctx.client.get(&url).send()?;
    let issues: Vec<Issue> = resp.json()?;

    // 2. ローカル時間で「今日」Closeされたものだけフィルタリング
    let local_now = Local::now();
    let today_str = local_now.format("%Y-%m-%d").to_string();

    let closed_today: Vec<&Issue> = issues
        .iter()
        .filter(|i| match i.closed_at {
            Some(dt) => {
                let local_dt = dt.with_timezone(&Local);
                local_dt.format("%Y-%m-%d").to_string() == today_str
            }
            None => false,
        })
        .collect();

    if closed_today.is_empty() {
        println!("No tasks completed today ({}).", today_str);
        return Ok(());
    }

    // 3. レポートディレクトリの作成
    let reports_dir = "reports";
    if !Path::new(reports_dir).exists() {
        fs::create_dir(reports_dir)?;
    }

    // 4. ファイル出力
    let file_path = format!("{}/{}.md", reports_dir, today_str);
    let mut file = File::create(&file_path)?;

    writeln!(file, "# Daily Report: {}\n", today_str)?;
    writeln!(file, "## Completed Tasks\n")?;

    println!("Generating report for {} tasks...", closed_today.len());

    for issue in closed_today {
        // コンソール出力
        println!(" - [x] #{} {}", issue.number, issue.title);
        // ファイル書き込み
        writeln!(file, "- [x] #{} {}", issue.number, issue.title)?;
    }

    println!("Report generated successfully: {}", file_path);
    Ok(())
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