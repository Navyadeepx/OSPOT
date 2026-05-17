package com.navyadeep.ospot

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OSPOTTheme {
                WorkoutAppScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutAppScreen() {
    var currentScreen by remember { mutableStateOf("workout_log") }
    var showRpe by remember { mutableStateOf(true) }
    var showRir by remember { mutableStateOf(true) }
    // Repurposed state variable to track default weight step instead of base unit
    var weightIncrement by remember { mutableStateOf("2.5") }
    var menuExpanded by remember { mutableStateOf(false) }

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
                                    menuExpanded = !menuExpanded
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "OSPOT")
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = if (menuExpanded) {
                                    Icons.Filled.KeyboardArrowUp
                                } else {
                                    Icons.Filled.KeyboardArrowDown
                                },
                                contentDescription = "Toggle Menu"
                            )
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            shape = RoundedCornerShape(12.dp),
                            containerColor = Color(0xFF1E1E1E),
                            modifier = Modifier
                                .width(85.dp)
                                .border(1.dp, Color.Gray.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        ) {
                            DropdownMenuItem(
                                text = { Text("Settings", color = Color.White, fontSize = 16.sp) },
                                onClick = {
                                    currentScreen = "settings"
                                    menuExpanded = false
                                },
                                modifier = Modifier.height(42.dp).padding(horizontal = 4.dp)
                            )
                            DropdownMenuItem(
                                text = { Text("Edit", color = Color.White, fontSize = 16.sp) },
                                onClick = { menuExpanded = false },
                                modifier = Modifier.height(42.dp).padding(horizontal = 4.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                ),
                actions = {
                    IconButton(onClick = { /* TODO: Add/New action */ }) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "New"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        if (currentScreen == "workout_log") {
            // --- WORKOUT LOG PAGE ---
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Main Workout Log Screen",
                    modifier = Modifier.padding(16.dp),
                    fontSize = 18.sp,
                    color = Color.White
                )
            }
        } else if (currentScreen == "settings") {
            // --- DEDICATED CONFIGURATIONS PAGE ---
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                SettingsScreen(
                    innerPadding = PaddingValues(0.dp),
                    showRpe = showRpe,
                    onRpeChange = { showRpe = it },
                    showRir = showRir,
                    onRirChange = { showRir = it },
                    increment = weightIncrement,
                    onIncrementChange = { weightIncrement = it },
                    onBackClick = { currentScreen = "workout_log" }
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
            Column(modifier = Modifier.padding(20.dp)) { // Slightly expanded bounding space
                // --- HIGHER CONTRAST & LARGER HEADER ---
                Text(
                    text = "Settings",
                    fontSize = 22.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // --- SHOW RIR ROW ---
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

                // Balanced spacing between sibling switch target blocks
                Spacer(modifier = Modifier.height(12.dp))

                // --- SHOW RPE ROW ---
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

                // Uniform spatial separation leading into the custom anchor box row
                Spacer(modifier = Modifier.height(16.dp))

                // --- WEIGHT INCREMENT SELECTION ROW ---
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
                                .padding(horizontal = 12.dp), // Fix: Cleared vertical padding to force perfect centering
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
                                .width(100.dp) // Widened slightly for decimal text strings
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