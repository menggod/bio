package app.biorhythms

import app.biorhythms.core.algorithm.LifeCompassCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class LifeCompassCalculatorTest {

    @Test
    fun testLifePathNumber() {
        // 1996-05-20: 1+9+9+6+0+5+2+0 = 32 -> 3+2 = 5
        val info = LifeCompassCalculator.calculateLifePathNumber(LocalDate.of(1996, 5, 20))
        assertEquals(5, info.number)
        assertEquals("自由探险家", info.title)
    }

    @Test
    fun testDayMaster() {
        // 2024-02-10（甲辰龙年正月初一）：干支日历验证
        val info = LifeCompassCalculator.calculateDayMaster(LocalDate.of(2024, 2, 10))
        assertTrue(info.heavenlyStem.isNotBlank())
        assertTrue(info.earthlyBranch.isNotBlank())
    }

    @Test
    fun testPlanetAges() {
        val birth = LocalDate.of(2000, 1, 1)
        val now = LocalDate.of(2020, 1, 1)
        val planetAges = LifeCompassCalculator.calculatePlanetAges(birth, now)
        assertEquals(5, planetAges.size)
        // 20 地球年，水星约 83 岁
        val mercury = planetAges.find { it.planetName.contains("水星") }!!
        assertTrue(mercury.age > 80.0)
    }

    @Test
    fun testLifeBatteryCustomExpectedAge() {
        val birth = LocalDate.of(2000, 1, 1)
        val now = LocalDate.of(2020, 1, 1) // 约 20 年 = 7305 天
        val battery85 = LifeCompassCalculator.calculateLifeBattery(birth, now, expectedAge = 85)
        assertEquals(85, battery85.expectedAge)
        assertEquals(31046L, battery85.totalLifeDays)

        val battery100 = LifeCompassCalculator.calculateLifeBattery(birth, now, expectedAge = 100)
        assertEquals(100, battery100.expectedAge)
        assertEquals(36525L, battery100.totalLifeDays)
        assertTrue(battery100.batteryPercent < battery85.batteryPercent)
        assertTrue(battery100.remainingDays > battery85.remainingDays)
    }
}
