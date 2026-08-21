<div align="center">
  <h1>Unfold</h1>
  <p><b>A modern, fast, and highly intentional Android Launcher experience.</b></p>
</div>

---

**Unfold** isn't just another generic icon-pack launcher. It's built around a distinct **dark control-board / HUD aesthetic**. Drop shadows on white cards are out. Panels are dark glass with a hairline border and inner glow. Icons are treated as physical objects with carved, concave-into-convex neumorphic finishes. Gestures aren't Easter eggs; they are first-class, intentional navigation methods.

## 🚀 Release v1.0.0 — What's Included

The initial release focuses on a lightning-fast foundation and our core feature set.

### 🔍 High-Performance Universal Search
- **Instant Search:** Search across your installed apps, contacts, and local files (documents, images, videos, audio) with near-zero memory footprint and no UI lag. 
- **Direct Dial:** Search for a contact and tap them to immediately launch your phone dialer.
- **Categorized Results:** Files are cleanly categorized into Photos, Videos, Audio, and Documents, automatically filtering out system-level junk.

### 🔒 Privacy & Organization
- **Hidden Space:** A secured, locked cabinet for apps you want removed from your main app drawer. Keep sensitive apps completely out of sight.
- **Customizable App Drawer:** Control your grid size, visual styling, icon scaling, and layout preferences.

### 👆 Fluid Navigation & Interface
- **Gesture Engine:** Support for advanced, customizable edge-swipes alongside native system navigation gestures.
- **Notification Badges:** Choose a global dot color to blend perfectly with your theme, complete with optional unread counts.
- **Home Screen Flexibility:** Full control over your grid, icon sizes, dock settings, and margins.

---

## 🔮 The Roadmap — Coming Soon

We are actively developing the next wave of features to complete the Unfold vision:

- **Smart Folders:** Deep customization of folder styles, shapes, grid previews, opening behaviors, and background treatments.
- **Total Appearance Engine:** Custom themes, dynamic transparency, global font choices, animation speed tuning, and system bar styling.
- **Desktop Extras:** Full home screen widget support, launcher layout imports, and complete backup/restore configurations.
- **Advanced Control:** Deep custom gestures, shortcut bindings, and automated icon normalization to force every app to adhere to the carved circular HUD aesthetic.

---

## 🛠 Developer Notes

### Tech Stack
- **Language:** 100% Kotlin
- **UI Framework:** Jetpack Compose (for fluid custom-shape icons and physics-based swipe animations)
- **Architecture:** MVVM + Clean Architecture (Presentation / Domain / Data)
- **Dependency Injection:** Hilt
- **Local Storage:** Room Database (for metadata, folders, hidden apps) & Jetpack DataStore (for theme tokens and prefs)
- **Image Pipeline:** Coil + Custom Modifiers/Shaders for the neumorphic bevels.

### Building Sideload Releases

To build a signed release APK yourself, simply run:
```bash
./gradlew assembleRelease
```
The final optimized `.apk` will be output to `app/build/outputs/apk/release/app-release.apk`.
