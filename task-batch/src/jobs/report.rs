use anyhow::{Result, anyhow};
use chrono::{Duration, Local, Utc};
use base64::{engine::general_purpose, Engine as _};
use serde_json::json;
use std::collections::HashMap;
use std::fmt::Write as FmtWrite;
use crate::github::GithubContext;
use crate::models::Issue;
use crate::utils::{parse_time_from_labels, parse_context_from_labels, format_issue_line};

pub fn run(ctx: &GithubContext) -> Result<()> {
    let today_str = Local::now().format("%Y-%m-%d").to_string();
    println!("== Running Report Job for: {} ==", today_str);

    let tasks = fetch_todays_closed_issues(ctx)?;
    let notes = fetch_todays_open_notes(ctx)?;

    if tasks.is_empty() && notes.is_empty() {
        println!("No tasks or notes found for today.");
        return Ok(());
    }

    let content = generate_rich_report_content(&today_str, &tasks, &notes);
    
    let file_path = format!("reports/{}.md", today_str);
    let message = format!("Add daily report: {}", today_str);
    upload_file_to_github(ctx, &file_path, &content, &message)?;
    println!("Report generated and pushed to: {}", file_path);

    if !notes.is_empty() {
        println!("Closing {} notes...", notes.len());
        for note in notes {
            ctx.close_issue(note.number)?;
        }
    }

    Ok(())
}

fn fetch_todays_closed_issues(ctx: &GithubContext) -> Result<Vec<Issue>> {
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

    let closed_today: Vec<Issue> = issues
        .into_iter()
        .filter(|i| {
            if i.labels.iter().any(|l| l.name == "type:note") { return false; }
            match i.closed_at {
                Some(dt) => dt.with_timezone(&Local).format("%Y-%m-%d").to_string() == today_str,
                None => false,
            }
        })
        .collect();
    
    Ok(closed_today)
}

fn fetch_todays_open_notes(ctx: &GithubContext) -> Result<Vec<Issue>> {
    let since_utc = Utc::now() - Duration::hours(24);
    let since_str = since_utc.format("%Y-%m-%dT%H:%M:%SZ").to_string();

    let url = format!(
        "https://api.github.com/repos/{}/{}/issues?state=open&labels=type:note&since={}&per_page=100",
        ctx.owner, ctx.repo, since_str
    );

    let resp = ctx.client.get(&url).send()?;
    let issues: Vec<Issue> = resp.json()?;
    Ok(issues)
}

fn generate_rich_report_content(date: &str, tasks: &[Issue], notes: &[Issue]) -> String {
    let mut md = String::new();
    let total_tasks = tasks.len();
    let mut total_minutes = 0;
    let mut category_map: HashMap<String, Vec<&Issue>> = HashMap::new();
    let mut highlights: Vec<&Issue> = Vec::new();
    let context_order = vec!["Work", "Dev", "Study", "Life", "Health", "Other"];

    for issue in tasks {
        total_minutes += parse_time_from_labels(&issue.labels);
        let category = parse_context_from_labels(&issue.labels).unwrap_or("Other".to_string());
        category_map.entry(category).or_default().push(issue);

        if issue.labels.iter().any(|l| l.name == "p:critical" || l.name == "p:high") {
            highlights.push(issue);
        }
    }

    let hours = total_minutes / 60;
    let mins = total_minutes % 60;
    let time_str = format!("{}h {}m", hours, mins);
    let focus_area = category_map.iter().max_by_key(|entry| entry.1.len()).map(|(k, _)| k.as_str()).unwrap_or("None");

    let _ = writeln!(md, "# 📅 Daily Report: {}", date);
    let _ = writeln!(md, "");
    let _ = writeln!(md, "> **Summary**");
    let _ = writeln!(md, "> ✅ Completed: **{} tasks**", total_tasks);
    let _ = writeln!(md, "> 📝 Notes: **{} posts**", notes.len());
    let _ = writeln!(md, "> ⏱️ Est. Time: **{}**", time_str);
    let _ = writeln!(md, "> 🏆 Focus: **{}**", focus_area);
    let _ = writeln!(md, "");

    if !highlights.is_empty() {
        let _ = writeln!(md, "## 🔥 Highlights");
        for issue in &highlights {
            let _ = writeln!(md, "{}", format_issue_line(issue));
        }
        let _ = writeln!(md, "");
    }

    for ctx in &context_order {
        if let Some(items) = category_map.get(*ctx) {
            let icon = match *ctx {
                "Work" => "🏢", "Dev" => "💻", "Study" => "📚", "Life" => "🏠", "Health" => "💪", _ => "📂"
            };
            let _ = writeln!(md, "## {} {}", icon, ctx);
            for issue in items {
                let _ = writeln!(md, "{}", format_issue_line(issue));
            }
            let _ = writeln!(md, "");
        }
    }

    for (ctx, items) in &category_map {
        if !context_order.contains(&ctx.as_str()) {
            let _ = writeln!(md, "## 📂 {}", ctx);
            for issue in items {
                let _ = writeln!(md, "{}", format_issue_line(issue));
            }
            let _ = writeln!(md, "");
        }
    }

    if !notes.is_empty() {
        let _ = writeln!(md, "## 📝 Daily Notes");
        let mut sorted_notes = notes.to_vec();
        sorted_notes.sort_by_key(|n| n.created_at);

        for note in sorted_notes {
            let local_time = note.created_at.with_timezone(&Local);
            let time_str = local_time.format("%H:%M").to_string();
            let _ = writeln!(md, "- `{}` {}", time_str, note.title);
        }
        let _ = writeln!(md, "");
    }

    let _ = writeln!(md, "---");
    let _ = write!(md, "*Generated by task-batch*");
    md
}

fn upload_file_to_github(ctx: &GithubContext, path: &str, content: &str, message: &str) -> Result<()> {
    let url = format!("https://api.github.com/repos/{}/{}/contents/{}", ctx.owner, ctx.repo, path);
    let get_resp = ctx.client.get(&url).send()?;
    let sha = if get_resp.status().is_success() {
        let json: serde_json::Value = get_resp.json()?;
        json["sha"].as_str().map(|s| s.to_string())
    } else { None };

    let encoded_content = general_purpose::STANDARD.encode(content);
    let mut body = json!({ "message": message, "content": encoded_content });
    if let Some(s) = sha {
        body.as_object_mut().unwrap().insert("sha".to_string(), json!(s));
    }

    let put_resp = ctx.client.put(&url).json(&body).send()?;
    if !put_resp.status().is_success() {
        let err_text = put_resp.text()?;
        return Err(anyhow!("GitHub upload failed: {}", err_text));
    }
    Ok(())
}