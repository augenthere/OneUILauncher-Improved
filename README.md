# OneUILauncher — Improved Edition

A significantly improved, more stable, and feature-rich version of the OneUILauncher (Samsung OneUI-style launcher).

## Improvements Over Original

### Stability
- **Global crash handler** (`CrashHandler.kt`) — catches uncaught exceptions, writes structured crash reports to app-private storage, provides `CoroutineExceptionHandler` for ViewModel scopes
- **Error-safe data loading** — all repository calls wrapped in try/catch, no crashes on missing packages or failed queries
- **Memory-efficient icon loading** — icons are `@Transient` (never serialized), cached with TTL, invalidated on install/uninstall
- **Lifecycle-aware coroutines** — `viewModelScope` with `crashHandler`, proper cleanup in `onStop`/`onDestroy`
- **StateFlow-based state** — single source of truth, `WhileSubscribed` for efficient collection

### New Features
- **Hidden apps** — hide apps from drawer and home screen, persisted in settings
- **Notification badges** — per-app unread counts from `NotificationListenerService` (counts only, never reads content)
- **App search** — real-time search with instant filtering
- **Edit mode** — long-press to enter edit mode, drag to rearrange
- **Grid customization** — adjustable rows/cols (3-8 rows, 3-6 cols)
- **Icon size control** — slider from 0.7x to 1.5x
- **Blur toggle** — enable/disable drawer blur
- **Label toggle** — show/hide app labels
- **Work profile support** — `UserManager.userProfiles` iteration
- **Gesture state machine** — IDLE/DRAGGING/RESIZING/SWIPING states for smooth gesture handling

### Architecture
- **Clean separation** — `data/`, `model/`, `ui/`, `viewmodel/`, `service/`, `util/` packages
- **Repository pattern** — `AppRepository` with caching and error handling
- **Sealed class models** — `LauncherItem` with `App`, `Folder`, `Widget`, `CustomWidget` subtypes
- **StateFlow + combine** — derived state (visible drawer items, home items with badges)
- **Room database ready** — dependency included for future icon cache / layout persistence

### Privacy
- **No network code** — zero HTTP requests, no analytics SDKs, no tracking
- **No sensitive data collection** — only reads installed apps via `LauncherApps` (standard launcher API)
- **Notification counts only** — never reads notification content
- **Local storage only** — DataStore preferences in app-private directory

## Build Instructions

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17
- Android SDK 36 (compileSdk)
- Gradle 8.7+

### Steps
1. Open this project in Android Studio
2. Sync Gradle
3. Build → Make Project
4. Run on device/emulator (API 26+)

### Setting as Default Launcher
After installing, go to Settings → Apps → Default Apps → Home App → select "OneUILauncher"

### Enabling Notification Badges
Go to Settings → Apps → Special Access → Notification Access → enable "OneUILauncher"

## Project Structure

```
OneUILauncher-Improved/
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/luna/oneuilauncher/
│       │   ├── MainActivity.kt
│       │   ├── OneUILauncherApp.kt
│       │   ├── data/
│       │   │   └── AppRepository.kt
│       │   ├── model/
│       │   │   └── LauncherItem.kt
│       │   ├── service/
│       │   │   └── NotificationService.kt
│       │   ├── ui/
│       │   │   ├── HomeScreen.kt
│       │   │   └── theme/
│       │   │       ├── Theme.kt
│       │   │       └── Type.kt
│       │   ├── util/
│       │   │   └── CrashHandler.kt
│       │   └── viewmodel/
│       │       └── LauncherViewModel.kt
│       └── res/
│           └── values/
│               └── strings.xml
├── build.gradle.kts
├── settings.gradle.kts
└── gradle/
    └── libs.versions.toml
```

## License
This is an improved version of the original OneUILauncher. All improvements are provided as-is for the original creator to adopt.
