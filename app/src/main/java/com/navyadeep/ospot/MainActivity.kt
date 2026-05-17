package com.navyadeep.ospot

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    WorkoutAppScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutAppScreen() {
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
                                onClick = { menuExpanded = false },
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
    )
    { innerPadding ->
    }
}