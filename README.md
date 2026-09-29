# BirdNote

**Learn to read notes on the staff by doing it—not by reading about it.**

https://github.com/user-attachments/assets/08a00675-d9c5-49a8-b191-7b77cd708e72

BirdNote is a free, offline Android app for anyone who wants to get faster at recognizing what’s written on a music staff. You see real notation, answer from your gut, hear whether you were right, and try again. No account, no ads, no internet required once installed.

## Why BirdNote exists

Reading sheet music is a skill of **sight**, not memory tricks. You improve when you look at many notes, intervals, and chords under a little pressure and get immediate feedback. BirdNote is built for that: short rounds that feel like a game, but the content is always honest notation on a staff.

It helps if you:

- Are learning an instrument or voice and want the staff to stop feeling like a foreign language
- Already play but treble, bass, or both still slow you down
- Want to drill intervals or chord *quality* by eye, not only by ear
- Prefer a simple app on your phone over a heavy course or a cluttered “music theory” tool

BirdNote is **not** a sheet-music editor, a full theory course, or a social leaderboard. It is deliberate practice: see → answer → hear → next.

## How it works

1. **Pick what to train** — single notes, intervals between two notes, or chord qualities (major, minor, sevenths, and more as you level up).
2. **Choose difficulty and clef** (where it applies) — the preview shows the range you will read.
3. **Play a timed round** — notation scrolls across the staff like continuous reading. Tap the right label before time runs out.
4. **Track yourself** — your best score for each setup is saved on your device so you can beat your own record next time.

Correct answers play the pitches on piano; mistakes cost time and play a clear “wrong” cue, so accuracy matters. The interface is in **French** (solfège note names by default; you can switch to English or German labels for the note drill in settings).

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

Pitch, clef, and scoring live in `domain/` as plain Kotlin, so they can be tested without Android or Compose. Screens in `ui/` render domain results and forward user events. One view model owns each quiz screen.

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

### Build and test

Install [Android Studio](https://developer.android.com/studio) (or an Android SDK plus JDK 25) and open this directory.

```bash
./gradlew :app:assembleDebug
./gradlew :app:test
./gradlew :app:connectedDebugAndroidTest
```

`assembleDebug` builds a debug APK. `test` runs unit tests. `connectedDebugAndroidTest` needs a running emulator or device.

## More detail

[docs/UTILIZATION_FLOW.md](docs/UTILIZATION_FLOW.md) describes every screen, drill variant, and what a round looks and sounds like.
