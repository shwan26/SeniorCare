package com.example.consultapp.viewmodel

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun CustomBottomBar(
    currentRoute: String,
    onNavigateToHome: () -> Unit,
    onNavigateToSelection: () -> Unit,
    onNavigateToChat: () -> Unit
) {
    BottomAppBar(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left section: Home icon
            IconButton(
                onClick = onNavigateToHome,
                modifier = Modifier.weight(1f)
            ) {
                val iconColor = if (currentRoute == "home") Color.Blue else Color.Gray
                Icon(Icons.Filled.Home, contentDescription = "Home", tint = iconColor)
            }

            // Middle section: Selection icon
            IconButton(
                onClick = onNavigateToSelection,
                modifier = Modifier.weight(1f)
            ) {
                val iconColor = if (currentRoute == "selection") Color.Blue else Color.Gray
                Icon(Icons.Filled.List, contentDescription = "Selection", tint = iconColor)
            }

            // Right section: Chat icon
            IconButton(
                onClick = onNavigateToChat,
                modifier = Modifier.weight(1f)
            ) {
                val iconColor = if (currentRoute == "chat") Color.Blue else Color.Gray
                Icon(Icons.Filled.Chat, contentDescription = "Chat", tint = iconColor)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CustomBottomBarPreview() {
    CustomBottomBar(
        currentRoute = "home",
        onNavigateToHome = {},
        onNavigateToSelection = {},
        onNavigateToChat = {}
    )
}
