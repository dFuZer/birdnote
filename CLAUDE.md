# CLAUDE.md

Guide for AI agents working on **BirdNote**, an Android app for practicing sight-reading. Read this first, then open the files listed for your task.

## What the app does

BirdNote helps people read music notation faster through timed practice: **see → answer → hear → next**. It is free, works offline, and has no account, backend, ads, or network use.

- **Three drills:** single notes, intervals (two notes), and chord qualities (major, minor, sevenths, and more at higher difficulty).
- **Setup:** pick a difficulty and a clef mode. Treble (`SOL`), bass (`FA`), alto, and tenor are available, and treble and bass can also be mixed. An animated preview shows what will be drilled.
- **Quiz:** a timed round. Notation scrolls across the staff and the user taps the answer label. A correct answer plays the pitches on piano. A wrong answer plays `wrong.mp3` and costs time (`MISTAKE_TIME_PENALTY_SECONDS`).
- **Scores:** the best score for each mode, difficulty, and clef combination is saved on the device.
- **Languages:** 12 UI languages. Note labels follow the language by default (C D E… or Do Ré Mi…), and the naming scheme can be changed in Settings.

Out of scope: a sheet-music editor, a full theory course, social features, and a backend. Don't add any of these unless asked.

## Stack

- Single module `:app`, package `com.dfuzer.birdnote`, with one activity (`MainActivity`).
- Kotlin 2.2.10 (official style) compiled to Java 11 bytecode. Jetpack Compose and Material 3; no XML layouts.
- AGP 9.4.1 and Gradle 9.6. Versions live in [gradle/libs.versions.toml](gradle/libs.versions.toml). The Gradle daemon uses JDK 25.
- `minSdk` 26, `compileSdk`/`targetSdk` 37. The version is set in [app/build.gradle.kts](app/build.gradle.kts) (currently `1.0.2`, versionCode 3).
- Persistence uses plain `SharedPreferences`. Clef and notation glyphs are SVGs loaded with Coil. Audio is OGG piano samples decoded with MediaCodec.

```bash
./gradlew :app:assembleDebug              # debug APK
./gradlew :app:test                       # JVM unit tests (domain, quiz, staff math, PCM)
./gradlew :app:connectedDebugAndroidTest  # needs emulator/device
```

## Architecture rules

Follow these rules:

- **`domain/` is pure Kotlin**, with no Android or Compose imports. Pitches, clefs, queue generation, correctness checks, and scoring all belong there, with unit tests.
- **The UI renders domain results and does not compute music logic.**
- **Keep it simple:** no UseCases, Repositories, DI, interfaces with a single implementation, or extra modules. Prefer functions and data classes, and name things for what they do (`pitchAt`, not `NoteManager`).
- **Keep these stable** (existing users depend on them): route strings, preference file names and keys, and score storage keys (`scoreStorageKey`). [StoreCompatibilityTest](app/src/androidTest/java/com/dfuzer/birdnote/StoreCompatibilityTest.kt) checks them.
- Put new user-facing text in [values/strings.xml](app/src/main/res/values/strings.xml) **and** in all 11 translations (`values-*/strings.xml`). Lint already reports existing `MissingTranslation` errors.

## Key files

Source root: `app/src/main/java/com/dfuzer/birdnote/`.

### Domain (start here for rule changes)
| File | Contents |
|--|--|
| [domain/Music.kt](app/src/main/java/com/dfuzer/birdnote/domain/Music.kt) | `Pitch`, `NoteName`, `NoteNaming`, `Clef`, `ClefMode`, `Accidental`, `StaffNote`, `IntervalName`, `ChordQuality`, the configs (`PracticeConfig`, `IntervalConfig`, `ChordConfig`), `spellChord`, `isCorrect`, note labels, and `pianoAssetPath` |
| [domain/Practice.kt](app/src/main/java/com/dfuzer/birdnote/domain/Practice.kt) | Difficulty → pitch range / intervals / chord qualities, plus question generation (`generateNote/Interval/Chord`), queues (`generate*Queue`, `advance*Queue`), and `nextClef` |
| [domain/Scores.kt](app/src/main/java/com/dfuzer/birdnote/domain/Scores.kt) | `PracticeMode`, `scoreStorageKey`, `bestScore`, `improvedBest`, `scoreGrid` |
| [domain/LastSetup.kt](app/src/main/java/com/dfuzer/birdnote/domain/LastSetup.kt), [domain/AppLanguage.kt](app/src/main/java/com/dfuzer/birdnote/domain/AppLanguage.kt) | Remembered last setup and the supported languages |

### Data and audio
- [data/SettingsStore.kt](app/src/main/java/com/dfuzer/birdnote/data/SettingsStore.kt): language, note naming, preferred clefs, and last setups.
- [data/ScoreStore.kt](app/src/main/java/com/dfuzer/birdnote/data/ScoreStore.kt): best scores.
- [audio/QuizSounds.kt](app/src/main/java/com/dfuzer/birdnote/audio/QuizSounds.kt): one audio worker thread that decodes, caches, mixes, and plays samples. [audio/VorbisPcm.kt](app/src/main/java/com/dfuzer/birdnote/audio/VorbisPcm.kt) decodes OGG and [audio/Pcm.kt](app/src/main/java/com/dfuzer/birdnote/audio/Pcm.kt) handles mixing and pitch-shifting. [audio/AnswerFeedback.kt](app/src/main/java/com/dfuzer/birdnote/audio/AnswerFeedback.kt) maps answers to sounds.
- Samples are in the **root** [assets/notes/](assets/notes/) folder (`2A.ogg`…`6C.ogg` and `wrong.mp3`), which is wired in with `sourceSets["main"].assets.srcDir("../assets")`. [scripts/build_piano_samples.py](scripts/build_piano_samples.py) builds them from Salamander Grand Piano. Licenses are in [ASSET-RIGHTS.md](ASSET-RIGHTS.md) and [licenses/](licenses/).

### UI flow
- [MainActivity.kt](app/src/main/java/com/dfuzer/birdnote/MainActivity.kt) → [ui/BirdNoteApp.kt](app/src/main/java/com/dfuzer/birdnote/ui/BirdNoteApp.kt) → [ui/navigation/BirdNoteNavGraph.kt](app/src/main/java/com/dfuzer/birdnote/ui/navigation/BirdNoteNavGraph.kt). The nav graph holds all `Routes` and the quiz route arguments (difficulty, `clefMode`). The flow is Home → Training menu → Note/Interval/Chord setup → Quiz → Result. Scores and Settings are opened from Home.
- [ui/quiz/QuizViewModel.kt](app/src/main/java/com/dfuzer/birdnote/ui/quiz/QuizViewModel.kt): **one typed quiz session** shared by all three modes. It owns the timer (50 ms tick), the mistake penalty, the queue, and the score.
- Screens are in [ui/screens/](app/src/main/java/com/dfuzer/birdnote/ui/screens/):
  - `QuizScreen.kt`: quiz UI and answer buttons.
  - `NoteSetupScreen.kt`, `IntervalSetupScreen.kt`, `ChordSetupScreen.kt`, and `ChordPreviewGrid.kt`: the setup screens and the chord preview grid.
  - `ResultScreen.kt`, `ScoresScreen.kt`, `SettingsScreen.kt`, `PreferredClefsScreen.kt`, `HomeScreen.kt`, `TrainingMenuScreen.kt`, `WelcomePromiseScreen.kt`, and `HelpDialog.kt`: the remaining screens and dialogs.
  - `AnswerLabels.kt` and `StaffDescription.kt`: answer labels and accessibility descriptions.
- [ui/components/](app/src/main/java/com/dfuzer/birdnote/ui/components/): shared buttons, screen framing (`DesignComponents.kt`), and setup controls (`SetupComponents.kt`).
- [ui/theme/](app/src/main/java/com/dfuzer/birdnote/ui/theme/): `BirdNoteTheme`, colors, and the Amaranth font.
- [ui/LayoutTuning.kt](app/src/main/java/com/dfuzer/birdnote/ui/LayoutTuning.kt): **all visual tuning numbers**, matched to the Figma mockups. Adjust sizes and positions here, not in the screens.

### Staff rendering (the most delicate code)
- [ui/staff/StaffLayout.kt](app/src/main/java/com/dfuzer/birdnote/ui/staff/StaffLayout.kt): staff geometry and glyph specifications shared by setup previews and the quiz.
- [ui/staff/ChordLayout.kt](app/src/main/java/com/dfuzer/birdnote/ui/staff/ChordLayout.kt): chord note and accidental placement.
- [ui/staff/StaffCanvas.kt](app/src/main/java/com/dfuzer/birdnote/ui/staff/StaffCanvas.kt) and [ui/staff/SetupPreviewEase.kt](app/src/main/java/com/dfuzer/birdnote/ui/staff/SetupPreviewEase.kt): the Compose canvas and animation for the **setup previews**.
- [ui/staff/StaffSurface.kt](app/src/main/java/com/dfuzer/birdnote/ui/staff/StaffSurface.kt): the **quiz** staff. It is drawn on its own hardware surface with a separate render thread that owns the sliding belt, glyph bitmaps, staff plate, and notation strip caches.
- [ui/staff/StaffUpdates.kt](app/src/main/java/com/dfuzer/birdnote/ui/staff/StaffUpdates.kt): the quiz sends ordered, immutable updates (questions plus config) to the surface, and a recreated surface replays the latest one.
- [ui/staff/StaffSlide.kt](app/src/main/java/com/dfuzer/birdnote/ui/staff/StaffSlide.kt): scroll and slide motion math based on elapsed time.

## Tests

- Unit tests are in `app/src/test/...` and mirror the packages: domain, `QuizSessionTest`, and staff layout and animation math. Add tests here when you change domain or quiz logic.
- Instrumented tests are in `app/src/androidTest/...`: `AppFlowTest`, `AudioDecoderTest`, `PublicationReadinessTest`, and `StoreCompatibilityTest`.

## Current state and known issues

- Version 1.0.2 was released after a large cleanup refactor; see [CLEANUP.md](CLEANUP.md). That refactor merged the per-mode quiz code into one typed session, removed dead code, and moved stores, audio, and components into their own packages.
- **Quiz-surface teardown (fixed 2026-10-15, not yet released):** there used to be a native SIGSEGV in `Surface::hook_cancelBuffer` on HWUI's RenderThread about 2 s after a round ended. It happened because `stop()` returned before the render thread and HWUI had finished with the surface, and the SurfaceView then disconnected its buffer queue mid-frame. `StaffRenderer.stop()` in [StaffSurface.kt](app/src/main/java/com/dfuzer/birdnote/ui/staff/StaffSurface.kt) now **blocks** until its stop message has removed the frame callback, called `surface.release()` (which waits for HWUI's pending frame), and recycled the bitmaps. Keep `stop()` synchronous with `surfaceDestroyed`, and never post or draw after it. The fix is verified on the emulator only; it hasn't been tested on a physical device. `device.py crashes` detects a regression.
- Visual, lifecycle, audio, RTL, and large-text parity after the refactor has not been fully checked on a device.

## Testing on a device (MCP)

To test the app on the emulator, use the **[validate-android-pr skill](.claude/skills/validate-android-pr/SKILL.md)**. It covers both a quick smoke test and full PR validation. Device control goes through a single script, `.claude/skills/validate-android-pr/scripts/device.py`, which has `boot`, `launch`, `tap`, `wait`, `dump`, `shot`, and `crashes` commands, finds the SDK on its own, and needs the sandbox disabled. The skill also has a map of every screen with exact labels. The quiz can be played from text alone. The mobile-mcp server in [.mcp.json](.mcp.json) is optional, and slower.
