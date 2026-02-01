use anyhow::Result;
use reqwest::header;
use std::env;
use crate::models::{CreateIssueRequest, IssueResponse, IssueUpdate};

pub struct GithubContext {
    pub client: reqwest::blocking::Client,
    pub owner: String,
    pub repo: String,
}

impl GithubContext {
    pub fn new() -> Result<Self> {
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

        Ok(Self { client, owner, repo })
    }

    pub fn create_issue(&self, title: &str, labels: Vec<String>) -> Result<()> {
        let url = format!("https://api.github.com/repos/{}/{}/issues", self.owner, self.repo);
        let body = CreateIssueRequest {
            title: title.to_string(),
            labels,
        };
        let resp = self.client.post(&url).json(&body).send()?;
        
        if resp.status().is_success() {
            let issue: IssueResponse = resp.json()?;
            println!("    [Success] Created Issue #{}", issue.number);
        } else {
            eprintln!("    [Error] Failed to create issue: {:?}", resp.text()?);
        }
        Ok(())
    }

    pub fn close_issue(&self, issue_number: u32) -> Result<()> {
        let url = format!("https://api.github.com/repos/{}/{}/issues/{}", self.owner, self.repo, issue_number);
        let body = IssueUpdate { state: "closed".to_string() };
        let resp = self.client.patch(&url).json(&body).send()?;
        
        if resp.status().is_success() {
            println!("    [Success] Closed Issue #{}", issue_number);
        } else {
            eprintln!("    [Error] Failed to close issue: {:?}", resp.text()?);
        }
        Ok(())
    }
}