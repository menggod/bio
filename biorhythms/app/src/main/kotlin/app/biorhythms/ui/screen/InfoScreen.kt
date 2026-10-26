package app.biorhythms.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.biorhythms.ui.components.ColorEmotional
import app.biorhythms.ui.components.ColorIntellect
import app.biorhythms.ui.components.ColorPhysical

@Composable
fun InfoScreen(
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (onBack != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                }
                Text(
                    text = "生物节律科学指南",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        // 1. 简介横幅
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Column {
                    Text(
                        text = "关于人体生物节律 (Biorhythms)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "生物节律理论源于19世纪末，认为人自出生起就存在特定周期的正弦波动，对健康指导与自我觉察具有趣味和参考价值。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 2. 三大基本周期
        Text(
            text = "三大核心生理节律",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        InfoSectionCard(
            title = "🔴 体力节律 (Physical Cycle) - 23天",
            color = ColorPhysical,
            content = "由德国医生威廉·弗里斯（Wilhelm Fliess）提出。影响身体机能、体力耐力、协调性、免疫力以及对疾病的抵抗能力。\n\n" +
                    "• 高潮期（前 11.5 天）：体力充沛、动作敏捷、耐力好，适宜高强度运动、竞技、出差或重体力工作。\n" +
                    "• 低潮期（后 11.5 天）：容易疲惫、恢复较慢，宜注意劳逸结合、充足睡眠。"
        )

        InfoSectionCard(
            title = "🔵 情绪节律 (Emotional Cycle) - 28天",
            color = ColorEmotional,
            content = "由奥地利心理学家赫尔曼·斯沃博达（Hermann Swoboda）提出。影响神经敏感度、情绪稳定性、共情力与心理韧性。\n\n" +
                    "• 高潮期（前 14 天）：情绪高昂、开朗乐观、富有感染力和创造力，适合社交、演讲与团队沟通。\n" +
                    "• 低潮期（后 14 天）：情绪敏感易波动、易感伤或焦虑，适合独处深思、静心休养。"
        )

        InfoSectionCard(
            title = "🟢 智力节律 (Intellectual Cycle) - 33天",
            color = ColorIntellect,
            content = "由奥地利因斯布鲁克大学阿尔弗雷德·特尔舍尔（Alfred Teltscher）教授通过对学生成绩研究提出。\n\n" +
                    "• 高潮期（前 16.5 天）：思维敏捷、逻辑严密、记忆力极佳，适合学习新知识、做重大战略决策、攻坚难题。\n" +
                    "• 低潮期（后 16.5 天）：思维反应相对较缓、注意力易分散，宜做重复性、事务性工作。"
        )

        // 3. 临界日特别警示
        Text(
            text = "特别关注：临界日 (Critical Days)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFFA000))
                    Text(
                        text = "什么是临界日？",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF5D4037)
                    )
                }

                Text(
                    text = "当正弦波曲线穿过零轴（由正半周进入负半周，或由负半周进入正半周）的那一天，称为【临界日】。\n\n" +
                            "在临界日，人体的内部状态处于剧烈转换与不稳定平衡期，最容易发生判断失误、情绪失控、意外受伤或过度疲劳。\n\n" +
                            "💡 建议：在应用图表中看到临界日标记时，出行注意安全，避免急躁争辩或仓促做不可逆的重大决断。",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF4E342E),
                    lineHeight = 20.sp
                )
            }
        }
    }
}

@Composable
private fun InfoSectionCard(
    title: String,
    color: Color,
    content: String
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = content,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )
        }
    }
}
