package app.biorhythms.core.model

import java.time.LocalDate

/**
 * 节律便签/日记数据模型
 */
data class BioNote(
    val id: Long = System.currentTimeMillis(),
    val profileId: Long,
    val date: LocalDate,
    val content: String,
    val physicalRating: Int = 3,  // 1-5 星
    val emotionalRating: Int = 3, // 1-5 星
    val intellectualRating: Int = 3, // 1-5 星
    val compatScore: Float = 0f, // 与理论节律的契合度百分比
    val createdTime: Long = System.currentTimeMillis()
)
