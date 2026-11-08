<div align="center">

# OSPOT

**Open Source Progressive Overload Tracker**

A fast, minimal, fully offline workout logger for Android, built around one idea: *beat your last session*.

![Platform](https://img.shields.io/badge/platform-Android%2013%2B-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.2-7F52FF?logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=white)
![License](https://img.shields.io/badge/license-AGPL--3.0-blue)
![Version](https://img.shields.io/badge/version-2.2.0-orange)

<img src="docs/screenshots/logging.jpg" width="320" alt="OSPOT logging screen with a running rest timer">

</div>

---

## Why OSPOT?

Most workout apps come with accounts, feeds, ads and subscriptions. OSPOT is the opposite: you define your sessions, log your sets, and see whether the numbers are going up. That is progressive overload, and that is the whole app.

- **No account, no cloud, no ads.** Your data lives on your device.
- **No permissions needed.** No permissions needed to use the app, no features locked behind permissions.
- **Open source** under AGPL-3.0.
- **Built for use between sets:** one-tap steppers and a rest timer that is quick to start.

## Features

### Session-based logging
- Create your own sessions (Upper, Lower, Push, Pull, Legs, whatever fits your split) and switch between them from the bottom bar.

  <img src="docs/screenshots/session-bar.jpg" width="320" alt="Session switcher in the bottom bar">

- Every exercise is a collapsible card with its own sets. Use **Collapse All** from the menu to get a clean overview.

  <img src="docs/screenshots/card-collapsed.jpg" width="260" alt="A collapsed exercise card">&nbsp;&nbsp;<img src="docs/screenshots/card-expanded.jpg" width="260" alt="The same exercise expanded">&nbsp;&nbsp;<img src="docs/screenshots/menu-top.jpg" width="260" alt="Menu with Collapse All">

- Each exercise has an optional **note**, shown by the note icon next to its name (it lights up when a note exists). Use it for cues, machine settings, tempo, or **units** if you want them (e.g. "lbs").

  <img src="docs/screenshots/note-with-closed.jpg" width="320" alt="Exercise with a note, note closed">&nbsp;&nbsp;<img src="docs/screenshots/note-with-open.jpg" width="320" alt="Exercise with a note, note open">
  <br><br>
  <img src="docs/screenshots/note-none-closed.jpg" width="320" alt="Exercise without a note, note closed">&nbsp;&nbsp;<img src="docs/screenshots/note-none-open.jpg" width="320" alt="Exercise without a note, note open for entering text">

### Three kinds of sets
Each set is tagged so warmups don't pollute your real numbers, and each type has its own marker:

| Marker | Type | Purpose |
|:---:|------|---------|
| 🟢 Circle | **Warmup** | Light set (~50% of working weight) |
| 🟧 Square | **Primer** | Heavy, post-activation set (~80%+ of working weight, 1-3 reps) |
| 🔺 Triangle | **Working** | Full-intensity set, close to failure (0-2 RIR) |

<p align="center">
  <img src="docs/screenshots/set-types.jpg" width="260" alt="An exercise with a warmup, a primer and a working set">&nbsp;&nbsp;<img src="docs/screenshots/empty-state.jpg" width="260" alt="First-launch screen with no sessions, showing the tooltip card">&nbsp;&nbsp;<img src="docs/screenshots/add-set-menu.jpg" width="260" alt="Add set dropdown for choosing the set type">
</p>

### Weight, reps, RIR and RPE
- Tap a value to type it, or use the green/red **steppers** to nudge it up and down.

  <img src="docs/screenshots/steppers.jpg" height="320" alt="Weight and reps steppers on an exercise card">

- **RIR** and **RPE** are optional. Turn either on in Settings.

  <img src="docs/screenshots/rir-toggles.jpg" width="320" alt="Settings toggles for RIR and RPE">&nbsp;&nbsp;<img src="docs/screenshots/rir-rpe-enabled.jpg" width="320" alt="Exercise card with RIR and RPE enabled">

- **Configurable weight increment** (1, 1.25, 2.5, 5, 10) so the stepper matches the plates you actually have.

  <img src="docs/screenshots/weight-increment.jpg" width="320" alt="Weight increment setting">

- **Unitless on purpose.** OSPOT tracks the number and nothing else, since you almost certainly train in one unit. If you ever need to say which, put it in the exercise note.

### Rest timer
- A timer pill sits in the header: **play/pause**, countdown, and **cancel**.

  <img src="docs/screenshots/timer-pill.jpg" width="320" alt="Timer pill in the header with a running countdown">

- Start it from the menu with a **preset** (defaults: 1:00, 1:30, 2:00, 3:00; add, remove or change them in Settings) or use **Custom...** for a one-off duration.

  <img src="docs/screenshots/menu.jpg" width="320" alt="Menu with rest timer presets and Custom...">&nbsp;&nbsp;<img src="docs/screenshots/settings-timer.jpg" width="320" alt="Rest timer settings with presets and custom timer toggle">

- Scheduled through Android's alarm system, so it still fires with the screen off or the app in the background.
- **Continuous vibration pulse** when it ends, until you dismiss it. Can be turned off.

### Progress chart
<p align="center"><img src="docs/screenshots/progress-chart.jpg" width="320" alt="OSPOT progress chart"></p>

- The main chart plots your **top set per exercise over time** (heaviest weight).
- **Pinch to zoom and pan** along the timeline. Axis labels and grid lines adapt to the zoom level.
- **Select exercises by session** with checkboxes, or hit **Select All**.

  <img src="docs/screenshots/chart-picker.jpg" width="320" alt="Exercise picker grouped by session">

- **Tap a legend entry to highlight** one exercise and fade the rest.

  <img src="docs/screenshots/legend-none.jpg" width="320" alt="Progress chart with no legend entry highlighted">&nbsp;&nbsp;<img src="docs/screenshots/legend-flat-press.jpg" width="320" alt="Progress chart with flat press highlighted and the rest faded">

- Line colors are assigned automatically so that exercises with similar numbers get **visually distinct colors**.
- **Exercises you delete keep their history.** They move into a "Removed" group in the picker, so old progress is never lost.
- **History follows the exercise name.** If you rename an exercise (even just its capitalization), the old history moves to the "Removed" group and the new name starts a fresh one. Exercises with the same name in different sessions share one history.
- **Exercise Specific Progress** is a separate chart under the main one. Pick a single exercise from the dropdown to see its weight over time with the **reps shown on the data points**, so progress that doesn't move the weight (like going from 7 to 8 reps) is visible.

  <img src="docs/screenshots/exercise-specific-chart.jpg" width="320" alt="Exercise Specific Progress chart for flat press with reps shown on the data points">

- A rep count is shown **only when the reps or the weight change** from the previous point, so long runs of identical sessions stay uncluttered. A point with no label has the same weight and reps as the one before it.
- The reps shown belong to your **top set** for that day: the heaviest weight, and if several sets share it, the one with the most reps.
- The weight axis **fits the exercise's own range**, so the line fills the chart instead of hugging the top. Pinch to zoom works here too, and labels on points that sit very close together stack with the later ones on top until you zoom in.

### Automatic progress tracking
There is no "finish workout" button. The app snapshots your top set for each exercise **once per day** when you open it. If you skip days, the gap is filled with your last values so the chart stays continuous. You can also run **Log Current Data for Today** manually from Settings.

<img src="docs/screenshots/settings-manual.jpg" width="320" alt="Log Current Data for Today button in Settings">

### Make it yours
- **Accent color:** violet, indigo, blue, green, yellow, orange, red or white.

  <img src="docs/screenshots/settings-accent.jpg" width="320" alt="Accent color picker">

- Dark, high-contrast Material 3 UI.

### Your data, portable
- **Export** everything (sessions, exercises, sets, settings, progress history) to one **JSON** file.
- **Import** it on a new phone or after a reinstall.

  <img src="docs/screenshots/settings-data.jpg" width="320" alt="Export and import buttons in Settings">

- Plain JSON, so it is easy to inspect, back up, edit or script against.

## What OSPOT is (and isn't)

OSPOT is a dedicated gym logging app. It is not a social fitness platform, a nutrition tracker or a personal trainer. It is for lifters who already know what to do in the gym and just need a place to log it, which includes two kinds of people:

- **Experienced or advanced lifters,** who have years of training behind them and know how to progress without help.
- **Science-based lifters,** who may not be advanced, but understand the principles of training well enough that they don't need an app to coach them.

That focus comes with trade-offs, and most of them are deliberate:

| Not included | Why |
|---|---|
| **Exercise library or demo videos** | By design. You type your own exercises, so you are never confined to a predefined list. |
| **Suggestions for the next weight or reps** | It shouldn't tell you. OSPOT shows your history and you decide when to progress, because you know better than an app when it's time to go up. |
| **Browsable workout history log** | A complexity trade-off. The chart shows your top set over time, and the exported JSON holds the daily history for every exercise if you want the raw data. |
| **Volume, estimated 1RM or other analytics** | By design. Volume (weight x reps, or set count) isn't a useful thing to track here, and set counts are hard to derive anyway since exercises aren't categorised by name. A 1RM estimate is an arbitrary formula that not everyone needs, so the main chart plots the heaviest weight and the exercise-specific chart adds the reps from that top set, with nothing calculated. |
| **Routines or programs, supersets, plate calculator** | By design, to keep the app flexible and simple. |
| **Cloud sync or automatic backup** | By design. Manual JSON export and import is there when you want a backup. |
| **iOS, wearable or widget support** | A widget is on my mind. iOS and wearables won't happen any time soon, since I don't own either. |
| **Play Store release** | I'm a student and can't afford the developer fee, so install the APK from [Releases](https://github.com/Navyadeepx/OSPOT/releases). It is also a single-developer project, so updates depend on one person. |

## Getting started

### Install

Download the latest APK from the [**Releases**](https://github.com/Navyadeepx/OSPOT/releases) page and install it on your device.

> **Requires** Android 13 (API 33) or newer.

### Build from source

```bash
git clone https://github.com/Navyadeepx/OSPOT.git
cd OSPOT
./gradlew assembleDebug
```

The debug APK ends up in `app/build/outputs/apk/debug/`. The debug build installs as `com.navyadeep.ospot.debug`, so it can sit next to a release install.

You can also open the project in a recent **Android Studio** and hit *Run*.

## Tech stack

- **Language:** Kotlin 2.2
- **UI:** Jetpack Compose with Material 3
- **Persistence:** Jetpack DataStore (Preferences), serialized with `kotlinx.serialization` (JSON)
- **Timer:** `AlarmManager` with a `BroadcastReceiver`
- **Charts:** custom Compose `Canvas` implementation (no charting library)
- **Min SDK:** 33, **Target SDK:** 36

## Project structure

```
app/src/main/java/com/navyadeep/ospot/
├── MainActivity.kt        # Screens, data models, persistence and chart
├── RestTimerReceiver.kt   # Alarm scheduling and vibration for the rest timer
└── ui/theme/              # Color, theme and typography
```

## Export format

Exports are a single JSON document shaped like this (abridged):

```jsonc
{
  "sessions": ["Upper", "Lower"],
  "sessionExercises": {
    "Upper": [
      {
        "name": "Flat Press",
        "note": "",
        "sets": [
          { "type": "PRIMER",  "weight": "75", "reps": "3", "rir": "-", "rpe": "-" },
          { "type": "WORKING", "weight": "95", "reps": "5", "rir": "-", "rpe": "-" }
        ]
      }
    ]
  },
  "progressData": { "Flat Press": { "2026-11-06": { "weight": 95.0, "reps": 5 } } },
  "timerPresets": [60, 90, 120, 180],
  "weightIncrement": "2.5"
  // ...plus display and appearance settings
}
```

## Contributing

Issues and pull requests are welcome. If you have an idea, open an issue first so we can talk it through. For code changes:

1. Fork the repo and create a branch (`git checkout -b feature/my-change`)
2. Commit your changes
3. Open a pull request describing what changed and why

## License

OSPOT is licensed under the **GNU Affero General Public License v3.0**. See [LICENSE](LICENSE) for details.

---

<div align="center">
Made by <a href="https://github.com/Navyadeepx">Navyadeep</a>. If OSPOT helps your training, consider giving it a ⭐
</div>
