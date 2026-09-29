# BirdNote

BirdNote is an offline Android app for reading music on the staff. You practice in short timed drills: name a note, name an interval, or name a chord quality while the notation scrolls. The score is how many correct answers fit in one round. Personal bests stay on the device.

There is no account and no network. The interface is in French.

## What you can practice

Open the app, tap **S'entraîner**, and pick a drill.

| Drill | What you read | What you answer |
|-------|----------------|-----------------|
| **Notes** | One note on treble, bass, or both | The note name: Do, Ré, Mi… by default |
| **Intervalles** | Two notes on the treble staff | The interval: Seconde, Tierce, Quarte… |
| **Accords** | A stacked chord, current one and the next | The quality: Majeur, Mineur, and more at higher levels |

Each drill has four difficulties. Notes and chords also have a clef: **Sol**, **Fa**, or **Sol + Fa** (the two clefs alternate). Intervals always use treble. That is 28 timed configurations, and each one keeps its own best score.

**Paramètres** changes how note names are written on the note drill only: solfège, English, German, or solfège with Ti. Interval and chord labels stay in French.

## A round

Setup shows a preview of the range you are about to read: a pink band on the staff, and sample notes for that difficulty and clef. **C'est parti !** starts the round.

- The round lasts **45 seconds**. A bar shows the time left. There is no numeric countdown.
- Each correct answer adds **1** to the score.
- Each wrong answer takes **3 seconds** off the clock and plays an error sound. It does not add a point.
- A correct answer plays the piano pitch you just read: one note, both notes of the interval, or the chord.
- The staff slides left to the next item either way. You can stop early with **Arrêter**.

The result is **Bravo !**, the score, then **Recommencer** (same setup) or **Menu**. If the score is strictly higher than the saved best for that configuration, it is stored. **Mes scores** shows those bests: red numbers where you have played, a dash where you have not.

## Technical stack

The project is a single Android application module, `:app`, package `com.example.birdnote`.

| | |
|--|--|
| Language | Kotlin 2.2.10, official code style. App bytecode is Java 11. |
| UI | Jetpack Compose and Material 3. One activity, `MainActivity`, hosts the navigation graph. |
| Build | Gradle 9.6, Android Gradle Plugin 9.4.1, version catalog in `gradle/libs.versions.toml`. The Gradle daemon uses JDK 25. |
| SDK | `minSdk` 26, `compileSdk` and `targetSdk` 37. |
| Navigation | Navigation Compose. Home → training menu → setup → quiz → result. |
| Persistence | `SharedPreferences` on device: note naming in `SettingsStore`, best scores in `ScoreStore`. |
| Notation | Compose canvas on setup. During a quiz the staff is drawn on its own surface so scrolling stays smooth. Glyphs come from SVG (Coil). |
| Audio | Short piano samples, decoded with JOrbis. A wrong answer uses a separate clip. |
| Tests | JUnit 4 for domain and view-model logic. Espresso and Compose UI tests for flows on a device or emulator. |

Pitch, clef, and scoring live in `domain/` as plain Kotlin, so they can be tested without Android or Compose. Screens in `ui/` draw that result and send taps back out. One view model owns each quiz.

## Layout

```
app/src/main/java/com/example/birdnote/
  domain/          notes, clefs, intervals, chords, scoring
  ui/screens/      home, setup, quiz, result, scores, settings
  ui/quiz/         quiz view models and playback
  ui/staff/        staff drawing and the sliding belt
  ui/navigation/   routes
  ui/theme/        colors and Amaranth type
assets/            illustrations, clef and note artwork, piano samples
```

## Build and test

Install [Android Studio](https://developer.android.com/studio) (or an Android SDK plus JDK 25) and open this directory.

```bash
./gradlew :app:assembleDebug
./gradlew :app:test
./gradlew :app:connectedDebugAndroidTest
```

`assembleDebug` builds a debug APK. `test` runs the local unit tests. `connectedDebugAndroidTest` needs a running emulator or a device.

## More detail

[docs/UTILIZATION_FLOW.md](docs/UTILIZATION_FLOW.md) walks through every screen, the difficulty tables, and what a round looks and sounds like.
