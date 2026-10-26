package app.biorhythms.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.biorhythms.core.algorithm.BioCalculator
import app.biorhythms.core.algorithm.DayRhythmStatus
import app.biorhythms.core.algorithm.OverallEvaluation
import app.biorhythms.core.algorithm.RhythmType
import app.biorhythms.core.model.BioNote
import app.biorhythms.core.model.Profile
import app.biorhythms.data.DataRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

data class MainUiState(
    val profiles: List<Profile> = emptyList(),
    val selectedProfile: Profile? = null,
    val selectedDate: LocalDate = LocalDate.now(),
    val dayRhythms: List<DayRhythmStatus> = emptyList(),
    val overall: OverallEvaluation? = null,
    val livedDays: Long = 0,
    val noteDates: Set<LocalDate> = emptySet(),
    val todayNote: BioNote? = null,
    // 双人契合度分析人物
    val compatProfile1: Profile? = null,
    val compatProfile2: Profile? = null,
    val expectedLifeAge: Int = 85,
    val notificationsEnabled: Boolean = true
)

private data class RepoData(
    val profiles: List<Profile>,
    val selected: Profile?,
    val notes: List<BioNote>,
    val expectedAge: Int,
    val notificationsEnabled: Boolean
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DataRepository.getInstance(application)

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    private val _compatProfile1 = MutableStateFlow<Profile?>(null)
    private val _compatProfile2 = MutableStateFlow<Profile?>(null)

    private val repoFlow = combine(
        repository.profiles,
        repository.selectedProfile,
        repository.notes,
        repository.expectedLifeAge,
        repository.notificationsEnabled
    ) { profiles, selected, notes, expectedAge, notifEnabled ->
        RepoData(profiles, selected, notes, expectedAge, notifEnabled)
    }

    val uiState: StateFlow<MainUiState> = combine(
        repoFlow,
        _selectedDate,
        _compatProfile1,
        _compatProfile2
    ) { repoData, date, c1, c2 ->
        val (profiles, currentProfile, notes, expectedAge, notifEnabled) = repoData
        val activeProfile = currentProfile ?: profiles.firstOrNull()
        val days = if (activeProfile != null) {
            BioCalculator.getDiffDays(activeProfile.birthDate, date)
        } else {
            0L
        }

        val rhythms = if (activeProfile != null) {
            BioCalculator.getDayRhythms(days)
        } else {
            emptyList()
        }

        val overall = if (activeProfile != null) {
            BioCalculator.getOverallEvaluation(days)
        } else null

        val noteDateSet = notes.filter { it.profileId == activeProfile?.id }.map { it.date }.toSet()
        val currentNote = notes.find { it.profileId == activeProfile?.id && it.date == date }

        // 默认双人比对档案设置
        val p1 = c1 ?: activeProfile ?: profiles.getOrNull(0)
        val p2 = c2 ?: profiles.find { it.id != p1?.id } ?: profiles.getOrNull(1)

        MainUiState(
            profiles = profiles,
            selectedProfile = activeProfile,
            selectedDate = date,
            dayRhythms = rhythms,
            overall = overall,
            livedDays = days,
            noteDates = noteDateSet,
            todayNote = currentNote,
            compatProfile1 = p1,
            compatProfile2 = p2,
            expectedLifeAge = expectedAge,
            notificationsEnabled = notifEnabled
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MainUiState())

    fun updateExpectedLifeAge(age: Int) {
        repository.updateExpectedLifeAge(age)
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        repository.setNotificationsEnabled(enabled)
    }

    fun exportBackup(): String = repository.exportBackupJson()
    fun importBackup(json: String): Boolean = repository.importBackupJson(json)

    fun selectProfile(profileId: Long) {
        repository.selectProfile(profileId)
    }

    fun addProfile(name: String, birthDate: LocalDate) {
        repository.addProfile(name, birthDate)
    }

    fun updateProfile(id: Long, name: String, birthDate: LocalDate) {
        repository.updateProfile(id, name, birthDate)
    }

    fun deleteProfile(id: Long) {
        repository.deleteProfile(id)
    }

    fun setDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun changeDays(delta: Long) {
        _selectedDate.value = _selectedDate.value.plusDays(delta)
    }

    fun resetToToday() {
        _selectedDate.value = LocalDate.now()
    }

    fun setCompatProfiles(p1: Profile?, p2: Profile?) {
        _compatProfile1.value = p1
        _compatProfile2.value = p2
    }

    fun saveNote(content: String, pRating: Int, eRating: Int, iRating: Int) {
        val profile = uiState.value.selectedProfile ?: return
        val date = uiState.value.selectedDate
        val days = BioCalculator.getDiffDays(profile.birthDate, date).toFloat()
        
        // 计算与算法理论的加权吻合度
        val cp = BioCalculator.getNoteCompat(RhythmType.PHYSIC, days, pRating.toFloat())
        val ce = BioCalculator.getNoteCompat(RhythmType.EMOTIONAL, days, eRating.toFloat())
        val ci = BioCalculator.getNoteCompat(RhythmType.INTELLECT, days, iRating.toFloat())
        val avgCompat = (cp + ce + ci) / 3f

        val existing = uiState.value.todayNote
        val note = existing?.copy(
            content = content,
            physicalRating = pRating,
            emotionalRating = eRating,
            intellectualRating = iRating,
            compatScore = avgCompat
        ) ?: BioNote(
            profileId = profile.id,
            date = date,
            content = content,
            physicalRating = pRating,
            emotionalRating = eRating,
            intellectualRating = iRating,
            compatScore = avgCompat
        )
        repository.saveNote(note)
    }

    fun deleteCurrentNote() {
        val note = uiState.value.todayNote ?: return
        repository.deleteNote(note.id)
    }
}
