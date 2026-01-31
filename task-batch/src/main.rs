use anyhow::{Context, Result};
use base64::{engine::general_purpose, Engine as _};
use chrono::{Datelike, Local, Weekday};
use dotenv::dotenv;
use reqwest::header;
use serde::{Deserialize, Serialize};
use std::env;

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

#[derive(Debug, Deserialize)]
struct IssueResponse {
    number: u32,
    title: String,
}

// ==========================================
// Main Entry Point
// ==========================================

fn main() -> Result<()> {
    dotenv().ok();

    // 1. 初期化: コンテキストの作成
    let ctx = init_github_context().context("Failed to initialize GitHub context")?;

    println!("Target Repository: {}/{}", ctx.owner, ctx.repo);

    // 2. 設定の読み込み
    let config = fetch_routine_config(&ctx).context("Failed to fetch routine config")?;

    // 3. 定期タスク生成ジョブの実行
    run_routine_generator(&ctx, &config).context("Failed to run routine generator")?;

    // 4. (Future) お掃除ジョブの実行
    // run_cleanup_job(&ctx)?;

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

    let resp: serde_json::Value = ctx.client.get(&url).send()?.json()?;

    let content_b64 = match resp.get("content") {
        Some(c) => c.as_str().unwrap_or(""),
        None => {
            println!("Warning: Config file not found.");
            // ファイルがない場合は空の設定を返す（エラーにはしない）
            return Ok(RoutineConfig { routines: vec![] });
        }
    };

    let clean_content = content_b64.replace("\n", "");
    let decoded_bytes = general_purpose::STANDARD.decode(clean_content)?;
    let yaml_str = String::from_utf8(decoded_bytes)?;

    let config: RoutineConfig = serde_yaml::from_str(&yaml_str)?;
    Ok(config)
}

/// 定期タスク生成ロジックのメイン部分
fn run_routine_generator(ctx: &GithubContext, config: &RoutineConfig) -> Result<()> {
    let now = Local::now();
    let current_weekday = now.weekday();
    println!("Running generator for: {:?}", current_weekday);

    for routine in &config.routines {
        if is_scheduled_today(&routine.schedule, current_weekday) {
            println!(" >> Creating task: {}", routine.title);
            create_issue(ctx, routine)?;
        } else {
            // println!("Skipping: {}", routine.title); // ログがうるさければコメントアウト
        }
    }
    Ok(())
}

/// 将来追加するお掃除機能のプレースホルダー
#[allow(dead_code)]
fn run_cleanup_job(ctx: &GithubContext) -> Result<()> {
    println!("Running cleanup job...");
    // TODO: Fetch closed issues and archive them, or close expired tasks
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

