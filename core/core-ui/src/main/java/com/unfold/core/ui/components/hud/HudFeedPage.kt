package com.unfold.core.ui.components.hud

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
import kotlinx.coroutines.launch
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
private const val PREF_AUTO_REFRESH_MS = "auto_refresh_interval_ms"
private const val PREF_LAST_REFRESH_AT = "last_refresh_at_ms"
private const val DEFAULT_AUTO_REFRESH_MS = 5 * 60 * 1000L

private val FeedWindows = listOf("1d", "3d", "7d", "14d", "30d")

private val DefaultTopics = listOf(
    FeedTopic.AI,
    FeedTopic.TECHNOLOGY,
    FeedTopic.SOCIAL,
    FeedTopic.ECONOMY,
    FeedTopic.SCIENCE,
    FeedTopic.SPORTS,
    FeedTopic.GAMING,
    FeedTopic.BUSINESS,
    FeedTopic.POLITICS,
    FeedTopic.HEALTH,
    FeedTopic.CLIMATE,
    FeedTopic.CRYPTO,
    FeedTopic.ENTERTAINMENT,
    FeedTopic.WORLD
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
    BUSINESS("Business", "business OR startups OR companies"),
    POLITICS("Politics", "politics OR government OR elections"),
    HEALTH("Health", "health OR wellness OR medicine"),
    CLIMATE("Climate", "climate OR environment OR sustainability"),
    CRYPTO("Crypto", "crypto OR bitcoin OR blockchain"),
    ENTERTAINMENT("Entertainment", "entertainment OR film OR music"),
    WORLD("World", "world news OR international affairs OR global economy");

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

internal data class ExtractedArticleContent(
    val title: String,
    val body: String,
    val imageUrl: String?,
    val author: String?,
    val publishedAtMillis: Long?,
    val source: String?
)

private class GoogleFeedRepository(private val context: Context) {
    private val prefs = context.getSharedPreferences(FEED_PREFS_NAME, Context.MODE_PRIVATE)
    private val articleCache = mutableMapOf<String, ExtractedArticleContent>()

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

    fun getAutoRefreshIntervalMs(): Long =
        prefs.getLong(PREF_AUTO_REFRESH_MS, DEFAULT_AUTO_REFRESH_MS)

    fun saveAutoRefreshIntervalMs(intervalMs: Long) {
        prefs.edit().putLong(PREF_AUTO_REFRESH_MS, intervalMs).apply()
    }

    fun getLastRefreshAt(): Long = prefs.getLong(PREF_LAST_REFRESH_AT, 0L)

    fun saveLastRefreshAt(timestamp: Long) {
        prefs.edit().putLong(PREF_LAST_REFRESH_AT, timestamp).apply()
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

    suspend fun loadExpandedArticle(
        link: String,
        fallbackTitle: String,
        fallbackImageUrl: String?
    ): ExtractedArticleContent? = withContext(Dispatchers.IO) {
        val normalizedLink = link.trim()
        if (normalizedLink.isBlank()) return@withContext null

        val resolvedLink = resolveRedirectUrl(normalizedLink) ?: normalizedLink
        val cacheKey = resolvedLink.lowercase(Locale.US)
        articleCache[cacheKey]?.let { return@withContext it }

        val fetched = fetchArticlePageContent(resolvedLink, fallbackTitle, fallbackImageUrl)
        if (fetched != null) {
            articleCache[cacheKey] = fetched
        }
        fetched
    }

    private fun resolveRedirectUrl(rawUrl: String): String? {
        runCatching {
            val connection = (URL(rawUrl).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = true
                connectTimeout = 7000
                readTimeout = 7000
                requestMethod = "GET"
            }
            return try {
                val finalUrl = connection.url?.toString()?.trim()
                if (finalUrl.isNullOrBlank()) rawUrl else finalUrl
            } finally {
                connection.disconnect()
            }
        }
        return null
    }

    private fun fetchArticlePageContent(
        link: String,
        fallbackTitle: String,
        fallbackImageUrl: String?
    ): ExtractedArticleContent? {
        val connection = (URL(link).openConnection() as HttpURLConnection).apply {
            connectTimeout = 7000
            readTimeout = 10000
            requestMethod = "GET"
            instanceFollowRedirects = true
        }

        return try {
            val html = connection.inputStream.bufferedReader().use { it.readText() }
            val extracted = ArticleContentExtractor.extract(html, fallbackTitle)
                ?: return null

            ExtractedArticleContent(
                title = extracted.title.ifBlank { fallbackTitle },
                body = extracted.body,
                imageUrl = extracted.imageUrl ?: fallbackImageUrl,
                author = extracted.author,
                publishedAtMillis = extracted.publishedAtMillis,
                source = extracted.source
            )
        } catch (_: Exception) {
            null
        } finally {
            connection.disconnect()
        }
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
        var contentEncoded = ""
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
                    "content:encoded" -> contentEncoded = parser.nextText().trim()
                    "source" -> source = parser.nextText().trim().ifBlank { source }
                    "pubDate" -> publishedAtMillis = parsePubDate(parser.nextText().trim())
                    "thumbnail", "media:thumbnail" -> {
                        val url = parser.getAttributeValue(null, "url")?.trim()
                        imageUrl = normalizeImageUrl(imageUrl ?: url)
                    }
                    "content", "media:content" -> {
                        val url = parser.getAttributeValue(null, "url")?.trim()
                        imageUrl = normalizeImageUrl(imageUrl ?: url)
                    }
                    "enclosure" -> {
                        val url = parser.getAttributeValue(null, "url")?.trim()
                        imageUrl = normalizeImageUrl(imageUrl ?: url)
                    }
                    "img" -> {
                        val url = parser.getAttributeValue(null, "src")?.trim()
                        imageUrl = normalizeImageUrl(imageUrl ?: url)
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
        val cleanImageUrl = normalizeImageUrl(
            imageUrl
                ?: extractImageUrlFromHtml(contentEncoded)
                ?: extractImageUrlFromHtml(description)
                ?: extractImageUrlFromHtml(title)
        )

        return FeedArticle(
            id = "${cleanLink.lowercase(Locale.US)}|${cleanTitle.lowercase(Locale.US)}",
            title = cleanTitle,
            details = "",
            source = cleanSource,
            publishedAtMillis = publishedAtMillis.takeIf { it > 0L } ?: System.currentTimeMillis(),
            imageUrl = cleanImageUrl,
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

    private fun normalizeImageUrl(value: String?): String? {
        val candidate = value?.trim() ?: return null
        val normalized = if (candidate.startsWith("//")) "https:$candidate" else candidate
        val lower = normalized.lowercase(Locale.US)
        if (lower.contains("placeholder") || lower.contains("blank") || lower.contains("pixel") || lower.contains("spacer")) {
            return null
        }
        return normalized
    }

    private fun extractImageUrlFromHtml(value: String): String? {
        if (value.isBlank()) return null
        return Regex("(?:src|data-src|content)=\\s*['\"]([^'\"]+)['\"]", RegexOption.IGNORE_CASE)
            .find(value)
            ?.groupValues
            ?.getOrNull(1)
            ?.let(::normalizeImageUrl)
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

internal object ArticleContentExtractor {
    fun extract(html: String, fallbackTitle: String): ExtractedArticleContent? {
        val normalizedHtml = html.ifBlank { return null }
        val articleBlock = findArticleBlock(normalizedHtml)
        val paragraphCandidates = extractParagraphCandidates(articleBlock ?: normalizedHtml, fallbackTitle)
        val body = paragraphCandidates.joinToString("\n\n").trim()

        val title = fallbackTitle.trim()
        if (body.isBlank() || body.length < 80 || body.equals(title, ignoreCase = true)) {
            return null
        }

        val articleTitle = if (title.isNotBlank()) title else "Article"
        val finalBody = if (body.startsWith(title, ignoreCase = true)) {
            body.removePrefix(title).trim().trim('-', '—', ':', '|', '•', '·').trim()
        } else body

        if (finalBody.isBlank() || finalBody.length < 80 || finalBody.equals(title, ignoreCase = true)) {
            return null
        }

        return ExtractedArticleContent(
            title = articleTitle,
            body = finalBody,
            imageUrl = extractImageUrlFromHtml(normalizedHtml),
            author = extractMetaValue(normalizedHtml, "author|article:author|twitter:creator|byline"),
            publishedAtMillis = extractMetaDate(normalizedHtml),
            source = extractMetaValue(normalizedHtml, "og:site_name|application-name")
        )
    }

    private fun findArticleBlock(html: String): String? {
        val patterns = listOf(
            Regex("(?is)<article[^>]*>(.*?)</article>"),
            Regex("(?is)<main[^>]*>(.*?)</main>"),
            Regex("(?is)<div[^>]*(?:class|id)=[\"'][^\"']*(?:article|story|post|content|entry)[^\"']*[\"'][^>]*>(.*?)</div>")
        )
        return patterns.firstNotNullOfOrNull { regex ->
            regex.find(html)?.groupValues?.getOrNull(1)
        }
    }

    private fun extractParagraphCandidates(html: String, fallbackTitle: String): List<String> {
        val content = html
            .replace(Regex("(?is)<script.*?</script>"), " ")
            .replace(Regex("(?is)<style.*?</style>"), " ")
            .replace(Regex("(?is)<(header|nav|footer|aside|form|button|svg|noscript|iframe).*?>.*?</\\1>"), " ")
            .replace(Regex("(?is)<(br|hr)[^>]*>"), "\n")
            .replace(Regex("(?is)</(p|li|div|section|article|main|h[1-6])>"), "\n")
            .replace(Regex("(?is)<(p|li|div|section|article|main|h[1-6])[^>]*>"), "")

        val paragraphs = Regex("(?is)\\n+|<[^>]+>").replace(content, " ")
            .replace(Regex("&nbsp;|&#160;|&amp;|&lt;|&gt;|&quot;|&#39;"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .split(Regex("(?<=\\.)\\s+(?=[A-Z])|\\n+"))
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .mapNotNull { paragraph ->
                val cleaned = stripHtml(paragraph)
                    .replace(Regex("\\s+"), " ")
                    .trim()
                if (cleaned.length < 80) return@mapNotNull null
                if (cleaned.equals(fallbackTitle, ignoreCase = true)) return@mapNotNull null
                if (cleaned.startsWith(fallbackTitle, ignoreCase = true) && cleaned.length < fallbackTitle.length + 40) return@mapNotNull null
                cleaned
            }
            .distinctBy { it.lowercase(Locale.US) }

        return paragraphs
    }

    private fun extractMetaValue(html: String, keyPattern: String): String? {
        val regex = Regex(
            "(?is)<meta[^>]+(?:name|property)=[\"'](?:${keyPattern})[\"'][^>]*content=[\"']([^\"']+)[\"'][^>]*>|<meta[^>]+content=[\"']([^\"']+)[\"'][^>]+(?:name|property)=[\"'](?:${keyPattern})[\"'][^>]*>",
            RegexOption.IGNORE_CASE
        )
        val match = regex.find(html) ?: return null
        return match.groupValues.drop(1).firstOrNull { it.isNotBlank() }?.trim()
            ?.replace(Regex("\\s+"), " ")
    }

    private fun extractMetaDate(html: String): Long? {
        val dateStrings = listOf(
            "article:published_time",
            "pubdate",
            "publishdate",
            "datePublished",
            "dateModified"
        )

        val value = dateStrings.firstNotNullOfOrNull { key ->
            extractMetaValue(html, key)
        } ?: return null

        val candidates = listOf(
            value,
            value.replace("Z", "+00:00")
        )

        for (candidate in candidates) {
            for (pattern in listOf(
                "yyyy-MM-dd'T'HH:mm:ssXXX",
                "yyyy-MM-dd'T'HH:mm:ss",
                "EEE, dd MMM yyyy HH:mm:ss z",
                "yyyy-MM-dd'T'HH:mm:ss.SSSXXX"
            )) {
                runCatching {
                    val parsed = SimpleDateFormat(pattern, Locale.US).parse(candidate)?.time
                    if (parsed != null && parsed > 0L) return parsed
                }
            }
        }

        return null
    }

    private fun extractImageUrlFromHtml(value: String): String? {
        if (value.isBlank()) return null
        return Regex("(?:src|data-src|content)=\\s*['\"]([^'\"]+)['\"]", RegexOption.IGNORE_CASE)
            .find(value)
            ?.groupValues
            ?.getOrNull(1)
            ?.trim()
            ?.let { candidate ->
                val normalized = if (candidate.startsWith("//")) "https:$candidate" else candidate
                return@let if (normalized.lowercase(Locale.US).contains("placeholder") || normalized.lowercase(Locale.US).contains("pixel") || normalized.lowercase(Locale.US).contains("blank")) null else normalized
            }
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
    var articleLoadingId by remember { mutableStateOf<String?>(null) }
    var hasMore by remember { mutableStateOf(true) }
    var pageIndex by remember { mutableIntStateOf(0) }
    val autoRefreshIntervalMs = remember(repository) { repository.getAutoRefreshIntervalMs() }

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
        repository.saveLastRefreshAt(System.currentTimeMillis())
        isLoading = false
    }

    LaunchedEffect(selectedTopics) {
        loadNextPage(reset = true)
        while (true) {
            kotlinx.coroutines.delay(autoRefreshIntervalMs.coerceAtLeast(60_000L))
            if (!isLoading) {
                loadNextPage(reset = false)
            }
        }
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
                    text = "live",
                    color = theme.textMuted,
                    fontSize = (9 * scale).sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
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

                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(50))
                        .background(theme.bgPanel.copy(alpha = 0.42f))
                        .border(1.dp, theme.accentPrimary.copy(alpha = 0.38f), RoundedCornerShape(50))
                        .clickable { scope.launch { loadNextPage(reset = true) } },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh feed",
                        tint = theme.accentPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                }
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
                    isLoadingBody = articleLoadingId == article.id,
                    onToggle = {
                        val nextExpanded = if (expandedItemId == article.id) null else article.id
                        expandedItemId = nextExpanded
                        if (nextExpanded != null && article.details.isBlank() && articleLoadingId != article.id) {
                            articleLoadingId = article.id
                            scope.launch {
                                val extracted = repository.loadExpandedArticle(
                                    article.link,
                                    article.title,
                                    article.imageUrl
                                )
                                val fallbackText = "Unable to load article preview"
                                feedItems = feedItems.map { current ->
                                    if (current.id != article.id) current else {
                                        current.copy(
                                            details = extracted?.body ?: fallbackText,
                                            imageUrl = extracted?.imageUrl ?: current.imageUrl
                                        )
                                    }
                                }
                                articleLoadingId = null
                            }
                        }
                    },
                    onOpenArticle = { url ->
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        context.startActivity(intent)
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
    isLoadingBody: Boolean,
    onToggle: () -> Unit,
    onOpenArticle: (String) -> Unit
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
        Text(
            text = article.title,
            color = theme.textPrimary,
            fontSize = (12 * scale).sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace,
            maxLines = if (isExpanded) 4 else 2,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = "${article.source} • ${formatPublishedTime(article.publishedAtMillis)}",
            color = theme.textMuted,
            fontSize = (9 * scale).sp,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        if (isExpanded) {
            FeedArticleImage(
                imageUrl = article.imageUrl,
                topic = article.topic,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
            )

            when {
                isLoadingBody -> {
                    Text(
                        text = "Loading article preview...",
                        color = theme.textSecondary,
                        fontSize = (10 * scale).sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                article.details.isNotBlank() && article.details != "Unable to load article preview" -> {
                    Text(
                        text = article.details,
                        color = theme.textSecondary,
                        fontSize = (10 * scale).sp,
                        lineHeight = (14 * scale).sp,
                        fontFamily = FontFamily.Monospace,
                        overflow = TextOverflow.Clip
                    )
                }
                article.details == "Unable to load article preview" -> {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                        modifier = Modifier.clickable { onOpenArticle(article.link) }
                    ) {
                        Text(
                            text = article.details,
                            color = theme.textSecondary,
                            fontSize = (10 * scale).sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Open article",
                            color = theme.accentPrimary,
                            fontSize = (9 * scale).sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FeedArticleImage(
    imageUrl: String?,
    topic: String,
    modifier: Modifier = Modifier
) {
    if (imageUrl.isNullOrBlank()) return

    val theme = LocalUnfoldTheme.current
    AsyncImage(
        model = imageUrl,
        contentDescription = topic,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(theme.bgVoid.copy(alpha = 0.35f)),
        contentScale = ContentScale.Crop
    )
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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
    ) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = theme.bgPanel.copy(alpha = 0.94f),
            tonalElevation = 0.dp,
            border = BorderStroke(1.dp, theme.accentPrimary.copy(alpha = 0.25f)),
            modifier = Modifier
                .fillMaxWidth(0.9f)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Customize Feed",
                    color = theme.textPrimary,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )

                Text(
                    text = "Choose the topics that shape your feed.",
                    color = theme.textSecondary,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.heightIn(max = 360.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(top = 4.dp)
                ) {
                    items(DefaultTopics) { topic ->
                        val selected = localSelection.contains(topic)
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (selected) theme.accentPrimary.copy(alpha = 0.18f)
                                    else theme.bgPanel.copy(alpha = 0.28f)
                                )
                                .border(
                                    1.dp,
                                    if (selected) theme.accentPrimary else theme.panelBorder.copy(alpha = 0.18f),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    localSelection = if (selected) {
                                        localSelection - topic
                                    } else {
                                        localSelection + topic
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = "Cancel",
                            color = theme.textSecondary,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    TextButton(
                        onClick = { onSave(localSelection.toList()) },
                        enabled = localSelection.isNotEmpty()
                    ) {
                        Text(
                            text = "Save",
                            color = if (localSelection.isNotEmpty()) theme.accentPrimary else theme.textMuted,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
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
