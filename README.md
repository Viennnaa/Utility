# Utilities

An Android super app: one launcher icon, many small tools. The home screen is a
grid of mini apps; tapping one opens it full screen.

Built with Kotlin, Jetpack Compose and Material 3 (dynamic color on Android 12+,
light and dark themes).

## Mini apps

| Mini app | What it does |
| --- | --- |
| 🔢 Random Number | Rolls a number in a chosen range — 1–6, 1–10, 1–20, 1–100, or a custom range you type. Keeps recent rolls. |
| 🤔 Choice Maker | Answers a yes/no question with a fair coin flip. Keeps recent answers and a running tally. |
| 🎯 List Picker | Add your own options and let it pick one. Optional remove-after-picking turns it into a draw. |
| 🎲 Dice Roller | Throws 1–10 dice, d4 through d100. Shows each die and the total, and keeps recent totals. |

## Building

```bash
./gradlew assembleDebug      # build the APK
./gradlew testDebugUnitTest  # run the unit tests
./gradlew installDebug       # install on a connected device or emulator
```

Requires the Android SDK (compileSdk 35) and JDK 17+. Minimum supported device is
Android 8.0 (API 26).

## Project layout

```
app/src/main/java/com/viennnaa/utilities/
├── MainActivity.kt              entry point
├── UtilitiesApp.kt              navigation: home + one route for all mini apps
├── miniapp/
│   ├── MiniApp.kt               what a mini app is
│   └── MiniAppCatalog.kt        the registry — every mini app is listed here
├── feature/                     one folder per mini app, each a *Logic.kt + *Screen.kt
│   ├── randomnumber/
│   ├── choicemaker/
│   ├── listpicker/
│   └── diceroller/
└── ui/
    ├── home/HomeScreen.kt       the grid of mini app tiles
    ├── components/              shared UI, e.g. MiniAppScaffold
    └── theme/                   colors, typography, ExtendedColors
```

Each mini app keeps its randomness and validation in a `*Logic.kt` file with no
Android or Compose imports, so it is covered by plain JVM unit tests in
`app/src/test/`. The `*Screen.kt` file holds the UI and its state.

## Adding a mini app

1. Create `feature/<yourapp>/` with a `*Logic.kt` (pure Kotlin, unit tested) and a
   `*Screen.kt` composable taking an `onBack: () -> Unit`.
2. Add its title and tagline to `res/values/strings.xml`, and an accent color to
   `ui/theme/Color.kt`.
3. Add one `MiniApp(...)` entry to `MiniAppCatalog`.

That is the whole wiring — the home screen tile and the navigation route both come
from the catalog entry. Mini app `id`s are used as navigation routes, so do not
change one once it has shipped.

Planned mini apps and the plumbing they need are tracked in `ROADMAP.md`.
