package com.unfold.core.data.local

import androidx.sqlite.db.SupportSQLiteDatabase
import com.unfold.core.data.local.entity.FolderEntity
import com.unfold.core.data.local.entity.GestureEntity
import com.unfold.core.domain.navigation.UnfoldRoute

object AppDatabaseSeedData {
    private const val SOCIAL = "SOCIAL"
    private const val MESSAGING = "MESSAGING"
    private const val COMMUNICATION = "COMMUNICATION"
    private const val GAMES = "GAMES"
    private const val ENTERTAINMENT = "ENTERTAINMENT"
    private const val MUSIC = "MUSIC"
    private const val VIDEO = "VIDEO"
    private const val PRODUCTIVITY = "PRODUCTIVITY"
    private const val OFFICE = "OFFICE"
    private const val EDUCATION = "EDUCATION"
    private const val FINANCE = "FINANCE"
    private const val SHOPPING = "SHOPPING"
    private const val FOOD_DELIVERY = "FOOD_DELIVERY"
    private const val TRAVEL = "TRAVEL"
    private const val MAPS_NAVIGATION = "MAPS_NAVIGATION"
    private const val PHOTOGRAPHY = "PHOTOGRAPHY"
    private const val VIDEO_EDITING = "VIDEO_EDITING"
    private const val HEALTH_FITNESS = "HEALTH_FITNESS"
    private const val NEWS = "NEWS"
    private const val WEATHER = "WEATHER"
    private const val UTILITIES = "UTILITIES"
    private const val TOOLS = "TOOLS"
    private const val BROWSER = "BROWSER"
    private const val EMAIL = "EMAIL"
    private const val CLOUD_STORAGE = "CLOUD_STORAGE"
    private const val DEVELOPER = "DEVELOPER"
    private const val SECURITY = "SECURITY"
    private const val LIFESTYLE = "LIFESTYLE"
    private const val GOVERNMENT = "GOVERNMENT"
    private const val TRANSPORTATION = "TRANSPORTATION"
    private const val DATING = "DATING"
    private const val BOOKS_READING = "BOOKS_READING"
    private const val KIDS = "KIDS"
    private const val ART_DESIGN = "ART_DESIGN"
    private const val AI = "AI"
    private const val OTHER = "OTHER"

    private val categoryToFolderId = mapOf(
        SOCIAL to "seed_social",
        MESSAGING to "seed_social",
        COMMUNICATION to "seed_social",
        GAMES to "seed_games",
        ENTERTAINMENT to "seed_entertainment",
        MUSIC to "seed_entertainment",
        VIDEO to "seed_entertainment",
        PRODUCTIVITY to "seed_work_productivity",
        OFFICE to "seed_work_productivity",
        EDUCATION to "seed_learn_read",
        FINANCE to "seed_finance",
        SHOPPING to "seed_shopping",
        FOOD_DELIVERY to "seed_shopping",
        TRAVEL to "seed_travel_maps",
        MAPS_NAVIGATION to "seed_travel_maps",
        PHOTOGRAPHY to "seed_creative",
        VIDEO_EDITING to "seed_creative",
        ART_DESIGN to "seed_creative",
        HEALTH_FITNESS to "seed_health_fitness",
        NEWS to "seed_news_info",
        WEATHER to "seed_news_info",
        UTILITIES to "seed_utilities",
        TOOLS to "seed_utilities",
        BROWSER to "seed_utilities",
        EMAIL to "seed_work_productivity",
        CLOUD_STORAGE to "seed_work_productivity",
        DEVELOPER to "seed_utilities",
        SECURITY to "seed_utilities",
        LIFESTYLE to "seed_lifestyle_dating",
        GOVERNMENT to "seed_utilities",
        TRANSPORTATION to "seed_travel_maps",
        DATING to "seed_lifestyle_dating",
        BOOKS_READING to "seed_learn_read",
        KIDS to "seed_learn_read",
        AI to "seed_ai",
        OTHER to "seed_other"
    )

    private val exactPackageMatches = mapOf(
        "com.instagram.android" to SOCIAL,
        "com.facebook.katana" to SOCIAL,
        "com.facebook.lite" to SOCIAL,
        "com.instagram.barcelona" to SOCIAL,
        "com.twitter.android" to SOCIAL,
        "com.reddit.frontpage" to SOCIAL,
        "com.snapchat.android" to SOCIAL,
        "com.pinterest" to SOCIAL,
        "com.linkedin.android" to SOCIAL,
        "com.zhiliaoapp.musically" to SOCIAL,
        "com.discord" to SOCIAL,
        "org.joinmastodon.android" to SOCIAL,
        "xyz.blueskyweb.app" to SOCIAL,
        "com.whatsapp" to MESSAGING,
        "com.whatsapp.w4b" to MESSAGING,
        "org.telegram.messenger" to MESSAGING,
        "org.thoughtcrime.securesms" to MESSAGING,
        "com.facebook.orca" to MESSAGING,
        "com.viber.voip" to MESSAGING,
        "jp.naver.line.android" to MESSAGING,
        "com.tencent.mm" to MESSAGING,
        "com.skype.raider" to MESSAGING,
        "com.google.android.apps.messaging" to MESSAGING,
        "com.google.android.apps.tachyon" to COMMUNICATION,
        "us.zoom.videomeetings" to COMMUNICATION,
        "com.microsoft.teams" to COMMUNICATION,
        "com.google.android.dialer" to COMMUNICATION,
        "com.tencent.ig" to GAMES,
        "com.pubg.imobile" to GAMES,
        "com.dts.freefireth" to GAMES,
        "com.roblox.client" to GAMES,
        "com.supercell.clashofclans" to GAMES,
        "com.supercell.clashroyale" to GAMES,
        "com.supercell.brawlstars" to GAMES,
        "com.king.candycrushsaga" to GAMES,
        "com.nianticlabs.pokemongo" to GAMES,
        "com.miHoYo.GenshinImpact" to GAMES,
        "com.netflix.mediaclient" to ENTERTAINMENT,
        "com.amazon.avod.thirdpartyclient" to ENTERTAINMENT,
        "com.spotify.music" to MUSIC,
        "com.google.android.apps.translate" to PRODUCTIVITY,
        "com.google.android.apps.docs" to PRODUCTIVITY,
        "com.google.android.gm" to EMAIL,
        "com.google.android.apps.messaging" to MESSAGING,
        "com.google.android.apps.translate" to PRODUCTIVITY,
        "com.paytm" to FINANCE,
        "com.phonepe.app" to FINANCE,
        "com.google.android.apps.nbu.paisa.user" to FINANCE,
        "com.amazon.mShop.android.shopping" to SHOPPING,
        "com.google.android.apps.maps" to MAPS_NAVIGATION,
        "com.google.android.apps.photos" to PHOTOGRAPHY,
        "com.google.android.apps.translate" to PRODUCTIVITY,
        "com.google.android.apps.docs" to PRODUCTIVITY,
        "com.android.chrome" to BROWSER,
        "com.google.android.apps.messaging" to MESSAGING
    )

    private val packagePrefixRules = listOf(
        "^com\\.supercell\\." to GAMES,
        "^com\\.king\\." to GAMES,
        "^com\\.miHoYo\\.|^com\\.HoYoverse\\." to GAMES,
        "^com\\.google\\.android\\.apps\\.docs.*" to PRODUCTIVITY,
        ".*\\.bank.*|.*\\.upi.*|.*payzapp.*|.*mobile.*" to FINANCE
    )

    private val keywordRules = listOf(
        "bank" to FINANCE,
        "pay" to FINANCE,
        "wallet" to FINANCE,
        "photo" to PHOTOGRAPHY,
        "camera" to PHOTOGRAPHY,
        "video" to VIDEO,
        "music" to MUSIC,
        "mail" to EMAIL,
        "drive" to CLOUD_STORAGE,
        "docs" to PRODUCTIVITY,
        "maps" to MAPS_NAVIGATION,
        "weather" to WEATHER,
        "news" to NEWS,
        "browser" to BROWSER,
        "chrome" to BROWSER,
        "chat" to MESSAGING,
        "messenger" to MESSAGING,
        "call" to COMMUNICATION,
        "meet" to COMMUNICATION,
        "zoom" to COMMUNICATION,
        "game" to GAMES,
        "play" to GAMES,
        "fitness" to HEALTH_FITNESS,
        "health" to HEALTH_FITNESS,
        "spotify" to MUSIC,
        "netflix" to ENTERTAINMENT,
        "travel" to TRAVEL,
        "hotel" to TRAVEL,
        "booking" to TRAVEL,
        "shop" to SHOPPING,
        "store" to SHOPPING,
        "ai" to AI,
        "assistant" to AI
    )

    fun defaultFolders(): List<FolderEntity> = listOf(
        FolderEntity(id = "seed_social", name = "Social", gridPosition = 0),
        FolderEntity(id = "seed_games", name = "Games", gridPosition = 1),
        FolderEntity(id = "seed_entertainment", name = "Entertainment", gridPosition = 2),
        FolderEntity(id = "seed_work_productivity", name = "Work & Productivity", gridPosition = 3),
        FolderEntity(id = "seed_finance", name = "Finance", gridPosition = 4),
        FolderEntity(id = "seed_shopping", name = "Shopping", gridPosition = 5),
        FolderEntity(id = "seed_travel_maps", name = "Travel & Maps", gridPosition = 6),
        FolderEntity(id = "seed_creative", name = "Creative", gridPosition = 7),
        FolderEntity(id = "seed_health_fitness", name = "Health & Fitness", gridPosition = 8),
        FolderEntity(id = "seed_learn_read", name = "Learn & Read", gridPosition = 9),
        FolderEntity(id = "seed_news_info", name = "News & Info", gridPosition = 10),
        FolderEntity(id = "seed_ai", name = "AI", gridPosition = 11),
        FolderEntity(id = "seed_utilities", name = "Utilities", gridPosition = 12),
        FolderEntity(id = "seed_lifestyle_dating", name = "Lifestyle & Dating", gridPosition = 13),
        FolderEntity(id = "seed_other", name = "Other", gridPosition = 14)
    )

    fun defaultGestures(): List<GestureEntity> = listOf(
        GestureEntity("SWIPE_LEFT_1F", "OPEN_INTENT", targetIntentUri = "tel:"),
        GestureEntity("SWIPE_RIGHT_1F", "LAUNCH_APP", targetPackage = "com.whatsapp"),
        GestureEntity("SWIPE_LEFT_2F", "OPEN_SCREEN", targetScreenRoute = UnfoldRoute.HiddenSpace.route),
        GestureEntity("DOCK_SWIPE_HOLD", "OPEN_SCREEN", targetScreenRoute = UnfoldRoute.HiddenSpace.route),
        GestureEntity("SWIPE_RIGHT_2F", "OPEN_INTENT", targetIntentUri = "market://details?id="),
        GestureEntity("SWIPE_DOWN_1F", "OPEN_SCREEN", targetScreenRoute = UnfoldRoute.UniversalSearch.route),
        GestureEntity("SWIPE_UP_1F", "OPEN_SCREEN", targetScreenRoute = UnfoldRoute.AppDrawer.route)
    )

    fun resolveCategory(packageName: String?, label: String?): String {
        val normalizedPackage = packageName.orEmpty().lowercase()
        val normalizedLabel = label.orEmpty().lowercase()

        exactPackageMatches[normalizedPackage]?.let { return it }

        packagePrefixRules.forEach { (pattern, category) ->
            if (normalizedPackage.matches(Regex(pattern, RegexOption.IGNORE_CASE))) {
                return category
            }
        }

        keywordRules.forEach { (keyword, category) ->
            if (normalizedPackage.contains(keyword, ignoreCase = true) || normalizedLabel.contains(keyword, ignoreCase = true)) {
                return category
            }
        }

        return OTHER
    }

    fun folderIdForCategory(category: String?): String {
        return categoryToFolderId[category] ?: "seed_other"
    }

    fun seedDatabase(db: SupportSQLiteDatabase) {
        defaultFolders().forEach { folder ->
            db.execSQL(
                "INSERT OR IGNORE INTO folders (id, name, gridPosition, accentColorOverride) VALUES (?, ?, ?, ?)",
                arrayOf(folder.id, folder.name, folder.gridPosition, folder.accentColorOverride)
            )
        }

        defaultGestures().forEach { gesture ->
            db.execSQL(
                "INSERT OR IGNORE INTO gesture_bindings (gestureType, actionType, targetPackage, targetIntentUri, targetScreenRoute, targetShortcutId, isUserModified) VALUES (?, ?, ?, ?, ?, ?, ?)",
                arrayOf(
                    gesture.gestureType,
                    gesture.actionType,
                    gesture.targetPackage,
                    gesture.targetIntentUri,
                    gesture.targetScreenRoute,
                    gesture.targetShortcutId,
                    if (gesture.isUserModified) 1 else 0
                )
            )
        }
    }
}
