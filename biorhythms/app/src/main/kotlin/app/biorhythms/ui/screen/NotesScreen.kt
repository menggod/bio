package app.biorhythms.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.biorhythms.ui.components.ColorEmotional
import app.biorhythms.ui.components.ColorIntellect
import app.biorhythms.ui.components.ColorPhysical
import app.biorhythms.ui.viewmodel.MainUiState
import app.biorhythms.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    viewModel: MainViewModel,
    uiState: MainUiState,
    modifier: Modifier = Modifier
) {
    val currentProfile = uiState.selectedProfile
    val currentDate = uiState.selectedDate
    val todayNote = uiState.todayNote

    var noteText by remember(todayNote) { mutableStateOf(todayNote?.content ?: "") }
    var physicalRating by remember(todayNote) { mutableIntStateOf(todayNote?.physicalRating ?: 3) }
    var emotionalRating by remember(todayNote) { mutableIntStateOf(todayNote?.emotionalRating ?: 3) }
    var intellectualRating by remember(todayNote) { mutableIntStateOf(todayNote?.intellectualRating ?: 3) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. 顶部日期与人物标头
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "节律日记与自我验证",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${currentProfile?.name ?: ""} · $currentDate",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (todayNote != null) {
                IconButton(onClick = { viewModel.deleteCurrentNote() }) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "删除今日便签", tint = MaterialTheme.colorScheme.error)
                }
            }
        }

        // 2. 日记编辑与打分卡片
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
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("记录今天的心情、精力或重要事件") },
                    placeholder = { Text("比如：今天精神饱满、思路清晰，或者感觉容易疲倦...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                )

                Text(
                    text = "自我真实体感打分 (1~5星)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // 3个维度的星级打分条
                StarRatingRow(
                    label = "体力感受",
                    rating = physicalRating,
                    color = ColorPhysical,
                    onRatingChanged = { physicalRating = it }
                )

                StarRatingRow(
                    label = "情绪感受",
                    rating = emotionalRating,
                    color = ColorEmotional,
                    onRatingChanged = { emotionalRating = it }
                )

                StarRatingRow(
                    label = "思维智力",
                    rating = intellectualRating,
                    color = ColorIntellect,
                    onRatingChanged = { intellectualRating = it }
                )

                if (todayNote != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("理论节律吻合度得分", style = MaterialTheme.typography.bodySmall)
                            Text(
                                text = "${String.format("%.1f", todayNote.compatScore)}%",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        viewModel.saveNote(noteText, physicalRating, emotionalRating, intellectualRating)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (todayNote != null) "更新日记打分" else "保存日记便签")
                }
            }
        }
    }
}

@Composable
private fun StarRatingRow(
    label: String,
    rating: Int,
    color: Color,
    onRatingChanged: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            for (i in 1..5) {
                Icon(
                    imageVector = if (i <= rating) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = "$i 星",
                    tint = if (i <= rating) color else MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { onRatingChanged(i) }
                )
            }
        }
    }
}
