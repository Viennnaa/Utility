# Roadmap

Everything originally on this list has shipped. What follows is the record of
what was built, and the ideas that came up along the way but were not taken.

## Shipped

All nineteen mini apps are in `MiniAppCatalog`, grouped into four categories on
the home screen. See `README.md` for what each one does.

- [x] **Decide** — Random Number, Choice Maker, List Picker, Dice Roller,
      Team Splitter, Rock Paper Scissors
- [x] **Calculate** — Tip Splitter, Unit Converter, Percentage, Date Calculator
- [x] **Text and codes** — Password Generator, Text Tools, QR Generator, QR Scanner
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
- [x] **Instrumented tests** — home grid, search behaviour, and opening and
      closing mini apps

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

## Worth doing next

- [ ] **Reorderable home screen or favourites.** Nineteen tiles is where a fixed
      order starts to chafe; the catalog is already the single source for ordering
- [ ] **Share actions.** QR Generator has no way to export its code, and Text
      Tools and Password Generator only copy to the clipboard
- [ ] **Widen instrumented coverage.** The current tests cover the home screen and
      navigation; individual mini app interactions are still only unit tested
- [ ] **Translations.** All user-facing text is already in `strings.xml`, so this
      is adding locale folders rather than reworking anything

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
  either decimal separator
