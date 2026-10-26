package app.biorhythms.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.biorhythms.core.algorithm.BioCalculator
import app.biorhythms.core.algorithm.RhythmType
import app.biorhythms.core.model.Profile
import app.biorhythms.ui.components.ColorEmotional
import app.biorhythms.ui.components.ColorIntellect
import app.biorhythms.ui.components.ColorPhysical
import app.biorhythms.ui.viewmodel.MainUiState
import app.biorhythms.ui.viewmodel.MainViewModel
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompatScreen(
    viewModel: MainViewModel,
    uiState: MainUiState,
    modifier: Modifier = Modifier
) {
    var showP1Dropdown by remember { mutableStateOf(false) }
    var showP2Dropdown by remember { mutableStateOf(false) }

    val p1 = uiState.compatProfile1
    val p2 = uiState.compatProfile2
    val targetDate = uiState.selectedDate

    // 原版核心算法计算契合度
    val (pCompat, eCompat, iCompat, totalCompat, todayFeeling) = remember(p1, p2, targetDate) {
        if (p1 != null && p2 != null) {
            val days1 = BioCalculator.getDiffDays(p1.birthDate, targetDate)
            val days2 = BioCalculator.getDiffDays(p2.birthDate, targetDate)

            // 原版静态契合度
            val pc = BioCalculator.getCompatStatic(days1, days2, RhythmType.PHYSIC)
            val ec = BioCalculator.getCompatStatic(days1, days2, RhythmType.EMOTIONAL)
            val ic = BioCalculator.getCompatStatic(days1, days2, RhythmType.INTELLECT)
            val tc = (pc + ec + ic) / 3f

            // 原版当天动态感受度
            val feel = BioCalculator.getFeeling(days1.toDouble(), days2.toDouble())

            CompatResult(pc, ec, ic, tc, feel)
        } else {
            CompatResult(0f, 0f, 0f, 0f, 0f)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. 标题与说明
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
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Column {
                    Text(
                        text = "双人节律契合度分析",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "100% 还原原版 Compat 算法：基于两人出生周期的相位重叠差，解析体力、情绪、智力与整体节奏的同步指数。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 2. 选择比对人物卡片
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "选择比对双方",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 人物 1
                    Box(modifier = Modifier.weight(1f)) {
                        ProfileSelectorBox(
                            profile = p1,
                            label = "人物 1",
                            onClick = { showP1Dropdown = true }
                        )
                        DropdownMenu(
                            expanded = showP1Dropdown,
                            onDismissRequest = { showP1Dropdown = false }
                        ) {
                            uiState.profiles.forEach { profile ->
                                DropdownMenuItem(
                                    text = { Text(profile.name) },
                                    onClick = {
                                        viewModel.setCompatProfiles(profile, p2)
                                        showP1Dropdown = false
                                    }
                                )
                            }
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.SyncAlt,
                        contentDescription = "对比",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )

                    // 人物 2
                    Box(modifier = Modifier.weight(1f)) {
                        ProfileSelectorBox(
                            profile = p2,
                            label = "人物 2",
                            onClick = { showP2Dropdown = true }
                        )
                        DropdownMenu(
                            expanded = showP2Dropdown,
                            onDismissRequest = { showP2Dropdown = false }
                        ) {
                            uiState.profiles.forEach { profile ->
                                DropdownMenuItem(
                                    text = { Text(profile.name) },
                                    onClick = {
                                        viewModel.setCompatProfiles(p1, profile)
                                        showP2Dropdown = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. 综合契合度得分大卡片
        if (p1 != null && p2 != null) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "综合节奏契合指数",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "${String.format("%.1f", totalCompat)}%",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )

                    LinearProgressIndicator(
                        progress = { totalCompat / 100f },
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                    )

                    Text(
                        text = "今日动态共鸣感受度: ${String.format("%.1f", todayFeeling)}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 4. 三大核心维度细分（还原原版 CompatActivity 的 3 个进度条）
            Text(
                text = "各维度相位契合明细",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            CompatDimensionCard(
                title = "体力契合度",
                percent = pCompat,
                cycleDays = 23,
                color = ColorPhysical,
                tip = "体力步调与生活作息、运动运动同步率"
            )

            CompatDimensionCard(
                title = "情绪契合度",
                percent = eCompat,
                cycleDays = 28,
                color = ColorEmotional,
                tip = "情感共鸣、心境体谅与日常沟通顺畅度"
            )

            CompatDimensionCard(
                title = "智力契合度",
                percent = iCompat,
                cycleDays = 33,
                color = ColorIntellect,
                tip = "思维观念、话题共鸣与决策配合度"
            )

            // 5. 关系与相处贴士
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
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
                        Icon(Icons.Default.TipsAndUpdates, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text = "相处与协作指引",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = getRelationshipAdvice(totalCompat, pCompat, eCompat, iCompat),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "至少需要有两个人物档案才能进行契合度比对，请先前往档案页添加人物。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private data class CompatResult(
    val physic: Float,
    val emotional: Float,
    val intellect: Float,
    val total: Float,
    val feeling: Float
)

@Composable
private fun ProfileSelectorBox(
    profile: Profile?,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Text(
                text = profile?.name ?: "点击选择",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            if (profile != null) {
                Text(
                    text = "${profile.birthDate}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CompatDimensionCard(
    title: String,
    percent: Float,
    cycleDays: Int,
    color: Color,
    tip: String
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text("(${cycleDays}天周期)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Text(
                    text = "${String.format("%.1f", percent)}%",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }

            LinearProgressIndicator(
                progress = { percent / 100f },
                color = color,
                trackColor = color.copy(alpha = 0.15f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
            )

            Text(tip, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun getRelationshipAdvice(total: Float, p: Float, e: Float, i: Float): String {
    return buildString {
        when {
            total >= 80f -> append("🌟 极高默契度：你们在生理、情绪与思维节律上展现出难得的高度同频，相处自然顺畅，合作共事极易产生1+1>2的效果。")
            total >= 60f -> append("✨ 良好互补型：你们的整体节律稳健，属于优秀的搭档或伴侣。在相处中能够互相理解支持。")
            total >= 40f -> append("⚖️ 动态平衡型：你们的生物周期存在一定错位，当一方精力低谷时，另一方往往处于上升期，正好可以互相照应与轮流掌舵。")
            else -> append("🤝 差异特质型：你们的周期相位差异较大，这意味着彼此拥有截然不同的节奏偏好。给予彼此足够的个人空间和包容将让关系更加融洽。")
        }
        append("\n")
        if (e >= 70f) {
            append("💡 情绪契合度高达 ${String.format("%.0f", e)}%，彼此的心情起伏几乎完全共振，极易产生同理心。")
        } else if (e <= 30f) {
            append("💡 情绪节律呈反相，当一人情绪敏感时另一人往往平静理性，非常适合作为情绪的避风港。")
        }
    }
}
