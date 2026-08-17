package com.unfold.core.ui.components.hud

import android.content.Context
import android.text.Html
import android.text.format.DateUtils
import android.util.Xml
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.unfold.core.ui.theme.LocalUnfoldTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val FEED_PREFS_NAME = "hud_feed_preferences"
private const val PREF_SELECTED_TOPICS = "selected_topics"

private val FeedWindows = listOf("1d", "3d", "7d", "14d", "30d")

private val DefaultTopics = listOf(
    FeedTopic.AI,
    FeedTopic.TECHNOLOGY,
    FeedTopic.SOCIAL,
    FeedTopic.ECONOMY,
    FeedTopic.SCIENCE,
    FeedTopic.SPORTS,
    FeedTopic.GAMING,
    FeedTopic.BUSINESS
)

private val AllowedTopicLabels = DefaultTopics.map { it.label }.toSet()

private enum class FeedTopic(
    val label: String,
    val query: String
) {
    AI("AI", "\"artificial intelligence\" OR AI"),
    TECHNOLOGY("Technology", "technology OR gadgets OR software"),
    SOCIAL("Social", "\"social media\" OR social"),
    ECONOMY("Economy", "economy OR inflation OR markets"),
    SCIENCE("Science", "science OR research OR space"),
    SPORTS("Sports", "sports OR football OR cricket"),
    GAMING("Gaming", "gaming OR \"video games\""),
    BUSINESS("Business", "business OR startups OR companies");

    companion object {
        fun fromLabel(label: String): FeedTopic? = entries.firstOrNull { it.label == label }
    }
}

private data class FeedArticle(
    val id: String,
    val title: String,
    val details: String,
    val source: String,
    val publishedAtMillis: Long,
    val imageUrl: String?,
    val link: String,
    val topic: String
)

private class GoogleFeedRepository(private val context: Context) {
    private val prefs = context.getSharedPreferences(FEED_PREFS_NAME, Context.MODE_PRIVATE)

    fun loadSelectedTopics(): List<FeedTopic> {
        val stored = prefs.getStringSet(PREF_SELECTED_TOPICS, null)
            ?.mapNotNull(FeedTopic::fromLabel)
            ?.filter { it.label in AllowedTopicLabels }
            ?.ifEmpty { null }

        return stored ?: DefaultTopics
    }

    fun saveSelectedTopics(topics: Set<String>) {
        prefs.edit().putStringSet(PREF_SELECTED_TOPICS, topics).apply()
    }

    suspend fun loadFeed(topics: List<FeedTopic>, pageIndex: Int): List<FeedArticle> = withContext(Dispatchers.IO) {
        if (topics.isEmpty()) return@withContext emptyList()

        val window = FeedWindows.getOrNull(pageIndex) ?: FeedWindows.last()

        val batches = coroutineScope {
            topics.map { topic ->
                async { fetchTopicFeed(topic, window) }
            }.awaitAll()
        }

        batches
            .flatten()
            .distinctBy { it.id }
            .sortedByDescending { it.publishedAtMillis }
            .take(24)
    }

    private fun fetchTopicFeed(topic: FeedTopic, window: String): List<FeedArticle> {
        val query = "${topic.query} when:$window"
        val url = URL(
            "https://news.google.com/rss/search?q=${
                URLEncoder.encode(query, Charsets.UTF_8.name())
            }&hl=en-US&gl=US&ceid=US:en"
        )

        val connection = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 5000
            readTimeout = 7000
            requestMethod = "GET"
            instanceFollowRedirects = true
        }

        return try {
            connection.inputStream.use { input ->
                parseRssFeed(input, topic.label)
            }
        } catch (_: Exception) {
            emptyList()
        } finally {
            connection.disconnect()
        }
    }

    private fun parseRssFeed(input: java.io.InputStream, topicLabel: String): List<FeedArticle> {
        val parser = Xml.newPullParser()
        parser.setInput(input, null)

        val items = mutableListOf<FeedArticle>()
        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && parser.name == "item") {
                parseItem(parser, topicLabel)?.let(items::add)
            }
            eventType = parser.next()
        }
        return items
    }

    private fun parseItem(parser: XmlPullParser, topicLabel: String): FeedArticle? {
        var title = ""
        var description = ""
        var link = ""
        var source = ""
        var imageUrl: String? = null
        var publishedAtMillis = 0L

        val startDepth = parser.depth
        while (true) {
            when (parser.next()) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "title" -> title = parser.nextText().trim()
                    "link" -> link = parser.nextText().trim()
                    "description" -> description = parser.nextText().trim()
                    "source" -> source = parser.nextText().trim().ifBlank { source }
                    "pubDate" -> publishedAtMillis = parsePubDate(parser.nextText().trim())
                    "thumbnail" -> imageUrl = parser.getAttributeValue(null, "url")?.trim()
                    "content" -> {
                        val url = parser.getAttributeValue(null, "url")?.trim()
                        if (imageUrl.isNullOrBlank() && !url.isNullOrBlank()) {
                            imageUrl = url
                        }
                    }
                    "enclosure" -> {
                        val url = parser.getAttributeValue(null, "url")?.trim()
                        if (imageUrl.isNullOrBlank() && !url.isNullOrBlank()) {
                            imageUrl = url
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.depth <= startDepth && parser.name == "item") {
                        break
                    }
                }
                XmlPullParser.END_DOCUMENT -> break
            }
        }

        val cleanTitle = title.ifBlank { return null }
        val cleanLink = link.ifBlank { return null }
        val cleanSource = source.ifBlank { "Google News" }
        val cleanDetails = stripHtml(description).ifBlank { cleanTitle }

        return FeedArticle(
            id = "${cleanLink.lowercase(Locale.US)}|${cleanTitle.lowercase(Locale.US)}",
            title = cleanTitle,
            details = cleanDetails,
            source = cleanSource,
            publishedAtMillis = publishedAtMillis.takeIf { it > 0L } ?: System.currentTimeMillis(),
            imageUrl = imageUrl,
            link = cleanLink,
            topic = topicLabel
        )
    }

    private fun parsePubDate(value: String): Long {
        val formats = listOf(
            SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US),
            SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US)
        )
        for (format in formats) {
            runCatching { return format.parse(value)?.time ?: 0L }
        }
        return 0L
    }

    private fun stripHtml(value: String): String {
        if (value.isBlank()) return ""
        return runCatching {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                Html.fromHtml(value, Html.FROM_HTML_MODE_COMPACT).toString().trim()
            } else {
                @Suppress("DEPRECATION")
                Html.fromHtml(value).toString().trim()
            }
        }.getOrDefault(value.trim())
    }
}

@Composable
fun HudGoogleFeed(
    modifier: Modifier = Modifier,
    gridRows: Int = 3,
    scale: Float = 1f
) {
    HudGoogleFeedContent(
        modifier = modifier,
        gridRows = gridRows,
        scale = scale
    )
}

@Composable
fun HudGoogleFeedContent(
    modifier: Modifier = Modifier,
    gridRows: Int = 3,
    scale: Float = 1f
) {
    val theme = LocalUnfoldTheme.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember(context) { GoogleFeedRepository(context.applicationContext) }
    val listState = rememberLazyListState()

    var selectedTopics by remember {
        mutableStateOf(repository.loadSelectedTopics())
    }
    var showCustomizeDialog by remember { mutableStateOf(false) }
    var feedItems by remember { mutableStateOf(listOf<FeedArticle>()) }
    var expandedItemId by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var hasMore by remember { mutableStateOf(true) }
    var pageIndex by remember { mutableIntStateOf(0) }

    fun mergeArticles(current: List<FeedArticle>, incoming: List<FeedArticle>): List<FeedArticle> {
        return (current + incoming)
            .distinctBy { it.id }
            .sortedByDescending { it.publishedAtMillis }
    }

    suspend fun loadNextPage(reset: Boolean) {
        if (isLoading) return
        isLoading = true
        if (reset) {
            pageIndex = 0
            hasMore = true
            expandedItemId = null
            feedItems = emptyList()
        }

        val items = repository.loadFeed(selectedTopics, pageIndex)
        if (items.isEmpty()) {
            hasMore = false
        } else {
            val merged = mergeArticles(feedItems, items)
            hasMore = merged.size > feedItems.size || pageIndex == 0
            feedItems = merged
            pageIndex += 1
        }
        isLoading = false
    }

    LaunchedEffect(selectedTopics) {
        loadNextPage(reset = true)
    }

    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val threshold = (feedItems.size - 5).coerceAtLeast(0)
            feedItems.isNotEmpty() && lastVisible >= threshold
        }
    }

    LaunchedEffect(shouldLoadMore, hasMore) {
        if (shouldLoadMore && hasMore) {
            loadNextPage(reset = false)
        }
    }

    if (showCustomizeDialog) {
        FeedCustomizeDialog(
            selectedTopics = selectedTopics,
            onDismiss = { showCustomizeDialog = false },
            onSave = { newTopics ->
                selectedTopics = newTopics
                repository.saveSelectedTopics(newTopics.map { it.label }.toSet())
                showCustomizeDialog = false
            }
        )
    }

    val topPadding = when (gridRows) {
        1 -> 18.dp
        2 -> 14.dp
        else -> 12.dp
    } * scale

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = topPadding)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "FEED",
                    color = theme.textPrimary,
                    fontSize = (14 * scale).sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "${selectedTopics.size} topics, live from Google News",
                    color = theme.textMuted,
                    fontSize = (9 * scale).sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            TextButton(
                onClick = { showCustomizeDialog = true },
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                colors = ButtonDefaults.textButtonColors(contentColor = theme.accentPrimary)
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    tint = theme.accentPrimary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Customize",
                    fontSize = (10 * scale).sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (feedItems.isEmpty() && isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = theme.accentPrimary,
                    strokeWidth = 2.dp
                )
            }
            return
        }

        if (feedItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (selectedTopics.isEmpty()) {
                        "Pick at least one topic to start the feed."
                    } else {
                        "No updates right now."
                    },
                    color = theme.textSecondary,
                    fontSize = (11 * scale).sp,
                    fontFamily = FontFamily.Monospace,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
            return
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                count = feedItems.size,
                key = { index -> feedItems[index].id }
            ) { index ->
                val article = feedItems[index]
                FeedArticleRow(
                    article = article,
                    isExpanded = expandedItemId == article.id,
                    scale = scale,
                    onToggle = {
                        expandedItemId = if (expandedItemId == article.id) null else article.id
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = theme.accentPrimary,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                    } else if (!hasMore) {
                        Text(
                            text = "End of current updates",
                            color = theme.textMuted,
                            fontSize = (9 * scale).sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FeedArticleRow(
    article: FeedArticle,
    isExpanded: Boolean,
    scale: Float,
    onToggle: () -> Unit
) {
    val theme = LocalUnfoldTheme.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(theme.bgPanel.copy(alpha = 0.26f))
            .border(
                BorderStroke(1.dp, theme.panelBorder.copy(alpha = 0.20f)),
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onToggle)
            .animateContentSize()
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = article.title,
                    color = theme.textPrimary,
                    fontSize = (12 * scale).sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace,
                    maxLines = if (isExpanded) 4 else 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${article.source} • ${formatPublishedTime(article.publishedAtMillis)}",
                    color = theme.textMuted,
                    fontSize = (9 * scale).sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.width(10.dp))
                FeedArticleImage(
                    imageUrl = article.imageUrl,
                    topic = article.topic,
                    modifier = Modifier.size(80.dp)
                )
            }
        }

        if (isExpanded) {
            Text(
                text = article.details,
                color = theme.textSecondary,
                fontSize = (10 * scale).sp,
                lineHeight = (14 * scale).sp,
                fontFamily = FontFamily.Monospace,
                maxLines = 8,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun FeedArticleImage(
    imageUrl: String?,
    topic: String,
    modifier: Modifier = Modifier
) {
    val theme = LocalUnfoldTheme.current

    if (!imageUrl.isNullOrBlank()) {
        AsyncImage(
            model = imageUrl,
            contentDescription = topic,
            modifier = modifier
                .clip(RoundedCornerShape(10.dp))
                .background(theme.bgVoid.copy(alpha = 0.35f)),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(10.dp))
                .background(theme.bgVoid.copy(alpha = 0.35f))
                .border(1.dp, theme.panelBorder.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = topic.take(2).uppercase(Locale.getDefault()),
                color = theme.accentPrimary,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun FeedCustomizeDialog(
    selectedTopics: List<FeedTopic>,
    onDismiss: () -> Unit,
    onSave: (List<FeedTopic>) -> Unit
) {
    val theme = LocalUnfoldTheme.current
    var localSelection by remember(selectedTopics) {
        mutableStateOf(selectedTopics.toSet())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Customize Feed",
                color = theme.textPrimary,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Choose the topics that shape your feed.",
                    color = theme.textSecondary,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.heightIn(max = 320.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(top = 4.dp)
                ) {
                    items(DefaultTopics) { topic ->
                        val selected = localSelection.contains(topic)
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (selected) theme.accentPrimary.copy(alpha = 0.16f)
                                    else theme.bgPanel.copy(alpha = 0.30f)
                                )
                                .border(
                                    1.dp,
                                    if (selected) theme.accentPrimary else theme.panelBorder.copy(alpha = 0.15f),
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    localSelection = if (selected) {
                                        localSelection - topic
                                    } else {
                                        localSelection + topic
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (selected) Icons.Default.Check else Icons.Default.Close,
                                contentDescription = null,
                                tint = if (selected) theme.accentPrimary else theme.textMuted,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = topic.label,
                                color = theme.textPrimary,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(localSelection.toList()) },
                enabled = localSelection.isNotEmpty()
            ) {
                Text(
                    text = "Save",
                    color = if (localSelection.isNotEmpty()) theme.accentPrimary else theme.textMuted,
                    fontFamily = FontFamily.Monospace
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Cancel",
                    color = theme.textSecondary,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    )
}

private fun formatPublishedTime(publishedAtMillis: Long): String {
    val now = System.currentTimeMillis()
    val relative = DateUtils.getRelativeTimeSpanString(
        publishedAtMillis,
        now,
        DateUtils.MINUTE_IN_MILLIS,
        DateUtils.FORMAT_ABBREV_RELATIVE
    ).toString()
    return if (relative.isBlank()) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(publishedAtMillis))
    } else {
        relative
    }
}
