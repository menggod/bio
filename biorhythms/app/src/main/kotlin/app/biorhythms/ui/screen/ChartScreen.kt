package app.biorhythms.ui.screen

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.biorhythms.core.algorithm.DayRhythmStatus
import app.biorhythms.core.algorithm.RhythmType
import app.biorhythms.ui.components.BiorhythmChart
import app.biorhythms.ui.components.getRhythmColor
import app.biorhythms.ui.viewmodel.MainUiState
import app.biorhythms.ui.viewmodel.MainViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartScreen(
    viewModel: MainViewModel,
    uiState: MainUiState,
    onNavigateToNotes: () -> Unit,
    onOpenPureChart: () -> Unit,
    onOpenInfo: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showProfileDropdown by remember { mutableStateOf(false) }
    var activeTypes by remember { mutableStateOf(setOf(RhythmType.PHYSIC, RhythmType.EMOTIONAL, RhythmType.INTELLECT)) }

    val currentProfile = uiState.selectedProfile
    val currentDate = uiState.selectedDate

    val dateFormatter = remember { DateTimeFormatter.ofPattern("yyyy年MM月dd日 EEEE", Locale.CHINESE) }

    // 日期选择弹窗
    val datePickerDialog = remember(currentDate) {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                viewModel.setDate(LocalDate.of(year, month + 1, dayOfMonth))
            },
            currentDate.year,
            currentDate.monthValue - 1,
            currentDate.dayOfMonth
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. 顶部人物选择条 (整卡支持点击展开，左右对称，右侧箭头舒适对齐)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showProfileDropdown = true }
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "用户档案",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = currentProfile?.name ?: "未选择档案",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (currentProfile != null) {
                                Text(
                                    text = "生日: ${currentProfile.birthDate} · 已生活 ${uiState.livedDays} 天",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // 右侧下拉箭头：保持标准的 16dp 内边距，绝不顶边
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "切换人物",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }

                DropdownMenu(
                    expanded = showProfileDropdown,
                    onDismissRequest = { showProfileDropdown = false }
                ) {
                    uiState.profiles.forEach { p ->
                        DropdownMenuItem(
                            text = { Text(p.name + if (p.id == currentProfile?.id) " (当前)" else "") },
                            onClick = {
                                viewModel.selectProfile(p.id)
                                showProfileDropdown = false
                            },
                            leadingIcon = {
                                Icon(Icons.Default.AccountCircle, contentDescription = null)
                            }
                        )
                    }
                }
            }
        }

        // 2. 日期控制栏
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = { viewModel.changeDays(-1) }) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "前一天")
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { datePickerDialog.show() }
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = currentDate.format(dateFormatter),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (currentDate == LocalDate.now()) "【今天】" else "点击更改日期",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row {
                    if (currentDate != LocalDate.now()) {
                        IconButton(onClick = { viewModel.resetToToday() }) {
                            Icon(Icons.Default.Today, contentDescription = "回到今天", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    IconButton(onClick = { viewModel.changeDays(1) }) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "后一天")
                    }
                }
            }
        }

        // 3. 核心节律波形图
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "生物节律曲线",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onOpenInfo, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "科普指南",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            FilledTonalButton(
                onClick = onOpenPureChart,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Icon(Icons.Default.Fullscreen, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("单页纯图模式", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        // 周期单选/多选筛选器 (支持单看体力/情绪/智力)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val chips = listOf(
                Triple(RhythmType.PHYSIC, "🔴 体力 (23d)", Color(0xFFE53935)),
                Triple(RhythmType.EMOTIONAL, "🔵 情绪 (28d)", Color(0xFF1E88E5)),
                Triple(RhythmType.INTELLECT, "🟢 智力 (33d)", Color(0xFF43A047))
            )
            chips.forEach { (type, label, color) ->
                val isSelected = activeTypes.contains(type)
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        val newSet = if (isSelected) {
                            if (activeTypes.size > 1) activeTypes - type else activeTypes
                        } else {
                            activeTypes + type
                        }
                        activeTypes = newSet
                    },
                    label = {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (currentProfile != null) {
                    BiorhythmChart(
                        birthDate = currentProfile.birthDate,
                        selectedDate = currentDate,
                        onSelectDate = { viewModel.setDate(it) },
                        noteDates = uiState.noteDates,
                        activeTypes = activeTypes.toList(),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(vertical = 12.dp)
                    )

                    IconButton(
                        onClick = onOpenPureChart,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInFull,
                            contentDescription = "单页纯图模式",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("请先添加或选择个人档案")
                    }
                }
            }
        }

        // 4. 综合状态评估与临界日预警
        if (uiState.overall != null) {
            val isCritical = uiState.overall.criticalCount > 0
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isCritical) Color(0xFFFFF3E0) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                ),
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
                        imageVector = if (isCritical) Icons.Default.WarningAmber else Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = if (isCritical) Color(0xFFE65100) else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Column {
                        Text(
                            text = uiState.overall.label,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isCritical) Color(0xFFBF360C) else MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = if (isCritical) {
                                "当日有 ${uiState.overall.criticalCount} 项节律穿过零点（临界转换期），机能敏感易疲劳或波动，建议放平心态、沉稳应对。"
                            } else {
                                "整体机能状态评估指数: ${String.format("%.1f", uiState.overall.score)}，节律平稳顺畅。"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // 5. 各周期状态指标卡片列表
        Text(
            text = "当日节律详标 (精确还原原版计算)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        uiState.dayRhythms.forEach { rhythm ->
            RhythmProgressCard(rhythm = rhythm)
        }

        // 6. 快捷便签入口
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToNotes() }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.EditNote,
                        contentDescription = "便签",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = if (uiState.todayNote != null) "已记录今日日记便签" else "记录今日感受与自我打分",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (uiState.todayNote != null) {
                                "吻合度: ${String.format("%.1f%%", uiState.todayNote.compatScore)} · 点击查看详情"
                            } else {
                                "打分检验自身体感与生物节律理论的吻合度"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun RhythmProgressCard(rhythm: DayRhythmStatus) {
    val color = getRhythmColor(rhythm.type)
    val percentValue = rhythm.percent.toFloat() / 100f

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
                    Text(
                        text = rhythm.type.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "(${rhythm.type.cycleDays.toInt()}天)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (rhythm.isCritical) {
                        Surface(
                            color = Color(0xFFFFE0B2),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "临界日",
                                color = Color(0xFFE65100),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Text(
                    text = "${String.format("%.1f", rhythm.value)}% (${String.format("%.0f", rhythm.percent)}%)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }

            LinearProgressIndicator(
                progress = { percentValue },
                color = color,
                trackColor = color.copy(alpha = 0.15f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
            )

            Text(
                text = rhythm.type.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
