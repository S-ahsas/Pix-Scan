package com.ahsas.pixqr

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onScanClick: () -> Unit,
    onSearchClick: () -> Unit,
    onItemClick: (ScanRecord) -> Unit,
    onScanImageClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onGenerateQrClick: () -> Unit
) {
    val context = LocalContext.current
    val db = (context.applicationContext as App).database
    val dao = db.scanDao()
    val scans by dao.getAllScans().collectAsState(initial = emptyList())
    val userPrefs = remember { UserPreferences(context) }
    val isGridView by userPrefs.isGridView.collectAsState(initial = false)
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    var fabExpanded by remember { mutableStateOf(false) }
    var fabPressed by remember { mutableStateOf(false) }

    val fabScale by animateFloatAsState(
        targetValue = if (fabPressed) 0.85f else 1f,
        animationSpec = spring(
            dampingRatio = 0.7f,
            stiffness = 400f
        ),
        label = "fab_scale"
    )

    LaunchedEffect(fabPressed) {
        if (fabPressed) {
            delay(150)
            fabPressed = false
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        scrimColor = Color.Black.copy(alpha = 0.5f),
        drawerContent = {
            DrawerContent(
                onClose = { scope.launch { drawerState.close() } },
                onSettingsClick = {
                    scope.launch { drawerState.close() }
                    onSettingsClick()
                },
                onGenerateQrClick = { onGenerateQrClick() }  // ← add this
            )
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {

            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = WindowInsets.systemBars
            ) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    // Top bar — surfaceContainerHigh for dark muted look
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = {
                                    scope.launch { drawerState.open() }
                                }) {
                                    Icon(
                                        Icons.Rounded.Menu,
                                        contentDescription = "Menu",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .squishClickable { onSearchClick() },
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Text(
                                        text = "Search",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .squishClickable {
                                            scope.launch { userPrefs.setGridView(!isGridView) }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    AnimatedContent(
                                        targetState = isGridView,
                                        transitionSpec = {
                                            fadeIn(
                                                animationSpec = tween(220, delayMillis = 90, easing = LinearEasing)
                                            ) togetherWith fadeOut(
                                                animationSpec = tween(90, easing = LinearEasing)
                                            )
                                        },
                                        label = "toggle_icon"
                                    ) { gridView ->
                                        Icon(
                                            imageVector = if (gridView) Icons.Rounded.ViewList else Icons.Rounded.GridView,
                                            contentDescription = "Toggle view",
                                            tint = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(4.dp))
                            }
                        }
                    }

                    // History title
                    Text(
                        text = "History",
                        fontSize = 48.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .padding(horizontal = 24.dp)
                            .padding(top = 8.dp, bottom = 16.dp)
                    )

                    // History container — surfaceContainer for dark card look
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer
                    ) {
                        if (scans.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Rounded.QrCodeScanner,
                                        contentDescription = null,
                                        modifier = Modifier.size(56.dp),
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                    )
                                    Text(
                                        text = "No scans yet",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                    Text(
                                        text = "Tap the button to scan",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                    )
                                }
                            }
                        } else {
                            AnimatedContent(
                                targetState = isGridView,
                                transitionSpec = {
                                    fadeIn(
                                        animationSpec = tween(220, delayMillis = 90, easing = LinearEasing)
                                    ) togetherWith fadeOut(
                                        animationSpec = tween(90, easing = LinearEasing)
                                    )
                                },
                                label = "view_toggle"
                            ) { gridView ->
                                if (gridView) {
                                    LazyVerticalStaggeredGrid(
                                        columns = StaggeredGridCells.Fixed(2),
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalItemSpacing = 8.dp,
                                        userScrollEnabled = !fabExpanded && drawerState.isClosed
                                    ) {
                                        items(scans, key = { it.id }) { scan ->
                                            HistoryGridItem(scan = scan, onItemClick = onItemClick)
                                        }
                                    }
                                } else {
                                    LazyColumn(
                                        userScrollEnabled = !fabExpanded && drawerState.isClosed
                                    ) {
                                        items(scans, key = { it.id }) { scan ->
                                            HistoryItem(scan = scan, onItemClick = onItemClick)
                                            HorizontalDivider(
                                                modifier = Modifier.padding(start = 72.dp),
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Scrim
            AnimatedVisibility(
                visible = fabExpanded,
                enter = fadeIn(animationSpec = tween(200)),
                exit = fadeOut(animationSpec = tween(200))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { fabExpanded = false }
                )
            }

            // FAB cluster
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 16.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AnimatedVisibility(
                    visible = fabExpanded,
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f)
                    ) + fadeIn(),
                    exit = slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f)
                    ) + fadeOut()
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.squishClickable {
                                    fabExpanded = false
                                    onScanClick()
                                }
                            ) {
                                Text(
                                    text = "Scan a QR code",
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                            SmallFloatingActionButton(
                                onClick = { fabExpanded = false; onScanClick() },
                                shape = RoundedCornerShape(14.dp),
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ) {
                                Icon(Icons.Rounded.QrCodeScanner, contentDescription = "Scan QR", modifier = Modifier.size(20.dp))
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.squishClickable {
                                    fabExpanded = false
                                    onScanImageClick()
                                }
                            ) {
                                Text(
                                    text = "Scan an image",
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                            SmallFloatingActionButton(
                                onClick = { fabExpanded = false; onScanImageClick() },
                                shape = RoundedCornerShape(14.dp),
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ) {
                                Icon(Icons.Rounded.Image, contentDescription = "Scan Image", modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }

                // Main FAB
                FloatingActionButton(
                    onClick = { fabPressed = true; fabExpanded = !fabExpanded },
                    shape = RoundedCornerShape(20.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .size(72.dp)
                        .graphicsLayer { scaleX = fabScale; scaleY = fabScale },
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
                ) {
                    AnimatedContent(
                        targetState = fabExpanded,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(200)) togetherWith
                                    fadeOut(animationSpec = tween(200))
                        },
                        label = "fab_icon"
                    ) { expanded ->
                        if (expanded) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close",
                                modifier = Modifier.size(30.dp)
                            )
                        } else {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_qr_scan),
                                contentDescription = "Scan",
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryItem(
    scan: ScanRecord,
    onItemClick: (ScanRecord) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd · hh:mm a", Locale.getDefault()) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .squishClickable { onItemClick(scan) }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = when (scan.type) {
                        "URL" -> Icons.Rounded.Link
                        "WIFI" -> Icons.Rounded.Wifi
                        else -> Icons.Rounded.TextSnippet
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = scan.cleanedContent,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = dateFormat.format(Date(scan.timestamp)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }

        Icon(
            Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun HistoryGridItem(
    scan: ScanRecord,
    onItemClick: (ScanRecord) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd · hh:mm a", Locale.getDefault()) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .squishClickable { onItemClick(scan) },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = when (scan.type) {
                    "URL" -> Icons.Rounded.Link
                    "WIFI" -> Icons.Rounded.Wifi
                    else -> Icons.Rounded.TextSnippet
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )

            Text(
                text = scan.cleanedContent,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = dateFormat.format(Date(scan.timestamp)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}