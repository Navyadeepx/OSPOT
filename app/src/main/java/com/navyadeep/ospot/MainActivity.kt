package com.navyadeep.ospot

import android.content.Context
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.DpOffset
import com.navyadeep.ospot.ui.theme.OSPOTTheme
import com.navyadeep.ospot.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollFactory
import io.github.fletchmckee.liquid.liquid
import io.github.fletchmckee.liquid.rememberLiquidState
import io.github.fletchmckee.liquid.liquefiable
import io.github.fletchmckee.liquid.LiquidState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

val Context.dataStore by preferencesDataStore(name = "ospot_settings")

@Serializable
enum class SetType { WARMUP, PRIMER, WORKING }

@Serializable
data class WorkoutSet(
    val type: SetType,
    val weight: String = "-",
    val reps: String = "-",
    val rir: String = "-",
    val rpe: String = "-"
)

@Serializable
data class Exercise(val name: String, val note: String = "", val sets: List<WorkoutSet> = emptyList())

@Serializable
data class DayProgress(
    val weight: Double,
    val reps: Int
)

@Serializable
data class WorkoutBackup(
    val sessions: List<String>,
    val sessionExercises: Map<String, List<Exercise>>,
    val showRpe: Boolean,
    val showRir: Boolean,
    val expandOnStartup: Boolean = false,
    val weightIncrement: String,
    val accentColor: Long = 0xFFFFFFFFL,
    val progressData: Map<String, Map<String, DayProgress>> = emptyMap(),
    val lastProcessDate: String = ""
)

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        val configuration = Configuration(newBase.resources.configuration)
        configuration.densityDpi = 600
        val context = newBase.createConfigurationContext(configuration)
        super.attachBaseContext(context)
    }

    private val SHOW_RPE_KEY = booleanPreferencesKey("show_rpe")
    private val SHOW_RIR_KEY = booleanPreferencesKey("show_rir")
    private val INCREMENT_KEY = stringPreferencesKey("weight_increment")
    private val ACCENT_COLOR_KEY = longPreferencesKey("accent_color")
    private val SESSIONS_KEY = stringPreferencesKey("workout_sessions")
    private val EXERCISES_KEY = stringPreferencesKey("session_exercises")
    private val PROGRESS_DATA_KEY = stringPreferencesKey("progress_data")
    private val LAST_PROCESS_DATE_KEY = stringPreferencesKey("last_process_date")
    private val EXPAND_ON_STARTUP_KEY = booleanPreferencesKey("expand_on_startup")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val initialPrefs = runBlocking { dataStore.data.first() }
        val initialSessionsJson = initialPrefs[SESSIONS_KEY] ?: ""
        val initialSessions = if (initialSessionsJson.isNotEmpty()) {
            try {
                Json.decodeFromString<List<String>>(initialSessionsJson)
            } catch (e: Exception) {
                emptyList()
            }
        } else emptyList()
        val initialSelectedSession = initialSessions.firstOrNull() ?: ""

        val initialExercisesJson = initialPrefs[EXERCISES_KEY] ?: ""
        val initialExercisesMap = if (initialExercisesJson.isNotEmpty()) {
            try {
                Json.decodeFromString<Map<String, List<Exercise>>>(initialExercisesJson)
            } catch (e: Exception) {
                emptyMap()
            }
        } else emptyMap()

        val initialAccentColor = initialPrefs[ACCENT_COLOR_KEY] ?: 0xFFFFFFFFL

        setContent {
            val scope = rememberCoroutineScope()
            val barLiquidState = rememberLiquidState()
            val cardLiquidState = rememberLiquidState()

            val exportLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.CreateDocument("application/json")
            ) { uri ->
                uri?.let {
                    scope.launch {
                        try {
                            val prefs = dataStore.data.first()
                            val sessionsJson = prefs[SESSIONS_KEY] ?: "[]"
                            val exercisesJson = prefs[EXERCISES_KEY] ?: "{}"
                            
                            val backup = WorkoutBackup(
                                sessions = Json.decodeFromString(sessionsJson),
                                sessionExercises = Json.decodeFromString(exercisesJson),
                                showRpe = prefs[SHOW_RPE_KEY] ?: false,
                                showRir = prefs[SHOW_RIR_KEY] ?: false,
                                expandOnStartup = prefs[EXPAND_ON_STARTUP_KEY] ?: false,
                                weightIncrement = prefs[INCREMENT_KEY] ?: "2.5",
                                accentColor = prefs[ACCENT_COLOR_KEY] ?: 0xFFFFFFFFL,
                                progressData = Json.decodeFromString(prefs[PROGRESS_DATA_KEY] ?: "{}"),
                                lastProcessDate = prefs[LAST_PROCESS_DATE_KEY] ?: ""
                            )
                            
                            val backupJson = Json.encodeToString(backup)
                            contentResolver.openOutputStream(it)?.use { outputStream ->
                                outputStream.write(backupJson.toByteArray())
                            }
                            Toast.makeText(this@MainActivity, "Data exported successfully", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(this@MainActivity, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }

            var currentScreen by remember { mutableStateOf("workout_log") }
            var showRpe by remember { mutableStateOf(initialPrefs[SHOW_RPE_KEY] ?: false) }
            var showRir by remember { mutableStateOf(initialPrefs[SHOW_RIR_KEY] ?: false) }
            var expandOnStartup by remember { mutableStateOf(initialPrefs[EXPAND_ON_STARTUP_KEY] ?: false) }
            var weightIncrement by remember { mutableStateOf(initialPrefs[INCREMENT_KEY] ?: "2.5") }
            val sessions = remember { mutableStateListOf<String>().apply { addAll(initialSessions) } }
            var selectedSession by remember { mutableStateOf(initialSelectedSession) }
            var menuExpanded by remember { mutableStateOf(false) }
            var isEditMode by remember { mutableStateOf(false) }

            var showAddSessionDialog by remember { mutableStateOf(false) }
            var newSessionNameText by remember { mutableStateOf("") }
            var showAddExerciseDialog by remember { mutableStateOf(false) }
            var newExerciseNameText by remember { mutableStateOf("") }
            var accentColor by remember { mutableStateOf(Color(initialAccentColor.toInt())) }
            val sessionExercises = remember {
                mutableStateMapOf<String, androidx.compose.runtime.snapshots.SnapshotStateList<Exercise>>().apply {
                    initialExercisesMap.forEach { (key, value) ->
                        put(key, mutableStateListOf<Exercise>().apply { addAll(value) })
                    }
                }
            }

            val importLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocument()
            ) { uri ->
                uri?.let {
                    scope.launch {
                        try {
                            contentResolver.openInputStream(it)?.use { inputStream ->
                                val backupJson = inputStream.bufferedReader().readText()
                                val backup = Json.decodeFromString<WorkoutBackup>(backupJson)
                                
                                dataStore.edit { prefs ->
                                    prefs[SESSIONS_KEY] = Json.encodeToString(backup.sessions)
                                    prefs[EXERCISES_KEY] = Json.encodeToString(backup.sessionExercises)
                                    prefs[SHOW_RPE_KEY] = backup.showRpe
                                    prefs[SHOW_RIR_KEY] = backup.showRir
                                    prefs[EXPAND_ON_STARTUP_KEY] = backup.expandOnStartup
                                    prefs[INCREMENT_KEY] = backup.weightIncrement
                                    prefs[ACCENT_COLOR_KEY] = backup.accentColor
                                    prefs[PROGRESS_DATA_KEY] = Json.encodeToString(backup.progressData)
                                    prefs[LAST_PROCESS_DATE_KEY] = backup.lastProcessDate
                                }

                                // Update local state
                                sessions.clear()
                                sessions.addAll(backup.sessions)
                                sessionExercises.clear()
                                backup.sessionExercises.forEach { (key, value) ->
                                    sessionExercises[key] = mutableStateListOf<Exercise>().apply { addAll(value) }
                                }
                                showRpe = backup.showRpe
                                showRir = backup.showRir
                                expandOnStartup = backup.expandOnStartup
                                weightIncrement = backup.weightIncrement
                                accentColor = Color(backup.accentColor.toInt())
                                if (sessions.isNotEmpty()) selectedSession = sessions.first()
                                
                                Toast.makeText(this@MainActivity, "Data imported successfully", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(this@MainActivity, "Import failed: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }

            LaunchedEffect(Unit) {
                // Background update just in case, though initial load covers it
                val prefs = dataStore.data.first()
                showRpe = prefs[SHOW_RPE_KEY] ?: false
                showRir = prefs[SHOW_RIR_KEY] ?: false
                expandOnStartup = prefs[EXPAND_ON_STARTUP_KEY] ?: false
                weightIncrement = prefs[INCREMENT_KEY] ?: "2.5"
                accentColor = Color((prefs[ACCENT_COLOR_KEY] ?: 0xFFFFFFFFL).toInt())

                val savedSessionsJson = prefs[SESSIONS_KEY] ?: ""
                if (savedSessionsJson.isNotEmpty()) {
                    try {
                        val decodedList = Json.decodeFromString<List<String>>(savedSessionsJson)
                        if (sessions.toList() != decodedList) {
                            sessions.clear()
                            sessions.addAll(decodedList)
                            if (selectedSession.isEmpty() && sessions.isNotEmpty()) {
                                selectedSession = sessions.first()
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                val savedExercisesJson = prefs[EXERCISES_KEY] ?: ""
                if (savedExercisesJson.isNotEmpty()) {
                    try {
                        val decodedMap = Json.decodeFromString<Map<String, List<Exercise>>>(savedExercisesJson)
                        decodedMap.forEach { (key, value) ->
                            val currentList = sessionExercises[key]
                            if (currentList == null || currentList.toList() != value) {
                                sessionExercises[key] = mutableStateListOf<Exercise>().apply { addAll(value) }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                // Auto-save logic for progress chart
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val today = dateFormat.format(Date())
                val lastProcessDate = prefs[LAST_PROCESS_DATE_KEY] ?: ""

                if (lastProcessDate.isNotEmpty() && lastProcessDate != today) {
                    val progressJson = prefs[PROGRESS_DATA_KEY] ?: "{}"
                    val progressMap = try {
                        Json.decodeFromString<MutableMap<String, MutableMap<String, DayProgress>>>(progressJson)
                    } catch (e: Exception) {
                        mutableMapOf()
                    }

                    val exercisesMap = if (savedExercisesJson.isNotEmpty()) {
                        try {
                            Json.decodeFromString<Map<String, List<Exercise>>>(savedExercisesJson)
                        } catch (e: Exception) {
                            emptyMap()
                        }
                    } else emptyMap()

                    // Calculate current maxes
                    val currentMaxes = mutableMapOf<String, DayProgress>()
                    exercisesMap.values.flatten().forEach { exercise ->
                        var maxWeight = 0.0
                        var maxReps = 0
                        exercise.sets.forEach { set ->
                            val w = set.weight.toDoubleOrNull() ?: 0.0
                            val r = set.reps.toIntOrNull() ?: 0
                            if (w > maxWeight) {
                                maxWeight = w
                                maxReps = r
                            } else if (w == maxWeight && r > maxReps) {
                                maxReps = r
                            }
                        }
                        if (maxWeight > 0 || maxReps > 0) {
                            val existing = currentMaxes[exercise.name]
                            if (existing == null || maxWeight > existing.weight || (maxWeight == existing.weight && maxReps > existing.reps)) {
                                currentMaxes[exercise.name] = DayProgress(maxWeight, maxReps)
                            }
                        }
                    }

                    // Fill days from lastProcessDate to yesterday (inclusive)
                    val calendar = java.util.Calendar.getInstance()
                    try {
                        val lastDate = dateFormat.parse(lastProcessDate)
                        val todayDate = dateFormat.parse(today)
                        if (lastDate != null && todayDate != null) {
                            calendar.time = lastDate
                            // We start from lastProcessDate and fill up to yesterday
                            // If user skipped days, those days will get the currentMaxes
                            while (calendar.time.before(todayDate)) {
                                val dateStr = dateFormat.format(calendar.time)
                                currentMaxes.forEach { (exerciseName, progress) ->
                                    val exerciseData = progressMap.getOrPut(exerciseName) { mutableMapOf() }
                                    exerciseData[dateStr] = progress
                                }
                                calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    dataStore.edit {
                        it[PROGRESS_DATA_KEY] = Json.encodeToString(progressMap)
                        it[LAST_PROCESS_DATE_KEY] = today
                    }
                } else if (lastProcessDate.isEmpty()) {
                    // First time initialization
                    dataStore.edit { it[LAST_PROCESS_DATE_KEY] = today }
                }
            }

            val saveRpe: (Boolean) -> Unit = { value ->
                showRpe = value
                scope.launch { dataStore.edit { it[SHOW_RPE_KEY] = value } }
            }

            val saveRir: (Boolean) -> Unit = { value ->
                showRir = value
                scope.launch { dataStore.edit { it[SHOW_RIR_KEY] = value } }
            }

            val saveExpandOnStartup: (Boolean) -> Unit = { value ->
                expandOnStartup = value
                scope.launch { dataStore.edit { it[EXPAND_ON_STARTUP_KEY] = value } }
            }

            val saveIncrement: (String) -> Unit = { value ->
                weightIncrement = value
                scope.launch { dataStore.edit { it[INCREMENT_KEY] = value } }
            }

            val saveAccentColor: (Color) -> Unit = { color ->
                accentColor = color
                // Use toArgb() to store a standard 32-bit color value, avoiding ColorSpace issues with packed ULong
                scope.launch { dataStore.edit { it[ACCENT_COLOR_KEY] = color.toArgb().toLong() } }
            }

            val saveSessions: () -> Unit = {
                scope.launch {
                    val jsonString = Json.encodeToString(sessions.toList())
                    dataStore.edit { it[SESSIONS_KEY] = jsonString }
                }
            }

            val saveExercises: () -> Unit = {
                scope.launch {
                    val mapToSave = sessionExercises.mapValues { it.value.toList() }
                    val jsonString = Json.encodeToString(mapToSave)
                    dataStore.edit { it[EXERCISES_KEY] = jsonString }
                }
            }

            @OptIn(ExperimentalFoundationApi::class)
            OSPOTTheme {
                CompositionLocalProvider(
                    LocalOverscrollFactory provides null
                ) {
                    WorkoutAppScreen(
                        currentScreen = currentScreen,
                        onScreenChange = { currentScreen = it },
                        showRpe = showRpe,
                        onRpeChange = saveRpe,
                        showRir = showRir,
                        onRirChange = saveRir,
                        expandOnStartup = expandOnStartup,
                        onExpandOnStartupChange = saveExpandOnStartup,
                        weightIncrement = weightIncrement,
                        onIncrementChange = saveIncrement,
                        sessions = sessions,
                        onSaveSessions = saveSessions,
                        selectedSession = selectedSession,
                        onSelectedSessionChange = { selectedSession = it },
                        menuExpanded = menuExpanded,
                        onMenuToggle = { menuExpanded = it },
                        showAddSessionDialog = showAddSessionDialog,
                        onShowDialogChange = { showAddSessionDialog = it },
                        newSessionNameText = newSessionNameText,
                        onDialogTextChange = { newSessionNameText = it },
                        showAddExerciseDialog = showAddExerciseDialog,
                        onShowExerciseDialogChange = { showAddExerciseDialog = it },
                        newExerciseNameText = newExerciseNameText,
                        onExerciseDialogTextChange = { newExerciseNameText = it },
                        exercises = sessionExercises[selectedSession] ?: emptyList(),
                        onAddExercise = { exerciseName ->
                            if (selectedSession.isNotEmpty()) {
                                val list = sessionExercises.getOrPut(selectedSession) { mutableStateListOf() }
                                list.add(Exercise(name = exerciseName))
                                saveExercises()
                            }
                        },
                        onAddSet = { exerciseIndex, setType ->
                            if (selectedSession.isNotEmpty()) {
                                val list = sessionExercises[selectedSession]
                                if (list != null && exerciseIndex in list.indices) {
                                    val exercise = list[exerciseIndex]
                                    val updatedSets = exercise.sets + WorkoutSet(type = setType)
                                    list[exerciseIndex] = exercise.copy(sets = updatedSets)
                                    saveExercises()
                                }
                            }
                        },
                        onUpdateSet = { exerciseIndex, setIndex, weight, reps, rir, rpe ->
                            if (selectedSession.isNotEmpty()) {
                                val list = sessionExercises[selectedSession]
                                if (list != null && exerciseIndex in list.indices) {
                                    val exercise = list[exerciseIndex]
                                    if (setIndex in exercise.sets.indices) {
                                        val updatedSets = exercise.sets.toMutableList()
                                        val oldSet = updatedSets[setIndex]
                                        updatedSets[setIndex] = oldSet.copy(
                                            weight = weight ?: oldSet.weight,
                                            reps = reps ?: oldSet.reps,
                                            rir = rir ?: oldSet.rir,
                                            rpe = rpe ?: oldSet.rpe
                                        )
                                        list[exerciseIndex] = exercise.copy(sets = updatedSets)
                                        saveExercises()
                                    }
                                }
                            }
                        },
                        onRemoveExercise = { index ->
                            if (selectedSession.isNotEmpty()) {
                                sessionExercises[selectedSession]?.removeAt(index)
                                saveExercises()
                            }
                        },
                        onRemoveSet = { exerciseIndex, setIndex ->
                            if (selectedSession.isNotEmpty()) {
                                val list = sessionExercises[selectedSession]
                                if (list != null && exerciseIndex in list.indices) {
                                    val exercise = list[exerciseIndex]
                                    if (setIndex in exercise.sets.indices) {
                                        val updatedSets = exercise.sets.toMutableList()
                                        updatedSets.removeAt(setIndex)
                                        list[exerciseIndex] = exercise.copy(sets = updatedSets)
                                        saveExercises()
                                    }
                                }
                            }
                        },
                        onRemoveSession = { session ->
                            sessions.remove(session)
                            sessionExercises.remove(session)
                            if (selectedSession == session) {
                                selectedSession = if (sessions.isNotEmpty()) sessions.first() else ""
                            }
                            saveSessions()
                            saveExercises()
                        },
                        onRenameExercise = { exerciseIndex, newName ->
                            if (selectedSession.isNotEmpty()) {
                                val list = sessionExercises[selectedSession]
                                if (list != null && exerciseIndex in list.indices) {
                                    list[exerciseIndex] = list[exerciseIndex].copy(name = newName)
                                    saveExercises()
                                }
                            }
                        },
                        onUpdateNote = { exerciseIndex, newNote ->
                            if (selectedSession.isNotEmpty()) {
                                val list = sessionExercises[selectedSession]
                                if (list != null && exerciseIndex in list.indices) {
                                    list[exerciseIndex] = list[exerciseIndex].copy(note = newNote)
                                    saveExercises()
                                }
                            }
                        },
                        onMoveExercise = { from, to ->
                            if (selectedSession.isNotEmpty()) {
                                val list = sessionExercises[selectedSession]
                                if (list != null && from in list.indices && to in list.indices) {
                                    val item = list.removeAt(from)
                                    list.add(to, item)
                                    saveExercises()
                                }
                            }
                        },
                        isEditMode = isEditMode,
                        onEditModeChange = { isEditMode = it },
                        accentColor = accentColor,
                        onAccentColorChange = saveAccentColor,
                        barLiquidState = barLiquidState,
                        cardLiquidState = cardLiquidState,
                        onExportData = {
                            val dateStr = SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault()).format(Date()).lowercase(Locale.getDefault())
                            exportLauncher.launch("OSPOT_EXPORT_$dateStr.json")
                        },
                        onImportData = { importLauncher.launch(arrayOf("application/json")) },
                        onClearProgressData = {
                            scope.launch {
                                dataStore.edit { it[PROGRESS_DATA_KEY] = "{}" }
                            }
                        },
                        onSetLastProcessDate = { date ->
                            scope.launch {
                                dataStore.edit { it[LAST_PROCESS_DATE_KEY] = date }
                            }
                        },
                        onManualSaveProgress = { date ->
                            scope.launch {
                                val prefs = dataStore.data.first()
                                val progressJson = prefs[PROGRESS_DATA_KEY] ?: "{}"
                                val progressMap = try {
                                    Json.decodeFromString<MutableMap<String, MutableMap<String, DayProgress>>>(progressJson)
                                } catch (e: Exception) {
                                    mutableMapOf()
                                }

                                val exercisesJson = prefs[EXERCISES_KEY] ?: "{}"
                                val exercisesMap = try {
                                    Json.decodeFromString<Map<String, List<Exercise>>>(exercisesJson)
                                } catch (e: Exception) {
                                    emptyMap()
                                }

                                val maxes = mutableMapOf<String, DayProgress>()
                                exercisesMap.values.flatten().forEach { exercise ->
                                    var maxWeight = 0.0
                                    var maxReps = 0
                                    exercise.sets.forEach { set ->
                                        val w = set.weight.toDoubleOrNull() ?: 0.0
                                        val r = set.reps.toIntOrNull() ?: 0
                                        if (w > maxWeight) {
                                            maxWeight = w
                                            maxReps = r
                                        } else if (w == maxWeight && r > maxReps) {
                                            maxReps = r
                                        }
                                    }
                                    if (maxWeight > 0 || maxReps > 0) {
                                        val existing = maxes[exercise.name]
                                        if (existing == null || maxWeight > existing.weight || (maxWeight == existing.weight && maxReps > existing.reps)) {
                                            maxes[exercise.name] = DayProgress(maxWeight, maxReps)
                                        }
                                    }
                                }

                                maxes.forEach { (exerciseName, progress) ->
                                    val exerciseData = progressMap.getOrPut(exerciseName) { mutableMapOf() }
                                    exerciseData[date] = progress
                                }

                                dataStore.edit { it[PROGRESS_DATA_KEY] = Json.encodeToString(progressMap) }
                            }
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutAppScreen(
    currentScreen: String,
    onScreenChange: (String) -> Unit,
    showRpe: Boolean,
    onRpeChange: (Boolean) -> Unit,
    showRir: Boolean,
    onRirChange: (Boolean) -> Unit,
    expandOnStartup: Boolean,
    onExpandOnStartupChange: (Boolean) -> Unit,
    weightIncrement: String,
    onIncrementChange: (String) -> Unit,
    sessions: androidx.compose.runtime.snapshots.SnapshotStateList<String>,
    onSaveSessions: () -> Unit,
    selectedSession: String,
    onSelectedSessionChange: (String) -> Unit,
    menuExpanded: Boolean,
    onMenuToggle: (Boolean) -> Unit,
    showAddSessionDialog: Boolean,
    onShowDialogChange: (Boolean) -> Unit,
    newSessionNameText: String,
    onDialogTextChange: (String) -> Unit,
    showAddExerciseDialog: Boolean,
    onShowExerciseDialogChange: (Boolean) -> Unit,
    newExerciseNameText: String,
    onExerciseDialogTextChange: (String) -> Unit,
    exercises: List<Exercise>,
    onAddExercise: (String) -> Unit,
    onAddSet: (Int, SetType) -> Unit,
    onUpdateSet: (Int, Int, String?, String?, String?, String?) -> Unit,
    onRemoveExercise: (Int) -> Unit,
    onRemoveSet: (Int, Int) -> Unit,
    onRemoveSession: (String) -> Unit,
    onRenameExercise: (Int, String) -> Unit,
    onUpdateNote: (Int, String) -> Unit,
    onMoveExercise: (Int, Int) -> Unit,
    isEditMode: Boolean,
    onEditModeChange: (Boolean) -> Unit,
    accentColor: Color,
    onAccentColorChange: (Color) -> Unit,
    barLiquidState: LiquidState,
    cardLiquidState: LiquidState,
    onExportData: () -> Unit,
    onImportData: () -> Unit,
    onClearProgressData: () -> Unit = {},
    onSetLastProcessDate: (String) -> Unit = {},
    onManualSaveProgress: (String) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    var showDeleteExerciseDialog by remember { mutableStateOf(false) }
    var exerciseIndexToDelete by remember { mutableIntStateOf(-1) }

    var showDeleteSessionDialog by remember { mutableStateOf(false) }
    var sessionToDelete by remember { mutableStateOf("") }

    val expandedStates = remember { mutableStateMapOf<String, Boolean>() }

    val cardGradient = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF1E1E1E),
                Color(0xFF161616)
            )
        )
    }

    val isAppStarting = remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(500) // Small grace period for initial layout
        isAppStarting.value = false
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            val topGradient = remember {
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 1.0f),
                        Color.Transparent
                    )
                )
            }
            Box(
                modifier = Modifier
                    .liquid(barLiquidState){
                        //tint = Color.Black.copy(alpha = 0.25f)
                        frost = 3.dp
                        shape = RoundedCornerShape(
                            topStart = 0.dp,
                            topEnd = 0.dp,
                            bottomStart = 24.dp,
                            bottomEnd = 24.dp
                        )
                        edge = 0.025f
                    }
                    .fillMaxWidth()
                    .background(topGradient)
            ) {
                TopAppBar(
                    modifier = Modifier.height(80.dp),
                    title = {
                        Box(modifier = Modifier.fillMaxHeight(), contentAlignment = Alignment.CenterStart) {
                            Row(
                                modifier = Modifier
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        onMenuToggle(!menuExpanded)
                                    }
                                    .padding(start = 0.dp)
                                    .offset(y = (-4).dp)
                                    .padding(vertical = 0.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.ospot_logo),
                                    contentDescription = "OSPOT",
                                    modifier = Modifier.height(28.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = if (menuExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                    contentDescription = "Toggle Menu"
                                )
                            }

                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { onMenuToggle(false) },
                                offset = DpOffset(x = 0.dp , y = 8.dp),
                                containerColor = Color.Transparent,
                                modifier = Modifier
                                    //.width(135.dp)
                                    .liquid(barLiquidState) {
                                        refraction = 0.20f
                                        frost = 3.dp
                                        edge = 0.01f
                                        //tint = Color.Black.copy(alpha = 0.25f)
                                        shape = RoundedCornerShape(16.dp)
                                    }

                            ) {
                                DropdownMenuItem(
                                    text = { Text("Settings", color = Color.White, fontSize = 16.sp) },
                                    onClick = {
                                        onScreenChange("settings")
                                        onMenuToggle(false)
                                    },
                                    modifier = Modifier.height(36.dp).padding(horizontal = 4.dp)
                                )
                                val allExpanded = exercises.isNotEmpty() && exercises.indices.all {
                                    expandedStates[exercises[it].name + it] ?: expandOnStartup
                                }
                                DropdownMenuItem(
                                    text = { Text(if (allExpanded) "Collapse All" else "Expand All", color = Color.White, fontSize = 16.sp) },
                                    onClick = {
                                        val target = !allExpanded
                                        exercises.forEachIndexed { index, exercise ->
                                            expandedStates[exercise.name + index] = target
                                        }
                                        onMenuToggle(false)
                                    },
                                    modifier = Modifier.height(36.dp).padding(horizontal = 4.dp)
                                )
                                DropdownMenuItem(
                                    text = { Text("Edit", color = Color.White, fontSize = 16.sp) },
                                    onClick = {
                                        onEditModeChange(true)
                                        onMenuToggle(false)
                                    },
                                    modifier = Modifier.height(36.dp).padding(horizontal = 4.dp)
                                )
                                DropdownMenuItem(
                                    text = { Text("Progress Chart", color = Color.White, fontSize = 16.sp) },
                                    onClick = {
                                        onScreenChange("progress_chart")
                                        onMenuToggle(false)
                                    },
                                    modifier = Modifier.height(36.dp).padding(horizontal = 4.dp)
                                )
                                DropdownMenuItem(
                                    text = { Text("Debug", color = Color.White, fontSize = 16.sp) },
                                    onClick = {
                                        onScreenChange("debug")
                                        onMenuToggle(false)
                                    },
                                    modifier = Modifier.height(36.dp).padding(horizontal = 4.dp)
                                )
                                DropdownMenuItem(
                                    text = { Text("v${BuildConfig.VERSION_NAME}", color = Color.Gray, fontSize = 12.sp) },
                                    onClick = { },
                                    modifier = Modifier.height(32.dp).padding(horizontal = 4.dp),
                                    enabled = false
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    ),
                    actions = {
                        Row(modifier = Modifier.fillMaxHeight(), verticalAlignment = Alignment.CenterVertically) {
                            if (currentScreen == "settings" || currentScreen == "progress_chart" || currentScreen == "debug") {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .offset(x = (-4).dp, y = (-4).dp)
                                        .liquid(barLiquidState) {
                                            curve = 1.0f
                                            frost = 3.dp
                                            edge = 0.025f
                                            //tint = Color.White.copy(alpha = 0.075f)
                                            shape = RoundedCornerShape(24.dp)
                                        }
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) {
                                            onScreenChange("workout_log")
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "Exit",
                                        tint = accentColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            } else {
                                IconButton(onClick = { onScreenChange("settings") }) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "Swap View",
                                        tint = Color.Transparent
                                    )
                                }
                                if (selectedSession.isNotEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .offset(x = (-4).dp, y = (-4).dp)
                                            .liquid(barLiquidState) {
                                                curve = 1.0f
                                                frost = 3.dp
                                                edge = 0.025f
                                                //tint = Color.White.copy(alpha = 0.075f)
                                                shape = RoundedCornerShape(24.dp)
                                            }
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null
                                            ) {
                                                if (isEditMode) {
                                                    onEditModeChange(false)
                                                } else {
                                                    onShowExerciseDialogChange(true)
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isEditMode) Icons.Filled.Close else Icons.Filled.Add,
                                            contentDescription = if (isEditMode) "Exit Edit Mode" else "New",
                                            tint = accentColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                )
            }
        },
        bottomBar = {
            if (currentScreen == "workout_log") {
                val bottomGradient = remember {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 1.0f)
                        )
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquid(barLiquidState){
                            frost = 3.dp
                            //tint = Color.Black.copy(alpha = 0.25f)
                            curve = 0.50f
                            shape = RoundedCornerShape(
                                topStart = 24.dp,
                                topEnd = 24.dp,
                                bottomStart = 0.dp,
                                bottomEnd = 0.dp
                            )
                            edge = 0.025f
                        }
                        //.background(bottomGradient)
                        .navigationBarsPadding()
                ) {
                    Surface(
                        color = Color.Transparent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(55.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(start = 10.dp, end = 10.dp)
                                .horizontalScroll(rememberScrollState()),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start
                        ) {
                            sessions.forEach { session ->
                                val isSelected = selectedSession == session
                                val sessionAlpha by animateFloatAsState(
                                    targetValue = if (isSelected) 0.2f else 0.0f,
                                    animationSpec = tween(durationMillis = 300),
                                    label = "SessionAlpha"
                                )
                                val sessionTint by animateColorAsState(
                                    targetValue = if (isSelected) accentColor else Color.White,
                                    animationSpec = tween(durationMillis = 300),
                                    label = "SessionTint"
                                )

                                Box(
                                    modifier = Modifier
                                        .padding(end = 8.dp)
                                        .liquid(barLiquidState) {
                                            frost = 3.dp
                                            curve = 1.0f
                                            edge = 0.025f
                                            tint = sessionTint.copy(alpha = sessionAlpha)
                                            shape = RoundedCornerShape(24.dp)
                                        }
                                        // Fix: Removed ripple effect from session button taps
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) {
                                            onSelectedSessionChange(session)
                                        }
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = session,
                                            //color = if (isSelected) Color.White else Color.Gray,
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal
                                        )
                                        if (isEditMode) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Box(
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .clickable(
                                                        interactionSource = remember { MutableInteractionSource() },
                                                        indication = null
                                                    ) {
                                                        sessionToDelete = session
                                                        showDeleteSessionDialog = true
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                // Custom Rounded Minus
                                                Box(
                                                    modifier = Modifier
                                                        .width(10.dp)
                                                        .height(2.dp)
                                                        .background(Color(0xffbf0000), RoundedCornerShape(1.dp))
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .liquid(barLiquidState) {
                                        curve = 1.0f
                                        frost = 3.dp
                                        edge = 0.025f
                                        //tint = Color.White.copy(alpha = 0.075f)
                                        shape = RoundedCornerShape(24.dp)
                                    }
                                    //.background(brush = cardGradient, shape = RoundedCornerShape(50.dp))
                                    //.border(width = 1.dp, color = Color.Gray.copy(alpha = 0.25f), shape = RoundedCornerShape(50.dp))
                                    // Fix: Removed ripple effect from dialog + button tap
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        onShowDialogChange(true)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = "Add New Session Folder",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            StarryBackground(
                modifier = Modifier
                    .fillMaxSize()
                    .liquefiable(cardLiquidState)
                    .liquefiable(barLiquidState)
            )
            if (currentScreen == "workout_log") {
                AnimatedContent(
                    targetState = selectedSession to exercises,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(300)) togetherWith
                                fadeOut(animationSpec = tween(300))
                    },
                    contentKey = { it.first }, // Only animate when the session name changes
                    label = "SessionChangeAnimation"
                ) { (targetSession, targetExercises) ->
                    if (targetSession.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .liquefiable(barLiquidState),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            ) {
                                Text(
                                    text = "Create a session to begin logging",
                                    fontSize = 18.sp,
                                    color = Color.White.copy(alpha = 0.9f),
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "Use the ",
                                        fontSize = 14.sp,
                                        color = Color.Gray
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .background(
                                                Brush.verticalGradient(
                                                    colors = listOf(
                                                        Color(0xFF1E1E1E),
                                                        Color(0xFF161616)
                                                    )
                                                ), shape = RoundedCornerShape(50.dp)
                                            )
                                            .border(width = 1.dp, color = Color.Gray.copy(alpha = 0.25f), shape = RoundedCornerShape(50.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Add,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                    Text(
                                        text = " icon at the bottom left to create a session",
                                        fontSize = 14.sp,
                                        color = Color.Gray
                                    )
                                }
                                Spacer(modifier = Modifier.height(24.dp))
                                SetTypeLegend()
                            }
                        }
                    } else {
                        if (targetExercises.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .liquefiable(barLiquidState),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(horizontal = 24.dp)
                                ) {
                                    Text(
                                        text = "No exercises added",
                                        fontSize = 18.sp,
                                        color = Color.White.copy(alpha = 0.9f),
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = "to add an exercise use the ",
                                            fontSize = 14.sp,
                                            color = Color.Gray
                                        )
                                        Icon(
                                            imageVector = Icons.Filled.Add,
                                            contentDescription = null,
                                            tint = accentColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = " icon at the top right",
                                            fontSize = 14.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }
                        } else {
                            val sessionTransitionTime = remember(targetSession) { System.currentTimeMillis() }
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .liquefiable(barLiquidState),
                                contentPadding = PaddingValues(
                                    top = innerPadding.calculateTopPadding(),
                                    bottom = innerPadding.calculateBottomPadding()
                                )
                            ) {
                                itemsIndexed(
                                    items = targetExercises,
                                    key = { index, exercise -> exercise.name + index }
                                ) { index, exercise ->
                                    val shouldAnimate = !isAppStarting.value && (System.currentTimeMillis() - sessionTransitionTime < 150)
                                    var isVisible by remember(targetSession) { mutableStateOf(!shouldAnimate) }

                                    LaunchedEffect(targetSession) {
                                        if (shouldAnimate) {
                                            delay(index * 60L)
                                            isVisible = true
                                        }
                                    }

                                    AnimatedVisibility(
                                        visible = isVisible,
                                        enter = slideInHorizontally(
                                            initialOffsetX = { -150 },
                                            animationSpec = tween(durationMillis = 400)
                                        ) + fadeIn(animationSpec = tween(durationMillis = 400)),
                                        exit = fadeOut(animationSpec = tween(durationMillis = 100))
                                    ) {
                                        val itemKey = exercise.name + index
                                        val noteKey = itemKey + "_note"
                                        ExerciseBox(
                                            name = exercise.name,
                                            note = exercise.note,
                                            sets = exercise.sets,
                                            isEditMode = isEditMode,
                                            showRpe = showRpe,
                                            showRir = showRir,
                                            weightIncrement = weightIncrement,
                                            onAddSet = { type -> onAddSet(index, type) },
                                            onUpdateSet = { setIndex, w, r, rir, rpe ->
                                                onUpdateSet(
                                                    index,
                                                    setIndex,
                                                    w,
                                                    r,
                                                    rir,
                                                    rpe
                                                )
                                            },
                                            onRemoveSet = { setIndex -> onRemoveSet(index, setIndex) },
                                            onRename = { newName -> onRenameExercise(index, newName) },
                                            onUpdateNote = { newNote -> onUpdateNote(index, newNote) },
                                            onMoveUp = if (index > 0) {
                                                { onMoveExercise(index, index - 1) }
                                            } else null,
                                            onMoveDown = if (index < targetExercises.size - 1) {
                                                { onMoveExercise(index, index + 1) }
                                            } else null,
                                            accentColor = accentColor,
                                            cardLiquidState = cardLiquidState,
                                            barLiquidState = barLiquidState,
                                            onDeleteClick = {
                                                exerciseIndexToDelete = index
                                                showDeleteExerciseDialog = true
                                            },
                                            isExpanded = expandedStates[itemKey] ?: expandOnStartup,
                                            onToggleExpand = {
                                                val current = expandedStates[itemKey] ?: expandOnStartup
                                                expandedStates[itemKey] = !current
                                            },
                                            isNoteExpanded = expandedStates[noteKey] ?: false,
                                            onToggleNoteExpand = {
                                                val current = expandedStates[noteKey] ?: false
                                                expandedStates[noteKey] = !current
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (currentScreen == "settings") {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    SettingsScreen(
                        innerPadding = innerPadding,
                        showRpe = showRpe,
                        onRpeChange = onRpeChange,
                        showRir = showRir,
                        onRirChange = onRirChange,
                        expandOnStartup = expandOnStartup,
                        onExpandOnStartupChange = onExpandOnStartupChange,
                        increment = weightIncrement,
                        onIncrementChange = onIncrementChange,
                        accentColor = accentColor,
                        onAccentColorChange = onAccentColorChange,
                        onBackClick = { onScreenChange("workout_log") },
                        onExportData = onExportData,
                        onImportData = onImportData
                    )
                }
            }

            if (currentScreen == "progress_chart") {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    ProgressChartScreen(
                        innerPadding = innerPadding,
                        accentColor = accentColor,
                        onBackClick = { onScreenChange("workout_log") }
                    )
                }
            }

            if (currentScreen == "debug") {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    DebugScreen(
                        innerPadding = innerPadding,
                        accentColor = accentColor,
                        onBackClick = { onScreenChange("workout_log") },
                        onClearProgressData = onClearProgressData,
                        onSetLastProcessDate = onSetLastProcessDate,
                        onManualSaveProgress = onManualSaveProgress
                    )
                }
            }
        }
    }

    if (showAddSessionDialog) {
        AlertDialog(
            onDismissRequest = {
                onShowDialogChange(false)
                onDialogTextChange("")
            },
            containerColor = Color(0xFF161616),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.border(
                width = 1.dp,
                color = Color.Gray.copy(alpha = 0.25f),
                shape = RoundedCornerShape(20.dp)
            ),
            title = {
                Text(
                    text = "New Session Name",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
            },
            text = {
                OutlinedTextField(
                    value = newSessionNameText,
                    onValueChange = onDialogTextChange,
                    placeholder = { Text("e.g., Push, Pull, Upper", color = Color.White.copy(alpha = 0.3f), fontSize = 14.sp) },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 15.sp),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF1E1E1E),
                        unfocusedContainerColor = Color(0xFF1E1E1E),
                        focusedBorderColor = Color.Gray.copy(alpha = 0.25f),
                        unfocusedBorderColor = Color.Gray.copy(alpha = 0.25f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val trimmedName = newSessionNameText.trim()
                        if (trimmedName.isNotEmpty() && !sessions.contains(trimmedName)) {
                            sessions.add(trimmedName)
                            onSelectedSessionChange(trimmedName)
                            onSaveSessions()
                        }
                        onShowDialogChange(false)
                        onDialogTextChange("")
                    }
                ) {
                    Text("Add", color = Color.White, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    onShowDialogChange(false)
                    onDialogTextChange("")
                }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    if (showAddExerciseDialog) {
        AlertDialog(
            onDismissRequest = {
                onShowExerciseDialogChange(false)
                onExerciseDialogTextChange("")
            },
            containerColor = Color(0xFF161616),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.border(
                width = 1.dp,
                color = Color.Gray.copy(alpha = 0.25f),
                shape = RoundedCornerShape(20.dp)
            ),
            title = {
                Text(
                    text = "New Exercise Name",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
            },
            text = {
                OutlinedTextField(
                    value = newExerciseNameText,
                    onValueChange = onExerciseDialogTextChange,
                    placeholder = { Text("e.g., Bench Press, Squat", color = Color.White.copy(alpha = 0.3f), fontSize = 14.sp) },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 15.sp),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF1E1E1E),
                        unfocusedContainerColor = Color(0xFF1E1E1E),
                        focusedBorderColor = Color.Gray.copy(alpha = 0.25f),
                        unfocusedBorderColor = Color.Gray.copy(alpha = 0.25f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val trimmedName = newExerciseNameText.trim()
                        if (trimmedName.isNotEmpty()) {
                            onAddExercise(trimmedName)
                        }
                        onShowExerciseDialogChange(false)
                        onExerciseDialogTextChange("")
                    }
                ) {
                    Text("Add", color = Color.White, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    onShowExerciseDialogChange(false)
                    onExerciseDialogTextChange("")
                }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    if (showDeleteSessionDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteSessionDialog = false },
            containerColor = Color(0xFF161616),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.border(
                width = 1.dp,
                color = Color.Gray.copy(alpha = 0.25f),
                shape = RoundedCornerShape(20.dp)
            ),
            title = {
                Text(
                    text = "Delete Session?",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to remove \"$sessionToDelete\" and all its exercises?",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (sessionToDelete.isNotEmpty()) {
                            onRemoveSession(sessionToDelete)
                        }
                        showDeleteSessionDialog = false
                    }
                ) {
                    Text("Delete", color = accentColor, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteSessionDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    if (showDeleteExerciseDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteExerciseDialog = false },
            containerColor = Color(0xFF161616),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.border(
                width = 1.dp,
                color = Color.Gray.copy(alpha = 0.25f),
                shape = RoundedCornerShape(20.dp)
            ),
            title = {
                Text(
                    text = "Delete Exercise?",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to remove this exercise box?",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (exerciseIndexToDelete != -1) {
                            onRemoveExercise(exerciseIndexToDelete)
                        }
                        showDeleteExerciseDialog = false
                    }
                ) {
                    Text("Delete", color = accentColor, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteExerciseDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }
}

@Composable
fun SetTypeLegend() {
    val cardGradient = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF1E1E1E),
                Color(0xFF161616)
            )
        )
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = cardGradient,
                shape = RoundedCornerShape(16.dp)
            )
            .border(
                width = 1.dp,
                color = Color.Gray.copy(alpha = 0.2f),
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LegendItem(
                type = SetType.WARMUP,
                text = "Light warmup set (optional): ~50% of working weight"
            )
            LegendItem(
                type = SetType.PRIMER,
                text = "post-activation / heavy warmup set (optional): ~80%+ of working weight, 1–3 reps"
            )
            LegendItem(
                type = SetType.WORKING,
                text = "Working set: Full intensity, close to failure (0-2 RIR)"
            )
        }
    }
}

@Composable
fun LegendItem(type: SetType, text: String) {
    val parts = text.split(": ", limit = 2)
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .padding(top = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            SetTypeShape(type)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            if (parts.size == 2) {
                Text(
                    text = parts[0] + ":",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = parts[1],
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    style = LocalTextStyle.current.copy(
                        textIndent = androidx.compose.ui.text.style.TextIndent(firstLine = 0.sp, restLine = 12.sp)
                    )
                )
            } else {
                Text(
                    text = text,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
fun SettingsScreen(
    innerPadding: PaddingValues,
    showRpe: Boolean,
    onRpeChange: (Boolean) -> Unit,
    showRir: Boolean,
    onRirChange: (Boolean) -> Unit,
    expandOnStartup: Boolean,
    onExpandOnStartupChange: (Boolean) -> Unit,
    increment: String,
    onIncrementChange: (String) -> Unit,
    accentColor: Color,
    onAccentColorChange: (Color) -> Unit,
    onBackClick: () -> Unit,
    onExportData: () -> Unit,
    onImportData: () -> Unit
){
    var incrementMenuExpanded by remember { mutableStateOf(false) }

    val cardGradient = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF1E1E1E),
                Color(0xFF161616)
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .verticalScroll(rememberScrollState())
    ) {
        BackHandler(enabled = true) {
            onBackClick()
        }

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Transparent
            ),
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxWidth()
                .background(
                    brush = cardGradient,
                    shape = RoundedCornerShape(20.dp)
                )
                .border(
                    width = 1.dp,
                    color = Color.Gray.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(20.dp)
                )
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Settings",
                    fontSize = 22.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 20.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Show RIR Selection",
                        fontSize = 16.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    Switch(
                        checked = showRir,
                        onCheckedChange = onRirChange
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Show RPE Selection",
                        fontSize = 16.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    Switch(
                        checked = showRpe,
                        onCheckedChange = onRpeChange
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Expand All on Startup",
                        fontSize = 16.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    Switch(
                        checked = expandOnStartup,
                        onCheckedChange = onExpandOnStartupChange
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Weight Increment",
                        fontSize = 16.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    Box {
                        Row(
                            modifier = Modifier
                                .height(32.dp)
                                .background(Color(0xFF1E1E1E), shape = RoundedCornerShape(8.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    incrementMenuExpanded = !incrementMenuExpanded
                                }
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = increment, color = Color.White, fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = if (incrementMenuExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                contentDescription = "Select Increment",
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = incrementMenuExpanded,
                            onDismissRequest = { incrementMenuExpanded = false },
                            shape = RoundedCornerShape(12.dp),
                            containerColor = Color.Transparent,
                            modifier = Modifier
                                .width(100.dp)
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0xFF1E1E1E),
                                            Color(0xFF161616)
                                        )
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .border(1.dp, Color.Gray.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        ) {
                            DropdownMenuItem(
                                text = { Text("1", color = Color.White, fontSize = 15.sp) },
                                onClick = {
                                    onIncrementChange("1")
                                    incrementMenuExpanded = false
                                },
                                modifier = Modifier.height(40.dp).padding(horizontal = 4.dp)
                            )
                            DropdownMenuItem(
                                text = { Text("1.25", color = Color.White, fontSize = 15.sp) },
                                onClick = {
                                    onIncrementChange("1.25")
                                    incrementMenuExpanded = false
                                },
                                modifier = Modifier.height(40.dp).padding(horizontal = 4.dp)
                            )
                            DropdownMenuItem(
                                text = { Text("2.5", color = Color.White, fontSize = 15.sp) },
                                onClick = {
                                    onIncrementChange("2.5")
                                    incrementMenuExpanded = false
                                },
                                modifier = Modifier.height(40.dp).padding(horizontal = 4.dp)
                            )
                            DropdownMenuItem(
                                text = { Text("5.0", color = Color.White, fontSize = 15.sp) },
                                onClick = {
                                    onIncrementChange("5.0")
                                    incrementMenuExpanded = false
                                },
                                modifier = Modifier.height(40.dp).padding(horizontal = 4.dp)
                            )
                            DropdownMenuItem(
                                text = { Text("10.0", color = Color.White, fontSize = 15.sp) },
                                onClick = {
                                    onIncrementChange("10.0")
                                    incrementMenuExpanded = false
                                },
                                modifier = Modifier.height(40.dp).padding(horizontal = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Accent Color",
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                val accentColors = listOf(
                    Color(0xFF8B00FF.toInt()), // Violet
                    Color(0xFF3F51B5.toInt()), // Indigo
                    Color(0xFF2196F3.toInt()), // Blue
                    Color(0xFF4CAF50.toInt()), // Green
                    Color(0xFFFFEB3B.toInt()), // Yellow
                    Color(0xFFFF9800.toInt()), // Orange
                    Color(0xFFF44336.toInt()), // Red
                    Color(0xFFFFFFFF.toInt())  // White
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    accentColors.forEach { color ->
                        val isSelected = accentColor == color
                        val isNone = color == Color(0xFFFFFFFF.toInt())
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(if (isNone) Color(0xFF2A2A2A) else color, RoundedCornerShape(16.dp))
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) Color.White else Color.Gray.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { onAccentColorChange(color) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isNone) {
                                Canvas(modifier = Modifier.size(20.dp)) {
                                    drawLine(
                                        color = Color.Gray,
                                        start = androidx.compose.ui.geometry.Offset(0f, size.height),
                                        end = androidx.compose.ui.geometry.Offset(size.width, 0f),
                                        strokeWidth = 1.5.dp.toPx()
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = Color.Gray.copy(alpha = 0.15f))
                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Data Management",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E1E1E), RoundedCornerShape(12.dp))
                        .border(1.dp, Color.Gray.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                        .clickable { onExportData() }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Export Workout Data (JSON)", color = Color.White, fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E1E1E), RoundedCornerShape(12.dp))
                        .border(1.dp, Color.Gray.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                        .clickable { onImportData() }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Import Workout Data (JSON)", color = Color.White, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun ExerciseBox(
    name: String,
    note: String,
    sets: List<WorkoutSet>,
    isEditMode: Boolean,
    showRpe: Boolean,
    showRir: Boolean,
    weightIncrement: String,
    onAddSet: (SetType) -> Unit,
    onUpdateSet: (Int, String?, String?, String?, String?) -> Unit,
    onRemoveSet: (Int) -> Unit,
    onRename: (String) -> Unit,
    onUpdateNote: (String) -> Unit,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
    accentColor: Color,
    cardLiquidState: LiquidState,
    barLiquidState: LiquidState,
    onDeleteClick: () -> Unit,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    isNoteExpanded: Boolean,
    onToggleNoteExpand: () -> Unit
) {
    var addSetMenuExpanded by remember { mutableStateOf(false) }
    var isEditingName by remember { mutableStateOf(false) }
    var editNameValue by remember { mutableStateOf(TextFieldValue(name)) }

    var isEditingNote by remember { mutableStateOf(false) }
    var editNoteValue by remember { mutableStateOf(TextFieldValue(note)) }

    var hasGainedFocus by remember { mutableStateOf(false) }
    var hasNoteGainedFocus by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }
    val noteFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(isEditingName) {
        if (isEditingName) {
            focusRequester.requestFocus()
            keyboardController?.show()
        } else {
            hasGainedFocus = false
        }
    }

    LaunchedEffect(isEditingNote) {
        if (isEditingNote) {
            noteFocusRequester.requestFocus()
            keyboardController?.show()
        } else {
            hasNoteGainedFocus = false
        }
    }

    val cardGradient = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF1E1E1E),
                Color(0xFF161616)
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Transparent
            ),
            modifier = Modifier
                .fillMaxWidth()
                .liquid(cardLiquidState) {
                    frost = 3.dp
                    edge = 0.01f
                    refraction = 0.15f
                    curve = 0.50f
                    //tint = Color.White.copy(alpha = 0.075f)
                    shape = RoundedCornerShape(24.dp)
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 12.dp, end = 0.dp, bottom = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(26.dp) // Fixed height to prevent card jump
                        .padding(end = 40.dp), // Maintain space for top-right button
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!isEditMode) {
                            Icon(
                                imageVector = if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                contentDescription = if (isExpanded) "Collapse" else "Expand",
                                tint = accentColor,
                                modifier = Modifier
                                    .size(20.dp)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { onToggleExpand() }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        if (isEditMode && isEditingName) {
                            androidx.compose.foundation.text.BasicTextField(
                                value = editNameValue,
                                onValueChange = { editNameValue = it },
                                textStyle = LocalTextStyle.current.copy(
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(focusRequester)
                                    .onFocusChanged { focusState ->
                                        if (focusState.isFocused) hasGainedFocus = true
                                        if (!focusState.isFocused && hasGainedFocus && isEditingName) {
                                            if (editNameValue.text.isNotBlank()) {
                                                onRename(editNameValue.text)
                                            }
                                            isEditingName = false
                                        }
                                    },
                                singleLine = true,
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                    imeAction = androidx.compose.ui.text.input.ImeAction.Done
                                ),
                                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                                    onDone = {
                                        if (editNameValue.text.isNotBlank()) {
                                            onRename(editNameValue.text)
                                        }
                                        isEditingName = false
                                    }
                                ),
                                cursorBrush = androidx.compose.ui.graphics.SolidColor(Color.White)
                            )
                        } else {
                            Text(
                                text = name,
                                style = LocalTextStyle.current.copy(
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                ),
                                modifier = if (isEditMode) {
                                    Modifier.clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        editNameValue = TextFieldValue(name, TextRange(name.length))
                                        isEditingName = true
                                    }
                                } else {
                                    Modifier.clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { onToggleExpand() }
                                }
                            )

                            if (isEditMode) {
                                Spacer(modifier = Modifier.width(8.dp))
                                if (onMoveUp != null) {
                                    IconButton(
                                        onClick = onMoveUp,
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowUp,
                                            contentDescription = "Move Up",
                                            tint = Color.Gray,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                if (onMoveDown != null) {
                                    IconButton(
                                        onClick = onMoveDown,
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = "Move Down",
                                            tint = Color.Gray,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            } else {
                                Spacer(modifier = Modifier.width(12.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Notes,
                                    contentDescription = "Note",
                                    tint = if (note.isNotEmpty()) accentColor else Color.Gray.copy(alpha = 0.5f),
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) { onToggleNoteExpand() }
                                )
                            }
                        }
                    }
                }

                AnimatedVisibility(
                    visible = isNoteExpanded && !isEditMode,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(modifier = Modifier.padding(top = 8.dp, end = 16.dp)) {
                        if (isEditingNote) {
                            androidx.compose.foundation.text.BasicTextField(
                                value = editNoteValue,
                                onValueChange = { editNoteValue = it },
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 13.sp
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(noteFocusRequester)
                                    .onFocusChanged { focusState ->
                                        if (focusState.isFocused) hasNoteGainedFocus = true
                                        if (!focusState.isFocused && hasNoteGainedFocus && isEditingNote) {
                                            onUpdateNote(editNoteValue.text)
                                            isEditingNote = false
                                        }
                                    },
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                    imeAction = androidx.compose.ui.text.input.ImeAction.Done
                                ),
                                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                                    onDone = {
                                        onUpdateNote(editNoteValue.text)
                                        isEditingNote = false
                                    }
                                ),
                                cursorBrush = androidx.compose.ui.graphics.SolidColor(Color.White)
                            )
                        } else {
                            Text(
                                text = if (note.isEmpty()) "Add a note..." else note,
                                color = if (note.isEmpty()) Color.Gray.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.8f),
                                fontSize = 13.sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        editNoteValue = TextFieldValue(note, TextRange(note.length))
                                        isEditingNote = true
                                    }
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = Color.Gray.copy(alpha = 0.1f))
                    }
                }

                AnimatedVisibility(
                    visible = isExpanded || isEditMode,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(12.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            sets.forEachIndexed { index, set ->
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(modifier = Modifier.width(82.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                if (isEditMode) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(20.dp)
                                                            .clickable(
                                                                interactionSource = remember { MutableInteractionSource() },
                                                                indication = null
                                                            ) { onRemoveSet(index) },
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .width(10.dp)
                                                                .height(2.dp)
                                                                .background(Color(0xffbf0000), RoundedCornerShape(1.dp))
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(2.dp))
                                                }
                                                SetTypeShape(set.type)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Set ${index + 1}",
                                                    color = Color.Gray,
                                                    fontSize = 14.sp
                                                )
                                            }
                                        }

                                        // Weight
                                        SetValueEditor(
                                            label = "Weight",
                                            value = set.weight,
                                            labelWidth = 38.dp,
                                            onValueChange = { onUpdateSet(index, it, null, null, null) },
                                            onIncrement = {
                                                val current = set.weight.toDoubleOrNull() ?: 0.0
                                                val inc = weightIncrement.toDoubleOrNull() ?: 2.5
                                                val next = current + inc
                                                onUpdateSet(index, if (next % 1.0 == 0.0) next.toInt().toString() else next.toString(), null, null, null)
                                            },
                                            onDecrement = {
                                                val current = set.weight.toDoubleOrNull() ?: 0.0
                                                val inc = weightIncrement.toDoubleOrNull() ?: 2.5
                                                val next = (current - inc).coerceAtLeast(0.0)
                                                onUpdateSet(index, if (next % 1.0 == 0.0) next.toInt().toString() else next.toString(), null, null, null)
                                            },
                                            modifier = Modifier.weight(1f)
                                        )

                                        Spacer(modifier = Modifier.width(30.dp))

                                        // Reps
                                        SetValueEditor(
                                            label = "Reps",
                                            value = set.reps,
                                            labelWidth = 24.dp,
                                            onValueChange = { onUpdateSet(index, null, it, null, null) },
                                            onIncrement = {
                                                val current = set.reps.toIntOrNull() ?: 0
                                                onUpdateSet(index, null, (current + 1).toString(), null, null)
                                            },
                                            onDecrement = {
                                                val current = set.reps.toIntOrNull() ?: 0
                                                onUpdateSet(index, null, (current - 1).coerceAtLeast(0).toString(), null, null)
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    if (showRpe || showRir) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Spacer(modifier = Modifier.width(82.dp))
                                            if (showRir) {
                                                SetValueEditor(
                                                    label = "RIR",
                                                    value = set.rir,
                                                    labelWidth = 38.dp,
                                                    onValueChange = { onUpdateSet(index, null, null, it, null) },
                                                    onIncrement = {
                                                        val current = set.rir.toIntOrNull() ?: 0
                                                        onUpdateSet(index, null, null, (current + 1).toString(), null)
                                                    },
                                                    onDecrement = {
                                                        val current = set.rir.toIntOrNull() ?: 0
                                                        onUpdateSet(index, null, null, (current - 1).coerceAtLeast(0).toString(), null)
                                                    },
                                                    modifier = Modifier.weight(1f)
                                                )
                                            } else {
                                                Spacer(modifier = Modifier.weight(1f))
                                            }

                                            Spacer(modifier = Modifier.width(30.dp))

                                            if (showRpe) {
                                                SetValueEditor(
                                                    label = "RPE",
                                                    value = set.rpe,
                                                    labelWidth = 24.dp,
                                                    onValueChange = { onUpdateSet(index, null, null, null, it) },
                                                    onIncrement = {
                                                        val current = set.rpe.toDoubleOrNull() ?: 0.0
                                                        val next = (current + 0.5).coerceAtMost(10.0)
                                                        onUpdateSet(index, null, null, null, if (next % 1.0 == 0.0) next.toInt().toString() else next.toString())
                                                    },
                                                    onDecrement = {
                                                        val current = set.rpe.toDoubleOrNull() ?: 0.0
                                                        val next = (current - 0.5).coerceAtLeast(0.0)
                                                        onUpdateSet(index, null, null, null, if (next % 1.0 == 0.0) next.toInt().toString() else next.toString())
                                                    },
                                                    modifier = Modifier.weight(1f)
                                                )
                                            } else {
                                                Spacer(modifier = Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 4.dp, end = 8.dp)
        ) {
            if (isEditMode) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onDeleteClick() },
                    contentAlignment = Alignment.Center
                ) {
                    // Custom Rounded Minus
                    Box(
                        modifier = Modifier
                            .width(12.dp)
                            .height(3.dp)
                            .background(Color(0xffbf0000), RoundedCornerShape(2.dp))
                    )
                }
            } else if (isExpanded) {
                Text(
                    text = "+ Add set",
                    color = accentColor.copy(alpha = 0.9f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            addSetMenuExpanded = true
                        }
                        .padding(8.dp)
                )

                DropdownMenu(
                    expanded = addSetMenuExpanded,
                    onDismissRequest = { addSetMenuExpanded = false },
                    shape = RoundedCornerShape(16.dp),
                    containerColor = Color.Transparent,
                    modifier = Modifier
                        .liquid(barLiquidState) {
                            refraction = 0.15f
                            frost = 3.dp
                            edge = 0.01f
                            //tint = Color.Black.copy(alpha = 0.025f)
                            shape = RoundedCornerShape(16.dp)
                        }
                ) {
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                SetTypeShape(SetType.WARMUP)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Warmup set", color = Color.White, fontSize = 14.sp)
                            }
                        },
                        onClick = {
                            onAddSet(SetType.WARMUP)
                            addSetMenuExpanded = false
                        },
                        modifier = Modifier.height(36.dp).padding(horizontal = 4.dp)
                    )
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                SetTypeShape(SetType.PRIMER)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Primer set", color = Color.White, fontSize = 14.sp)
                            }
                        },
                        onClick = {
                            onAddSet(SetType.PRIMER)
                            addSetMenuExpanded = false
                        },
                        modifier = Modifier.height(36.dp).padding(horizontal = 4.dp)
                    )
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                SetTypeShape(SetType.WORKING)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Working set", color = Color.White, fontSize = 14.sp)
                            }
                        },
                        onClick = {
                            onAddSet(SetType.WORKING)
                            addSetMenuExpanded = false
                        },
                        modifier = Modifier.height(36.dp).padding(horizontal = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SetValueEditor(
    label: String,
    value: String,
    suffix: String = "",
    labelWidth: androidx.compose.ui.unit.Dp? = null,
    onValueChange: (String) -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isEditing by remember { mutableStateOf(false) }
    var editValue by remember { mutableStateOf(TextFieldValue(value)) }
    var hasGainedFocus by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(isEditing) {
        if (isEditing) {
            focusRequester.requestFocus()
            keyboardController?.show()
        } else {
            hasGainedFocus = false
        }
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Text(
            text = label,
            color = Color.Gray,
            fontSize = 11.sp,
            modifier = if (labelWidth != null) Modifier.width(labelWidth) else Modifier.width(IntrinsicSize.Min)
        )
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Box(
                modifier = Modifier
                    .widthIn(min = 32.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        val text = if (value == "-") "" else value
                        editValue = TextFieldValue(
                            text = text,
                            selection = TextRange(text.length)
                        )
                        isEditing = true
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isEditing) {
                    androidx.compose.foundation.text.BasicTextField(
                        value = editValue,
                        onValueChange = { editValue = it },
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = Color.White,
                            fontSize = 14.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        ),
                        modifier = Modifier
                            .width(32.dp)
                            .focusRequester(focusRequester)
                            .onFocusChanged { focusState ->
                                if (focusState.isFocused) hasGainedFocus = true
                                if (!focusState.isFocused && hasGainedFocus && isEditing) {
                                    onValueChange(editValue.text.ifEmpty { "-" })
                                    isEditing = false
                                }
                            },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal,
                            imeAction = androidx.compose.ui.text.input.ImeAction.Done
                        ),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                            onDone = {
                                onValueChange(editValue.text.ifEmpty { "-" })
                                isEditing = false
                            }
                        ),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(Color.White)
                    )
                } else {
                    Text(
                        text = value,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                    )
                }
            }

            if (suffix.isNotEmpty()) {
                Text(
                    text = suffix,
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(end = 1.dp)
                )
            }

            // Increment (Up)
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .background(Color(0xFF0A2610), RoundedCornerShape(4.dp))
                    .border(1.dp, Color(0xFF2E7D32), RoundedCornerShape(4.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onIncrement() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowUp,
                    contentDescription = "Increment",
                    tint = Color(0xFF2E7D32),
                    modifier = Modifier.size(14.dp)
                )
            }

            // Decrement (Down)
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .background(Color(0xFF260A0A), RoundedCornerShape(4.dp))
                    .border(1.dp, Color(0xFFC62828), RoundedCornerShape(4.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onDecrement() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = "Decrement",
                    tint = Color(0xFFC62828),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
fun SetTypeShape(type: SetType) {
    when (type) {
        SetType.WARMUP -> {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(Color(0xFF0A2610), RoundedCornerShape(5.dp))
                    .border(1.dp, Color(0xFF2E7D32), RoundedCornerShape(5.dp))
            )
        }
        SetType.PRIMER -> {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(Color(0xFF26180A), RoundedCornerShape(2.dp))
                    .border(1.dp, Color(0xFFEF6C00), RoundedCornerShape(2.dp))
            )
        }
        SetType.WORKING -> {
            Canvas(modifier = Modifier.size(10.dp)) {
                val path = Path().apply {
                    moveTo(size.width / 2f, 0f)
                    lineTo(size.width, size.height)
                    lineTo(0f, size.height)
                    close()
                }
                drawPath(
                    path = path,
                    color = Color(0xFF260A0A)
                )
                drawPath(
                    path = path,
                    color = Color(0xFFC62828),
                    style = Stroke(width = 1.dp.toPx())
                )
            }
        }
    }
}

@Composable
fun ProgressChartScreen(
    innerPadding: PaddingValues,
    accentColor: Color,
    onBackClick: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val PROGRESS_DATA_KEY = stringPreferencesKey("progress_data")
    val SESSIONS_KEY = stringPreferencesKey("workout_sessions")
    val EXERCISES_KEY = stringPreferencesKey("session_exercises")
    
    val progressData = remember { mutableStateMapOf<String, Map<String, DayProgress>>() }
    val sessionExercisesMap = remember { mutableStateMapOf<String, List<Exercise>>() }
    val sessionsList = remember { mutableStateListOf<String>() }
    val selectedExercises = remember { mutableStateListOf<String>() }
    val expandedSessions = remember { mutableStateMapOf<String, Boolean>() }
    
    // Legend Interaction State
    var highlightedExercise by remember { mutableStateOf<String?>(null) }
    val highlightAlpha by animateFloatAsState(
        targetValue = if (highlightedExercise == null) 1f else 0.2f,
        animationSpec = tween(durationMillis = 300),
        label = "HighlightAlpha"
    )

    val cardGradient = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF1E1E1E),
                Color(0xFF161616)
            )
        )
    }

    LaunchedEffect(Unit) {
        val prefs = context.dataStore.data.first()
        
        val progressJson = prefs[PROGRESS_DATA_KEY] ?: "{}"
        try {
            val decoded = Json.decodeFromString<Map<String, Map<String, DayProgress>>>(progressJson)
            progressData.putAll(decoded)
        } catch (e: Exception) { e.printStackTrace() }

        val sessionsJson = prefs[SESSIONS_KEY] ?: "[]"
        try {
            val decoded = Json.decodeFromString<List<String>>(sessionsJson)
            sessionsList.addAll(decoded)
        } catch (e: Exception) { e.printStackTrace() }

        val exercisesJson = prefs[EXERCISES_KEY] ?: "{}"
        try {
            val decoded = Json.decodeFromString<Map<String, List<Exercise>>>(exercisesJson)
            sessionExercisesMap.putAll(decoded)
        } catch (e: Exception) { e.printStackTrace() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .verticalScroll(rememberScrollState())
    ) {
        BackHandler(enabled = true) {
            onBackClick()
        }

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Transparent
            ),
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxWidth()
                .background(
                    brush = cardGradient,
                    shape = RoundedCornerShape(20.dp)
                )
                .border(
                    width = 1.dp,
                    color = Color.Gray.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(20.dp)
                )
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Progress Chart",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (progressData.isEmpty()) {
                    Text(
                        text = "No progress data logged yet. Data is saved daily when you open the app.",
                        color = Color.Gray,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(vertical = 24.dp)
                    )
                } else {
                    val allExerciseNames = remember(progressData) { progressData.keys.sorted() }
                    
                    // Advanced Color Mapping Logic
                    val chartColors = remember(accentColor, progressData, selectedExercises) {
                        val colors = mutableMapOf<String, Color>()
                        if (progressData.isEmpty()) return@remember colors

                        val exerciseList = progressData.keys.toList()
                        val n = exerciseList.size
                        val allDates = progressData.values.flatMap { it.keys }.distinct().sorted()
                        
                        // 1. Pairwise Distance Metric
                        val distances = Array(n) { DoubleArray(n) }
                        for (i in 0 until n) {
                            for (j in i + 1 until n) {
                                val ex1 = progressData[exerciseList[i]]!!
                                val ex2 = progressData[exerciseList[j]]!!
                                
                                var sumSqDiff = 0.0
                                var count = 0
                                allDates.forEach { date ->
                                    val w1 = ex1[date]?.weight
                                    val w2 = ex2[date]?.weight
                                    if (w1 != null && w2 != null) {
                                        sumSqDiff += (w1 - w2) * (w1 - w2)
                                        count++
                                    }
                                }
                                val dist = if (count > 0) sqrt(sumSqDiff) / count else 1000.0
                                distances[i][j] = dist
                                distances[j][i] = dist
                            }
                        }

                        // 2. Maximal-Dissimilarity Seriation
                        val sequence = mutableListOf<Int>()
                        val remaining = (0 until n).toMutableSet()
                        
                        if (n > 0) {
                            sequence.add(0)
                            remaining.remove(0)
                            
                            while (remaining.isNotEmpty()) {
                                val last = sequence.last()
                                val next = remaining.maxByOrNull { r ->
                                    distances[r][last] + (sequence.sumOf { distances[r][it] } / sequence.size)
                                } ?: remaining.first()
                                
                                sequence.add(next)
                                remaining.remove(next)
                            }
                        }

                        // 3. Equidistant Color Wheel Mapping
                        sequence.forEachIndexed { seqIndex, exerciseIdx ->
                            val hue = (seqIndex.toFloat() / n.coerceAtLeast(1)) * 360f
                            val lightness = if (seqIndex % 2 == 0) 0.6f else 0.4f
                            colors[exerciseList[exerciseIdx]] = Color.hsl(hue, 0.8f, lightness)
                        }

                        if (n > 0) {
                            colors[exerciseList[sequence[0]]] = accentColor
                        }
                        
                        colors
                    }

                    // Legend - Fixed height with internal scroll and visible scrollbar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .padding(bottom = 8.dp)
                            .background(Color.Black.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .border(1.dp, Color.Gray.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                            .nestedScroll(remember {
                                object : NestedScrollConnection {
                                    override fun onPostScroll(
                                        consumed: Offset,
                                        available: Offset,
                                        source: NestedScrollSource
                                    ): Offset {
                                        return available
                                    }
                                }
                            })
                    ) {
                        if (selectedExercises.isNotEmpty()) {
                            val scrollState = rememberScrollState()
                            Column(modifier = Modifier.verticalScroll(scrollState).padding(end = 8.dp)) {
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    selectedExercises.forEach { name ->
                                        val color = chartColors[name] ?: Color.Gray
                                        Row(
                                            modifier = Modifier
                                                .pointerInput(name) {
                                                    awaitPointerEventScope {
                                                        while (true) {
                                                            awaitFirstDown()
                                                            highlightedExercise = name
                                                            waitForUpOrCancellation()
                                                            highlightedExercise = null
                                                        }
                                                    }
                                                },
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .background(color, RoundedCornerShape(2.dp))
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(text = name, color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                                        }
                                    }
                                }
                            }

                            if (scrollState.maxValue > 0) {
                                Canvas(
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd)
                                        .fillMaxHeight()
                                        .width(4.dp)
                                ) {
                                    val scrollPercentage = scrollState.value.toFloat() / scrollState.maxValue
                                    val barHeight = size.height * (size.height / (size.height + scrollState.maxValue))
                                    val topOffset = (size.height - barHeight) * scrollPercentage
                                    
                                    drawRoundRect(
                                        color = Color.Gray.copy(alpha = 0.4f),
                                        topLeft = Offset(0f, topOffset),
                                        size = androidx.compose.ui.geometry.Size(size.width, barHeight),
                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx())
                                    )
                                }
                            }
                        } else {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No exercises selected", color = Color.Gray, fontSize = 11.sp)
                            }
                        }
                    }

                    // Chart Area
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .background(Color(0xFF1E1E1E), RoundedCornerShape(12.dp))
                            .border(1.dp, Color.Gray.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        if (selectedExercises.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Select exercises to display progress", color = Color.Gray, fontSize = 12.sp)
                            }
                        } else {
                            ProgressLineChart(
                                progressData = progressData.filterKeys { it in selectedExercises },
                                chartColorMap = chartColors,
                                highlightedExercise = highlightedExercise,
                                otherAlpha = highlightAlpha
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Select Exercises",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                        
                        val allSelected = selectedExercises.size == progressData.size
                        TextButton(
                            onClick = {
                                if (allSelected) {
                                    selectedExercises.clear()
                                } else {
                                    selectedExercises.clear()
                                    selectedExercises.addAll(allExerciseNames)
                                }
                            }
                        ) {
                            Text(
                                text = if (allSelected) "Deselect All" else "Select All",
                                color = accentColor,
                                fontSize = 12.sp
                            )
                        }
                    }

                    // Checklist grouped by session
                    sessionsList.forEach { sessionName ->
                        val exercisesInSession = sessionExercisesMap[sessionName] ?: emptyList()
                        val validExercises = exercisesInSession.map { it.name }.filter { it in progressData }
                        
                        if (validExercises.isNotEmpty()) {
                            val isExpanded = expandedSessions[sessionName] ?: false
                            val selectedCountInSession = validExercises.count { selectedExercises.contains(it) }
                            val allInSessionSelected = selectedCountInSession == validExercises.size
                            val someInSessionSelected = selectedCountInSession > 0

                            Column(modifier = Modifier.fillMaxWidth()) {
                                // Session Header
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { expandedSessions[sessionName] = !isExpanded }
                                        .padding(vertical = 0.dp), // Reduced from 4.dp
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = Color.Gray,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Checkbox(
                                        checked = allInSessionSelected,
                                        onCheckedChange = { checked ->
                                            validExercises.forEach { ex ->
                                                if (checked) {
                                                    if (!selectedExercises.contains(ex)) selectedExercises.add(ex)
                                                } else {
                                                    selectedExercises.remove(ex)
                                                }
                                            }
                                        },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = accentColor,
                                            uncheckedColor = Color.Gray,
                                            checkmarkColor = if (accentColor == Color.White) Color.Black else Color.White
                                        ),
                                        modifier = Modifier.scale(0.85f)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = sessionName,
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "$selectedCountInSession/${validExercises.size}",
                                        color = Color.Gray,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                }

                                // Exercises List (Collapsible)
                                AnimatedVisibility(
                                    visible = isExpanded,
                                    enter = expandVertically() + fadeIn(),
                                    exit = shrinkVertically() + fadeOut()
                                ) {
                                    Column(modifier = Modifier.padding(start = 32.dp)) {
                                        validExercises.forEach { exerciseName ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        if (selectedExercises.contains(exerciseName)) {
                                                            selectedExercises.remove(exerciseName)
                                                        } else {
                                                            selectedExercises.add(exerciseName)
                                                        }
                                                    }
                                                    .padding(vertical = 0.dp), // Reduced from 2.dp
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Checkbox(
                                                    checked = selectedExercises.contains(exerciseName),
                                                    onCheckedChange = { checked ->
                                                        if (checked) selectedExercises.add(exerciseName)
                                                        else selectedExercises.remove(exerciseName)
                                                    },
                                                    colors = CheckboxDefaults.colors(
                                                        checkedColor = accentColor,
                                                        uncheckedColor = Color.Gray,
                                                        checkmarkColor = if (accentColor == Color.White) Color.Black else Color.White
                                                    ),
                                                    modifier = Modifier.scale(0.85f)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(text = exerciseName, color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    // Handle exercises that might not be in any current session but have progress data
                    val exercisesInSessions = sessionExercisesMap.values.flatten().map { it.name }.toSet()
                    val orphanedExercises = allExerciseNames.filter { it !in exercisesInSessions }
                    
                    if (orphanedExercises.isNotEmpty()) {
                        val sessionName = "Other"
                        val isExpanded = expandedSessions[sessionName] ?: false
                        val selectedCountInSession = orphanedExercises.count { selectedExercises.contains(it) }
                        val allInSessionSelected = selectedCountInSession == orphanedExercises.size

                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { expandedSessions[sessionName] = !isExpanded }
                                    .padding(vertical = 0.dp), // Reduced from 4.dp
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(20.dp)
                                )
                                Checkbox(
                                    checked = allInSessionSelected,
                                    onCheckedChange = { checked ->
                                        orphanedExercises.forEach { ex ->
                                            if (checked) {
                                                if (!selectedExercises.contains(ex)) selectedExercises.add(ex)
                                            } else {
                                                selectedExercises.remove(ex)
                                            }
                                        }
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = accentColor,
                                        uncheckedColor = Color.Gray,
                                        checkmarkColor = if (accentColor == Color.White) Color.Black else Color.White
                                    ),
                                    modifier = Modifier.scale(0.85f)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = sessionName,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "$selectedCountInSession/${orphanedExercises.size}",
                                    color = Color.Gray,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                            }

                            AnimatedVisibility(
                                visible = isExpanded,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Column(modifier = Modifier.padding(start = 32.dp)) {
                                    orphanedExercises.forEach { exerciseName ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    if (selectedExercises.contains(exerciseName)) {
                                                        selectedExercises.remove(exerciseName)
                                                    } else {
                                                        selectedExercises.add(exerciseName)
                                                    }
                                                }
                                                .padding(vertical = 0.dp), // Reduced from 2.dp
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(
                                                checked = selectedExercises.contains(exerciseName),
                                                onCheckedChange = { checked ->
                                                    if (checked) selectedExercises.add(exerciseName)
                                                    else selectedExercises.remove(exerciseName)
                                                },
                                                colors = CheckboxDefaults.colors(
                                                    checkedColor = accentColor,
                                                    uncheckedColor = Color.Gray,
                                                    checkmarkColor = if (accentColor == Color.White) Color.Black else Color.White
                                                ),
                                                modifier = Modifier.scale(0.85f)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(text = exerciseName, color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DebugScreen(
    innerPadding: PaddingValues,
    accentColor: Color,
    onBackClick: () -> Unit,
    onClearProgressData: () -> Unit,
    onSetLastProcessDate: (String) -> Unit,
    onManualSaveProgress: (String) -> Unit
) {
    var dateInput by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Debug Controls", fontSize = 24.sp, color = Color.White, fontWeight = FontWeight.Bold)

        OutlinedTextField(
            value = dateInput,
            onValueChange = { dateInput = it },
            label = { Text("Target Date (yyyy-MM-dd)", color = Color.Gray) },
            textStyle = androidx.compose.ui.text.TextStyle(color = Color.White),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accentColor,
                unfocusedBorderColor = Color.Gray
            )
        )

        Button(
            onClick = { onManualSaveProgress(dateInput) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = accentColor)
        ) {
            Text("Save Current Data for Date", color = Color.Black)
        }

        Button(
            onClick = { onSetLastProcessDate(dateInput) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
        ) {
            Text("Set Last Process Date to Input", color = Color.White)
        }

        Button(
            onClick = onClearProgressData,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB00020))
        ) {
            Text("Clear All Progress Data", color = Color.White)
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onBackClick,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            border = BorderStroke(1.dp, Color.Gray)
        ) {
            Text("Back", color = Color.White)
        }
    }
}

@Composable
fun ProgressLineChart(
    progressData: Map<String, Map<String, DayProgress>>,
    chartColorMap: Map<String, Color>,
    highlightedExercise: String? = null,
    otherAlpha: Float = 1f
) {
    val allDates = progressData.values.flatMap { it.keys }.distinct().sorted()
    if (allDates.isEmpty()) return

    // Transform State
    var scaleX by remember { mutableFloatStateOf(1f) }
    var scaleY by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    
    // Size for boundaries
    var chartSize by remember { mutableStateOf(IntSize.Zero) }
    val paddingLeft = 60f
    val paddingRight = 20f
    val paddingTop = 20f
    val paddingBottom = 60f

    val state = rememberTransformableState { zoomChange, panChange, _ ->
        scaleX = (scaleX * zoomChange).coerceIn(1f, 10f)
        scaleY = (scaleY * zoomChange).coerceIn(1f, 10f)
        
        val width = chartSize.width.toFloat()
        val height = chartSize.height.toFloat()
        val chartWidth = (width - paddingLeft - paddingRight).coerceAtLeast(0f)
        val chartHeight = (height - paddingTop - paddingBottom).coerceAtLeast(0f)
        val totalXWidth = chartWidth * scaleX
        val totalYHeight = chartHeight * scaleY

        offsetX = (offsetX + panChange.x).coerceIn(-(totalXWidth - chartWidth), 0f)
        offsetY = (offsetY + panChange.y).coerceIn(0f, (totalYHeight - chartHeight).coerceAtLeast(0f))
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { chartSize = it }
            .transformable(state = state)
            .nestedScroll(remember {
                object : NestedScrollConnection {
                    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                        // When interacting with graph, prevent parent scrolling
                        return Offset(0f, available.y)
                    }
                }
            })
    ) {
        val width = size.width
        val height = size.height

        val minWeightRaw = progressData.values.flatMap { it.values }.minOfOrNull { it.weight } ?: 0.0
        val maxWeightRaw = progressData.values.flatMap { it.values }.maxOfOrNull { it.weight } ?: 100.0
        
        val minWeight = minWeightRaw
        val maxWeight = maxWeightRaw
        val weightRange = (maxWeight - minWeight).coerceAtLeast(1.0)

        val chartWidth = width - paddingLeft - paddingRight
        val chartHeight = height - paddingTop - paddingBottom

        // Apply Zoom and Pan to the coordinate system
        val totalXWidth = chartWidth * scaleX

        val xStep = if (allDates.size > 1) totalXWidth / (allDates.size - 1) else totalXWidth
        
        val labelPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.GRAY
            textSize = 24f
            textAlign = android.graphics.Paint.Align.RIGHT
        }
        
        val gridSteps = (10 * scaleY).toInt().coerceIn(5, 50)
        // Use a cleaner increment based on range and zoom
        val weightPerStep = if (scaleY > 2f) {
            // Finer increments when zoomed in
            val ideal = weightRange / gridSteps
            if (ideal < 0.5) 0.5 else if (ideal < 1.0) 1.0 else if (ideal < 2.5) 2.5 else 5.0
        } else {
            // Clean step for non-zoomed view (e.g., nearest 5, 10, or 20)
            val step = weightRange / 10.0
            if (step < 2.5) 2.5 else if (step < 5.0) 5.0 else if (step < 10.0) 10.0 else 20.0
        }
        
        // Calculate visible weight range for labels
        val drawnLabels = mutableSetOf<String>()
        
        // Draw Min and Max labels first to ensure they are always present
        val minMaxWeights = listOf(minWeight, maxWeight)
        minMaxWeights.forEach { weightVal ->
            val y = paddingTop + chartHeight - (((weightVal - minWeight) / weightRange * chartHeight) * scaleY).toFloat() + offsetY
            if (y in paddingTop..(height - paddingBottom + 30f)) {
                val labelText = String.format(Locale.getDefault(), "%.1f", weightVal)
                if (!drawnLabels.contains(labelText)) {
                    drawContext.canvas.nativeCanvas.drawText(
                        labelText,
                        paddingLeft - 10f,
                        y + 8f,
                        labelPaint
                    )
                    drawnLabels.add(labelText)
                }
            }
        }

        for (i in -50..(gridSteps + 100)) { // Extended range for panning
            val weightValRaw = minWeight + (i * weightPerStep)
            // Round to nearest weightPerStep to ensure logical values
            val weightVal = (Math.round(weightValRaw / weightPerStep) * weightPerStep)
            
            // Ensure we only draw labels that make sense for the current viewport
            val y = paddingTop + chartHeight - (((weightVal - minWeight) / weightRange * chartHeight) * scaleY).toFloat() + offsetY
            
            if (y in paddingTop..(height - paddingBottom + 30f)) {
                drawLine(
                    color = Color.Gray.copy(alpha = 0.15f),
                    start = Offset(paddingLeft, y),
                    end = Offset(width - paddingRight, y),
                    strokeWidth = 1f
                )
                
                val labelText = String.format(Locale.getDefault(), "%.1f", weightVal)
                // Avoid overlapping labels by checking proximity
                val isCloseToAny = drawnLabels.any { existing ->
                    val existingVal = existing.toDoubleOrNull() ?: -1.0
                    Math.abs(existingVal - weightVal) < (weightPerStep * 0.5)
                }

                if (!isCloseToAny) {
                    drawContext.canvas.nativeCanvas.drawText(
                        labelText,
                        paddingLeft - 10f,
                        y + 8f,
                        labelPaint
                    )
                    drawnLabels.add(labelText)
                }
            }
        }

        // X-axis labels (Dates)
        val xLabelPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.GRAY
            textSize = 20f
            textAlign = android.graphics.Paint.Align.CENTER
        }
        
        allDates.forEachIndexed { index, dateStr ->
            val x = paddingLeft + index * xStep + offsetX
            
            if (x in (paddingLeft - 50f)..(width - paddingRight + 50f)) {
                // Format date
                val displayDate = try {
                    val parts = dateStr.split("-")
                    if (parts.size >= 3) "${parts[1]}/${parts[2]}" else dateStr
                } catch (e: Exception) { dateStr }
                
                // Show label only if there is enough space or it's a major step
                val firstVisibleIdx = ((-offsetX) / xStep).toInt().coerceAtLeast(0)
                val lastVisibleIdx = ((chartWidth - offsetX) / xStep).toInt().coerceAtMost(allDates.size - 1)
                val visibleCount = lastVisibleIdx - firstVisibleIdx + 1

                val labelFrequency = when {
                    visibleCount <= 7 -> 1
                    scaleX > 4f -> 1
                    scaleX > 2f -> 2
                    scaleX > 1.5f -> 3
                    else -> 5
                }
                
                if (index == 0 || index == allDates.size - 1 || index % labelFrequency == 0) {
                    drawContext.canvas.nativeCanvas.drawText(
                        displayDate,
                        x,
                        height - 10f,
                        xLabelPaint
                    )
                }
                
                // Vertical grid line
                drawLine(
                    color = Color.Gray.copy(alpha = 0.1f),
                    start = Offset(x, paddingTop),
                    end = Offset(x, height - paddingBottom),
                    strokeWidth = 1f
                )
            }
        }
        
        // Clip for plots (lines and circles) - keep them within the chart area
        drawContext.canvas.save()
        drawContext.canvas.nativeCanvas.clipRect(
            paddingLeft, paddingTop, width - paddingRight, height - paddingBottom
        )

        progressData.entries.forEach { entry ->
            val exerciseName = entry.key
            val exerciseData = entry.value
            val baseColor = chartColorMap[exerciseName] ?: Color.Gray
            val isHighlighted = highlightedExercise == exerciseName
            val currentAlpha = if (highlightedExercise == null || isHighlighted) 1f else otherAlpha
            val color = baseColor.copy(alpha = currentAlpha)
            val path = Path()
            
            var firstPoint = true
            allDates.forEachIndexed { dateIndex, dateStr ->
                val progress = exerciseData[dateStr]
                if (progress != null) {
                    val x = paddingLeft + dateIndex * xStep + offsetX
                    val y = paddingTop + chartHeight - (((progress.weight - minWeight) / weightRange * chartHeight) * scaleY).toFloat() + offsetY
                    
                    if (firstPoint) {
                        path.moveTo(x, y)
                        firstPoint = false
                    } else {
                        path.lineTo(x, y)
                    }
                    
                    // Always draw circles (clipping will handle overflow)
                    drawCircle(color = color, radius = 6f, center = Offset(x, y))
                }
            }
            
            drawPath(
                path = path,
                color = color,
                style = Stroke(width = 4f, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round)
            )
        }

        drawContext.canvas.restore()
    }
}

private data class Star(
    val x: Float,          // 0f..1f, relative horizontal position
    val y: Float,          // 0f..1f, relative vertical position
    val radius: Float,     // px, base size
    val baseAlpha: Float,  // base brightness
    val twinkleSpeed: Float,   // how fast it pulses
    val twinklePhase: Float,   // phase offset so stars don't pulse in sync
    val driftSpeed: Float,     // px/sec drift (parallax layer speed)
    val colorTint: Color        // slight color variation (white/blue/yellow)
)

/**
 * @param starCount total number of stars across all depth layers
 * @param layers number of parallax depth layers (more = more realistic depth)
 * @param driftAngleDegrees direction stars drift in, e.g. 90f = straight down,
 *   0f = rightward. Use a small nonzero value for a subtle "flying through space" feel.
 * @param baseSpeedPxPerSec drift speed of the nearest (fastest) layer; farther
 *   layers automatically move slower for parallax depth.
 * @param backgroundGradient the deep-space background gradient colors
 */
@Composable
fun StarryBackground(
    modifier: Modifier = Modifier,
    starCount: Int = 2000,
    layers: Int = 5,
    driftAngleDegrees: Float = 45f,
    baseSpeedPxPerSec: Float = 24f,
    backgroundGradient: List<Color> = listOf(
        Color(0xFF000000),
        Color(0xFF000000)
    )
) {
    // Generate stable star field once per composition (not on every recomposition)
    val stars = remember(starCount, layers) {
        val random = Random(System.nanoTime())
        List(starCount) { index ->
            val layer = index % layers
            // Farther layers (higher index) -> smaller, dimmer, slower
            val depthFactor = 1f - (layer.toFloat() / layers) * 0.75f

            val tint = when (random.nextInt(6)) {
                0 -> Color(0xFFBFD7FF) // cool blue-white
                1 -> Color(0xFFFFF4D6) // warm yellowish
                else -> Color.White
            }

            Star(
                x = random.nextFloat(),
                y = random.nextFloat(),
                radius = (0.8f + random.nextFloat() * 3f) * depthFactor,
                baseAlpha = (0.35f + random.nextFloat() * 0.65f) * depthFactor,
                twinkleSpeed = 0.5f + random.nextFloat() * 1.8f,
                twinklePhase = random.nextFloat() * 6.2831855f,
                driftSpeed = baseSpeedPxPerSec * depthFactor * (0.5f + random.nextFloat()),
                colorTint = tint
            )
        }
    }

    // Single infinite time driver in seconds, looping cleanly to avoid float overflow
    val infiniteTransition = rememberInfiniteTransition(label = "starfield_time")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1_000_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time"
    )

    val angleRad = Math.toRadians(driftAngleDegrees.toDouble())
    val dirX = kotlin.math.cos(angleRad).toFloat()
    val dirY = kotlin.math.sin(angleRad).toFloat()

    Box(
        modifier = modifier
            .background(Brush.verticalGradient(backgroundGradient))
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                // graphicsLayer + CompositingStrategy improves blending quality for glow
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        ) {
            val w = size.width
            val h = size.height

            stars.forEach { star ->
                drawStar(star, time, w, h, dirX, dirY)
            }
        }
    }
}

private fun DrawScope.drawStar(
    star: Star,
    time: Float,
    width: Float,
    height: Float,
    dirX: Float,
    dirY: Float
) {
    // Base position in pixels
    val baseX = star.x * width
    val baseY = star.y * height

    // Drift offset, wrapped around screen edges (toroidal wrap for seamless looping)
    val driftX = dirX * star.driftSpeed * time
    val driftY = dirY * star.driftSpeed * time

    var px = (baseX + driftX).mod(width)
    var py = (baseY + driftY).mod(height)
    if (px < 0f) px += width
    if (py < 0f) py += height

    // Twinkle: smooth sinusoidal brightness pulse, phase-shifted per star
    val twinkle = 0.6f + 0.4f * sin(time * star.twinkleSpeed + star.twinklePhase)
    val alpha = (star.baseAlpha * twinkle).coerceIn(0.05f, 1f)

    // Soft outer glow for brighter/nearer stars (adds realism)
    if (star.radius > 1.2f) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    star.colorTint.copy(alpha = alpha * 0.35f),
                    star.colorTint.copy(alpha = 0f)
                ),
                center = Offset(px, py),
                radius = star.radius * 4f
            ),
            radius = star.radius * 4f,
            center = Offset(px, py)
        )
    }

    // Sharp core
    drawCircle(
        color = star.colorTint.copy(alpha = alpha),
        radius = star.radius,
        center = Offset(px, py)
    )
}
