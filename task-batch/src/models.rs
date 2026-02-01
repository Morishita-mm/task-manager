use serde::{Deserialize, Serialize};

#[derive(Debug, Deserialize)]
pub struct RoutineConfig {
    pub routines: Vec<Routine>,
}

#[derive(Debug, Deserialize)]
pub struct Routine {
    pub title: String,
    pub schedule: String,
    pub labels: Vec<String>,
}

#[derive(Debug, Serialize)]
pub struct CreateIssueRequest {
    pub title: String,
    pub labels: Vec<String>,
}

#[derive(Debug, Deserialize)]
pub struct IssueResponse {
    pub number: u32,
    #[allow(dead_code)]
    pub title: String,
}

#[derive(Debug, Deserialize, Clone)]
pub struct Issue {
    pub number: u32,
    pub title: String,
    #[allow(dead_code)]
    pub state: String,
    pub created_at: chrono::DateTime<chrono::Utc>,
    pub closed_at: Option<chrono::DateTime<chrono::Utc>>,
    pub labels: Vec<Label>,
    pub body: Option<String>,
}

#[derive(Debug, Deserialize, Clone)]
pub struct Label {
    pub name: String,
}

#[derive(Debug, Serialize)]
pub struct IssueUpdate {
    pub state: String,
}