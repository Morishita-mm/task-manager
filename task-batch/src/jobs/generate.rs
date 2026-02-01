use anyhow::{Context, Result};
use chrono::{Datelike, Local};
use base64::{engine::general_purpose, Engine as _};
use crate::github::GithubContext;
use crate::models::RoutineConfig;
use crate::utils::is_scheduled_today;

pub fn run(ctx: &GithubContext) -> Result<()> {
    println!("== Running Task Generator ==");
    let config = fetch_routine_config(ctx).context("Failed to fetch routine config")?;
    
    let now = Local::now();
    let current_weekday = now.weekday();
    println!("Running generator for: {:?}", current_weekday);

    for routine in &config.routines {
        if is_scheduled_today(&routine.schedule, current_weekday) {
            println!(" >> Creating task: {}", routine.title);
            ctx.create_issue(&routine.title, routine.labels.clone())?;
        }
    }
    Ok(())
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