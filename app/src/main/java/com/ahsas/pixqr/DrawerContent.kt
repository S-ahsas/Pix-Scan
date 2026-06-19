package com.ahsas.pixqr

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun DrawerContent(
    onClose: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val context = LocalContext.current
    val userPrefs = remember { UserPreferences(context) }
    val labels by userPrefs.labels.collectAsState(initial = emptySet())
    val scope = rememberCoroutineScope()

    var showCreateDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var selectedLabel by remember { mutableStateOf("") }
    var editLabelName by remember { mutableStateOf("") }
    var newLabelName by remember { mutableStateOf("") }
    var isEditMode by remember { mutableStateOf(false) }

    // Create label dialog
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = {
                showCreateDialog = false
                newLabelName = ""
            },
            title = {
                Text(text = "New Label", style = MaterialTheme.typography.titleLarge)
            },
            text = {
                OutlinedTextField(
                    value = newLabelName,
                    onValueChange = { newLabelName = it },
                    placeholder = { Text("Label name") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val trimmedName = newLabelName.trim()
                        if (trimmedName.isNotEmpty()) {
                            scope.launch { userPrefs.addLabel(trimmedName) }
                            newLabelName = ""
                            showCreateDialog = false
                        }
                    }
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showCreateDialog = false
                        newLabelName = ""
                    }
                ) { Text("Cancel") }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Edit label dialog
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = {
                showEditDialog = false
                editLabelName = ""
                selectedLabel = ""
            },
            title = {
                Text(text = "Edit Label", style = MaterialTheme.typography.titleLarge)
            },
            text = {
                OutlinedTextField(
                    value = editLabelName,
                    onValueChange = { editLabelName = it },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val oldLabel = selectedLabel
                        val newLabel = editLabelName.trim()
                        if (newLabel.isNotBlank() && newLabel != oldLabel) {
                            scope.launch { userPrefs.renameLabel(oldLabel, newLabel) }
                        }
                        showEditDialog = false
                        editLabelName = ""
                        selectedLabel = ""
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            showEditDialog = false
                            editLabelName = ""
                            selectedLabel = ""
                        }
                    ) { Text("Cancel") }

                    TextButton(
                        onClick = {
                            val labelToRemove = selectedLabel
                            scope.launch { userPrefs.removeLabel(labelToRemove) }
                            showEditDialog = false
                            editLabelName = ""
                            selectedLabel = ""
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) { Text("Remove") }
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

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

            Text(
                text = "Pix Scan",
                fontSize = 24.sp,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            NavigationDrawerItem(
                icon = { Icon(Icons.Rounded.History, contentDescription = null) },
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

            NavigationDrawerItem(
                icon = { Icon(Icons.Rounded.FavoriteBorder, contentDescription = null) },
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

            NavigationDrawerItem(
                icon = { Icon(Icons.Rounded.Archive, contentDescription = null) },
                label = { Text("Archive") },
                selected = false,
                onClick = { },
                shape = RoundedCornerShape(50),
                colors = NavigationDrawerItemDefaults.colors(
                    unselectedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            NavigationDrawerItem(
                icon = { Icon(Icons.Rounded.QrCode2, contentDescription = null) },
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

            // Labels header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Labels",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (labels.isNotEmpty()) {
                    TextButton(
                        onClick = { isEditMode = !isEditMode },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text(
                            text = if (isEditMode) "Done" else "Edit",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Dynamic labels
            labels.sorted().forEach { label ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .squishClickable {
                            if (isEditMode) {
                                selectedLabel = label
                                editLabelName = label
                                showEditDialog = true
                            } else {
                                onClose()
                            }
                        }
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Rounded.Label,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    if (isEditMode) {
                        Icon(
                            Icons.Rounded.Edit,
                            contentDescription = "Edit label",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            NavigationDrawerItem(
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                label = { Text("Create a new label") },
                selected = false,
                onClick = {
                    isEditMode = false
                    showCreateDialog = true
                },
                shape = RoundedCornerShape(50),
                colors = NavigationDrawerItemDefaults.colors(
                    unselectedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            NavigationDrawerItem(
                icon = { Icon(Icons.Rounded.DeleteOutline, contentDescription = null) },
                label = { Text("Trash") },
                selected = false,
                onClick = { },
                shape = RoundedCornerShape(50),
                colors = NavigationDrawerItemDefaults.colors(
                    unselectedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            NavigationDrawerItem(
                icon = { Icon(Icons.Rounded.Settings, contentDescription = null) },
                label = { Text("Settings") },
                selected = false,
                onClick = {
                    scope.launch { onClose() }
                    onSettingsClick()
                },
                shape = RoundedCornerShape(50),
                colors = NavigationDrawerItemDefaults.colors(
                    unselectedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}