package app.biorhythms.core.algorithm

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * 经典三大生物节律定义 (100% 忠实还原原版 BioCalc.State)
 */
enum class RhythmType(
    val cycleDays: Float,
    val title: String,
    val description: String
) {
    PHYSIC(23.0f, "体力", "身体耐力、抗病能力、身体协调与充沛活力"),
    EMOTIONAL(28.0f, "情绪", "情绪稳定性、同理心、创造力与神经兴奋性"),
    INTELLECT(33.0f, "智力", "逻辑推理、记忆力、分析判断与思维敏捷度");
}

/**
 * 某一日期的具体节律状态
 */
data class DayRhythmStatus(
    val type: RhythmType,
    val stat: Double,       // [-1.0, 1.0]
    val percent: Double,    // [0.0, 100.0]
    val value: Double,      // [-100.0, 100.0]
    val isCritical: Boolean // 是否为临界日（穿越0轴）
)

/**
 * 综合状态指标
 */
data class OverallEvaluation(
    val score: Float, // +1.0 (极佳), +0.5 (偏好), -0.5 (偏弱), -1.0 (低谷)
    val label: String,
    val criticalCount: Int
)

/**
 * 生物节律精准计算器 (精准还原 app.biorhythms.BioCalc)
 */
object BioCalculator {

    private const val TWO_PI = 6.283185307179586

    /**
     * 计算两个日期之间的精确天数差 (利用标准 java.time 解决旧版 Calendar 的时区/夏令时 Bug)
     */
    fun getDiffDays(birthDate: LocalDate, targetDate: LocalDate): Long {
        return ChronoUnit.DAYS.between(birthDate, targetDate)
    }

    /**
     * 原版 getStat: 正弦值 [-1.0, 1.0]
     */
    fun getStat(type: RhythmType, days: Double): Double {
        return sin((TWO_PI * days) / type.cycleDays)
    }

    /**
     * 原版 getStatMiddleDay: 中午时分状态
     */
    fun getStatMiddleDay(type: RhythmType, days: Double): Double {
        return sin((TWO_PI * (0.5 + days)) / type.cycleDays)
    }

    /**
     * 原版 getPercent: 映射到 [0%, 100%]
     */
    fun getPercent(type: RhythmType, days: Double): Double {
        return (getStat(type, days) + 1.0) * 50.0
    }

    /**
     * 原版 getValue: 映射到 [-100%, +100%]
     */
    fun getValue(type: RhythmType, days: Double): Double {
        return getStat(type, days) * 100.0
    }

    /**
     * 判断某一天对于某周期是否为临界日（曲线穿过0轴当天）
     */
    fun isCriticalDay(type: RhythmType, days: Long): Boolean {
        val statToday = getStat(type, days.toDouble())
        val statYesterday = getStat(type, (days - 1).toDouble())
        val statTomorrow = getStat(type, (days + 1).toDouble())
        
        // 符号异号或极接近0说明穿过零点
        return (statToday * statYesterday <= 0.0) || (statToday * statTomorrow <= 0.0) || abs(statToday) < 0.15
    }

    /**
     * 原版 getStatTotal: 综合三大基本节律的极性判定
     */
    fun getStatTotal(days: Double): Float {
        val primaryStats = RhythmType.values()
        var count = 0
        for (state in primaryStats) {
            count = if (getStatMiddleDay(state, days) < 0.0) count - 1 else count + 1
        }
        return when (count) {
            primaryStats.size -> 1.0f
            -primaryStats.size -> -1.0f
            in 1 until primaryStats.size -> 0.5f
            else -> -0.5f
        }
    }

    /**
     * 获取某一天的综合评估
     */
    fun getOverallEvaluation(days: Long): OverallEvaluation {
        val total = getStatTotal(days.toDouble())
        var criticalCount = 0
        RhythmType.values().forEach {
            if (isCriticalDay(it, days)) criticalCount++
        }

        val label = when {
            criticalCount >= 2 -> "多重临界转换日 (注意身心平衡)"
            criticalCount == 1 -> "临界转换日 (状态易波动)"
            total >= 1.0f -> "三重巅峰期 (精力旺盛)"
            total > 0.0f -> "良好上升期 (状态积极)"
            total <= -1.0f -> "多维低谷期 (宜休养蓄锐)"
            else -> "相对平缓期 (平稳渡过)"
        }

        return OverallEvaluation(total, label, criticalCount)
    }

    /**
     * 原版 getCompatStatic: 双人静态契合度百分比 [0%, 100%]
     */
    fun getCompatStatic(days1: Long, days2: Long, type: RhythmType): Float {
        val diff1 = ((days1 % type.cycleDays) + type.cycleDays) % type.cycleDays
        val diff2 = ((days2 % type.cycleDays) + type.cycleDays) % type.cycleDays
        val daysDiff = abs(diff1 - diff2)
        val stateHalf = type.cycleDays / 2.0f

        return if (daysDiff < stateHalf) {
            100.0f - ((daysDiff * 100.0f) / stateHalf)
        } else {
            val daysDiff2 = daysDiff - stateHalf
            if (daysDiff2 > 0.0f) {
                (daysDiff2 * 100.0f) / stateHalf
            } else {
                0.0f
            }
        }
    }

    /**
     * 原版 getCompatPercent: 某天动态契合度
     */
    fun getCompatPercent(type: RhythmType, days1: Double, days2: Double): Double {
        return 100.0 - abs(getPercent(type, days1) - getPercent(type, days2))
    }

    /**
     * 原版 getFeeling: 某天双人综合契合感受
     */
    fun getFeeling(days1: Double, days2: Double): Float {
        val p = getCompatPercent(RhythmType.PHYSIC, days1, days2)
        val e = getCompatPercent(RhythmType.EMOTIONAL, days1, days2)
        val i = getCompatPercent(RhythmType.INTELLECT, days1, days2)
        return ((p + e + i) / 3.0).toFloat()
    }

    /**
     * 原版 getNoteCompat: 日记打分 (1~5星) 与实际生物节律的吻合度百分比
     */
    fun getNoteCompat(type: RhythmType, days: Float, rating: Float): Float {
        val percent = getPercent(type, days.toDouble()).roundToInt()
        val kof = percent % 5
        val roundedValue = if (kof < 3) percent - kof else percent + (5 - kof)
        return (100.0f - abs((20.0f * rating) - roundedValue)).coerceIn(0.0f, 100.0f)
    }

    /**
     * 获取某一天的三大节律状态列表
     */
    fun getDayRhythms(days: Long): List<DayRhythmStatus> {
        return RhythmType.values().map { type ->
            DayRhythmStatus(
                type = type,
                stat = getStat(type, days.toDouble()),
                percent = getPercent(type, days.toDouble()),
                value = getValue(type, days.toDouble()),
                isCritical = isCriticalDay(type, days)
            )
        }
    }
}
