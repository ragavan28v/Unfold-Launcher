# Custom Android Launcher — Complete R&D & Build Specification
### Working title: **VOLT** (placeholder — swap for whatever fits your brand; structure below doesn't change)

Design language target: dark control-board / HUD aesthetic, circular icons with a **carved (concave-into-convex) neumorphic** finish like the "Top up Data" reference button, connected node-style navigation rail, accent-on-glass rather than glossy skeuomorphism. Not a clone of the reference — same *feeling*, different visual vocabulary (your own iconography, spacing, motion).

---

## 0. Design Philosophy (read this before Section 1)

Three rules that separate this from a generic icon-pack launcher:

1. **Everything is a HUD panel, not a card.** No drop shadows-on-white-card material design. Panels are dark glass with a hairline border and inner glow — think heads-up display, not Google Now.
2. **Icons are physical objects.** The "Top up Data" reference button isn't flat — it has a raised outer bevel and a soft inner shadow, like a real carved button on a control panel. Every circular icon in your launcher should be rendered with that same dual-shadow technique, not just a `clip(CircleShape)` on a flat PNG.
3. **Gestures are first-class navigation, not a hidden Easter egg.** The whole left rail / swipe system should feel like a designed input language, with a visual "gesture cheat sheet" the user can summon, not something they have to discover by accident.

---

## 1. Launcher Architecture

### 1.1 Tech stack

| Layer | Choice | Why |
|---|---|---|
| Language | Kotlin | Only sane choice in 2026 for a launcher |
| UI | Jetpack Compose | You need fluid custom-shape icons, physics-based swipe animations, and a dynamic HUD — Compose + Canvas is far less painful than XML + custom Views for this |
| Architecture pattern | MVVM + Clean Architecture (3-layer: presentation / domain / data) | Keeps gesture engine, icon renderer, and app-drawer logic testable and swappable |
| DI | Hilt | Standard, low boilerplate |
| Local DB | Room | App metadata, folders, hidden apps, gesture map, usage stats cache |
| Prefs | Jetpack DataStore (Proto) | Theme tokens, per-user settings, typed and migration-safe |
| Async | Kotlin Coroutines + Flow | App list changes, notification badge stream, weather/system stats stream |
| Image/Icon pipeline | Coil (bitmap loading) + custom `Modifier`/`Canvas` shader layer for the carved effect | Coil for icon caching, custom draw layer for the neumorphic bevel |
| Background work | WorkManager (periodic, e.g. weather refresh) + a lightweight foreground service only if you add live system-stat polling | Avoid a permanent foreground service unless you actually need sub-second stat updates |
| Notification badges | `NotificationListenerService` | Required to read unread counts (Instagram-style dot in your reference) |
| System stats (RAM/Storage/Temp) | `ActivityManager.MemoryInfo`, `StatFs`, `BatteryManager` | All queryable without root |
| Widgets | `AppWidgetHost` + `AppWidgetManager` | To support 3rd-party widgets inside your own HUD panels |
| Gesture capture | Compose `pointerInput` + `awaitPointerEventScope` for on-launcher gestures; `AccessibilityService` only if you want gestures to fire **outside** the launcher (e.g. from any app) | Most of your requested gestures (swipe on home) don't need Accessibility — only add it if you want global gesture capture |

### 1.2 Why it must be a real "Launcher" (Android mechanics, not skippable)

- `AndroidManifest.xml` must declare the main activity with:
  ```xml
  <intent-filter>
      <action android:name="android.intent.action.MAIN" />
      <category android:name="android.intent.category.HOME" />
      <category android:name="android.intent.category.DEFAULT" />
  </intent-filter>
  ```
- This makes Android offer it in the "Select Home App" chooser. `LauncherApps` + `PackageManager` (`queryIntentActivities` with `Intent.ACTION_MAIN` / `CATEGORY_LAUNCHER`) is how you enumerate installed apps — never hardcode a list.
- You need `android:launchMode="singleTask"` on the home activity, and you must **not** finish it on back-press when at the root (standard launcher back-press = no-op, matching system expectation).
- Icon shape: from Android 8+ (Adaptive Icons), you get a `foreground` + `background` layer per app icon. You will build your own `AdaptiveIconDrawable` → circular mask → bevel/shadow compositor, because most apps ship square/adaptive icons and you need every icon forced into your carved-circle style consistently.

### 1.3 Layered architecture (text diagram)

```
┌──────────────────────────────────────────────────────────┐
│  PRESENTATION (Compose)                                    │
│  HomeScreen · AppDrawer · GestureOverlay · HUDPanels ·     │
│  SettingsScreen · FolderScreen · HiddenSpace · WidgetHost  │
└───────────────▲──────────────────────────────────────────┘
                │  ViewModels (Hilt-injected, one per screen)
┌───────────────┴──────────────────────────────────────────┐
│  DOMAIN                                                    │
│  UseCases: GetInstalledApps, ResolveGestureAction,          │
│  ToggleHiddenApp, GetSystemStats, GetWeather,               │
│  ApplyIconStyle, ReorderHomeGrid, ManageFolders             │
└───────────────▲──────────────────────────────────────────┘
                │  Repository interfaces
┌───────────────┴──────────────────────────────────────────┐
│  DATA                                                       │
│  AppRepository (PackageManager + Room cache)                │
│  GestureRepository (Room: gesture→action map)                │
│  ThemeRepository (DataStore proto)                           │
│  SystemStatsRepository (ActivityManager/StatFs/Battery)      │
│  NotificationRepository (NotificationListenerService bridge) │
│  WeatherRepository (network, optional — or skip entirely)    │
└──────────────────────────────────────────────────────────┘
```

### 1.4 Required permissions

| Permission | Purpose | Notes |
|---|---|---|
| `QUERY_ALL_PACKAGES` | Enumerate all installed apps | Needed for a launcher; Play Store allows this for declared launchers |
| `PACKAGE_USAGE_STATS` | "Recently/most used apps" smart sorting | User must grant via Settings, can't be requested via dialog |
| `BIND_NOTIFICATION_LISTENER_SERVICE` | Notification badge counts | Also Settings-granted |
| `EXPAND_STATUS_BAR` (optional) | If you build a custom notification shade | Only if you go that far |
| `SET_WALLPAPER` | If you offer in-app wallpaper/live-wallpaper control | |
| `READ_PHONE_STATE` (avoid if possible) | Not needed just to *launch* dialer via Intent | Use `Intent(Intent.ACTION_DIAL)` instead — no permission required |
| Accessibility Service (optional, separate opt-in) | Only for global (outside-launcher) gestures | Heavy permission — gate behind an explicit advanced-settings toggle, explain clearly why |

---

## 2. Project Structure — End to End

Recommended as a **multi-module Gradle project** (not one giant `app` module) — makes the gesture engine and icon renderer independently testable/reusable, and keeps build times sane as it grows.

```
volt-launcher/
├── build.gradle.kts                        (root)
├── settings.gradle.kts                     (module includes)
├── gradle.properties
├── gradle/libs.versions.toml               (version catalog — pin Compose, Hilt, Room versions)
│
├── app/                                    (thin shell module — wires everything together)
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml             (HOME intent-filter, all permissions, services)
│       ├── java/com/volt/launcher/
│       │   ├── VoltApp.kt                  (Application class, @HiltAndroidApp)
│       │   ├── MainActivity.kt             (single host Activity for the whole launcher)
│       │   └── di/
│       │       └── AppModule.kt
│       └── res/
│           ├── xml/
│           │   ├── accessibility_service_config.xml
│           │   └── notification_listener_config.xml (if needed)
│           └── mipmap-anydpi-v26/ic_launcher.xml
│
├── core/
│   ├── core-ui/                            (design system module)
│   │   └── src/main/java/com/volt/core/ui/
│   │       ├── theme/
│   │       │   ├── Color.kt                (token definitions — see Section 3)
│   │       │   ├── Typography.kt
│   │       │   ├── Shape.kt                (carved-circle shape definitions)
│   │       │   ├── Elevation.kt            (glass/glow elevation tokens)
│   │       │   └── VoltTheme.kt            (CompositionLocal-based theme provider)
│   │       ├── components/
│   │       │   ├── CarvedIcon.kt           (the neumorphic circular icon composable — core reusable piece)
│   │       │   ├── GlassPanel.kt           (blurred glass surface container)
│   │       │   ├── NodeRail.kt             (the connected-dot vertical nav rail)
│   │       │   ├── SignalBar.kt            (LED/signal-bar style indicators, reused from your GitHub SVG work)
│   │       │   ├── HUDGauge.kt             (circular progress ring — battery/RAM/storage style)
│   │       │   ├── PillBadge.kt            (REST-API-pill style tag chip, reused motif)
│   │       │   └── AnimatedBlurBackground.kt
│   │       └── util/
│   │           ├── BevelShaderUtil.kt      (RenderEffect/Canvas dual-shadow math)
│   │           └── HapticUtil.kt
│   │
│   ├── core-domain/
│   │   └── src/main/java/com/volt/core/domain/
│   │       ├── model/
│   │       │   ├── AppInfo.kt
│   │       │   ├── FolderInfo.kt
│   │       │   ├── GestureBinding.kt
│   │       │   ├── SystemStats.kt
│   │       │   └── ThemeConfig.kt
│   │       ├── repository/                 (interfaces only)
│   │       │   ├── AppRepository.kt
│   │       │   ├── GestureRepository.kt
│   │       │   ├── ThemeRepository.kt
│   │       │   ├── NotificationRepository.kt
│   │       │   └── SystemStatsRepository.kt
│   │       └── usecase/
│   │           ├── GetInstalledAppsUseCase.kt
│   │           ├── ResolveGestureActionUseCase.kt
│   │           ├── ToggleHiddenAppUseCase.kt
│   │           ├── ApplyIconStyleUseCase.kt
│   │           ├── ReorderHomeGridUseCase.kt
│   │           └── GetSystemStatsUseCase.kt
│   │
│   └── core-data/
│       └── src/main/java/com/volt/core/data/
│           ├── local/
│           │   ├── AppDatabase.kt          (Room)
│           │   ├── dao/
│           │   │   ├── AppDao.kt
│           │   │   ├── FolderDao.kt
│           │   │   ├── GestureDao.kt
│           │   │   └── HiddenAppDao.kt
│           │   └── entity/
│           │       ├── AppEntity.kt
│           │       ├── FolderEntity.kt
│           │       └── GestureEntity.kt
│           ├── datastore/
│           │   ├── ThemeProto.proto        (Proto DataStore schema)
│           │   └── ThemeDataStoreImpl.kt
│           ├── system/
│           │   ├── SystemStatsProvider.kt  (ActivityManager/StatFs/BatteryManager wrappers)
│           │   └── PackageManagerBridge.kt (queryIntentActivities, uninstall/app-info intents)
│           └── repositoryimpl/
│               ├── AppRepositoryImpl.kt
│               ├── GestureRepositoryImpl.kt
│               ├── ThemeRepositoryImpl.kt
│               └── SystemStatsRepositoryImpl.kt
│
├── feature/
│   ├── feature-home/
│   │   └── src/main/java/com/volt/feature/home/
│   │       ├── HomeScreen.kt                (root Compose screen — pager of panels, like your reference's 3-dot pager)
│   │       ├── HomeViewModel.kt
│   │       ├── panels/
│   │       │   ├── ClockWeatherPanel.kt     (Image 4 equivalent)
│   │       │   ├── SystemHUDPanel.kt        (Image 2 equivalent — RAM/Storage/Temp/Battery ring)
│   │       │   ├── MediaPanel.kt            (Image 3 equivalent — now-playing via MediaSessionManager)
│   │       │   └── FlashlightQuickPanel.kt
│   │       ├── dock/
│   │       │   └── DockBar.kt               (bottom 4-icon dock, carved style)
│   │       └── grid/
│   │           ├── AppGrid.kt
│   │           └── FolderBubble.kt
│   │
│   ├── feature-drawer/
│   │   └── AppDrawerScreen.kt, AppDrawerViewModel.kt, AlphabetFastScroll.kt, SearchBar.kt
│   │
│   ├── feature-gestures/
│   │   └── src/main/java/com/volt/feature/gestures/
│   │       ├── GestureDetectorOverlay.kt    (transparent full-screen pointerInput layer)
│   │       ├── GestureEngine.kt             (interprets raw MotionEvents → GestureType)
│   │       ├── GestureActionResolver.kt     (GestureType + binding → executes Intent/action)
│   │       ├── GestureMappingScreen.kt      (user-facing "record your own gesture" UI)
│   │       └── model/GestureType.kt         (SWIPE_LEFT, SWIPE_RIGHT, SWIPE_LEFT_2F, SWIPE_RIGHT_2F, SWIPE_UP_2F, PINCH, LONG_PRESS_HOLD, DOUBLE_TAP_BG, EDGE_SWIPE...)
│   │
│   ├── feature-hidden-space/
│   │   └── HiddenAppsScreen.kt, HiddenFilesScreen.kt, BiometricGate.kt
│   │
│   ├── feature-widgets/
│   │   └── WidgetHostScreen.kt, WidgetPickerSheet.kt, VoltAppWidgetHost.kt
│   │
│   ├── feature-notifications/
│   │   └── VoltNotificationListenerService.kt, BadgeRepositoryBridge.kt
│   │
│   ├── feature-settings/
│   │   └── SettingsScreen.kt, ThemeEditorScreen.kt, GestureSettingsScreen.kt,
│   │       IconPackPickerScreen.kt, BackupRestoreScreen.kt, AboutScreen.kt
│   │
│   └── feature-search/
│       └── UniversalSearchScreen.kt          (apps + contacts + web fallback + in-app deep search)
│
├── buildSrc/  (or version catalog if you skip buildSrc)
└── docs/
    ├── architecture.md
    ├── gesture-spec.md
    └── theme-tokens.md
```

**Why multi-module:** `feature-gestures` and `core-ui` (specifically `CarvedIcon.kt` and `BevelShaderUtil.kt`) are the two pieces with real reusable IP — worth isolating so you could even open-source the icon-rendering module separately later, similar to how you've split your GitHub profile assets into standalone SVGs.

---

## 3. Theme System — Accent-on-Glass, Not Glossy

The key visual distinction you asked for: **glossy** = specular highlight streak, high-contrast white gradient on a curved surface, plastic look. **Glass/accent** = translucent dark surface, subtle blur, a *thin* accent-colored edge-light, and the "carve" comes from two soft directional shadows rather than one bright highlight.

### 3.1 Core palette

| Token | Hex | Usage |
|---|---|---|
| `bg.void` | `#05070A` | True background, screen base |
| `bg.panel` | `#0B0F14` @ 72% opacity + blur(24dp) | Glass HUD panels |
| `bg.panel.border` | `#1C2530` | 1px hairline panel border |
| `surface.icon.base` | `#0E141B` | Base fill of every circular icon |
| `accent.primary` | `#38BDF8` (sky/cyan) | Primary glow, active states, rail connectors |
| `accent.secondary` | `#6366F1` (indigo — matches your existing brand badge color) | Secondary accents, selected folder, gesture-mapping highlights |
| `accent.warn` | `#F97316` | Low battery, notification badge dot |
| `accent.danger` | `#EF4444` | Destructive actions (uninstall, hide, delete) |
| `text.primary` | `#E6F1FF` | Headlines, clock |
| `text.secondary` | `#7E93A7` | Labels, captions ("BATTERY", "STORAGE") |
| `text.muted` | `#4C5C6B` | Disabled/placeholder |
| `carve.shadow.dark` | `rgba(0,0,0,0.55)` offset (3dp, 4dp) blur 6dp | Bottom-right inner shadow → concave read |
| `carve.shadow.light` | `rgba(120,190,255,0.10)` offset (-2dp,-2dp) blur 4dp | Top-left inner highlight → convex counter-read |
| `carve.edge.glow` | 1px stroke, `accent.primary` @ 35% opacity | Rim light around every icon and active nav node |

### 3.2 The "carved" render recipe (applies to icons + dock, exactly as requested)

For each circular icon container, layer (bottom → top):
1. Flat base circle, `surface.icon.base`.
2. Inner shadow, dark, bottom-right — via `Canvas` + `BlurMaskFilter` or Compose `Modifier.shadow` with `spotColor` trick, or simplest: two overlaid radial gradients clipped to the circle.
3. Inner shadow, faint accent-tinted highlight, top-left (opposite corner) — this is what makes it read as *carved into* the panel rather than a flat sticker.
4. 1px rim stroke at `carve.edge.glow`.
5. The app icon/foreground glyph, centered, slightly inset (≈14% padding) so the bevel is visible all around — this is exactly why the reference "GB+" text sits well inside its circle rather than touching the edge.
6. On press: animate the two shadows to **invert** (dark shadow moves to top-left, highlight to bottom-right) over ~120ms — this simulates the button physically depressing. This single detail will make your launcher feel dramatically more "designed" than 99% of icon packs.

### 3.3 Typography

- Display/clock: a geometric monospaced-leaning sans (e.g. **Space Grotesk** or **JetBrains Mono** for numerals specifically — ties into your "control panel" numeric readouts like `12%`, `5.3 GB`, `32°C`).
- UI labels: **Inter** or **Manrope**, all-caps + letter-spacing for micro-labels (`BATTERY`, `TEMP`, `STORAGE`) — matches the reference exactly and matches your existing no-emoji, typographic-icon personal brand.
- No emoji anywhere in system copy — consistent with your established brand rule.

### 3.4 Motion tokens

- Standard transition: 220ms, `FastOutSlowInEasing`.
- Icon press-carve inversion: 120ms.
- Panel pager swipe: spring, `dampingRatio = 0.8f`, `stiffness = Medium`.
- Gesture-triggered app launch: circular reveal from the swipe's origin point, 280ms — ties the gesture visually to its result instead of a generic fade.

---

## 4. Feature List (Full — Beyond Standard Launcher Territory)

### 4.1 Home & Visual System
- Multi-panel horizontal pager home (Clock/Weather · System HUD · Media · custom panels) — reorderable, panels individually toggleable.
- Carved circular grid + dock, as specified.
- **Live bevel response to ambient light** (optional, via `SensorManager` light sensor) — subtly shifts highlight angle, like the panel is catching real light.
- Folder bubbles that show a mini 2×2 preview of contained app icons *inside* a shared carved circle, not a square.
- Node-rail quick-nav (Home/Media/System/Flashlight) exactly like the reference, but user-customizable which 4 nodes appear.
- Depth-of-field wallpaper parallax on swipe (subtle, glass panels float slightly above wallpaper layer).
- At-a-glance HUD gauges: battery ring, RAM, storage, CPU temp — all real data, refresh via Flow, not fake.

### 4.2 App Drawer
- Alphabetical fast-scroll rail (long-press-drag to jump letters).
- Usage-based smart section ("Frequently Used" auto-sorted via `UsageStatsManager`).
- Search-as-you-type with fuzzy match + inline app actions (App Info / Uninstall / Hide / Add to gesture) via long-press context sheet.
- Category auto-tagging (Social/Productivity/Games/Finance) inferred from Play Store category metadata where available, user-overridable.
- Private/Hidden space with its own PIN or biometric gate (see 4.5).

### 4.3 Gesture Engine (see Section 5 for full spec)
- Fully remappable gesture → action table (not hardcoded), stored in Room.
- Multi-finger gesture support (1-finger and 2-finger swipes, distinguished cleanly).
- Visual gesture trainer: user performs the gesture live, launcher confirms detection before saving the binding.
- Per-gesture action types: Launch App, Open Contact/Direct WhatsApp Chat, System Toggle (flashlight, silent mode, wifi), Open Folder, Open Hidden Space, Open Specific File/Folder (via SAF `ACTION_OPEN_DOCUMENT_TREE`), Run Shortcut, Open Widget Panel.
- Optional on-screen gesture cheat-sheet overlay (double-tap+hold on empty space to reveal a fading diagram of all active bindings — huge onboarding/discoverability win).

### 4.4 Notifications & Live Data
- Notification badge dots + count bubble on icons (reference shows this on Instagram).
- Notification digest panel — batched, grouped by app, swipe-to-clear, without needing to pull down system shade.
- Media panel with real transport controls via `MediaSessionManager` (matches your reference Image 3 exactly, but functional).
- Weather panel using device location + a free-tier API (Open-Meteo has no key requirement — good fit for a personal-brand project with no backend cost).

### 4.5 Privacy & Security
- Hidden App Space: PIN/biometric (`BiometricPrompt`) gated, apps excluded from the visible drawer entirely (not just visually hidden — also excluded from search-index unless unlocked).
- Hidden Files/Folders vault: SAF-scoped folder picker + optional decoy/duress PIN (enter wrong PIN opens a harmless empty space instead of erroring — nice paranoid-grade touch).
- App-lock: require biometric before opening specific sensitive apps directly from the grid.
- **Gesture-based panic gesture**: e.g. a specific edge-swipe instantly locks the launcher / opens a decoy home layout.

### 4.6 Personalization Engine
- Full theme editor: accent color picker (constrained to your token system so users can't accidentally make it glossy/off-brand), icon bevel intensity slider, blur radius slider, panel opacity slider.
- Time-of-day adaptive theme (subtle accent hue shift dawn/day/dusk/night — not a jarring light/dark swap, just hue temperature drift, keeps the "glass" feel intact at all hours).
- Icon pack import support (adaptive icon → still runs through your carve-compositor so 3rd-party icon packs *also* get the bevel treatment, unifying visual style regardless of source icon).
- Custom grid density (4×5 through 6×8).
- Layout presets export/import as JSON — shareable "themes" file, a nice community feature later.

### 4.7 Productivity & Smart Layer
- **Focus Mode**: one-tap collapses home to only whitelisted apps + a session timer, other icons dim/lock behind a soft blur until session ends.
- **App suggestions dock slot**: a single "adaptive" dock icon that predicts your next likely app (time-of-day + location + usage pattern, fully on-device, no cloud) — this is the kind of AI-systems-engineering touch that fits your background well.
- Screen-time HUD panel (reuses the same gauge component as RAM/Storage — nice architectural reuse).
- Quick-note / quick-capture widget panel that doesn't require opening a notes app.

### 4.8 Performance & Engineering Quality
- Cold start under 300ms target — lazy-load non-critical panels after first frame.
- Icon bitmap caching (disk + memory, Coil) so the bevel compositing isn't recomputed every recomposition.
- Baseline Profiles for the launcher's hot paths (app grid scroll, drawer open).
- Doze-friendly background work — WorkManager constraints, no polling loops.

### 4.9 Accessibility
- TalkBack labels on every carved icon (don't let custom Canvas rendering break semantics — use `Modifier.semantics`).
- Minimum touch target 48dp maintained even though icons render smaller/carved.
- Reduced-motion setting that disables the press-carve inversion + parallax for users sensitive to motion.

### 4.10 Backup, Restore & Portability
- Full launcher state export (layout + gestures + theme) to a single JSON, restorable on reinstall or a new device.
- Optional Google Drive backup using the Drive API (nice tie-in since you already work with Drive integrations).

---

## 5. Gesture Engine — Full Technical Spec

### 5.1 Requested default bindings (all are *defaults*, all remappable)

| Gesture | Default Action | Implementation |
|---|---|---|
| Swipe left (1 finger, home screen) | Open Phone Dialer | `Intent(Intent.ACTION_DIAL)` |
| Swipe right (1 finger, home screen) | Open WhatsApp (chat list or last chat) | `packageManager.getLaunchIntentForPackage("com.whatsapp")` |
| Swipe left (2 fingers) | Open Hidden Space (apps + files) | Navigate to `HiddenAppsScreen`, gated by biometric |
| Swipe right (2 fingers) | Open Play Store | `Intent(Intent.ACTION_VIEW, Uri.parse("market://..."))` |
| Swipe up (1 finger) | Open App Drawer | Standard, but themable |
| Swipe up (2 fingers) | *(free slot)* — suggest: Quick Settings HUD panel | User-assignable |
| Long-press + hold empty space | Gesture cheat-sheet overlay | Local overlay, no permission needed |
| Double-tap empty space | Screen off / lock (needs device admin OR just visual "locked" overlay if no root) | Optional, clearly explained in settings |
| Pinch in | Zoom-out grid editor (rearrange mode) | Standard launcher UX, but styled to match |
| Edge-swipe from left bezel | Panic/decoy mode | See 4.5 |

### 5.2 Detection approach

- All single-surface gestures detected via a **transparent full-screen `Box`** with `Modifier.pointerInput` using `awaitPointerEventScope`, reading `event.changes.size` to distinguish 1-finger vs 2-finger, and computing drag delta + velocity to classify swipe direction (`abs(dx) > abs(dy)` and a minimum velocity threshold to avoid accidental triggers).
- Gesture classification lives in `GestureEngine.kt`, completely decoupled from the action it triggers — it emits a sealed `GestureType`, nothing else. This is the reason the system is genuinely "customizable dynamically" rather than hardcoded if/else: the resolver layer does a Room lookup `GestureType → GestureBinding → ActionType` at runtime.
- `GestureActionResolver.kt` is a single `when` over `ActionType` (LaunchApp / OpenIntent / OpenScreen / SystemToggle / RunShortcut) — adding a **new action type** later only touches this one file.
- Conflict handling: if a user tries to bind two actions to the same gesture, `GestureSettingsScreen` warns and asks to overwrite rather than silently failing.
- **Only if you want gestures to fire from *outside* the launcher** (e.g. swipe left while inside Instagram to jump to dialer) do you need `AccessibilityService` with a transparent overlay window (`TYPE_ACCESSIBILITY_OVERLAY`) — this is a meaningfully heavier permission ask and should be a clearly-explained opt-in in Settings, not default-on.

### 5.3 Data model

```kotlin
data class GestureBinding(
    val id: String,
    val gestureType: GestureType,      // SWIPE_LEFT_1F, SWIPE_RIGHT_2F, etc.
    val actionType: ActionType,        // LAUNCH_APP, OPEN_INTENT, OPEN_SCREEN, SYSTEM_TOGGLE
    val targetPackage: String? = null, // for LAUNCH_APP
    val targetIntentUri: String? = null,
    val targetScreenRoute: String? = null,
    val isUserModified: Boolean = false
)
```

---

## 6. Build Roadmap (Suggested Phasing)

| Phase | Scope | Outcome |
|---|---|---|
| 0 | Project scaffold, module setup, HOME intent-filter, can be set as default launcher showing a blank grid | You can already "install" it as your phone's launcher |
| 1 | `core-ui` design system: `CarvedIcon`, `GlassPanel`, color/typography tokens | Visual language locked before anything else is built on top of it |
| 2 | Real app grid + dock using `PackageManager`, drag-to-reorder, folders | Launcher is now daily-usable at a basic level |
| 3 | App Drawer + search + fast-scroll | Feature parity with a "normal" launcher |
| 4 | Gesture Engine (Section 5) with the 4 requested defaults | Your headline differentiator ships |
| 5 | HUD panels: System stats, Clock/Weather, Media | Reference-matching pager experience |
| 6 | Hidden Space + biometric gate + file vault | Privacy layer |
| 7 | Theme editor + backup/export | Personalization layer complete |
| 8 | Notification badges/listener, Focus Mode, smart app-suggestion dock slot | "Surprise" layer — this is where it stops feeling like a student project |
| 9 | Performance pass: Baseline Profiles, cold-start tuning, icon cache tuning | Ship-quality polish |

---

## 7. Surprise Additions (Beyond What You Asked For)

- **Gesture Macro Chaining**: allow a gesture to trigger a *sequence* (e.g. swipe left+hold → open WhatsApp *and* pre-fill a specific contact's chat) — small addition to the resolver, disproportionate "wow" factor in a demo.
- **Ambient control-panel idle screen**: after N seconds idle, dim everything except the clock and a single breathing accent ring — an actual always-on-display-style state, not just a screensaver.
- **"System Active" status line** (you'll notice the reference has a small `SYSTEM ACTIVE` status text under the clock) — repurpose this as a genuinely useful live status: shows Doze state, last sync time, or a custom user-set status string. Small detail, disproportionately makes it feel alive.
- **Sound design, not just haptics**: extremely subtle UI tick sounds (optional, off by default) on gesture confirmation — control-panel apps in film/games use this constantly and it's almost never done in real launchers.
- **Developer/Nerd HUD mode**: a secondary System HUD panel variant showing FPS, battery drain rate (%/hr), network throughput graph — trivial to build given you're already pulling `ActivityManager`/`BatteryManager` data, and it's a great personal flex feature that fits your engineering-heavy portfolio brand.
- **Shareable theme cards**: auto-generate a shareable image/export of your current theme + gesture map (styled like a spec sheet) — perfect fit for your existing no-emoji/typographic-icon LinkedIn and GitHub aesthetic if you ever want to post about building this.
- **Gesture-record onboarding flow**: first-launch experience literally has the user physically perform each gesture as a mini-tutorial before the launcher is usable — turns setup into product delight instead of a settings-menu chore.

---

## 8. Key Technical Risks & Mitigations

| Risk | Mitigation |
|---|---|
| Setting as default launcher can be finicky across OEM skins (MIUI/OneUI/etc.) | Test on at least 2 OEM skins early; provide a clear "Set as Default" onboarding step using `RoleManager.createRequestRoleIntent(ROLE_HOME)` on API 29+ |
| Custom carved-icon compositing is expensive if redrawn every frame | Cache the composited bitmap per icon (base + bevel), invalidate only on theme change, not on every recomposition |
| Two-finger gesture detection colliding with normal scroll/drag | Require gesture start point to be on empty background, not on an icon/panel — icons/panels consume their own pointer events first |
| `PACKAGE_USAGE_STATS` and notification-listener permissions can't be requested via normal dialog | Deep-link directly to the specific Settings screen (`Settings.ACTION_USAGE_ACCESS_SETTINGS` / `ACTION_NOTIFICATION_LISTENER_SETTINGS`) with a clear in-app explainer screen first |
| Biometric hidden-space UX getting bypassed via recent-apps screenshot | Set `FLAG_SECURE` on the Hidden Space screen so it doesn't appear in recents/screenshots |

---

## 9. Next Step

Once you've reviewed this, the natural next artifact is a **Compose implementation of `CarvedIcon.kt` + `GlassPanel.kt`** (Section 3.2's render recipe as actual working code) — that single component, once nailed, defines the visual identity of the entire launcher and everything else is built by composing it. Say the word and I'll build that component next.