package com.unfold.core.ui.components.hud

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import com.unfold.core.ui.theme.LocalUnfoldTheme
import com.unfold.core.domain.model.TimelineItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

enum class FlowMode {
    FLOW, NOTES, PLAN
}

@Composable
fun HudFlow(
    timelineItems: List<TimelineItem>,
    notes: List<com.unfold.core.domain.model.Note> = emptyList(),
    modifier: Modifier = Modifier,
    gridRows: Int = 3,
    scale: Float = 1f,
    onLoadMore: () -> Unit = {},
    onRefreshTimeline: () -> Unit = {},
    onSaveNote: (com.unfold.core.domain.model.Note) -> Unit = {},
    onDeleteNote: (String) -> Unit = {}
) {
    val theme = LocalUnfoldTheme.current
    val context = LocalContext.current
    var currentMode by remember { mutableStateOf(FlowMode.FLOW) }
    
    var hasCalendarPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions -> 
            hasCalendarPermission = permissions[Manifest.permission.READ_CALENDAR] == true && 
                                    permissions[Manifest.permission.WRITE_CALENDAR] == true 
        }
    )

    LaunchedEffect(Unit) {
        val permsToRequest = mutableListOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permsToRequest.add("android.permission.POST_NOTIFICATIONS")
        }
        if (!hasCalendarPermission) {
            permissionLauncher.launch(permsToRequest.toTypedArray())
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = when (gridRows) {
                1 -> 18.dp
                2 -> 14.dp
                else -> 12.dp
            } * scale),
        verticalArrangement = Arrangement.spacedBy((12 * scale).dp)
    ) {
        // Mode Switcher
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy((8 * scale).dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FlowMode.values().forEach { mode ->
                val isSelected = currentMode == mode
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape((8 * scale).dp))
                        .background(if (isSelected) theme.accentPrimary.copy(alpha = 0.2f) else theme.bgPanel.copy(alpha = 0.3f))
                        .border(1.dp, if (isSelected) theme.accentPrimary else theme.panelBorder.copy(alpha = 0.2f), RoundedCornerShape((8 * scale).dp))
                        .clickable { currentMode = mode }
                        .padding(vertical = (8 * scale).dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = mode.name,
                        color = if (isSelected) theme.accentPrimary else theme.textSecondary,
                        fontSize = (10 * scale).sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }
            }
            // System Alarm Bell Icon
            Box(
                modifier = Modifier
                    .size((30 * scale).dp)
                    .clip(RoundedCornerShape((8 * scale).dp))
                    .background(theme.bgPanel.copy(alpha = 0.3f))
                    .border(1.dp, theme.panelBorder.copy(alpha = 0.2f), RoundedCornerShape((8 * scale).dp))
                    .clickable { 
                        try {
                            val alarmIntent = android.content.Intent(android.provider.AlarmClock.ACTION_SHOW_ALARMS)
                            alarmIntent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                            context.startActivity(alarmIntent)
                        } catch (e: Exception) {
                            android.widget.Toast.makeText(context, "No alarm app found.", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Alarms",
                    tint = theme.textSecondary,
                    modifier = Modifier.size((16 * scale).dp)
                )
            }
        }

        // Content Area based on mode
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (currentMode) {
                FlowMode.FLOW -> FlowTimelineContent(timelineItems, scale, onLoadMore, onRefreshTimeline)
                FlowMode.NOTES -> FlowNotesContent(notes, scale, onSaveNote, onDeleteNote)
                FlowMode.PLAN -> FlowPlanContent(scale, hasCalendarPermission) {
                    if (!hasCalendarPermission) {
                        permissionLauncher.launch(arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR))
                    }
                }
            }
        }
    }
}

@Composable
fun QuickActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    scale: Float,
    modifier: Modifier = Modifier
) {
    val theme = LocalUnfoldTheme.current
    Row(
        modifier = modifier
            .clip(RoundedCornerShape((12 * scale).dp))
            .background(theme.bgPanel.copy(alpha = 0.4f))
            .border(1.dp, theme.panelBorder.copy(alpha = 0.3f), RoundedCornerShape((12 * scale).dp))
            .clickable { /* Handle action */ }
            .padding(horizontal = (6 * scale).dp, vertical = (6 * scale).dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = theme.accentPrimary,
            modifier = Modifier.size((14 * scale).dp)
        )
        Spacer(modifier = Modifier.width((4 * scale).dp))
        Text(
            text = label,
            color = theme.textPrimary,
            fontSize = (9f * scale).sp,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun FlowTimelineContent(timelineItems: List<TimelineItem>, scale: Float, onLoadMore: () -> Unit = {}, onRefresh: () -> Unit = {}) {
    val theme = LocalUnfoldTheme.current
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    
    val nowItems = timelineItems.filter { 
        val now = System.currentTimeMillis()
        val endTime = it.endTimeMillis
        it.startTimeMillis <= now && (endTime == null || endTime >= now)
    }.map { it.title to timeFormat.format(Date(it.startTimeMillis)) }
    
    val nextItems = timelineItems.filter { 
        val now = System.currentTimeMillis()
        it.startTimeMillis > now && it.startTimeMillis < now + 2 * 3600 * 1000 // Next 2 hours
    }.map { it.title to timeFormat.format(Date(it.startTimeMillis)) }
    
    val laterItems = timelineItems.filter { 
        val now = System.currentTimeMillis()
        it.startTimeMillis >= now + 2 * 3600 * 1000
    }.map { it.title to timeFormat.format(Date(it.startTimeMillis)) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val isGoogleCalendarInstalled = remember(context) {
        try {
            context.packageManager.getPackageInfo("com.google.android.calendar", 0)
            true
        } catch (e: android.content.pm.PackageManager.NameNotFoundException) {
            false
        }
    }
    
    if (!isGoogleCalendarInstalled) {
        Column(
            modifier = Modifier.fillMaxSize().padding((16 * scale).dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Google Calendar is required for Flow.",
                color = theme.textPrimary,
                fontSize = (12 * scale).sp,
                fontFamily = FontFamily.Monospace,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height((16 * scale).dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape((8 * scale).dp))
                    .background(theme.accentPrimary)
                    .clickable {
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("market://details?id=com.google.android.calendar"))
                        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.calendar")))
                        }
                    }
                    .padding(horizontal = (16 * scale).dp, vertical = (8 * scale).dp)
            ) {
                Text("Install Google Calendar", color = theme.bgPanel, fontWeight = FontWeight.Bold, fontSize = (10 * scale).sp)
            }
        }
        return
    }

    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    
    val shouldLoadMore = remember(listState, timelineItems.size) {
        derivedStateOf {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()
            val totalItems = listState.layoutInfo.totalItemsCount
            lastVisibleItem != null && lastVisibleItem.index >= totalItems - 8
        }
    }
    
    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value) {
            onLoadMore()
        }
    }

    val upcomingItems = timelineItems.filter { 
        val now = System.currentTimeMillis()
        val endTime = it.endTimeMillis ?: it.startTimeMillis
        it.startTimeMillis >= now || endTime >= now
    }.sortedBy { it.startTimeMillis }
    .distinctBy { it.title to it.startTimeMillis }
    .map { 
        val dateTitle = when (val diff = (it.startTimeMillis - System.currentTimeMillis()) / (1000 * 60 * 60 * 24)) {
            0L -> "TODAY"
            1L -> "TOMORROW"
            else -> dateFormat.format(Date(it.startTimeMillis)).uppercase(Locale.getDefault())
        }
        dateTitle to (it.title to timeFormat.format(Date(it.startTimeMillis))) 
    }

    val groupedItems = upcomingItems.groupBy { it.first }.mapValues { entry -> entry.value.map { it.second } }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy((12 * scale).dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = (8 * scale).dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "UPCOMING",
                    color = theme.accentPrimary,
                    fontSize = (12 * scale).sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape((6 * scale).dp))
                        .background(theme.bgPanel.copy(alpha = 0.5f))
                        .clickable { onRefresh() }
                        .padding(horizontal = (8 * scale).dp, vertical = (4 * scale).dp)
                ) {
                    Text(
                        text = "SYNC",
                        color = theme.textMuted,
                        fontSize = (10 * scale).sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
        
        if (groupedItems.isNotEmpty()) {
            groupedItems.forEach { (dateTitle, items) ->
                item {
                    TimelineSection(title = dateTitle, items = items, scale = scale)
                }
            }
        } else {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding((16 * scale).dp), contentAlignment = Alignment.Center) {
                    Text("No upcoming events", color = theme.textMuted, fontFamily = FontFamily.Monospace, fontSize = (10 * scale).sp)
                }
            }
        }
    }
}

@Composable
fun TimelineSection(title: String, items: List<Pair<String, String>>, scale: Float, condensed: Boolean = false) {
    val theme = LocalUnfoldTheme.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy((6 * scale).dp)
    ) {
        Text(
            text = title,
            color = theme.accentPrimary,
            fontSize = (12 * scale).sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )
        items.forEach { (task, time) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape((8 * scale).dp))
                    .background(theme.bgPanel.copy(alpha = if (condensed) 0.1f else 0.2f))
                    .border(1.dp, theme.panelBorder.copy(alpha = 0.1f), RoundedCornerShape((8 * scale).dp))
                    .clickable { /* View details */ }
                    .padding((10 * scale).dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = task,
                    color = theme.textPrimary,
                    fontSize = (if (condensed) 11f else 12f * scale).sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width((8 * scale).dp))
                Text(
                    text = time,
                    color = theme.accentPrimary,
                    fontSize = (if (condensed) 9f else 10f * scale).sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun FlowNotesContent(
    notes: List<com.unfold.core.domain.model.Note>,
    scale: Float,
    onSaveNote: (com.unfold.core.domain.model.Note) -> Unit,
    onDeleteNote: (String) -> Unit
) {
    val theme = LocalUnfoldTheme.current
    val context = LocalContext.current
    
    var showNoteSelection by remember { mutableStateOf(false) }
    var currentNoteId by remember { mutableStateOf(notes.firstOrNull()?.id) }
    
    // Ensure we have a valid note id
    LaunchedEffect(notes.size, currentNoteId) {
        if (currentNoteId == null && notes.isNotEmpty()) {
            currentNoteId = notes.first().id
        }
    }
    
    val currentNote = notes.find { it.id == currentNoteId } ?: com.unfold.core.domain.model.Note(
        id = java.util.UUID.randomUUID().toString().also { currentNoteId = it },
        text = "",
        lastModified = System.currentTimeMillis(),
        pinned = false
    )
    
    var title by remember(currentNote.id) { 
        mutableStateOf(androidx.compose.ui.text.input.TextFieldValue(currentNote.text.substringBefore("\n---\n").takeIf { currentNote.text.contains("\n---\n") } ?: "")) 
    }
    var content by remember(currentNote.id) { 
        mutableStateOf(androidx.compose.ui.text.input.TextFieldValue(currentNote.text.substringAfter("\n---\n").takeIf { currentNote.text.contains("\n---\n") } ?: currentNote.text)) 
    }
    
    val fullText = if (title.text.isEmpty() && content.text.isEmpty()) "" else "${title.text}\n---\n${content.text}"
    
    // Auto-save logic
    LaunchedEffect(fullText) {
        if (fullText != currentNote.text) {
            kotlinx.coroutines.delay(1000) // Debounce 1 second
            onSaveNote(currentNote.copy(text = fullText))
        }
    }

    if (showNoteSelection) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = (8 * scale).dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SELECT PAGE",
                    color = theme.accentPrimary,
                    fontSize = (12 * scale).sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "BACK",
                    color = theme.textMuted,
                    fontSize = (10 * scale).sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.clickable { showNoteSelection = false }
                )
            }
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy((8 * scale).dp)
            ) {
                items(notes.size) { index ->
                    val note = notes[index]
                    val title = if (note.text.isBlank()) "Empty Note" else note.text.take(20).replace("\n", " ") + "..."
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape((8 * scale).dp))
                            .background(theme.bgPanel.copy(alpha = 0.2f))
                            .border(1.dp, if (note.id == currentNoteId) theme.accentPrimary else theme.panelBorder.copy(alpha = 0.1f), RoundedCornerShape((8 * scale).dp))
                            .clickable {
                                currentNoteId = note.id
                                showNoteSelection = false
                            }
                            .padding((12 * scale).dp)
                    ) {
                        Text(text = title, color = theme.textPrimary, fontSize = (12 * scale).sp, fontFamily = FontFamily.Monospace)
                    }
                }
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = (8 * scale).dp)
                            .clip(RoundedCornerShape((8 * scale).dp))
                            .background(theme.accentPrimary.copy(alpha = 0.2f))
                            .border(1.dp, theme.accentPrimary.copy(alpha = 0.5f), RoundedCornerShape((8 * scale).dp))
                            .clickable {
                                val newNoteId = java.util.UUID.randomUUID().toString()
                                val newNote = com.unfold.core.domain.model.Note(
                                    id = newNoteId,
                                    text = "",
                                    lastModified = System.currentTimeMillis()
                                )
                                onSaveNote(newNote)
                                currentNoteId = newNoteId
                                showNoteSelection = false
                            }
                            .padding((12 * scale).dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "+ NEW PAGE", color = theme.accentPrimary, fontSize = (12 * scale).sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = (8 * scale).dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "NOTES",
                    color = theme.accentPrimary,
                    fontSize = (12 * scale).sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy((12 * scale).dp)) {
                    Text(
                        text = "REMIND",
                        color = theme.accentPrimary,
                        fontSize = (10 * scale).sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.clickable {
                            onSaveNote(currentNote.copy(text = fullText)) // Save before remind
                            val calendar = java.util.Calendar.getInstance()
                            android.app.DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    calendar.set(java.util.Calendar.YEAR, year)
                                    calendar.set(java.util.Calendar.MONTH, month)
                                    calendar.set(java.util.Calendar.DAY_OF_MONTH, dayOfMonth)
                                    android.app.TimePickerDialog(
                                        context,
                                        { _, hourOfDay, minute ->
                                            calendar.set(java.util.Calendar.HOUR_OF_DAY, hourOfDay)
                                            calendar.set(java.util.Calendar.MINUTE, minute)
                                            calendar.set(java.util.Calendar.SECOND, 0)
                                            setReminder(context, fullText, calendar.timeInMillis)
                                            android.widget.Toast.makeText(context, "Reminder set", android.widget.Toast.LENGTH_SHORT).show()
                                        },
                                        calendar.get(java.util.Calendar.HOUR_OF_DAY),
                                        calendar.get(java.util.Calendar.MINUTE),
                                        true
                                    ).show()
                                },
                                calendar.get(java.util.Calendar.YEAR),
                                calendar.get(java.util.Calendar.MONTH),
                                calendar.get(java.util.Calendar.DAY_OF_MONTH)
                            ).show()
                        }
                    )
                    Text(
                        text = "PAGES",
                        color = theme.accentPrimary,
                        fontSize = (10 * scale).sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.clickable { showNoteSelection = true }
                    )
                }
            }
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape((8 * scale).dp))
                    .background(theme.bgPanel.copy(alpha = 0.2f))
                    .border(1.dp, theme.panelBorder.copy(alpha = 0.1f), RoundedCornerShape((8 * scale).dp))
                    .padding((12 * scale).dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    androidx.compose.foundation.text.BasicTextField(
                        value = title,
                        onValueChange = { title = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = theme.accentPrimary,
                            fontSize = (14 * scale).sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        ),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(theme.accentPrimary),
                        decorationBox = { innerTextField ->
                            if (title.text.isEmpty()) {
                                Text("Title", color = theme.accentPrimary, fontSize = (14 * scale).sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                            innerTextField()
                        }
                    )
                    
                    androidx.compose.material3.HorizontalDivider(
                        modifier = Modifier.padding(vertical = (8 * scale).dp),
                        color = theme.panelBorder.copy(alpha = 0.2f)
                    )
                    
                    androidx.compose.foundation.text.BasicTextField(
                        value = content,
                        onValueChange = { content = it },
                        modifier = Modifier.fillMaxSize(),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = theme.textPrimary,
                            fontSize = (12 * scale).sp,
                            fontFamily = FontFamily.Monospace
                        ),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(theme.accentPrimary),
                        decorationBox = { innerTextField ->
                            if (content.text.isEmpty()) {
                                Text("Type your notes here...", color = theme.textMuted, fontSize = (12 * scale).sp, fontFamily = FontFamily.Monospace)
                            }
                            innerTextField()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun FlowPlanContent(scale: Float, hasPermission: Boolean, requestPermission: () -> Unit) {
    val theme = LocalUnfoldTheme.current
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    
    val startCalendar = remember { java.util.Calendar.getInstance() }
    val endCalendar = remember { java.util.Calendar.getInstance().apply { add(java.util.Calendar.HOUR_OF_DAY, 1) } }
    
    var startTimeFormatted by remember { mutableStateOf(formatDateTime(startCalendar.timeInMillis)) }
    var endTimeFormatted by remember { mutableStateOf(formatDateTime(endCalendar.timeInMillis)) }
    
    Column(
        modifier = Modifier.fillMaxSize().padding((12 * scale).dp),
        verticalArrangement = Arrangement.spacedBy((12 * scale).dp)
    ) {
        Text(
            text = "PLAN NEW EVENT",
            color = theme.accentPrimary,
            fontSize = (12 * scale).sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )
        
        androidx.compose.foundation.text.BasicTextField(
            value = title,
            onValueChange = { title = it },
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape((8 * scale).dp))
                .background(theme.bgPanel.copy(alpha = 0.2f))
                .border(1.dp, theme.panelBorder.copy(alpha = 0.1f), RoundedCornerShape((8 * scale).dp))
                .padding((12 * scale).dp),
            textStyle = androidx.compose.ui.text.TextStyle(
                color = theme.textPrimary,
                fontSize = (12 * scale).sp,
                fontFamily = FontFamily.Monospace
            ),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(theme.accentPrimary),
            decorationBox = { innerTextField ->
                if (title.isEmpty()) {
                    Text("Event Title", color = theme.textMuted, fontSize = (12 * scale).sp, fontFamily = FontFamily.Monospace)
                }
                innerTextField()
            }
        )
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy((8 * scale).dp)) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape((8 * scale).dp))
                    .background(theme.bgPanel.copy(alpha = 0.2f))
                    .border(1.dp, theme.panelBorder.copy(alpha = 0.1f), RoundedCornerShape((8 * scale).dp))
                    .clickable {
                        android.app.DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                startCalendar.set(java.util.Calendar.YEAR, year)
                                startCalendar.set(java.util.Calendar.MONTH, month)
                                startCalendar.set(java.util.Calendar.DAY_OF_MONTH, dayOfMonth)
                                android.app.TimePickerDialog(
                                    context,
                                    { _, hourOfDay, minute ->
                                        startCalendar.set(java.util.Calendar.HOUR_OF_DAY, hourOfDay)
                                        startCalendar.set(java.util.Calendar.MINUTE, minute)
                                        startTimeFormatted = formatDateTime(startCalendar.timeInMillis)
                                        
                                        // Auto adjust end time if it's before start time
                                        if (endCalendar.timeInMillis <= startCalendar.timeInMillis) {
                                            endCalendar.timeInMillis = startCalendar.timeInMillis + 60 * 60 * 1000
                                            endTimeFormatted = formatDateTime(endCalendar.timeInMillis)
                                        }
                                    },
                                    startCalendar.get(java.util.Calendar.HOUR_OF_DAY),
                                    startCalendar.get(java.util.Calendar.MINUTE),
                                    true
                                ).show()
                            },
                            startCalendar.get(java.util.Calendar.YEAR),
                            startCalendar.get(java.util.Calendar.MONTH),
                            startCalendar.get(java.util.Calendar.DAY_OF_MONTH)
                        ).show()
                    }
                    .padding((12 * scale).dp)
            ) {
                Column {
                    Text("Start Time", color = theme.textMuted, fontSize = (10 * scale).sp, fontFamily = FontFamily.Monospace)
                    Text(startTimeFormatted, color = theme.textPrimary, fontSize = (12 * scale).sp, fontFamily = FontFamily.Monospace)
                }
            }
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape((8 * scale).dp))
                    .background(theme.bgPanel.copy(alpha = 0.2f))
                    .border(1.dp, theme.panelBorder.copy(alpha = 0.1f), RoundedCornerShape((8 * scale).dp))
                    .clickable {
                        android.app.DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                endCalendar.set(java.util.Calendar.YEAR, year)
                                endCalendar.set(java.util.Calendar.MONTH, month)
                                endCalendar.set(java.util.Calendar.DAY_OF_MONTH, dayOfMonth)
                                android.app.TimePickerDialog(
                                    context,
                                    { _, hourOfDay, minute ->
                                        endCalendar.set(java.util.Calendar.HOUR_OF_DAY, hourOfDay)
                                        endCalendar.set(java.util.Calendar.MINUTE, minute)
                                        endTimeFormatted = formatDateTime(endCalendar.timeInMillis)
                                    },
                                    endCalendar.get(java.util.Calendar.HOUR_OF_DAY),
                                    endCalendar.get(java.util.Calendar.MINUTE),
                                    true
                                ).show()
                            },
                            endCalendar.get(java.util.Calendar.YEAR),
                            endCalendar.get(java.util.Calendar.MONTH),
                            endCalendar.get(java.util.Calendar.DAY_OF_MONTH)
                        ).show()
                    }
                    .padding((12 * scale).dp)
            ) {
                Column {
                    Text("End Time", color = theme.textMuted, fontSize = (10 * scale).sp, fontFamily = FontFamily.Monospace)
                    Text(endTimeFormatted, color = theme.textPrimary, fontSize = (12 * scale).sp, fontFamily = FontFamily.Monospace)
                }
            }
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Save to Google Calendar",
                color = theme.textPrimary,
                fontSize = (12 * scale).sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.weight(1f).padding(end = (8 * scale).dp)
            )
            
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape((16 * scale).dp))
                    .background(theme.accentPrimary)
                    .clickable {
                        if (title.isBlank()) {
                            android.widget.Toast.makeText(context, "Title cannot be empty", android.widget.Toast.LENGTH_SHORT).show()
                            return@clickable
                        }
                        if (!hasPermission) {
                            requestPermission()
                            return@clickable
                        }
                        insertEventToCalendar(context, title, startCalendar.timeInMillis, endCalendar.timeInMillis)
                        android.widget.Toast.makeText(context, "Event saved to Google Calendar", android.widget.Toast.LENGTH_SHORT).show()
                        title = ""
                    }
                    .padding(horizontal = (16 * scale).dp, vertical = (8 * scale).dp)
            ) {
                Text(
                    "SAVE",
                    color = theme.bgVoid,
                    fontSize = (12 * scale).sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

fun formatDateTime(timeInMillis: Long): String {
    val formatter = java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault())
    return formatter.format(java.util.Date(timeInMillis))
}

fun insertEventToCalendar(context: android.content.Context, title: String, startTimeMillis: Long, endTimeMillis: Long) {
    if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.WRITE_CALENDAR) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
        return
    }
    
    var calendarId = 1L // fallback
    
    val projection = arrayOf(
        android.provider.CalendarContract.Calendars._ID,
        android.provider.CalendarContract.Calendars.ACCOUNT_TYPE
    )
    val selection = "${android.provider.CalendarContract.Calendars.VISIBLE} = 1"
    
    val cursor = context.contentResolver.query(
        android.provider.CalendarContract.Calendars.CONTENT_URI,
        projection,
        selection,
        null,
        null
    )
    
    cursor?.use {
        val idIndex = it.getColumnIndexOrThrow(android.provider.CalendarContract.Calendars._ID)
        val accTypeIndex = it.getColumnIndexOrThrow(android.provider.CalendarContract.Calendars.ACCOUNT_TYPE)
        
        while (it.moveToNext()) {
            val id = it.getLong(idIndex)
            val accType = it.getString(accTypeIndex)
            
            // Prioritize google calendar
            if (accType == "com.google") {
                calendarId = id
                break
            } else if (calendarId == 1L) {
                calendarId = id
            }
        }
    }
    
    val values = android.content.ContentValues().apply {
        put(android.provider.CalendarContract.Events.DTSTART, startTimeMillis)
        put(android.provider.CalendarContract.Events.DTEND, endTimeMillis)
        put(android.provider.CalendarContract.Events.TITLE, title)
        put(android.provider.CalendarContract.Events.CALENDAR_ID, calendarId)
        put(android.provider.CalendarContract.Events.EVENT_TIMEZONE, java.util.TimeZone.getDefault().id)
    }
    context.contentResolver.insert(android.provider.CalendarContract.Events.CONTENT_URI, values)
}

fun setReminder(context: android.content.Context, noteText: String, timeInMillis: Long) {
    try {
        val calendar = java.util.Calendar.getInstance().apply {
            this.timeInMillis = timeInMillis
        }
        val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        val minute = calendar.get(java.util.Calendar.MINUTE)

        val intent = android.content.Intent(android.provider.AlarmClock.ACTION_SET_ALARM).apply {
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra(android.provider.AlarmClock.EXTRA_MESSAGE, noteText)
            putExtra(android.provider.AlarmClock.EXTRA_HOUR, hour)
            putExtra(android.provider.AlarmClock.EXTRA_MINUTES, minute)
            putExtra(android.provider.AlarmClock.EXTRA_SKIP_UI, true)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "Unable to set alarm. Missing app or permission.", android.widget.Toast.LENGTH_LONG).show()
    }
}
