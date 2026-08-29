# Utilities

An Android super app: one launcher icon, twenty-two small tools. The home screen
groups mini apps by category with a search box; tapping one opens it full screen,
and pressing and holding pins it to a Favourites section at the top.

Built with Kotlin, Jetpack Compose and Material 3. The theme is a setting —
System, Light or Dark — with optional wallpaper colours on Android 12+.

## Mini apps

### Decide

| Mini app | What it does |
| --- | --- |
| 🔢 Random Number | Rolls a number in a chosen range — 1–6, 1–10, 1–20, 1–100, or a custom range you type. |
| 🤔 Choice Maker | Answers a yes/no question with a fair coin flip, with a running tally. |
| 🎯 List Picker | Add your own options and let it pick one. Optional remove-after-picking turns it into a draw. |
| 🎲 Dice Roller | Throws 1–10 dice, d4 through d100, showing each die and the total. |
| 👥 Team Splitter | Deals names into evenly sized teams. One team just shuffles the order. |
| ✊ Rock Paper Scissors | Best of anything against the phone, with a running score. |

### Calculate

| Mini app | What it does |
| --- | --- |
| 💸 Tip Splitter | Bill, tip and each person's share, with the odd cents handed out fairly. |
| 📏 Unit Converter | Length, mass, temperature and volume. |
| 📊 Percentage | Percent of, is-what-percent, and percent change. |
| 📅 Date Calculator | Days between two dates, add days, and age. |
| 🏷️ Discount and VAT | Take a percentage off, add tax on, or strip tax back out of a gross price. |
| 🌍 Time Zones | One moment shown in eighteen zones, daylight saving included. |

### Text and codes

| Mini app | What it does |
| --- | --- |
| 🔐 Password Generator | Strong passwords from `SecureRandom`, with a strength estimate. |
| ✍️ Text Tools | Change case, count characters and words, tidy whitespace. |
| ⬛ QR Generator | Turns text or a link into a QR code. |
| 📷 QR Scanner | Reads a QR code with the camera. |
| 📶 WiFi QR | Shares a network as a code another phone can scan to join. |

### Device

| Mini app | What it does |
| --- | --- |
| 🔦 Flashlight | Torch on, torch off. |
| 🪧 Bubble Level | Roll and pitch from the accelerometer. |
| 🧭 Compass | Heading and nearest cardinal point. |
| 📐 Ruler | An on-screen scale, with calibration for the device. |
| ⏱️ Stopwatch | Stopwatch with laps, and a countdown timer with a notification. |

## Settings

Reached from the icon in the home top bar.

- **Theme** — System, Light or Dark. An explicit choice overrides the device.
- **Colours from your wallpaper** — Material You, on Android 12+. Mini app accents
  stay fixed either way, so tiles keep their identity whatever the palette does.
- **Favourites** — up to eight pinned mini apps, cleared from here.

Settings are read as a stream rather than once, so a theme change applies
immediately rather than on the next launch.

## Building

```bash
./gradlew assembleDebug             # build the APK
./gradlew testDebugUnitTest         # run the unit tests
./gradlew connectedDebugAndroidTest # run the instrumented tests on a device
./gradlew installDebug              # install on a connected device or emulator
```

CI builds, unit-tests and lints every push, then runs the instrumented tests on
an emulator. Requires the Android SDK (compileSdk 35) and JDK 17+. Minimum
supported device is Android 8.0 (API 26).

## Permissions

Nineteen of the twenty-two mini apps need no permission at all.

- **Camera** — QR Scanner only. Declared optional, so the app still installs on a
  device without one.
- **Notifications** — Stopwatch and Timer, requested when a run starts rather than
  on opening the mini app. Refusing it costs the alert, not the timing.
- **Foreground service** — the ongoing notification while a stopwatch or timer runs.

## Project layout

```
app/src/main/java/com/viennnaa/utilities/
├── MainActivity.kt              entry point; publishes launcher shortcuts
├── UtilitiesApp.kt              navigation: home, one route for all mini apps, deep links
├── miniapp/
│   ├── MiniApp.kt               what a mini app is
│   ├── MiniAppIds.kt            ids, used as both route and settings namespace
│   ├── MiniAppCategory.kt       home screen grouping
│   └── MiniAppCatalog.kt        the registry — every mini app is listed here
├── core/
│   ├── settings/                theme mode and favourites rules, free of Android types
│   ├── options/                 list-editing rules shared by the list-building mini apps
│   ├── money/                   cent-exact amounts and basis-point rates
│   ├── qr/                      QR encoding, shared by the mini apps that produce codes
│   ├── storage/                 one DataStore: per mini app settings, app settings, list encoding
│   ├── sensors/                 lifecycle-aware sensor subscription
│   └── shortcuts/               launcher shortcuts built from the catalog
├── feature/                     one folder per mini app, each a *Logic.kt + *Screen.kt
└── ui/
    ├── home/HomeScreen.kt       favourites, the category grid and search
    ├── settings/                theme, wallpaper colours and about
    ├── components/              MiniAppScaffold, OptionEditor, CountStepper
    └── theme/                   colors, typography, ExtendedColors
```

Each mini app keeps its rules and arithmetic in a `*Logic.kt` file with no Android
or Compose imports, so it is covered by plain JVM unit tests in `app/src/test/`.
The `*Screen.kt` file holds the UI and its state.

## Adding a mini app

1. Create `feature/<yourapp>/` with a `*Logic.kt` (pure Kotlin, unit tested) and a
   `*Screen.kt` composable taking an `onBack: () -> Unit`.
2. Add its title and tagline to `res/values/strings.xml`, an accent color to
   `ui/theme/Color.kt`, and an id to `MiniAppIds`.
3. Add one `MiniApp(...)` entry to `MiniAppCatalog`.

That is the whole wiring — the home screen tile, the navigation route, the deep
link and the launcher shortcut all come from the catalog entry.

Mini app ids are load-bearing in two places: the navigation route and the settings
namespace. Changing one orphans that mini app's saved settings and breaks any
shortcut pointing at it, so treat them as fixed once shipped.

## Deep links

`utilities://miniapp/<id>` opens a mini app directly, for example
`utilities://miniapp/dice-roller`. Launcher shortcuts use the same links.

Remaining ideas are tracked in `ROADMAP.md`.
