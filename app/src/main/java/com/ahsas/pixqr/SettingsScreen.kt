package com.ahsas.pixqr

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import android.app.StatusBarManager
import android.content.ComponentName
import android.os.Build
import android.widget.Toast

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val userPrefs = remember { UserPreferences(context) }
    val quickTileEnabled by userPrefs.quickTileEnabled.collectAsState(initial = false)
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 48.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.displaySmall.copy(
                fontWeight = FontWeight.Normal
            ),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 16.dp, start = 8.dp)
        )

        SettingsCardItem(
            icon = Icons.Rounded.Palette,
            iconColor = Color(0xFF6B9BF2),
            title = "Themes",
            subtitle = "Personalise your experience",
            onClick = { }
        )

        SettingsCardItem(
            icon = Icons.Rounded.TextFormat,
            iconColor = Color(0xFFA8C97F),
            title = "Fonts",
            subtitle = "Change text appearance",
            onClick = { }
        )

        SettingsCardItem(
            icon = Icons.Rounded.VolumeUp,
            iconColor = Color(0xFF9B8FD4),
            title = "Sound and Vibration",
            subtitle = "Audio and haptic feedback",
            onClick = { }
        )

        SettingsCardToggleItem(
            icon = Icons.Rounded.GridView,
            iconColor = Color(0xFF5BC4C4),
            title = "Quick Tile",
            subtitle = "Add tile to quick settings",
            checked = quickTileEnabled,
            onCheckedChange = { enabled ->
                scope.launch { userPrefs.setQuickTileEnabled(enabled) }
                if (enabled) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val statusBarManager =
                            context.getSystemService(StatusBarManager::class.java)
                        statusBarManager.requestAddTileService(
                            ComponentName(context, QrScannerTileService::class.java),
                            "Scan QR",
                            android.graphics.drawable.Icon.createWithResource(
                                context, R.drawable.ic_qr_scan
                            ),
                            { }, { }
                        )
                    } else {
                        Toast.makeText(
                            context,
                            "Open Quick Settings, tap Edit, and add the Scan QR tile manually",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        )

        SettingsCardItem(
            icon = Icons.Rounded.Widgets,
            iconColor = Color(0xFFE8A87C),
            title = "Widgets",
            subtitle = "At a glance on your home screen",
            onClick = { }
        )

        SettingsCardItem(
            icon = Icons.Rounded.SwapVert,
            iconColor = Color(0xFF6B9BF2),
            title = "History",
            subtitle = "Browse your past scans",
            onClick = { }
        )

        SettingsCardItem(
            icon = Icons.Rounded.Info,
            iconColor = Color(0xFF9B8FD4),
            title = "About",
            subtitle = "App info and version",
            onClick = { }
        )
    }
}

@Composable
fun SettingsCardItem(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun SettingsCardToggleItem(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    }
}