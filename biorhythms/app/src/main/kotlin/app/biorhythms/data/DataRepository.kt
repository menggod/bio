package app.biorhythms.data

import android.content.Context
import android.content.SharedPreferences
import app.biorhythms.core.model.BioNote
import app.biorhythms.core.model.Profile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

class DataRepository private constructor(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("biorhythms_data_store", Context.MODE_PRIVATE)

    private val _profiles = MutableStateFlow<List<Profile>>(emptyList())
    val profiles: StateFlow<List<Profile>> = _profiles.asStateFlow()

    private val _selectedProfile = MutableStateFlow<Profile?>(null)
    val selectedProfile: StateFlow<Profile?> = _selectedProfile.asStateFlow()

    private val _notes = MutableStateFlow<List<BioNote>>(emptyList())
    val notes: StateFlow<List<BioNote>> = _notes.asStateFlow()

    private val _expectedLifeAge = MutableStateFlow(85)
    val expectedLifeAge: StateFlow<Int> = _expectedLifeAge.asStateFlow()

    private val _notificationsEnabled = MutableStateFlow(true)
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        val profilesJson = prefs.getString(KEY_PROFILES, null)
        val loadedProfiles = if (!profilesJson.isNullOrEmpty()) {
            parseProfiles(profilesJson)
        } else {
            // 首次安装预置默认档案
            val defaultList = listOf(
                Profile(id = 1, name = "我 (自己)", birthDate = LocalDate.of(1996, 5, 20), isDefault = true),
                Profile(id = 2, name = "搭档 / 朋友", birthDate = LocalDate.of(1997, 8, 15), isDefault = false)
            )
            saveProfiles(defaultList)
            defaultList
        }
        _profiles.value = loadedProfiles
        _selectedProfile.value = loadedProfiles.firstOrNull { it.isDefault } ?: loadedProfiles.firstOrNull()

        val notesJson = prefs.getString(KEY_NOTES, null)
        val loadedNotes = if (!notesJson.isNullOrEmpty()) {
            parseNotes(notesJson)
        } else {
            emptyList()
        }
        _notes.value = loadedNotes

        _expectedLifeAge.value = prefs.getInt(KEY_EXPECTED_LIFE_AGE, 85)
        _notificationsEnabled.value = prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        _notificationsEnabled.value = enabled
        prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply()
    }

    fun updateExpectedLifeAge(age: Int) {
        val clamped = age.coerceIn(50, 150)
        _expectedLifeAge.value = clamped
        prefs.edit().putInt(KEY_EXPECTED_LIFE_AGE, clamped).apply()
    }

    fun selectProfile(profileId: Long) {
        val updated = _profiles.value.map {
            it.copy(isDefault = it.id == profileId)
        }
        _profiles.value = updated
        _selectedProfile.value = updated.find { it.id == profileId }
        saveProfiles(updated)
    }

    fun addProfile(name: String, birthDate: LocalDate): Profile {
        val newProfile = Profile(
            id = System.currentTimeMillis(),
            name = name,
            birthDate = birthDate,
            isDefault = _profiles.value.isEmpty()
        )
        val updated = _profiles.value + newProfile
        _profiles.value = updated
        if (newProfile.isDefault || _selectedProfile.value == null) {
            _selectedProfile.value = newProfile
        }
        saveProfiles(updated)
        return newProfile
    }

    fun updateProfile(id: Long, name: String, birthDate: LocalDate) {
        val updated = _profiles.value.map {
            if (it.id == id) it.copy(name = name, birthDate = birthDate) else it
        }
        _profiles.value = updated
        if (_selectedProfile.value?.id == id) {
            _selectedProfile.value = updated.find { it.id == id }
        }
        saveProfiles(updated)
    }

    fun deleteProfile(id: Long) {
        val updated = _profiles.value.filter { it.id != id }
        _profiles.value = updated
        if (_selectedProfile.value?.id == id) {
            _selectedProfile.value = updated.firstOrNull()
        }
        saveProfiles(updated)
        // 级联删除对应的便签
        val updatedNotes = _notes.value.filter { it.profileId != id }
        _notes.value = updatedNotes
        saveNotes(updatedNotes)
    }

    fun saveNote(note: BioNote) {
        val existing = _notes.value.indexOfFirst { it.id == note.id }
        val updated = if (existing >= 0) {
            _notes.value.toMutableList().apply { set(existing, note) }
        } else {
            _notes.value + note
        }
        _notes.value = updated
        saveNotes(updated)
    }

    fun deleteNote(noteId: Long) {
        val updated = _notes.value.filter { it.id != noteId }
        _notes.value = updated
        saveNotes(updated)
    }

    fun exportBackupJson(): String {
        val root = JSONObject()
        root.put("version", 1)
        root.put("appName", "BioRhythms")
        root.put("exportTime", System.currentTimeMillis())
        root.put("expectedLifeAge", _expectedLifeAge.value)

        val profilesArray = JSONArray()
        _profiles.value.forEach { p ->
            profilesArray.put(JSONObject().apply {
                put("id", p.id)
                put("name", p.name)
                put("birthDate", p.birthDate.toString())
                put("isDefault", p.isDefault)
                put("createdTime", p.createdTime)
            })
        }
        root.put("profiles", profilesArray)

        val notesArray = JSONArray()
        _notes.value.forEach { n ->
            notesArray.put(JSONObject().apply {
                put("id", n.id)
                put("profileId", n.profileId)
                put("date", n.date.toString())
                put("content", n.content)
                put("physicalRating", n.physicalRating)
                put("emotionalRating", n.emotionalRating)
                put("intellectualRating", n.intellectualRating)
                put("compatScore", n.compatScore.toDouble())
                put("createdTime", n.createdTime)
            })
        }
        root.put("notes", notesArray)

        return root.toString(2)
    }

    fun importBackupJson(jsonString: String): Boolean {
        return try {
            val root = JSONObject(jsonString)
            if (root.has("expectedLifeAge")) {
                updateExpectedLifeAge(root.getInt("expectedLifeAge"))
            }
            if (root.has("profiles")) {
                val parsedProfiles = parseProfiles(root.getJSONArray("profiles").toString())
                if (parsedProfiles.isNotEmpty()) {
                    _profiles.value = parsedProfiles
                    _selectedProfile.value = parsedProfiles.firstOrNull { it.isDefault } ?: parsedProfiles.firstOrNull()
                    saveProfiles(parsedProfiles)
                }
            }
            if (root.has("notes")) {
                val parsedNotes = parseNotes(root.getJSONArray("notes").toString())
                _notes.value = parsedNotes
                saveNotes(parsedNotes)
            }
            true
        } catch (e: Throwable) {
            e.printStackTrace()
            false
        }
    }

    private fun saveProfiles(list: List<Profile>) {
        val array = JSONArray()
        list.forEach { p ->
            val obj = JSONObject().apply {
                put("id", p.id)
                put("name", p.name)
                put("birthDate", p.birthDate.toString())
                put("isDefault", p.isDefault)
                put("createdTime", p.createdTime)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_PROFILES, array.toString()).apply()
        app.biorhythms.core.widget.BioWidgetProvider.updateAllWidgets(context)
    }

    private fun parseProfiles(json: String): List<Profile> {
        val result = mutableListOf<Profile>()
        val array = JSONArray(json)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            result.add(
                Profile(
                    id = obj.optLong("id", System.currentTimeMillis()),
                    name = obj.getString("name"),
                    birthDate = LocalDate.parse(obj.getString("birthDate")),
                    isDefault = obj.optBoolean("isDefault", false),
                    createdTime = obj.optLong("createdTime", System.currentTimeMillis())
                )
            )
        }
        return result
    }

    private fun saveNotes(list: List<BioNote>) {
        val array = JSONArray()
        list.forEach { n ->
            val obj = JSONObject().apply {
                put("id", n.id)
                put("profileId", n.profileId)
                put("date", n.date.toString())
                put("content", n.content)
                put("physicalRating", n.physicalRating)
                put("emotionalRating", n.emotionalRating)
                put("intellectualRating", n.intellectualRating)
                put("compatScore", n.compatScore.toDouble())
                put("createdTime", n.createdTime)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_NOTES, array.toString()).apply()
        app.biorhythms.core.widget.BioWidgetProvider.updateAllWidgets(context)
    }

    private fun parseNotes(json: String): List<BioNote> {
        val result = mutableListOf<BioNote>()
        val array = JSONArray(json)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            result.add(
                BioNote(
                    id = obj.optLong("id", System.currentTimeMillis()),
                    profileId = obj.getLong("profileId"),
                    date = LocalDate.parse(obj.getString("date")),
                    content = obj.getString("content"),
                    physicalRating = obj.optInt("physicalRating", 3),
                    emotionalRating = obj.optInt("emotionalRating", 3),
                    intellectualRating = obj.optInt("intellectualRating", 3),
                    compatScore = obj.optDouble("compatScore", 0.0).toFloat(),
                    createdTime = obj.optLong("createdTime", System.currentTimeMillis())
                )
            )
        }
        return result
    }

    companion object {
        private const val KEY_PROFILES = "profiles_list"
        private const val KEY_NOTES = "notes_list"
        private const val KEY_EXPECTED_LIFE_AGE = "key_expected_life_age"
        private const val KEY_NOTIFICATIONS_ENABLED = "key_notifications_enabled"

        @Volatile
        private var instance: DataRepository? = null

        fun getInstance(context: Context): DataRepository {
            return instance ?: synchronized(this) {
                instance ?: DataRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
