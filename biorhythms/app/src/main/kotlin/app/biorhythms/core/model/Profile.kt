package app.biorhythms.core.model

import java.time.LocalDate

/**
 * 个人档案数据模型
 */
data class Profile(
    val id: Long = System.currentTimeMillis(),
    val name: String,
    val birthDate: LocalDate,
    val isDefault: Boolean = false,
    val createdTime: Long = System.currentTimeMillis()
)
