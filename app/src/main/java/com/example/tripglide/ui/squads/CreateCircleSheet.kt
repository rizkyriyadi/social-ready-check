package com.example.tripglide.ui.squads

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.unit.dp
import com.example.tripglide.ui.theme.Black
import com.example.tripglide.ui.theme.White

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateCircleSheet(
    onDismiss: () -> Unit,
    onCreate: (String, String) -> Unit,
    isLoading: Boolean
) {
    var name by remember { mutableStateOf("") }
    var selectedGame by remember { mutableStateOf("Dota 2") }
    val games = listOf("Dota 2", "Valorant", "Mobile Legends")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .padding(bottom = 32.dp), // Extra padding for rounded corners/navigation bar
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Create New Squad",
            style = MaterialTheme.typography.headlineSmall,
            color = Black
        )

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Squad Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
        )

        Text(
            text = "Select Game",
            style = MaterialTheme.typography.labelLarge,
            color = Black
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(games) { game ->
                FilterChip(
                    selected = selectedGame == game,
                    onClick = { selectedGame = game },
                    label = { Text(game) },
                    leadingIcon = if (selectedGame == game) {
                        { Icon(androidx.compose.material.icons.Icons.Default.Check, contentDescription = null) }
                    } else null
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { onCreate(name, selectedGame) },
            enabled = name.isNotBlank() && !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Black)
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = White, modifier = Modifier.size(24.dp))
            } else {
                Text("CREATE SQUAD")
            }
        }
    }
}
