package com.navyadeep.ospot

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.ui.graphics.Color
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

val Context.dataStore by preferencesDataStore(name = "ospot_settings")

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
            val sessionExercises = remember { mutableStateMapOf<String, androidx.compose.runtime.snapshots.SnapshotStateList<String>>() }

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
                        val decodedMap = Json.decodeFromString<Map<String, List<String>>>(savedExercisesJson)
                        decodedMap.forEach { (key, value) ->
                            sessionExercises[key] = mutableStateListOf<String>().apply { addAll(value) }
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
                    exercises = sessionExercises[selectedSession] ?: emptyList(),
                    onAddExercise = {
                        if (selectedSession.isNotEmpty()) {
                            val list = sessionExercises.getOrPut(selectedSession) { mutableStateListOf() }
                            list.add("Empty Box")
                            saveExercises()
                        }
                    },
                    onRemoveExercise = { index ->
                        if (selectedSession.isNotEmpty()) {
                            sessionExercises[selectedSession]?.removeAt(index)
                            saveExercises()
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
    exercises: List<String>,
    onAddExercise: () -> Unit,
    onRemoveExercise: (Int) -> Unit,
    onRemoveSession: (String) -> Unit,
    isEditMode: Boolean,
    onEditModeChange: (Boolean) -> Unit
) {
    var showDeleteExerciseDialog by remember { mutableStateOf(false) }
    var exerciseIndexToDelete by remember { mutableIntStateOf(-1) }

    var showDeleteSessionDialog by remember { mutableStateOf(false) }
    var sessionToDelete by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
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
                            Text(text = "OSPOT")
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
                                onAddExercise()
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
        },
        bottomBar = {
            if (currentScreen == "workout_log") {
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
                                                    .background(Color.Red, RoundedCornerShape(1.dp))
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
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (currentScreen == "workout_log") {
                if (selectedSession.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
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
                            .padding(innerPadding)
                            .verticalScroll(rememberScrollState())
                    ) {
                        exercises.forEachIndexed { index, _ ->
                            ExerciseBox(
                                isEditMode = isEditMode,
                                onDeleteClick = {
                                    exerciseIndexToDelete = index
                                    showDeleteExerciseDialog = true
                                }
                            )
                        }
                    }
                }
            }

            if (currentScreen == "settings") {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    SettingsScreen(
                        innerPadding = PaddingValues(0.dp),
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
fun ExerciseBox(isEditMode: Boolean, onDeleteClick: () -> Unit) {
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Empty Exercise",
                    color = Color.Gray.copy(alpha = 0.5f),
                    fontSize = 14.sp
                )
            }
        }

        if (isEditMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 4.dp, end = 8.dp)
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
        }
    }
}