use anyhow::Result;
use chrono::Utc;
use crate::github::GithubContext;
use crate::models::Issue;

pub fn run(ctx: &GithubContext) -> Result<()> {
    println!("== Running Cleanup Job ==");
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
            ctx.close_issue(issue.number)?;
        }
    }
    Ok(())
}