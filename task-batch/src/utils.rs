use chrono::Weekday;
use regex::Regex;
use crate::models::{Issue, Label};

pub fn is_scheduled_today(schedule: &str, today: Weekday) -> bool {
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

pub fn parse_time_from_labels(labels: &[Label]) -> u32 {
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

pub fn parse_context_from_labels(labels: &[Label]) -> Option<String> {
    for label in labels {
        if label.name.starts_with("c:") {
            let ctx = label.name.trim_start_matches("c:");
            let mut c = ctx.chars();
            return Some(match c.next() {
                None => String::new(),
                Some(f) => f.to_uppercase().collect::<String>() + c.as_str(),
            });
        }
    }
    None
}

pub fn format_issue_line(issue: &Issue) -> String {
    let tags: Vec<String> = issue.labels.iter()
        .filter(|l| !l.name.starts_with("c:") && l.name != "mobile-entry" && l.name != "routine")
        .map(|l| format!("`{}`", l.name))
        .collect();
    
    let tags_str = if tags.is_empty() { String::new() } else { format!(" {}", tags.join(" ")) };
    
    format!("- [x] #{} {}{}", issue.number, issue.title, tags_str)
}