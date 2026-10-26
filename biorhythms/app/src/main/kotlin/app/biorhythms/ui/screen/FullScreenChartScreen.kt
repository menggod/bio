package app.biorhythms.ui.screen

import android.app.DatePickerDialog
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import app.biorhythms.core.algorithm.BioCalculator
import app.biorhythms.core.algorithm.RhythmType
import app.biorhythms.ui.components.BiorhythmChart
import app.biorhythms.ui.components.ColorEmotional
import app.biorhythms.ui.components.ColorIntellect
import app.biorhythms.ui.components.ColorPhysical
import app.biorhythms.ui.viewmodel.MainUiState
import app.biorhythms.ui.viewmodel.MainViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun FullScreenChartScreen(
    viewModel: MainViewModel,
    uiState: MainUiState,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentProfile = uiState.selectedProfile
    val currentDate = uiState.selectedDate
    val dateFormatter = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd EEEE", Locale.CHINESE) }
    var activeTypes by remember { mutableStateOf(setOf(RhythmType.PHYSIC, RhythmType.EMOTIONAL, RhythmType.INTELLECT)) }

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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 1. 铺满全屏的核心波形图表
        if (currentProfile != null) {
            BiorhythmChart(
                birthDate = currentProfile.birthDate,
                selectedDate = currentDate,
                onSelectDate = { viewModel.setDate(it) },
                noteDates = uiState.noteDates,
                activeTypes = activeTypes.toList(),
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("暂未选择个人档案", style = MaterialTheme.typography.bodyLarge)
            }
        }

        // 2. 顶部悬浮微胶囊工具栏（极简、半透明毛玻璃）
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
            shadowElevation = 6.dp,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 12.dp, start = 16.dp, end = 16.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 退出全屏按钮
                IconButton(
                    onClick = onExit,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "退出全屏",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                // 中间人物与日期（点击可快捷换日期）
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { datePickerDialog.show() }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = currentProfile?.name ?: "",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "·",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = currentDate.format(dateFormatter),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }

                // 回到今天快捷按钮
                if (currentDate != LocalDate.now()) {
                    FilledTonalButton(
                        onClick = { viewModel.resetToToday() },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("今天", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 3. 底部悬浮极简数值 HUD（一眼看清当日三大数值）
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
            shadowElevation = 6.dp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentProfile != null) {
                    val days = BioCalculator.getDiffDays(currentProfile.birthDate, currentDate).toDouble()
                    val pVal = BioCalculator.getValue(RhythmType.PHYSIC, days)
                    val eVal = BioCalculator.getValue(RhythmType.EMOTIONAL, days)
                    val iVal = BioCalculator.getValue(RhythmType.INTELLECT, days)

                    HudPill(
                        label = "体力",
                        value = pVal,
                        color = ColorPhysical,
                        isActive = activeTypes.contains(RhythmType.PHYSIC),
                        onToggle = {
                            val newSet = if (activeTypes.contains(RhythmType.PHYSIC)) {
                                if (activeTypes.size > 1) activeTypes - RhythmType.PHYSIC else activeTypes
                            } else activeTypes + RhythmType.PHYSIC
                            activeTypes = newSet
                        }
                    )
                    HudPill(
                        label = "情绪",
                        value = eVal,
                        color = ColorEmotional,
                        isActive = activeTypes.contains(RhythmType.EMOTIONAL),
                        onToggle = {
                            val newSet = if (activeTypes.contains(RhythmType.EMOTIONAL)) {
                                if (activeTypes.size > 1) activeTypes - RhythmType.EMOTIONAL else activeTypes
                            } else activeTypes + RhythmType.EMOTIONAL
                            activeTypes = newSet
                        }
                    )
                    HudPill(
                        label = "智力",
                        value = iVal,
                        color = ColorIntellect,
                        isActive = activeTypes.contains(RhythmType.INTELLECT),
                        onToggle = {
                            val newSet = if (activeTypes.contains(RhythmType.INTELLECT)) {
                                if (activeTypes.size > 1) activeTypes - RhythmType.INTELLECT else activeTypes
                            } else activeTypes + RhythmType.INTELLECT
                            activeTypes = newSet
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun HudPill(
    label: String,
    value: Double,
    color: Color,
    isActive: Boolean = true,
    onToggle: () -> Unit = {}
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onToggle)
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (isActive) color else color.copy(alpha = 0.25f))
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal
        )
        Text(
            text = "${if (value >= 0) "+" else ""}${String.format("%.0f", value)}%",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = if (isActive) color else color.copy(alpha = 0.25f)
        )
    }
}
