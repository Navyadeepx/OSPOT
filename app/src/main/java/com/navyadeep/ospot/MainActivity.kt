package com.navyadeep.ospot

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.navyadeep.ospot.ui.theme.OSPOTTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.Serializable

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
data class Exercise(val name: String, val sets: List<WorkoutSet> = emptyList())

class MainActivity : ComponentActivity() {

    private val SHOW_RPE_KEY = booleanPreferencesKey("show_rpe")
    private val SHOW_RIR_KEY = booleanPreferencesKey("show_rir")
    private val INCREMENT_KEY = stringPreferencesKey("weight_increment")
    private val SESSIONS_KEY = stringPreferencesKey("workout_sessions")
    private val EXERCISES_KEY = stringPreferencesKey("session_exercises")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val scope = rememberCoroutineScope()

            var currentScreen by remember { mutableStateOf("workout_log") }
            var showRpe by remember { mutableStateOf(true) }
            var showRir by remember { mutableStateOf(true) }
            var weightIncrement by remember { mutableStateOf("2.5") }
            val sessions = remember { mutableStateListOf<String>() }
            var selectedSession by remember { mutableStateOf("") }
            var menuExpanded by remember { mutableStateOf(false) }
            var isEditMode by remember { mutableStateOf(false) }

            var showAddSessionDialog by remember { mutableStateOf(false) }
            var newSessionNameText by remember { mutableStateOf("") }
            var showAddExerciseDialog by remember { mutableStateOf(false) }
            var newExerciseNameText by remember { mutableStateOf("") }
            val sessionExercises = remember { mutableStateMapOf<String, androidx.compose.runtime.snapshots.SnapshotStateList<Exercise>>() }

            LaunchedEffect(Unit) {
                val prefs = dataStore.data.first()
                showRpe = prefs[SHOW_RPE_KEY] ?: true
                showRir = prefs[SHOW_RIR_KEY] ?: true
                weightIncrement = prefs[INCREMENT_KEY] ?: "2.5"

                val savedSessionsJson = prefs[SESSIONS_KEY] ?: ""
                if (savedSessionsJson.isNotEmpty()) {
                    try {
                        val decodedList = Json.decodeFromString<List<String>>(savedSessionsJson)
                        sessions.clear()
                        sessions.addAll(decodedList)
                        if (sessions.isNotEmpty()) {
                            selectedSession = sessions.first()
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
                            sessionExercises[key] = mutableStateListOf<Exercise>().apply { addAll(value) }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
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

            val saveIncrement: (String) -> Unit = { value ->
                weightIncrement = value
                scope.launch { dataStore.edit { it[INCREMENT_KEY] = value } }
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

            OSPOTTheme {
                WorkoutAppScreen(
                    currentScreen = currentScreen,
                    onScreenChange = { currentScreen = it },
                    showRpe = showRpe,
                    onRpeChange = saveRpe,
                    showRir = showRir,
                    onRirChange = saveRir,
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
                    onUpdateSet = { exerciseIndex, setIndex, updatedSet ->
                        if (selectedSession.isNotEmpty()) {
                            val list = sessionExercises[selectedSession]
                            if (list != null && exerciseIndex in list.indices) {
                                val exercise = list[exerciseIndex]
                                if (setIndex in exercise.sets.indices) {
                                    val updatedSets = exercise.sets.toMutableList()
                                    updatedSets[setIndex] = updatedSet
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
                    onEditModeChange = { isEditMode = it }
                )
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
    onUpdateSet: (Int, Int, WorkoutSet) -> Unit,
    onRemoveExercise: (Int) -> Unit,
    onRemoveSet: (Int, Int) -> Unit,
    onRemoveSession: (String) -> Unit,
    onRenameExercise: (Int, String) -> Unit,
    onMoveExercise: (Int, Int) -> Unit,
    isEditMode: Boolean,
    onEditModeChange: (Boolean) -> Unit
) {
    var showDeleteExerciseDialog by remember { mutableStateOf(false) }
    var exerciseIndexToDelete by remember { mutableIntStateOf(-1) }

    var showDeleteSessionDialog by remember { mutableStateOf(false) }
    var sessionToDelete by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 1.0f),
                                Color.Transparent
                            )
                        )
                    )
            ) {
                TopAppBar(
                    title = {
                        Box {
                            Row(
                                modifier = Modifier
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        onMenuToggle(!menuExpanded)
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "OSPOT",
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = FontWeight.Black
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
                                shape = RoundedCornerShape(12.dp),
                                containerColor = Color(0xFF1E1E1E),
                                modifier = Modifier
                                    .width(85.dp)
                                    .border(1.dp, Color.Gray.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Settings", color = Color.White, fontSize = 16.sp) },
                                    onClick = {
                                        onScreenChange("settings")
                                        onMenuToggle(false)
                                    },
                                    modifier = Modifier.height(42.dp).padding(horizontal = 4.dp)
                                )
                                DropdownMenuItem(
                                    text = { Text("Edit", color = Color.White, fontSize = 16.sp) },
                                    onClick = {
                                        onEditModeChange(true)
                                        onMenuToggle(false)
                                    },
                                    modifier = Modifier.height(42.dp).padding(horizontal = 4.dp)
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    ),
                    actions = {
                        if (currentScreen == "settings") {
                            IconButton(onClick = { onScreenChange("workout_log") }) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Exit Settings",
                                    tint = Color.White
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
                            IconButton(onClick = {
                                if (isEditMode) {
                                    onEditModeChange(false)
                                } else {
                                    onShowExerciseDialogChange(true)
                                }
                            }) {
                                Icon(
                                    imageVector = if (isEditMode) Icons.Filled.Close else Icons.Filled.Add,
                                    contentDescription = if (isEditMode) "Exit Edit Mode" else "New",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                )
            }
        },
        bottomBar = {
            if (currentScreen == "workout_log") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 1.0f)
                                )
                            )
                        )
                ) {
                    Surface(
                        color = Color.Transparent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(55.dp)
                            .navigationBarsPadding()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp)
                                .horizontalScroll(rememberScrollState()),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start
                        ) {
                            sessions.forEach { session ->
                                val isSelected = selectedSession == session

                                Box(
                                    modifier = Modifier
                                        .padding(end = 8.dp)
                                        .background(
                                            color = if (isSelected) Color(0xFF242424) else Color(0xFF161616),
                                            shape = RoundedCornerShape(20.dp)
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) Color.White.copy(alpha = 0.4f) else Color.Gray.copy(alpha = 0.25f),
                                            shape = RoundedCornerShape(20.dp)
                                        )
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
                                            color = if (isSelected) Color.White else Color.Gray,
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
                                    .background(Color(0xFF161616), shape = RoundedCornerShape(50.dp))
                                    .border(width = 1.dp, color = Color.Gray.copy(alpha = 0.25f), shape = RoundedCornerShape(50.dp))
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
        Box(modifier = Modifier.fillMaxSize()) {
            if (currentScreen == "workout_log") {
                if (selectedSession.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Create a session to begin logging",
                            fontSize = 18.sp,
                            color = Color.Gray
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Spacer(modifier = Modifier.height(innerPadding.calculateTopPadding()))
                        exercises.forEachIndexed { index, exercise ->
                            ExerciseBox(
                                name = exercise.name,
                                sets = exercise.sets,
                                isEditMode = isEditMode,
                                showRpe = showRpe,
                                showRir = showRir,
                                weightIncrement = weightIncrement,
                                onAddSet = { type -> onAddSet(index, type) },
                                onUpdateSet = { setIndex, updatedSet -> onUpdateSet(index, setIndex, updatedSet) },
                                onRemoveSet = { setIndex -> onRemoveSet(index, setIndex) },
                                onRename = { newName -> onRenameExercise(index, newName) },
                                onMoveUp = if (index > 0) { { onMoveExercise(index, index - 1) } } else null,
                                onMoveDown = if (index < exercises.size - 1) { { onMoveExercise(index, index + 1) } } else null,
                                onDeleteClick = {
                                    exerciseIndexToDelete = index
                                    showDeleteExerciseDialog = true
                                }
                            )
                        }
                        Spacer(modifier = Modifier.height(innerPadding.calculateBottomPadding()))
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
                        increment = weightIncrement,
                        onIncrementChange = onIncrementChange,
                        onBackClick = { onScreenChange("workout_log") }
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
                    Text("Delete", color = Color.Red, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
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
                    Text("Delete", color = Color.Red, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
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
fun SettingsScreen(
    innerPadding: PaddingValues,
    showRpe: Boolean,
    onRpeChange: (Boolean) -> Unit,
    showRir: Boolean,
    onRirChange: (Boolean) -> Unit,
    increment: String,
    onIncrementChange: (String) -> Unit,
    onBackClick: () -> Unit
){
    var incrementMenuExpanded by remember { mutableStateOf(false) }

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
                containerColor = Color(0xFF161616)
            ),
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxWidth()
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
                            containerColor = Color(0xFF1E1E1E),
                            modifier = Modifier
                                .width(100.dp)
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
            }
        }
    }
}

@Composable
fun ExerciseBox(
    name: String,
    sets: List<WorkoutSet>,
    isEditMode: Boolean,
    showRpe: Boolean,
    showRir: Boolean,
    weightIncrement: String,
    onAddSet: (SetType) -> Unit,
    onUpdateSet: (Int, WorkoutSet) -> Unit,
    onRemoveSet: (Int) -> Unit,
    onRename: (String) -> Unit,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
    onDeleteClick: () -> Unit
) {
    var addSetMenuExpanded by remember { mutableStateOf(false) }
    var isEditingName by remember { mutableStateOf(false) }
    var editNameValue by remember { mutableStateOf(TextFieldValue(name)) }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(isEditingName) {
        if (isEditingName) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF161616)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = Color.Gray.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(16.dp)
                )
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
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
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
                                    Modifier
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
                            }
                        }
                    }
                }

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
                                    onValueChange = { onUpdateSet(index, set.copy(weight = it)) },
                                    onIncrement = {
                                        val current = set.weight.toDoubleOrNull() ?: 0.0
                                        val inc = weightIncrement.toDoubleOrNull() ?: 2.5
                                        val next = current + inc
                                        onUpdateSet(index, set.copy(weight = if (next % 1.0 == 0.0) next.toInt().toString() else next.toString()))
                                    },
                                    onDecrement = {
                                        val current = set.weight.toDoubleOrNull() ?: 0.0
                                        val inc = weightIncrement.toDoubleOrNull() ?: 2.5
                                        val next = (current - inc).coerceAtLeast(0.0)
                                        onUpdateSet(index, set.copy(weight = if (next % 1.0 == 0.0) next.toInt().toString() else next.toString()))
                                    },
                                    modifier = Modifier.weight(1f)
                                )

                                Spacer(modifier = Modifier.width(30.dp))

                                // Reps
                                SetValueEditor(
                                    label = "Reps",
                                    value = set.reps,
                                    labelWidth = 24.dp,
                                    onValueChange = { onUpdateSet(index, set.copy(reps = it)) },
                                    onIncrement = {
                                        val current = set.reps.toIntOrNull() ?: 0
                                        onUpdateSet(index, set.copy(reps = (current + 1).toString()))
                                    },
                                    onDecrement = {
                                        val current = set.reps.toIntOrNull() ?: 0
                                        onUpdateSet(index, set.copy(reps = (current - 1).coerceAtLeast(0).toString()))
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            if (showRir || showRpe) {
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
                                            onValueChange = { onUpdateSet(index, set.copy(rir = it)) },
                                            onIncrement = {
                                                val current = set.rir.toIntOrNull() ?: 0
                                                onUpdateSet(index, set.copy(rir = (current + 1).toString()))
                                            },
                                            onDecrement = {
                                                val current = set.rir.toIntOrNull() ?: 0
                                                onUpdateSet(index, set.copy(rir = (current - 1).coerceAtLeast(0).toString()))
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
                                            onValueChange = { onUpdateSet(index, set.copy(rpe = it)) },
                                            onIncrement = {
                                                val current = set.rpe.toDoubleOrNull() ?: 0.0
                                                val next = (current + 0.5).coerceAtMost(10.0)
                                                onUpdateSet(index, set.copy(rpe = if (next % 1.0 == 0.0) next.toInt().toString() else next.toString()))
                                            },
                                            onDecrement = {
                                                val current = set.rpe.toDoubleOrNull() ?: 0.0
                                                val next = (current - 0.5).coerceAtLeast(0.0)
                                                onUpdateSet(index, set.copy(rpe = if (next % 1.0 == 0.0) next.toInt().toString() else next.toString()))
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
            } else {
                Text(
                    text = "+ Add set",
                    color = Color.Gray.copy(alpha = 0.7f),
                    fontSize = 13.sp,
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
                    shape = RoundedCornerShape(12.dp),
                    containerColor = Color(0xFF1E1E1E),
                    modifier = Modifier
                        .width(180.dp)
                        .border(1.dp, Color.Gray.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
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
                        }
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
                        }
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
                        }
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
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(isEditing) {
        if (isEditing) {
            focusRequester.requestFocus()
            keyboardController?.show()
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
                            .focusRequester(focusRequester),
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
