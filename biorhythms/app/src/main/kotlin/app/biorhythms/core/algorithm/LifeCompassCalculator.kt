package app.biorhythms.core.algorithm

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs

/**
 * 宇宙行星年龄实体
 */
data class PlanetAge(
    val planetName: String,
    val orbitalDays: Double,
    val age: Double,
    val iconEmoji: String,
    val description: String
)

/**
 * 东方干支日主元神实体
 */
data class DayMasterInfo(
    val heavenlyStem: String,     // 天干：甲、乙、丙、丁...
    val earthlyBranch: String,     // 地支：子、丑、寅、卯...
    val element: String,           // 五行：木、火、土、金、水
    val natureSymbol: String,      // 自然意象：如“参天大树”、“太阳光辉”、“江河奔流”
    val characteristic: String     // 核心性格特质
)

/**
 * 西方生命灵数实体
 */
data class LifePathInfo(
    val number: Int,
    val title: String,
    val keyword: String,
    val description: String
)

/**
 * 生命电量与时间里程碑实体
 */
data class LifeBatteryInfo(
    val livedDays: Long,
    val totalLifeDays: Long,       // 目标寿命总天数 (如 85 岁约 31,046 天)
    val batteryPercent: Float,     // 已走过人生百分比
    val remainingDays: Long,       // 剩余天数
    val traveledKmBillions: Double,// 跟随地球公转太空狂奔里程（亿公里）
    val heartBeatsBillions: Double,// 心跳估算（亿次）
    val stageName: String,         // 人生四时阶段：朝阳启蒙、盛夏繁茂、金秋沉淀、从容远阔
    val milestone10kDaysLeft: Long,// 距离第 10,000 天（万日礼）还有多少天（已过则为负）
    val milestone20kDaysLeft: Long,// 距离第 20,000 天还有多少天
    val expectedAge: Int = 85      // 设定的目标预期寿命基准 (岁)
)

/**
 * 生命罗盘核心计算器 (生命电量、宇宙行星年龄、毕达哥拉斯灵数、东方日元干支)
 */
object LifeCompassCalculator {

    private const val EARTH_ORBIT_SPEED_KM_PER_DAY = 2572992.0 // 地球公转每日飞行约 257.3 万公里

    /**
     * 计算生命电量与人体天文里程碑
     */
    fun calculateLifeBattery(birthDate: LocalDate, targetDate: LocalDate = LocalDate.now(), expectedAge: Int = 85): LifeBatteryInfo {
        val livedDays = ChronoUnit.DAYS.between(birthDate, targetDate).coerceAtLeast(0)
        val totalDays = (expectedAge * 365.25).toLong()
        val percent = ((livedDays.toDouble() / totalDays.toDouble()) * 100.0).toFloat().coerceIn(0f, 100f)
        val remainingDays = (totalDays - livedDays).coerceAtLeast(0)

        // 跟随地球公转行驶里程 (1公里 = 1/100000000 亿公里)
        val kmBillions = (livedDays * EARTH_ORBIT_SPEED_KM_PER_DAY) / 100000000.0

        // 心跳估算 (按每分钟平均 72 次)
        val heartBeats = (livedDays * 24.0 * 60.0 * 72.0) / 100000000.0

        val ageYears = livedDays / 365.25
        val stageName = when {
            ageYears < 25 -> "🌱 朝阳启蒙期 (积蓄力量·探索世界)"
            ageYears < 50 -> "☀️ 盛夏炽热期 (事业开拓·黄金担当)"
            ageYears < 75 -> "🍂 金秋丰硕期 (智慧沉淀·从容圆融)"
            else -> "🌌 宁静从容期 (阅尽千帆·天地自在)"
        }

        val milestone10k = 10000L - livedDays
        val milestone20k = 20000L - livedDays

        return LifeBatteryInfo(
            livedDays = livedDays,
            totalLifeDays = totalDays,
            batteryPercent = percent,
            remainingDays = remainingDays,
            traveledKmBillions = kmBillions,
            heartBeatsBillions = heartBeats,
            stageName = stageName,
            milestone10kDaysLeft = milestone10k,
            milestone20kDaysLeft = milestone20k,
            expectedAge = expectedAge
        )
    }

    /**
     * 计算宇宙行星年龄
     */
    fun calculatePlanetAges(birthDate: LocalDate, targetDate: LocalDate = LocalDate.now()): List<PlanetAge> {
        val livedDays = ChronoUnit.DAYS.between(birthDate, targetDate).toDouble().coerceAtLeast(0.0)

        return listOf(
            PlanetAge(
                planetName = "水星 (Mercury)",
                orbitalDays = 87.97,
                age = livedDays / 87.969,
                iconEmoji = "☿️",
                description = "公转周期仅 88 天，离太阳最近的极速行者"
            ),
            PlanetAge(
                planetName = "金星 (Venus)",
                orbitalDays = 224.70,
                age = livedDays / 224.701,
                iconEmoji = "♀️",
                description = "晨昏长明，炽热而浪漫的启明星"
            ),
            PlanetAge(
                planetName = "火星 (Mars)",
                orbitalDays = 686.98,
                age = livedDays / 686.980,
                iconEmoji = "♂️",
                description = "赤红荒原，人类未来的星际驿站"
            ),
            PlanetAge(
                planetName = "木星 (Jupiter)",
                orbitalDays = 4332.59,
                age = livedDays / 4332.589,
                iconEmoji = "♃",
                description = "公转近 12 地球年，太阳系的巍峨守护神"
            ),
            PlanetAge(
                planetName = "土星 (Saturn)",
                orbitalDays = 10759.22,
                age = livedDays / 10759.22,
                iconEmoji = "♄",
                description = "公转约 29.5 地球年，拥有瑰丽光环的宏伟巨星"
            )
        )
    }

    /**
     * 计算毕达哥拉斯西方生命灵数 (1~9 及 11/22/33 大师数)
     */
    fun calculateLifePathNumber(birthDate: LocalDate): LifePathInfo {
        var sum = 0
        val dateString = "${birthDate.year}${birthDate.monthValue}${birthDate.dayOfMonth}"
        for (ch in dateString) {
            sum += ch.digitToInt()
        }

        // 持续累加单字直到成为一位数或大师数
        while (sum > 9 && sum != 11 && sum != 22 && sum != 33) {
            var temp = 0
            for (ch in sum.toString()) {
                temp += ch.digitToInt()
            }
            sum = temp
        }

        return when (sum) {
            1 -> LifePathInfo(1, "开拓领导者", "独立 · 创见 · 魄力", "天生的先锋与破局者，意志坚韧，渴望开创属于自己的独特天地。")
            2 -> LifePathInfo(2, "和谐协作者", "敏锐 · 同理 · 优雅", "天生的和平使者与协调纽带，心思细致，极具洞察力与艺术感悟。")
            3 -> LifePathInfo(3, "灵感表达者", "创意 · 热情 · 感染力", "生命洋溢着创造火花与表达欲，善于传递快乐，具有出众的感染力。")
            4 -> LifePathInfo(4, "秩序筑基者", "稳健 · 务实 · 专注", "基石般可靠稳重，讲究逻辑规律与执行落地，是团队不可或缺的磐石。")
            5 -> LifePathInfo(5, "自由探险家", "冒险 · 蜕变 · 旷野", "渴望打破成规与探索未知，适应力极强，生命在于丰富多彩的体验。")
            6 -> LifePathInfo(6, "温情守护者", "责任 · 关怀 · 奉献", "天生具有浓郁的慈爱与保护本能，注重家庭与谐和，极具责任担当。")
            7 -> LifePathInfo(7, "真理探索者", "哲思 · 洞见 · 静心", "拥有极深的思想深度与追根究底的直觉力，喜好独立深思，探求本质。")
            8 -> LifePathInfo(8, "实务掌舵者", "意志 · 格局 · 商业魄力", "对资源整合与目标达成具有卓越直觉，敢作敢当，具备强大领导魄力。")
            9 -> LifePathInfo(9, "博爱远见者", "包容 · 人道 · 大格局", "心怀广阔世界与深沉同理心，不斤斤计较，追求精神升华与利他价值。")
            11 -> LifePathInfo(11, "灵性先知 (大师数)", "超感直觉 · 启蒙 · 灵性", "拥有超越常人的敏锐直觉与精神天赋，注定成为他人的引路明灯。")
            22 -> LifePathInfo(22, "大厦筑造师 (大师数)", "宏图实干 · 远见 · 铸造", "兼具 11 的宏大梦想与 4 的钢铁执行力，拥有化梦想为现实的伟力。")
            else -> LifePathInfo(33, "大爱宗师 (大师数)", "纯粹利他 · 启迪 · 救赎", "生命灵数的最高境界，以无私的关爱与智慧启迪温暖周围的人。")
        }
    }

    /**
     * 东方哲学：根据出生日期计算日柱干支（日主元神）
     * 采用现代标准公历转儒略日算法（Julian Day Number），精准到每一天的日柱
     */
    fun calculateDayMaster(birthDate: LocalDate): DayMasterInfo {
        // 计算儒略日编号 JDN
        val a = (14 - birthDate.monthValue) / 12
        val y = birthDate.year + 4800 - a
        val m = birthDate.monthValue + 12 * a - 3
        val jdn = birthDate.dayOfMonth + (153 * m + 2) / 5 + 365 * y + y / 4 - y / 100 + y / 400 - 32045

        // 日柱干支计算公式：(JDN + 49) % 60
        val dayIndex = ((jdn + 49) % 60 + 60) % 60

        val stems = arrayOf("甲", "乙", "丙", "丁", "戊", "己", "庚", "辛", "壬", "癸")
        val branches = arrayOf("子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥")

        val stem = stems[(dayIndex % 10)]
        val branch = branches[(dayIndex % 12)]

        val (element, symbol, desc) = when (stem) {
            "甲" -> Triple("阳木", "🌲 参天古木", "刚直不阿、仁德博爱、向上生发，具备顶天立地的君子风骨。")
            "乙" -> Triple("阴木", "🌿 兰草藤萝", "温和柔韧、善于借势、生命力极度顽强，外圆内方富有艺术灵性。")
            "丙" -> Triple("阳火", "☀️ 太阳烈焰", "热情开朗、光明坦荡、慷慨豪迈，如同普照大地的骄阳充满感染力。")
            "丁" -> Triple("阴火", "🕯️ 幽夜星火", "思想敏锐、深邃温柔、富有奉献精神，外表文静内心充满炽热力量。")
            "戊" -> Triple("阳土", "⛰️ 崇山厚岳", "厚重包容、沉稳坚定、讲求信义，如同泰山般稳健让人深感安心。")
            "己" -> Triple("阴土", "🌾 田园沃土", "宽厚包容、兼收并蓄、善于蓄积与涵养万物，极具奉献与协调智慧。")
            "庚" -> Triple("阳金", "⚔️ 霜刃刀剑", "刚健果决、义气深厚、棱角分明，讲求效率不拖泥带水，敢为人先。")
            "辛" -> Triple("阴金", "💎 金玉珠贝", "细腻敏感、自尊心强、追求完美与品味，才华内敛温润有光。")
            "壬" -> Triple("阳水", "🌊 江河奔流", "智谋深远、开阔大气、奔流不息，善于适应变化，具有包容天下的胸襟。")
            else -> Triple("阴水", "🌧️ 润物甘霖", "心思缜密、柔情似水、善解人意，具有潜移默化影响周围人的无声智慧。")
        }

        return DayMasterInfo(
            heavenlyStem = stem,
            earthlyBranch = branch,
            element = element,
            natureSymbol = symbol,
            characteristic = desc
        )
    }
}
