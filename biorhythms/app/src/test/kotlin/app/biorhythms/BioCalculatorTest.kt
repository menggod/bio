package app.biorhythms

import app.biorhythms.core.algorithm.BioCalculator
import app.biorhythms.core.algorithm.RhythmType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class BioCalculatorTest {

    @Test
    fun testCycleValues() {
        // 出生当天 days = 0，sin(0) = 0, percent = 50%, value = 0%
        val stat0 = BioCalculator.getStat(RhythmType.PHYSIC, 0.0)
        assertEquals(0.0, stat0, 0.0001)

        val percent0 = BioCalculator.getPercent(RhythmType.PHYSIC, 0.0)
        assertEquals(50.0, percent0, 0.0001)

        // 1/4 周期，波峰：sin(pi/2) = 1, percent = 100%, value = 100%
        val quarterDays = RhythmType.PHYSIC.cycleDays / 4.0
        val statPeak = BioCalculator.getStat(RhythmType.PHYSIC, quarterDays)
        assertEquals(1.0, statPeak, 0.0001)

        val percentPeak = BioCalculator.getPercent(RhythmType.PHYSIC, quarterDays)
        assertEquals(100.0, percentPeak, 0.0001)

        // 3/4 周期，波谷：sin(3pi/2) = -1, percent = 0%, value = -100%
        val valleyDays = RhythmType.PHYSIC.cycleDays * 0.75
        val statValley = BioCalculator.getStat(RhythmType.PHYSIC, valleyDays)
        assertEquals(-1.0, statValley, 0.0001)

        val percentValley = BioCalculator.getPercent(RhythmType.PHYSIC, valleyDays)
        assertEquals(0.0, percentValley, 0.0001)
    }

    @Test
    fun testCompatibilityStatic() {
        // 同一天出生的人，契合度应为 100%
        val compatSame = BioCalculator.getCompatStatic(1000L, 1000L, RhythmType.EMOTIONAL)
        assertEquals(100.0f, compatSame, 0.001f)

        // 相差半个周期的人，契合度应为 0%
        val halfPeriod = (RhythmType.PHYSIC.cycleDays / 2f).toLong()
        val compatOpposite = BioCalculator.getCompatStatic(1000L, 1000L + halfPeriod, RhythmType.PHYSIC)
        assertTrue("相差半周期契合度接近0", compatOpposite <= 10.0f)
    }

    @Test
    fun testDiffDays() {
        val birth = LocalDate.of(2000, 1, 1)
        val target = LocalDate.of(2000, 1, 11)
        assertEquals(10L, BioCalculator.getDiffDays(birth, target))
    }
}
