use anyhow::{Context, Result};
use base64::{engine::general_purpose, Engine as _};
use chrono::{Datelike, Duration, Local, Utc, Weekday};
use dotenv::dotenv;
use reqwest::header;
use serde::{Deserialize, Serialize};
use serde_json::json; // json! マクロを使用するために追加
use std::env;
// std::fs, std::io::Write は不要になったため削除
use std::fmt::Write as FmtWrite; // Stringへの書き込み用

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

/// ▼▼▼ 修正版: GitHubリポジトリに直接ファイルをPushする ▼▼▼
fn run_report_job(ctx: &GithubContext) -> Result<()> {
    // 1. 集計対象のIssueを取得
    let today_start_utc = Utc::now() - Duration::hours(24);
    let since_str = today_start_utc.format("%Y-%m-%dT%H:%M:%SZ").to_string();

    let url = format!(
        "https://api.github.com/repos/{}/{}/issues?state=closed&since={}&per_page=100",
        ctx.owner, ctx.repo, since_str
    );

    let resp = ctx.client.get(&url).send()?;
    let issues: Vec<Issue> = resp.json()?;

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

    // 2. Markdownコンテンツの作成 (Stringバッファに書き込む)
    let mut content = String::new();
    writeln!(&mut content, "# Daily Report: {}\n", today_str)?;
    writeln!(&mut content, "## Completed Tasks\n")?;

    for issue in closed_today {
        writeln!(&mut content, "- [x] #{} {}", issue.number, issue.title)?;
        println!(" - [x] #{} {}", issue.number, issue.title);
    }

    // 3. GitHub API経由でファイルをコミット(作成/更新)
    let file_path = format!("reports/{}.md", today_str);
    let file_url = format!(
        "https://api.github.com/repos/{}/{}/contents/{}",
        ctx.owner, ctx.repo, file_path
    );

    // 既にファイルがあるか確認 (上書き用SHAを取得するため)
    let get_resp = ctx.client.get(&file_url).send()?;
    let sha = if get_resp.status().is_success() {
        let json: serde_json::Value = get_resp.json()?;
        json["sha"].as_str().map(|s| s.to_string())
    } else {
        None
    };

    // Base64エンコード
    let encoded_content = general_purpose::STANDARD.encode(&content);

    // PUTリクエストのボディ作成
    let mut body = json!({
        "message": format!("Add daily report: {}", today_str),
        "content": encoded_content
    });

    // 上書きの場合はSHAを含める
    if let Some(s) = sha {
        body.as_object_mut().unwrap().insert("sha".to_string(), json!(s));
    }

    let put_resp = ctx.client.put(&file_url).json(&body).send()?;

    if put_resp.status().is_success() {
        println!("Successfully pushed report to {}/{}", ctx.repo, file_path);
    } else {
        eprintln!("Failed to push report: {:?}", put_resp.text()?);
    }

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