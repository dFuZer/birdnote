# BirdNote

**Learn to read notes on the staff by doing it.**

https://github.com/user-attachments/assets/08a00675-d9c5-49a8-b191-7b77cd708e72

BirdNote is a free, offline Android app for anyone who wants to get faster at recognizing what’s written on a music staff. You see real notation, answer from your gut, hear whether you were right, and try again. No account, no ads, no internet required once installed.

## Why BirdNote exists

Reading sheet music is a skill of **sight**, not memory tricks. You improve when you look at many notes, intervals, and chords under a little pressure and get immediate feedback. BirdNote is built for that.

It helps if you:

- Are learning an instrument or voice and want the staff to stop feeling like a foreign language
- Already play but treble, bass, or both still slow you down
- Want to drill intervals or chord *quality* by eye, not only by ear
- Prefer a simple app on your phone over a heavy course or a cluttered “music theory” tool

BirdNote is **not** a sheet-music editor, a full theory course, or a social leaderboard. It is deliberate practice: see → answer → hear → next.

## How it works

- **Pick what to train**: single notes, intervals between two notes, or chord qualities (major, minor, sevenths, and more as you level up).
- **Choose difficulty and clef**: the preview shows the range you will read.
- **Play a timed round**: notation scrolls across the staff like continuous reading. Tap the right label before time runs out.
- **Track yourself**: your best score for each setup is saved on your device so you can beat your own record next time.

Correct answers play the pitches on piano; mistakes cost time and play a clear “wrong” cue, so accuracy matters. The interface supports twelve languages. Note labels follow the selected language by default, and you can choose a different naming scheme in settings. Treble, bass, alto, and tenor clefs are available; treble and bass can also be practiced together.

## Technical stack

The project is a single Android application module, `:app`, package `com.dfuzer.birdnote`.

| | |
|--|--|
| Language | Kotlin 2.2.10, official code style. App bytecode is Java 11. |
| UI | Jetpack Compose and Material 3. One activity, `MainActivity`, hosts the navigation graph. |
| Build | Gradle 9.6, Android Gradle Plugin 9.4.1, version catalog in `gradle/libs.versions.toml`. The Gradle daemon uses JDK 25. |
| SDK | `minSdk` 26, `compileSdk` and `targetSdk` 37. |
| Navigation | Navigation Compose. Home → training menu → setup → quiz → result. |
| Persistence | Concrete `SharedPreferences` stores in `data/`: settings, preferred clefs, last setups, and best scores. |
| Notation | Compose canvas on setup. During a quiz the staff is drawn on its own surface so scrolling stays smooth. Glyphs come from SVG (Coil). |
| Audio | Short piano samples, decoded with JOrbis. A wrong answer uses a separate clip. |
| Tests | JUnit 4 for domain and view-model logic. Espresso and Compose UI tests for flows on a device or emulator. |

Pitch, clef, and scoring live in `domain/` as plain Kotlin, so they can be tested without Android or Compose. Screens in `ui/` render domain results and forward user events. One typed quiz implementation owns each quiz session, with concrete note, interval, and chord configuration functions.

```
app/src/main/java/com/dfuzer/birdnote/
  domain/          notes, clefs, intervals, chords, scoring
  data/            concrete settings and score stores
  audio/           playback, Vorbis decoding, PCM mixing and pitch shifting
  ui/components/   shared buttons, screen framing and setup controls
  ui/screens/      home, setup, quiz, result, scores, settings
  ui/quiz/         shared typed quiz state and timer
  ui/staff/        shared notation layout, animated previews and surface rendering
  ui/navigation/   routes
  ui/theme/        colors and Amaranth type
assets/            root-level artwork and piano samples
```

### Build and test

Install [Android Studio](https://developer.android.com/studio) (or an Android SDK plus JDK 25) and open this directory.

```bash
./gradlew :app:assembleDebug
./gradlew :app:test
./gradlew :app:connectedDebugAndroidTest
```

`assembleDebug` builds a debug APK. `test` runs unit tests. `connectedDebugAndroidTest` needs a running emulator or device.

### Rendering and state ownership

Compose owns controls and setup previews. Quiz notation is published as ordered, immutable updates containing both questions and rendering configuration. The staff render thread owns the sliding belt, geometry, glyph bitmaps, staff plate, and notation strip. Recreated surfaces replay the latest update. The render loop samples elapsed-time motion independently of UI work; countdown ticks are observed only by the quiz top bar.

Preview animation and quiz rendering share geometry, chord placement, and glyph specifications, with small drawing functions for each canvas backend. Audio decoding, sample caching, mixing, playback, and cleanup stay on one audio worker. Navigation keeps the existing route strings, while persistence keeps the existing preference files and serialized keys.

Unit tests cover music rules, practice generation, scoring, quiz sessions, staff geometry and animation, and audio decoding and mixing. `QuizSessionTest` checks all three practice modes through a shared parameterized suite. Instrumented tests cover app navigation, settings, storage compatibility, and audio cleanup. Visual and surface lifecycle checks require manual acceptance testing.
