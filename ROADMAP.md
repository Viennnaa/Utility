# Roadmap

Planned mini apps and the plumbing they need. Grouped by how much new machinery
each one requires, not by priority.

Ordering principle: everything in "Same family" and "Everyday tools" needs no new
permissions and no third-party libraries. The moment we take on the hardware group
we start editing the manifest, so those are held until the core set is in.

## Shipped

- [x] **Random Number** — preset and custom ranges, roll animation, recent rolls
- [x] **Choice Maker** — fair yes/no coin flip, tally, recent answers
- [x] **List Picker** — add options, pick one at random, optional remove-after-pick
- [x] **Dice Roller** — 1–10 dice, d4 through d100, per-die values and total

## Same family (reuses the `*Logic.kt` + `*Screen.kt` pattern, no new deps)

- [x] **Shuffler / Team Splitter** — shipped as Team Splitter
- [ ] **Rock Paper Scissors** — trivial logic, most of the work is the animation
- [ ] **Password / PIN Generator** — length plus character-class toggles.
      Must use `java.security.SecureRandom`, **not** `kotlin.random.Random`

## Everyday tools (pure logic, broadens the app past randomness)

- [x] **Tip & Bill Splitter** — shipped as Tip Splitter
- [ ] **Unit Converter** — length, weight, temperature, volume; mostly a conversion table
- [ ] **Percentage Calculator** — % of, % change, X is what % of Y
- [ ] **Date Calculator** — days between dates, countdown, age. `java.time`, no deps
- [ ] **Text Tools** — case conversion, word and character count, reverse, strip line breaks
- [ ] **Stopwatch & Timer** — logic is easy, but surviving backgrounding needs a
      foreground service and notification permission. Budget more than it looks like

## Hardware-backed (needs manifest changes — hold until the core set is in)

- [ ] **Flashlight** — `CameraManager.setTorchMode()`, no permission required
- [ ] **Bubble Level** — accelerometer, no permission
- [ ] **Compass** — magnetometer, no permission
- [ ] **Ruler** — screen density based; needs per-device calibration to be honest
- [ ] **QR Generator** — needs a library such as ZXing
- [ ] **QR Scanner** — needs CameraX and the camera permission

## Plumbing

Both of these get more expensive the longer they wait.

- [x] **Persistence (DataStore)** — each mini app keeps the settings the user built up
      (option lists, rosters, preferred dice, usual tip) in one namespaced DataStore.
      Results and history stay deliberately transient
- [ ] **Home screen structure** — categories or favourites in `MiniAppCatalog`. A flat
      grid is fine up to roughly 8 tiles
- [ ] **Deep links / launcher shortcuts** — one per mini app; `MiniApp.id` is already the
      route, so the ids are the stable part
- [ ] **Search across mini apps** — worth it once the catalog outgrows one screen

## Open items

- [x] **Verify the Compose UI compiles.** Done, and no longer a manual step: CI builds
      and tests every push (`.github/workflows/android.yml`), since GitHub's runners have
      the Android SDK that the authoring environment does not
- [ ] **Instrumented tests** — the dependencies are declared, nothing is written yet

## Conventions

- Each mini app is a folder under `feature/`, holding a `*Logic.kt` with no Android or
  Compose imports plus a `*Screen.kt` taking `onBack: () -> Unit`
- Randomness and validation live in `*Logic.kt` so they are covered by plain JVM tests
- Register in `MiniAppCatalog` — that is the only wiring; the home tile and the
  navigation route both come from the entry
- `MiniApp.id` is the navigation route. Never change one after it ships
- User-facing text goes in `res/values/strings.xml`
- Settings a user built up belong in `MiniAppPreferences`, keyed by `MiniAppIds`. Results
  and history stay in `rememberSaveable` — they are meant to be transient
- Reusable pieces live outside `feature/`: list-editing rules in `core/options/`, and the
  `OptionEditor` / `CountStepper` / `MiniAppScaffold` composables in `ui/components/`.
  Reach for those before writing a third copy of something
