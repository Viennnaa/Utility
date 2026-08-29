# Roadmap

Everything originally on this list has shipped. What follows is the record of
what was built, and the ideas that came up along the way but were not taken.

## Shipped

All twenty-two mini apps are in `MiniAppCatalog`, grouped into four categories on
the home screen. See `README.md` for what each one does.

- [x] **Decide** — Random Number, Choice Maker, List Picker, Dice Roller,
      Team Splitter, Rock Paper Scissors
- [x] **Calculate** — Tip Splitter, Unit Converter, Percentage, Date Calculator,
      Discount and VAT, Time Zones
- [x] **Text and codes** — Password Generator, Text Tools, QR Generator, QR Scanner,
      WiFi QR
- [x] **Device** — Flashlight, Bubble Level, Compass, Ruler, Stopwatch and Timer

### Plumbing

- [x] **Persistence** — one namespaced DataStore per mini app. Settings the user
      built up are kept; results and history stay transient on purpose
- [x] **Home screen structure** — categories in enum declaration order, plus search
      over titles and taglines
- [x] **Deep links and launcher shortcuts** — `utilities://miniapp/<id>`, with
      shortcut icons drawn from each mini app's emoji and accent
- [x] **CI** — build, unit tests and lint on every push, then instrumented tests
      on an emulator
- [x] **Instrumented tests** — home grid, search behaviour, favourites, settings,
      and opening and closing mini apps
- [x] **Settings and theme choice** — System/Light/Dark and wallpaper colours,
      read as a stream so a change applies at once
- [x] **Favourites** — press and hold to pin, up to eight, sanitised against the
      catalog before drawing

## Not taken

Ideas that came up and were deliberately left out, with the reason.

- **Currency converter.** Needs a live rate feed, so it would be the first mini
  app that stops working offline and the first to need a network permission and
  an API key. Everything else here works on a plane.
- **Notes and to-do.** Real user data, which means backup, export and a migration
  story for the storage format. That is a bigger commitment than a mini app.
- **AlarmManager-backed timer.** The current timer is correct across backgrounding
  because it derives from a timestamp, and the foreground service notification
  covers the alert. Exact alarms would additionally survive the process being
  killed, at the cost of `SCHEDULE_EXACT_ALARM` or `USE_EXACT_ALARM`.

## Ideas not yet taken

Sketched but not built. Grouped by what each would cost, which is the axis that
actually decides the order.

**Nearly free — the dependency or component is already here**

- Other barcode formats (EAN-13, Code 128) through the ZXing writer already in
- Light meter and barometer, on the existing lifecycle-aware `SensorEffect`
- Magnifier, on the CameraX preview the QR scanner already sets up

**Pure logic, no new anything**

- Loan calculator, number base converter, Base64 and URL encoding, hash
  generator, Roman numerals, screen test, device info, bingo caller

**Needs a permission the app does not yet ask for**

- Sound meter (`RECORD_AUDIO`), step counter (`ACTIVITY_RECOGNITION`),
  speedometer (location)

## Worth doing next

- [ ] **Drag to reorder favourites.** Pinning exists; the order within Favourites
      is still the order things were pinned in
- [ ] **Share actions.** QR Generator and WiFi QR have no way to export their
      codes, and Text Tools and Password Generator only copy to the clipboard.
      This is a gap in shipped features rather than a missing one
- [ ] **Widen instrumented coverage.** The current tests cover the home screen and
      navigation; individual mini app interactions are still only unit tested
- [ ] **Translations.** All user-facing text is already in `strings.xml`, so this
      is adding locale folders rather than reworking anything

### Known trade

The first frame after a cold start may use the default theme before the stored
one arrives: DataStore is read off the main thread, and blocking startup to
avoid a brief flash is the worse trade. If it turns out to be visible in
practice, the fix is a small synchronous mirror of just the theme key.

## Conventions

- Each mini app is a folder under `feature/`, holding a `*Logic.kt` with no Android
  or Compose imports plus a `*Screen.kt` taking `onBack: () -> Unit`
- Rules, arithmetic and randomness live in `*Logic.kt` so they are covered by plain
  JVM tests
- Register in `MiniAppCatalog` — the home tile, the route, the deep link and the
  shortcut all come from that one entry
- `MiniApp.id` is both the navigation route and the settings namespace. Never
  change one after it ships
- User-facing text goes in `res/values/strings.xml`
- Settings a user built up belong in `MiniAppPreferences`, keyed by `MiniAppIds`.
  Results and history stay in `rememberSaveable` — they are meant to be transient
- Reusable pieces live outside `feature/`: `core/options`, `core/storage`,
  `core/sensors`, `core/shortcuts`, and the `ui/components` composables. Reach for
  those before writing a third copy of something
- Number formatting that gets trimmed is pinned to `Locale.US`; parsing accepts
  either decimal separator. Trimming a trailing '.' after formatting in a
  comma-decimal locale leaves a stray separator, so the formatters have tests
  that set one — the default-locale tests pass on a dot machine either way
- Money is whole cents and basis points in `core/money`, never a Double
- App-wide settings live in `core/settings` (rules, no Android types) and
  `core/storage/AppPreferences` (storage). There is exactly one DataStore
  delegate, in `UtilitiesDataStore.kt` — declaring a second over the same file
  throws at runtime
