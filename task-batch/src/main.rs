mod models;
mod github;
mod utils;
mod jobs;

use anyhow::{Context, Result};
use dotenv::dotenv;
use std::env;
use crate::github::GithubContext;

fn main() -> Result<()> {
    dotenv().ok();

    let ctx = GithubContext::new().context("Failed to initialize GitHub context")?;
    println!("Target Repository: {}/{}", ctx.owner, ctx.repo);

    let args: Vec<String> = env::args().collect();
    let command = if args.len() > 1 {
        args[1].as_str()
    } else {
        "generate"
    };

    match command {
        "generate" => jobs::generate::run(&ctx)?,
        "cleanup" => jobs::cleanup::run(&ctx)?,
        "report" => jobs::report::run(&ctx)?,
        _ => {
            eprintln!("Unknown command: {}", command);
            eprintln!("Available commands: generate, cleanup, report");
        }
    }

    Ok(())
}