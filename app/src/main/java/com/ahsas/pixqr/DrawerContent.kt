package com.ahsas.pixqr

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DrawerContent(
    onClose: () -> Unit
) {
    ModalDrawerSheet(
        drawerShape = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp),
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.width(280.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Header
            Text(
                text = "Pix Scan",
                fontSize = 24.sp,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // History — active item
            NavigationDrawerItem(
                icon = {
                    Icon(Icons.Rounded.History, contentDescription = null)
                },
                label = { Text("History") },
                selected = true,
                onClick = { onClose() },
                shape = RoundedCornerShape(50),
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )

            // Favorites
            NavigationDrawerItem(
                icon = {
                    Icon(Icons.Rounded.FavoriteBorder, contentDescription = null)
                },
                label = { Text("Favorites") },
                badge = {
                    Text(
                        text = "12",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                selected = false,
                onClick = { },
                shape = RoundedCornerShape(50),
                colors = NavigationDrawerItemDefaults.colors(
                    unselectedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            // Archive
            NavigationDrawerItem(
                icon = {
                    Icon(Icons.Rounded.Archive, contentDescription = null)
                },
                label = { Text("Archive") },
                selected = false,
                onClick = { },
                shape = RoundedCornerShape(50),
                colors = NavigationDrawerItemDefaults.colors(
                    unselectedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            // Generate QR
            NavigationDrawerItem(
                icon = {
                    Icon(Icons.Rounded.QrCode2, contentDescription = null)
                },
                label = { Text("Generate QR") },
                selected = false,
                onClick = { },
                shape = RoundedCornerShape(50),
                colors = NavigationDrawerItemDefaults.colors(
                    unselectedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            // Labels section
            Text(
                text = "Labels",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )

            NavigationDrawerItem(
                icon = {
                    Icon(Icons.Rounded.FolderOpen, contentDescription = null)
                },
                label = { Text("Product QR") },
                selected = false,
                onClick = { },
                shape = RoundedCornerShape(50),
                colors = NavigationDrawerItemDefaults.colors(
                    unselectedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            NavigationDrawerItem(
                icon = {
                    Icon(Icons.Rounded.FolderOpen, contentDescription = null)
                },
                label = { Text("Book QR") },
                selected = false,
                onClick = { },
                shape = RoundedCornerShape(50),
                colors = NavigationDrawerItemDefaults.colors(
                    unselectedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            NavigationDrawerItem(
                icon = {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                },
                label = { Text("Create a new label") },
                selected = false,
                onClick = { },
                shape = RoundedCornerShape(50),
                colors = NavigationDrawerItemDefaults.colors(
                    unselectedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            // Trash
            NavigationDrawerItem(
                icon = {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = null)
                },
                label = { Text("Trash") },
                selected = false,
                onClick = { },
                shape = RoundedCornerShape(50),
                colors = NavigationDrawerItemDefaults.colors(
                    unselectedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            // Settings
            NavigationDrawerItem(
                icon = {
                    Icon(Icons.Rounded.Settings, contentDescription = null)
                },
                label = { Text("Settings") },
                selected = false,
                onClick = { },
                shape = RoundedCornerShape(50),
                colors = NavigationDrawerItemDefaults.colors(
                    unselectedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}