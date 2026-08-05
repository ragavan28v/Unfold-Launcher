# VOLT Launcher — Implementation Blueprint (Addendum)
### Pairs with `launcher-RnD-spec.md`. This document is the one to paste into Antigravity alongside the R&D doc — it removes ambiguity so the agent builds *your* design instead of its own defaults.

---

## 1. Gradle Version Catalog

`gradle/libs.versions.toml` — pin these explicitly so the agent doesn't pick mismatched versions:

```toml
[versions]
kotlin = "2.0.21"
agp = "8.7.2"
composeBom = "2024.11.00"
composeCompiler = "1.5.15"
hilt = "2.52"
room = "2.6.1"
datastore = "1.1.1"
coroutines = "1.9.0"
coil = "2.7.0"
lifecycle = "2.8.7"
navigationCompose = "2.8.4"
workManager = "2.10.0"
biometric = "1.2.0-alpha05"

[libraries]
androidx-core-ktx = { module = "androidx.core:core-ktx", version = "1.15.0" }
compose-bom = { module = "androidx.compose:compose-bom", version.ref = "composeBom" }
compose-ui = { module = "androidx.compose.ui:ui" }
compose-material3 = { module = "androidx.compose.material3:material3" }
compose-foundation = { module = "androidx.compose.foundation:foundation" }
hilt-android = { module = "com.google.dagger:hilt-android", version.ref = "hilt" }
hilt-compiler = { module = "com.google.dagger:hilt-compiler", version.ref = "hilt" }
hilt-navigation-compose = { module = "androidx.hilt:hilt-navigation-compose", version = "1.2.0" }
room-runtime = { module = "androidx.room:room-runtime", version.ref = "room" }
room-ktx = { module = "androidx.room:room-ktx", version.ref = "room" }
room-compiler = { module = "androidx.room:room-compiler", version.ref = "room" }
datastore-proto = { module = "androidx.datastore:datastore", version.ref = "datastore" }
coroutines-android = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-android", version.ref = "coroutines" }
coil-compose = { module = "io.coil-kt:coil-compose", version.ref = "coil" }
navigation-compose = { module = "androidx.navigation:navigation-compose", version.ref = "navigationCompose" }
lifecycle-viewmodel-compose = { module = "androidx.lifecycle:lifecycle-viewmodel-compose", version.ref = "lifecycle" }
work-runtime-ktx = { module = "androidx.work:work-runtime-ktx", version.ref = "workManager" }
biometric-ktx = { module = "androidx.biometric:biometric-ktx", version.ref = "biometric" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
android-library = { id = "com.android.library", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-kapt = { id = "org.jetbrains.kotlin.kapt", version.ref = "kotlin" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
```

`minSdk = 26` (Adaptive Icons require it, and your carve-compositor depends on `AdaptiveIconDrawable`), `targetSdk = 35`, `compileSdk = 35`.

---

## 2. Navigation Graph

Single `NavHost` inside `MainActivity`, routes as sealed destinations (not raw strings, so Antigravity can't typo a route):

```kotlin
sealed class VoltRoute(val route: String) {
    data object Home : VoltRoute("home")
    data object AppDrawer : VoltRoute("drawer")
    data class Folder(val folderId: String) : VoltRoute("folder/{folderId}") {
        companion object { const val PATTERN = "folder/{folderId}" }
    }
    data object HiddenSpace : VoltRoute("hidden_space")
    data object HiddenFiles : VoltRoute("hidden_files")
    data object WidgetPicker : VoltRoute("widget_picker")
    data object Settings : VoltRoute("settings")
    data object ThemeEditor : VoltRoute("settings/theme")
    data object GestureSettings : VoltRoute("settings/gestures")
    data object GestureTrainer : VoltRoute("settings/gestures/train/{gestureType}") {
        companion object { const val PATTERN = "settings/gestures/train/{gestureType}" }
    }
    data object IconPackPicker : VoltRoute("settings/icon_pack")
    data object BackupRestore : VoltRoute("settings/backup")
    data object UniversalSearch : VoltRoute("search")
    data object FocusMode : VoltRoute("focus")
}
```

Transition rule: Home → Drawer uses vertical slide (matches swipe-up gesture direction). Everything else uses the 280ms circular-reveal defined in the theme motion tokens, originating from the triggering icon/gesture point — pass origin `Offset` via a shared `SharedTransitionScope` or a simple singleton `LastTapPosition` holder if you're not on Compose's shared-element APIs yet.

---

## 3. Room Schema — Exact Entities

```kotlin
@Entity(tableName = "apps")
data class AppEntity(
    @PrimaryKey val packageName: String,
    val activityName: String,
    val label: String,
    val isHidden: Boolean = false,
    val isLocked: Boolean = false,          // biometric gate on open
    val customLabel: String? = null,
    val folderId: String? = null,
    val gridPosition: Int? = null,          // null = in drawer only, not on grid
    val category: String? = null,           // auto-tagged, user-overridable
    val installTimestamp: Long,
    val lastUsedTimestamp: Long = 0L,
    val launchCount: Long = 0L
)

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey val id: String,
    val name: String,
    val gridPosition: Int,
    val accentColorOverride: String? = null // hex, null = inherit theme accent
)

@Entity(tableName = "gesture_bindings")
data class GestureEntity(
    @PrimaryKey val gestureType: String,    // enum name, e.g. "SWIPE_LEFT_1F"
    val actionType: String,                 // "LAUNCH_APP" | "OPEN_INTENT" | "OPEN_SCREEN" | "SYSTEM_TOGGLE" | "SHORTCUT"
    val targetPackage: String? = null,
    val targetIntentUri: String? = null,
    val targetScreenRoute: String? = null,
    val targetShortcutId: String? = null,
    val isUserModified: Boolean = false
)

@Entity(tableName = "hidden_files")
data class HiddenFileEntity(
    @PrimaryKey val uriString: String,      // SAF persisted URI
    val displayName: String,
    val addedTimestamp: Long
)

@Entity(tableName = "layout_snapshots")     // for backup/export/import
data class LayoutSnapshotEntity(
    @PrimaryKey val id: String,
    val jsonPayload: String,
    val createdTimestamp: Long,
    val label: String
)
```

`AppDatabase.kt`:
```kotlin
@Database(
    entities = [AppEntity::class, FolderEntity::class, GestureEntity::class,
                HiddenFileEntity::class, LayoutSnapshotEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao
    abstract fun folderDao(): FolderDao
    abstract fun gestureDao(): GestureDao
    abstract fun hiddenFileDao(): HiddenFileDao
    abstract fun layoutSnapshotDao(): LayoutSnapshotDao
}
```

Seed the 4 default gesture rows on first DB creation via a `RoomDatabase.Callback`:
```kotlin
val DEFAULT_GESTURES = listOf(
    GestureEntity("SWIPE_LEFT_1F", "OPEN_INTENT", targetIntentUri = "tel:"),
    GestureEntity("SWIPE_RIGHT_1F", "LAUNCH_APP", targetPackage = "com.whatsapp"),
    GestureEntity("SWIPE_LEFT_2F", "OPEN_SCREEN", targetScreenRoute = VoltRoute.HiddenSpace.route),
    GestureEntity("SWIPE_RIGHT_2F", "OPEN_INTENT", targetIntentUri = "market://details?id=")
)
```

---

## 4. Theme DataStore (Proto)

`theme_config.proto`:
```proto
syntax = "proto3";
option java_package = "com.volt.core.data.datastore";
option java_multiple_classes = true;

message ThemeConfig {
  string accent_primary_hex = 1;      // default "#38BDF8"
  string accent_secondary_hex = 2;    // default "#6366F1"
  float bevel_intensity = 3;          // 0.0 - 1.0, default 0.6
  float blur_radius_dp = 4;           // default 24
  float panel_opacity = 5;            // default 0.72
  bool time_adaptive_hue = 6;         // default true
  bool reduced_motion = 7;            // default false
  int32 grid_columns = 8;             // default 4
  int32 grid_rows = 9;                // default 6
  string icon_pack_package = 10;      // "" = built-in
  bool sound_feedback_enabled = 11;   // default false
}
```

---

## 5. Core UI — Exact Composable Signatures

```kotlin
@Composable
fun CarvedIcon(
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    icon: @Composable () -> Unit,        // slot: Image/Text/glyph content, inset ~14%
    isPressed: Boolean = false,           // drives the shadow-inversion animation
    accentTint: Color = LocalVoltTheme.current.accentPrimary,
    bevelIntensity: Float = LocalVoltTheme.current.bevelIntensity,
    badgeCount: Int? = null,              // notification badge, null = no badge
    onClick: (() -> Unit)? = null,
    onLongPress: (() -> Unit)? = null,
    contentDescription: String            // required — accessibility, no default
)

@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    blurRadius: Dp = LocalVoltTheme.current.blurRadius,
    opacity: Float = LocalVoltTheme.current.panelOpacity,
    borderColor: Color = LocalVoltTheme.current.panelBorder,
    content: @Composable BoxScope.() -> Unit
)

@Composable
fun NodeRail(
    modifier: Modifier = Modifier,
    nodes: List<RailNode>,                // user-customizable, min 2 max 5
    activeNodeId: String,
    onNodeSelected: (String) -> Unit
)
data class RailNode(val id: String, val icon: ImageVector, val label: String)

@Composable
fun HUDGauge(
    modifier: Modifier = Modifier,
    value: Float,                         // 0f..1f
    label: String,                        // e.g. "BATTERY"
    valueText: String,                    // e.g. "12%"
    ringColor: Color = LocalVoltTheme.current.accentPrimary,
    warningThreshold: Float? = 0.15f      // renders ringColor as accent.warn below this
)

@Composable
fun SignalBar(
    modifier: Modifier = Modifier,
    level: Int,                           // 0..4
    activeColor: Color = LocalVoltTheme.current.accentPrimary
)

@Composable
fun PillBadge(
    modifier: Modifier = Modifier,
    text: String,
    tint: Color = LocalVoltTheme.current.accentSecondary
)
```

`BevelShaderUtil.kt` — the actual carve math, as a `DrawScope` extension so it's reusable outside `CarvedIcon` too (e.g. dock, folder bubbles):

```kotlin
fun DrawScope.drawCarvedBevel(
    radius: Float,
    intensity: Float,
    isInverted: Boolean,       // true while pressed
    accentColor: Color
) {
    val darkOffset = if (isInverted) Offset(-3f, -4f) else Offset(3f, 4f)
    val lightOffset = if (isInverted) Offset(3f, 4f) else Offset(-2f, -2f)

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.Black.copy(alpha = 0.55f * intensity), Color.Transparent),
            center = center + darkOffset
        ),
        radius = radius
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(accentColor.copy(alpha = 0.10f * intensity), Color.Transparent),
            center = center + lightOffset
        ),
        radius = radius
    )
    drawCircle(
        color = accentColor.copy(alpha = 0.35f),
        radius = radius,
        style = Stroke(width = 1.dp.toPx())
    )
}
```

---

## 6. ViewModel State/Intent Contracts

Consistent pattern across every screen: `UiState` data class + sealed `UiIntent`, `ViewModel` exposes `StateFlow<UiState>` and a single `onIntent(intent: UiIntent)` entry point. This is the contract Antigravity should replicate for every new screen — spell it out once so it doesn't reinvent it five different ways.

```kotlin
// HomeViewModel.kt
data class HomeUiState(
    val panels: List<HomePanelType> = emptyList(),
    val gridApps: List<AppInfo> = emptyList(),
    val folders: List<FolderInfo> = emptyList(),
    val systemStats: SystemStats? = null,
    val isLoading: Boolean = true
)
sealed interface HomeUiIntent {
    data class ReorderGrid(val fromIndex: Int, val toIndex: Int) : HomeUiIntent
    data class OpenApp(val packageName: String) : HomeUiIntent
    data class OpenFolder(val folderId: String) : HomeUiIntent
    data object RefreshStats : HomeUiIntent
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getInstalledApps: GetInstalledAppsUseCase,
    private val getSystemStats: GetSystemStatsUseCase,
    private val reorderGrid: ReorderHomeGridUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun onIntent(intent: HomeUiIntent) { /* when(intent) { ... } */ }
}
```

Apply the identical shape (`XUiState` + `XUiIntent` + `onIntent`) to: `AppDrawerViewModel`, `GestureSettingsViewModel`, `HiddenSpaceViewModel`, `ThemeEditorViewModel`, `WidgetHostViewModel`. Don't let each screen invent its own event-handling style — this is the single biggest thing that keeps a large agent-built codebase coherent.

---

## 7. Domain Layer — UseCase Signatures

```kotlin
class GetInstalledAppsUseCase @Inject constructor(
    private val repo: AppRepository
) {
    operator fun invoke(includeHidden: Boolean = false): Flow<List<AppInfo>>
}

class ResolveGestureActionUseCase @Inject constructor(
    private val gestureRepo: GestureRepository
) {
    suspend operator fun invoke(gestureType: GestureType): GestureBinding?
}

class ToggleHiddenAppUseCase @Inject constructor(
    private val appRepo: AppRepository
) {
    suspend operator fun invoke(packageName: String, hidden: Boolean)
}

class ApplyIconStyleUseCase @Inject constructor(
    private val themeRepo: ThemeRepository
) {
    suspend operator fun invoke(config: ThemeConfig)
}

class ReorderHomeGridUseCase @Inject constructor(
    private val appRepo: AppRepository
) {
    suspend operator fun invoke(packageName: String, newPosition: Int)
}

class GetSystemStatsUseCase @Inject constructor(
    private val statsRepo: SystemStatsRepository
) {
    operator fun invoke(pollIntervalMs: Long = 3000L): Flow<SystemStats>
}
```

Repository interfaces (domain layer owns these, data layer implements):

```kotlin
interface AppRepository {
    fun observeApps(includeHidden: Boolean): Flow<List<AppInfo>>
    suspend fun refreshFromPackageManager()
    suspend fun setHidden(packageName: String, hidden: Boolean)
    suspend fun setGridPosition(packageName: String, position: Int?)
    suspend fun recordLaunch(packageName: String)
}

interface GestureRepository {
    fun observeBindings(): Flow<List<GestureBinding>>
    suspend fun getBinding(gestureType: GestureType): GestureBinding?
    suspend fun setBinding(binding: GestureBinding)
    suspend fun resetToDefaults()
}

interface SystemStatsRepository {
    fun observeStats(pollIntervalMs: Long): Flow<SystemStats>
}
```

---

## 8. Gesture Detection — Full Implementation Skeleton

```kotlin
// GestureDetectorOverlay.kt
@Composable
fun GestureDetectorOverlay(
    onGestureDetected: (GestureType) -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        Modifier.pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val fingerCount = mutableSetOf(down.id)
                    var totalDrag = Offset.Zero
                    var event: PointerEvent
                    do {
                        event = awaitPointerEvent()
                        event.changes.forEach { fingerCount.add(it.id) }
                        val drag = event.changes.firstOrNull { it.id == down.id }
                        drag?.let { totalDrag += it.positionChange() }
                    } while (event.changes.any { it.pressed })

                    val gesture = classifyGesture(totalDrag, fingerCount.size)
                    gesture?.let(onGestureDetected)
                }
            }
        }
    ) { content() }
}

private const val SWIPE_THRESHOLD_PX = 120f

private fun classifyGesture(drag: Offset, fingerCount: Int): GestureType? {
    if (drag.getDistance() < SWIPE_THRESHOLD_PX) return null
    val isHorizontal = abs(drag.x) > abs(drag.y)
    return when {
        isHorizontal && drag.x < 0 && fingerCount == 1 -> GestureType.SWIPE_LEFT_1F
        isHorizontal && drag.x > 0 && fingerCount == 1 -> GestureType.SWIPE_RIGHT_1F
        isHorizontal && drag.x < 0 && fingerCount == 2 -> GestureType.SWIPE_LEFT_2F
        isHorizontal && drag.x > 0 && fingerCount == 2 -> GestureType.SWIPE_RIGHT_2F
        !isHorizontal && drag.y < 0 && fingerCount == 1 -> GestureType.SWIPE_UP_1F
        !isHorizontal && drag.y < 0 && fingerCount == 2 -> GestureType.SWIPE_UP_2F
        else -> null
    }
}
```

```kotlin
// GestureActionResolver.kt
class GestureActionResolver @Inject constructor(
    private val resolveAction: ResolveGestureActionUseCase,
    @ApplicationContext private val context: Context,
    private val navController: NavController
) {
    suspend fun execute(gestureType: GestureType) {
        val binding = resolveAction(gestureType) ?: return
        when (binding.actionType) {
            ActionType.LAUNCH_APP -> {
                val intent = context.packageManager
                    .getLaunchIntentForPackage(binding.targetPackage ?: return)
                intent?.let { context.startActivity(it) }
            }
            ActionType.OPEN_INTENT -> {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(binding.targetIntentUri))
                context.startActivity(intent)
            }
            ActionType.OPEN_SCREEN -> {
                binding.targetScreenRoute?.let { navController.navigate(it) }
            }
            ActionType.SYSTEM_TOGGLE -> { /* flashlight/wifi/silent via respective managers */ }
            ActionType.SHORTCUT -> { /* ShortcutManager.getShortcuts + startShortcut */ }
        }
    }
}
```

---

## 9. Acceptance Criteria Per Phase

Give this checklist to Antigravity as the definition-of-done for each phase — without it, an agent will keep "improving" indefinitely or stop early.

**Phase 0 — Scaffold**
- [ ] App appears in Android's "Select Home App" chooser
- [ ] Setting as default doesn't crash on back-press from an empty grid
- [ ] All modules listed in Section 2 of the R&D doc compile with the version catalog above

**Phase 1 — Design System**
- [ ] `CarvedIcon` renders with press-inversion animation at 120ms, visually distinct concave/convex states
- [ ] `GlassPanel` shows blur + hairline border, no gloss/specular streak
- [ ] All color tokens sourced from `Color.kt`, zero hardcoded hex in feature modules

**Phase 2 — Grid & Dock**
- [ ] Real installed apps populate the grid via `PackageManager`, not mock data
- [ ] Drag-to-reorder persists to Room and survives process death
- [ ] Folders show 2×2 icon preview inside a shared carved circle

**Phase 3 — App Drawer**
- [ ] Fast-scroll alphabet rail jumps correctly for 100+ installed apps
- [ ] Search returns results within 1 frame of keystroke (debounced correctly)
- [ ] Long-press context sheet offers Info/Uninstall/Hide/Bind-Gesture

**Phase 4 — Gesture Engine**
- [ ] All 4 default bindings from Section 3 fire correctly on a physical device (emulator multi-touch is unreliable — test on hardware)
- [ ] Rebinding a gesture in `GestureSettingsScreen` persists and takes effect without app restart
- [ ] 1-finger vs 2-finger swipes never misclassify in 20 consecutive manual tests

**Phase 5 — HUD Panels**
- [ ] Battery/RAM/Storage/Temp gauges show live, real device data
- [ ] Media panel responds to an actual active `MediaSession` (test with any music app)
- [ ] Weather panel handles "no location permission" and "no network" gracefully, not a crash

**Phase 6 — Hidden Space**
- [ ] Biometric prompt gates entry; app is invisible from the main drawer when hidden
- [ ] `FLAG_SECURE` confirmed active — hidden screen doesn't appear in Recents screenshot
- [ ] Wrong-PIN decoy path (if implemented) never logs or surfaces the real vault

**Phase 7 — Personalization**
- [ ] Theme editor changes reflect live across all open screens (DataStore Flow, not requiring restart)
- [ ] Exported theme JSON re-imports correctly on a clean install
- [ ] Time-adaptive hue drift is subtle — no visible "jump" at hour boundaries

**Phase 8 — Notifications, Focus Mode, Smart Suggestions**
- [ ] Notification badges update within 2 seconds of a real notification arriving
- [ ] Focus Mode correctly restricts to whitelist and reverses cleanly on timer end
- [ ] Adaptive dock suggestion updates based on real usage patterns over a multi-day test, not static

**Phase 9 — Performance**
- [ ] Cold start measured via `adb shell am start -W` under 300ms target
- [ ] Baseline Profile generated and verified via `ProfileInstaller`
- [ ] No jank (dropped frames) scrolling a 100+ app drawer, verified with Layout Inspector / Perfetto

---

## 10. What To Literally Paste Into Antigravity

Recommended prompt structure when you hand this off:

```
Build an Android launcher app following these two attached specs exactly:
1. launcher-RnD-spec.md — product architecture, features, visual design system
2. launcher-implementation-blueprint.md — exact schemas, signatures, and acceptance criteria

Build in the phase order defined in Section 6 of the R&D doc / Section 9 of the
blueprint. After each phase, verify against that phase's acceptance criteria
before moving to the next. Do not deviate from the Composable signatures,
Room schemas, or navigation routes defined in the blueprint — treat them as
fixed contracts. Ask before introducing any new library not in the version
catalog.
```

That last line matters most — it stops the agent from silently swapping in a different DI framework, a different image loader, or Material 2 instead of 3 halfway through.git branch -M main