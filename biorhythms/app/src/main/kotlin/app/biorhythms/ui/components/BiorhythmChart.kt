package app.biorhythms.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.biorhythms.core.algorithm.BioCalculator
import app.biorhythms.core.algorithm.RhythmType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

// 经典三大周期颜色配置
val ColorPhysical = Color(0xFFE53935)   // 体力: 鲜红
val ColorEmotional = Color(0xFF1E88E5)  // 情绪: 科技蓝
val ColorIntellect = Color(0xFF43A047)  // 智力: 生机绿

fun getRhythmColor(type: RhythmType): Color {
    return when (type) {
        RhythmType.PHYSIC -> ColorPhysical
        RhythmType.EMOTIONAL -> ColorEmotional
        RhythmType.INTELLECT -> ColorIntellect
    }
}

@Composable
fun BiorhythmChart(
    birthDate: LocalDate,
    selectedDate: LocalDate,
    onSelectDate: (LocalDate) -> Unit,
    noteDates: Set<LocalDate>,
    activeTypes: List<RhythmType> = listOf(RhythmType.PHYSIC, RhythmType.EMOTIONAL, RhythmType.INTELLECT),
    modifier: Modifier = Modifier
) {
    // 视图中心基准日期偏移（支持拖动平移）
    var scrollOffsetPx by remember { mutableFloatStateOf(0f) }
    // 默认黄金跨度 26dp：一屏可容纳 15 天左右，完整展现周期跌宕起伏的波峰与波谷
    var stepWidthDp by remember { mutableFloatStateOf(26f) }
    val density = LocalDensity.current
    val stepWidthPx = with(density) { stepWidthDp.dp.toPx() }
    val dateTextSizePx = with(density) { 11.5.sp.toPx() }
    val weekTextSizePx = with(density) { 9.5.sp.toPx() }

    val isDark = MaterialTheme.colorScheme.background.red < 0.5f
    val gridLineColor = if (isDark) Color(0x22FFFFFF) else Color(0x18000000)
    val textPrimary = if (isDark) Color(0xCCFFFFFF) else Color(0xCC000000)
    val textMuted = if (isDark) Color(0x66FFFFFF) else Color(0x66000000)
    val weekendColor = Color(0xFFE53935)
    val selectHighlight = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
    val selectLineColor = MaterialTheme.colorScheme.primary

    var hoverOffset by remember { mutableStateOf<Offset?>(null) }
    var chartWidth by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { hoverOffset = it },
                    onDrag = { change, _ ->
                        change.consume()
                        hoverOffset = change.position
                    },
                    onDragEnd = { hoverOffset = null },
                    onDragCancel = { hoverOffset = null }
                )
            }
            .pointerInput(Unit) {
                // 同时支持横向自由拖拽平移 + 双指捏合缩放视野（可缩小看一整个月或放大细看）
                detectTransformGestures { _, pan, zoom, _ ->
                    if (hoverOffset == null) {
                        scrollOffsetPx += pan.x
                        stepWidthDp = (stepWidthDp * zoom).coerceIn(16f, 44f)
                    }
                }
            }
            .pointerInput(selectedDate, scrollOffsetPx, stepWidthPx) {
                detectTapGestures { offset ->
                    val width = size.width
                    val totalOffset = offset.x - (width / 2f + scrollOffsetPx)
                    val daysDelta = (totalOffset / stepWidthPx).roundToInt()
                    val target = selectedDate.plusDays(daysDelta.toLong())
                    onSelectDate(target)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            chartWidth = width
            val centerY = height / 2f
            val waveAmplitude = (height * 0.35f) // 波形最大振幅

            val visibleDaysCount = (width / stepWidthPx).toInt() + 4
            val halfDays = visibleDaysCount / 2

            // 当前中心日期
            val offsetDays = (-scrollOffsetPx / stepWidthPx).toInt()
            val baseDate = selectedDate.plusDays(offsetDays.toLong())
            val fineOffsetPx = scrollOffsetPx + (offsetDays * stepWidthPx)

            // 1. 绘制水平参考线（0轴、+100%、-100%）
            drawLine(gridLineColor, Offset(0f, centerY), Offset(width, centerY), strokeWidth = 2f)
            drawLine(gridLineColor.copy(alpha = 0.5f), Offset(0f, centerY - waveAmplitude), Offset(width, centerY - waveAmplitude), strokeWidth = 1f)
            drawLine(gridLineColor.copy(alpha = 0.5f), Offset(0f, centerY + waveAmplitude), Offset(width, centerY + waveAmplitude), strokeWidth = 1f)

            // 2. 绘制原版经典综合电平柱 (DrawChart.drawLevels)
            for (i in -halfDays - 2..halfDays + 2) {
                val curDate = baseDate.plusDays(i.toLong())
                val dayX = (width / 2f) + fineOffsetPx + (i * stepWidthPx)
                if (dayX + stepWidthPx < 0f || dayX > width) continue

                val daysFromBirth = BioCalculator.getDiffDays(birthDate, curDate)
                val statTotal = BioCalculator.getStatTotal(daysFromBirth.toDouble())
                val barHeight = (waveAmplitude * 0.25f) * kotlin.math.abs(statTotal)

                val levelColor = when {
                    statTotal >= 1.0f -> Color(0x334CAF50)
                    statTotal > 0f -> Color(0x228BC34A)
                    statTotal <= -1.0f -> Color(0x33E53935)
                    else -> Color(0x22FF9800)
                }

                if (statTotal > 0f) {
                    drawRect(
                        color = levelColor,
                        topLeft = Offset(dayX + 2f, centerY - barHeight),
                        size = Size(stepWidthPx - 4f, barHeight)
                    )
                } else if (statTotal < 0f) {
                    drawRect(
                        color = levelColor,
                        topLeft = Offset(dayX + 2f, centerY),
                        size = Size(stepWidthPx - 4f, barHeight)
                    )
                }

                // 便签指示块（原版顶部标记）
                if (noteDates.contains(curDate)) {
                    drawRect(
                        color = Color(0xFFFFB300),
                        topLeft = Offset(dayX + 4f, 4f),
                        size = Size(stepWidthPx - 8f, 6f)
                    )
                }

                // 选中日高亮列背景
                if (curDate == selectedDate) {
                    drawRect(
                        color = selectHighlight,
                        topLeft = Offset(dayX, 0f),
                        size = Size(stepWidthPx, height)
                    )
                    // 选中垂直指示线
                    drawLine(
                        color = selectLineColor,
                        start = Offset(dayX + stepWidthPx / 2f, 0f),
                        end = Offset(dayX + stepWidthPx / 2f, height),
                        strokeWidth = 2.5f
                    )
                }

                // 垂直网格线
                drawLine(
                    color = gridLineColor,
                    start = Offset(dayX, 0f),
                    end = Offset(dayX, height),
                    strokeWidth = 1f
                )

                // 绘制日期数字与星期文本
                val isWeekend = curDate.dayOfWeek == DayOfWeek.SATURDAY || curDate.dayOfWeek == DayOfWeek.SUNDAY
                val dayColor = if (curDate == selectedDate) selectLineColor else if (isWeekend) weekendColor else textPrimary
                // 单字星期简写：避免“周一”、“周二”双字拥挤，极简美观，呼吸感充沛
                val weekDayName = when (curDate.dayOfWeek) {
                    DayOfWeek.MONDAY -> "一"
                    DayOfWeek.TUESDAY -> "二"
                    DayOfWeek.WEDNESDAY -> "三"
                    DayOfWeek.THURSDAY -> "四"
                    DayOfWeek.FRIDAY -> "五"
                    DayOfWeek.SATURDAY -> "六"
                    DayOfWeek.SUNDAY -> "日"
                }

                drawContext.canvas.nativeCanvas.apply {
                    val paint = android.graphics.Paint().apply {
                        isAntiAlias = true
                        textAlign = android.graphics.Paint.Align.CENTER
                        textSize = dateTextSizePx
                        color = dayColor.hashCode()
                        if (curDate == selectedDate) isFakeBoldText = true
                    }
                    // 顶部画天数
                    drawText("${curDate.dayOfMonth}", dayX + stepWidthPx / 2f, 42f, paint)
                    // 底部画单字星期
                    paint.textSize = weekTextSizePx
                    paint.color = if (curDate == selectedDate) selectLineColor.hashCode() else if (isWeekend) weekendColor.hashCode() else textMuted.hashCode()
                    drawText(weekDayName, dayX + stepWidthPx / 2f, height - 18f, paint)
                }
            }

            // 3. 绘制各生物周期正弦曲线
            activeTypes.forEach { type ->
                val waveColor = getRhythmColor(type)
                val path = Path()
                var isFirst = true

                // 遍历可见范围内的 X 像素，以步长 4px 连线平滑波形
                var px = -20f
                while (px <= width + 20f) {
                    val totalOffset = px - (width / 2f + fineOffsetPx)
                    val daysDouble = (totalOffset / stepWidthPx).toDouble()
                    val curDays = BioCalculator.getDiffDays(birthDate, baseDate) + daysDouble
                    val stat = BioCalculator.getStat(type, curDays)
                    val py = centerY - (stat.toFloat() * waveAmplitude)

                    if (isFirst) {
                        path.moveTo(px, py)
                        isFirst = false
                    } else {
                        path.lineTo(px, py)
                    }
                    px += 4f
                }

                drawPath(
                    path = path,
                    color = waveColor,
                    style = Stroke(width = 3.5f, cap = StrokeCap.Round)
                )

                // 4. 临界日穿零指示点
                for (i in -halfDays - 1..halfDays + 1) {
                    val curDate = baseDate.plusDays(i.toLong())
                    val daysFromBirth = BioCalculator.getDiffDays(birthDate, curDate)
                    if (BioCalculator.isCriticalDay(type, daysFromBirth)) {
                        val dayCenterX = (width / 2f) + fineOffsetPx + (i * stepWidthPx) + (stepWidthPx / 2f)
                        val stat = BioCalculator.getStat(type, daysFromBirth.toDouble())
                        val dotY = centerY - (stat.toFloat() * waveAmplitude)

                        // 现代微晶光晕质感临界指示点 (秀气精致不挡波形)
                        val glowRadius = with(density) { 5.dp.toPx() }
                        val solidRadius = with(density) { 3.dp.toPx() }
                        val coreRadius = with(density) { 1.5.dp.toPx() }

                        // 外层柔光晕
                        drawCircle(
                            color = waveColor.copy(alpha = 0.3f),
                            radius = glowRadius,
                            center = Offset(dayCenterX, dotY)
                        )
                        // 中层实体色环
                        drawCircle(
                            color = waveColor,
                            radius = solidRadius,
                            center = Offset(dayCenterX, dotY)
                        )
                        // 内部白色高光微核
                        drawCircle(
                            color = Color.White,
                            radius = coreRadius,
                            center = Offset(dayCenterX, dotY)
                        )
                    }
                }
            }

            // 5. 绘制长按拖拽十字准星线与各曲线交点高亮环
            hoverOffset?.let { hOffset ->
                val hDaysDelta = ((hOffset.x - (width / 2f + fineOffsetPx)) / stepWidthPx).roundToInt()
                val hCenterX = (width / 2f) + fineOffsetPx + (hDaysDelta * stepWidthPx) + (stepWidthPx / 2f)
                val hDate = baseDate.plusDays(hDaysDelta.toLong())
                val hDaysFromBirth = BioCalculator.getDiffDays(birthDate, hDate)

                // 垂直十字准星虚线
                drawLine(
                    color = selectLineColor,
                    start = Offset(hCenterX, 0f),
                    end = Offset(hCenterX, height),
                    strokeWidth = 2.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )

                // 各条曲线上的焦点环
                activeTypes.forEach { type ->
                    val cColor = getRhythmColor(type)
                    val stat = BioCalculator.getStat(type, hDaysFromBirth.toDouble())
                    val dotY = centerY - (stat.toFloat() * waveAmplitude)
                    // 外发光环
                    drawCircle(color = cColor.copy(alpha = 0.4f), radius = 8.dp.toPx(), center = Offset(hCenterX, dotY))
                    drawCircle(color = Color.White, radius = 4.dp.toPx(), center = Offset(hCenterX, dotY))
                    drawCircle(color = cColor, radius = 2.5.dp.toPx(), center = Offset(hCenterX, dotY))
                }
            }
        }

        // 6. 悬浮气泡 HUD（Tooltip）实时展示当前准星位置数值
        if (hoverOffset != null && chartWidth > 0f) {
            val offsetDays = (-scrollOffsetPx / stepWidthPx).toInt()
            val baseDate = selectedDate.plusDays(offsetDays.toLong())
            val fineOffsetPx = scrollOffsetPx + (offsetDays * stepWidthPx)
            val hDaysDelta = ((hoverOffset!!.x - (chartWidth / 2f + fineOffsetPx)) / stepWidthPx).roundToInt()
            val hDate = baseDate.plusDays(hDaysDelta.toLong())
            val hDaysFromBirth = BioCalculator.getDiffDays(birthDate, hDate)

            val pVal = BioCalculator.getValue(RhythmType.PHYSIC, hDaysFromBirth.toDouble())
            val eVal = BioCalculator.getValue(RhythmType.EMOTIONAL, hDaysFromBirth.toDouble())
            val iVal = BioCalculator.getValue(RhythmType.INTELLECT, hDaysFromBirth.toDouble())

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                shadowElevation = 6.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = hDate.format(DateTimeFormatter.ofPattern("M月d日 E", Locale.CHINESE)),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (activeTypes.contains(RhythmType.PHYSIC)) {
                        Text(
                            text = "体 ${if (pVal >= 0) "+" else ""}${pVal.toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = ColorPhysical
                        )
                    }
                    if (activeTypes.contains(RhythmType.EMOTIONAL)) {
                        Text(
                            text = "绪 ${if (eVal >= 0) "+" else ""}${eVal.toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = ColorEmotional
                        )
                    }
                    if (activeTypes.contains(RhythmType.INTELLECT)) {
                        Text(
                            text = "智 ${if (iVal >= 0) "+" else ""}${iVal.toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = ColorIntellect
                        )
                    }
                }
            }
        }
    }
}
