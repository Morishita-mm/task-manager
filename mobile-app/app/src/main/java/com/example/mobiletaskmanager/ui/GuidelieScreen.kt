package com.example.mobiletaskmanager.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun GuidelineScreen() {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        Text("Usage Guidelines", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        SectionTitle("🏷 Label Strategy")

        SubSection("🅰️ Priority (p:)")
        GuidelineItem("p:critical", "緊急・最優先 (今日中に終わらせる)")
        GuidelineItem("p:high", "今日やる (朝イチで着手)")
        GuidelineItem("p:medium", "今週やる")
        GuidelineItem("p:low", "いつかやる")

        Spacer(modifier = Modifier.height(8.dp))

        SubSection("🅱️ Context (c:)")
        GuidelineItem("c:dev", "開発・技術")
        GuidelineItem("c:work", "本業")
        GuidelineItem("c:life", "生活・家事")
        GuidelineItem("c:study", "学習・インプット")

        Spacer(modifier = Modifier.height(8.dp))

        SubSection("🆎 Time (t:)")
        GuidelineItem("t:15m", "隙間時間 (移動中など)")
        GuidelineItem("t:1h", "まとまった時間")

        Divider(modifier = Modifier.padding(vertical = 16.dp))

        SectionTitle("📏 Granularity Rules")
        Text(
            "1. 「1タスク = 1アクション」\n身体をどう動かせばいいか分かる粒度にする。",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "2. 「2時間ルール」\n2時間以上かかるものはプロジェクト。小分けにする。",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
fun SubSection(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 4.dp, top = 4.dp)
    )
}

@Composable
fun GuidelineItem(label: String, desc: String) {
    Row(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(100.dp)
        )
        Text(
            text = desc,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
    }
}