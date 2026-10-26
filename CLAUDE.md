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
- Kotlin (official style) compiled to Java 11 bytecode. Jetpack Compose and Material 3; no XML layouts.
- Versions live in [gradle/libs.versions.toml](gradle/libs.versions.toml); `minSdk` 26. The app version is set in [app/build.gradle.kts](app/build.gradle.kts).
- Persistence uses plain `SharedPreferences`. Clef and notation glyphs are SVGs loaded with Coil. Audio is OGG piano samples decoded with MediaCodec.

```bash
./gradlew :app:assembleDebug              # debug APK
./gradlew :app:test                       # JVM unit tests (domain, quiz, staff math, PCM)
./gradlew :app:connectedDebugAndroidTest  # needs emulator/device
```

## Architecture rules

- **`domain/` is pure Kotlin**, with no Android or Compose imports. Pitches, clefs, queue generation, correctness checks, and scoring all belong there, with unit tests.
- **The UI renders domain results and does not compute music logic.**
- **Keep it simple:** no UseCases, Repositories, DI, interfaces with a single implementation, or extra modules. Prefer functions and data classes, and name things for what they do (`pitchAt`, not `NoteManager`).
- **Keep these stable** (existing users depend on them): route strings, preference file names and keys, and score storage keys (`scoreStorageKey`). [StoreCompatibilityTest](app/src/androidTest/java/com/dfuzer/birdnote/StoreCompatibilityTest.kt) checks them.
- Put new user-facing text in [values/strings.xml](app/src/main/res/values/strings.xml) **and** in all 11 translations (`values-*/strings.xml`).

## Key files

Source root: `app/src/main/java/com/dfuzer/birdnote/`.

### Domain (start here for rule changes)
| File | Contents |
|--|--|
| [domain/Music.kt](app/src/main/java/com/dfuzer/birdnote/domain/Music.kt) | `Pitch`, `NoteName`, `NoteNaming`, `Clef`, `ClefMode`, `Accidental`, `StaffNote`, `IntervalName`, `ChordQuality`, the configs, `spellChord`, `isCorrect`, note labels, and `pianoAssetPath` |
| [domain/Practice.kt](app/src/main/java/com/dfuzer/birdnote/domain/Practice.kt) | Difficulty → pitch range / intervals / chord qualities, question generation, queues, and `nextClef` |
| [domain/Scores.kt](app/src/main/java/com/dfuzer/birdnote/domain/Scores.kt) | `PracticeMode`, `scoreStorageKey`, `bestScore`, `improvedBest`, `scoreGrid` |
| [domain/LastSetup.kt](app/src/main/java/com/dfuzer/birdnote/domain/LastSetup.kt), [domain/AppLanguage.kt](app/src/main/java/com/dfuzer/birdnote/domain/AppLanguage.kt) | Remembered last setup and the supported languages |

### Data and audio
- [data/SettingsStore.kt](app/src/main/java/com/dfuzer/birdnote/data/SettingsStore.kt) (language, note naming, preferred clefs, last setups) and [data/ScoreStore.kt](app/src/main/java/com/dfuzer/birdnote/data/ScoreStore.kt) (best scores).
- [audio/QuizSounds.kt](app/src/main/java/com/dfuzer/birdnote/audio/QuizSounds.kt): one audio worker thread that decodes, caches, mixes, and plays samples. `VorbisPcm.kt` decodes OGG, `Pcm.kt` mixes and pitch-shifts, `AnswerFeedback.kt` maps answers to sounds.
- Samples are in the **root** [assets/notes/](assets/notes/) folder, wired in with `sourceSets["main"].assets.srcDir("../assets")`. [scripts/build_piano_samples.py](scripts/build_piano_samples.py) builds them from Salamander Grand Piano. Licenses are in [ASSET-RIGHTS.md](ASSET-RIGHTS.md) and [licenses/](licenses/).

### UI flow
- [MainActivity.kt](app/src/main/java/com/dfuzer/birdnote/MainActivity.kt) → [ui/BirdNoteApp.kt](app/src/main/java/com/dfuzer/birdnote/ui/BirdNoteApp.kt) → [ui/navigation/BirdNoteNavGraph.kt](app/src/main/java/com/dfuzer/birdnote/ui/navigation/BirdNoteNavGraph.kt), which holds all `Routes` and the quiz route arguments. Flow: Home → Training menu → Note/Interval/Chord setup → Quiz → Result. Scores and Settings open from Home.
- [ui/quiz/QuizViewModel.kt](app/src/main/java/com/dfuzer/birdnote/ui/quiz/QuizViewModel.kt): **one typed quiz session** shared by all three modes. It owns the timer (50 ms tick), the mistake penalty, the queue, and the score.
- Screens are in [ui/screens/](app/src/main/java/com/dfuzer/birdnote/ui/screens/); shared buttons, framing, and setup controls in [ui/components/](app/src/main/java/com/dfuzer/birdnote/ui/components/); theme in [ui/theme/](app/src/main/java/com/dfuzer/birdnote/ui/theme/).
- [ui/LayoutTuning.kt](app/src/main/java/com/dfuzer/birdnote/ui/LayoutTuning.kt): **all visual tuning numbers**, matched to the Figma mockups. Adjust sizes and positions here, not in the screens.

### Staff rendering (the most delicate code)
- [ui/staff/StaffLayout.kt](app/src/main/java/com/dfuzer/birdnote/ui/staff/StaffLayout.kt) and [ui/staff/ChordLayout.kt](app/src/main/java/com/dfuzer/birdnote/ui/staff/ChordLayout.kt): staff geometry, glyph specs, and chord note/accidental placement, shared by setup previews and the quiz.
- [ui/staff/StaffCanvas.kt](app/src/main/java/com/dfuzer/birdnote/ui/staff/StaffCanvas.kt) and [ui/staff/SetupPreviewEase.kt](app/src/main/java/com/dfuzer/birdnote/ui/staff/SetupPreviewEase.kt): the Compose canvas and animation for the **setup previews**.
- [ui/staff/StaffSurface.kt](app/src/main/java/com/dfuzer/birdnote/ui/staff/StaffSurface.kt) and [ui/staff/StaffGl.kt](app/src/main/java/com/dfuzer/birdnote/ui/staff/StaffGl.kt): the **quiz** staff. A `StaffRender` thread draws on its own surface with **its own OpenGL ES 2 context**. The staff lines and clefs (the plate) and each distinct chord are painted once on the CPU and uploaded as textures; an answer only paints the chord entering the belt, and a sliding frame is a few textured quads.
- [ui/staff/StaffUpdates.kt](app/src/main/java/com/dfuzer/birdnote/ui/staff/StaffUpdates.kt): the quiz sends ordered, immutable updates to the surface, and a recreated surface replays the latest one. [ui/staff/StaffSlide.kt](app/src/main/java/com/dfuzer/birdnote/ui/staff/StaffSlide.kt): belt and slide motion math based on elapsed time.

Rules for the quiz staff, learned from measuring it on a 90 Hz phone:
- **Don't draw it through HWUI** (`lockHardwareCanvas`, Compose, a `View`). The app has one HWUI RenderThread, and staff frames queued there wait behind Compose frames. A software `lockCanvas` is too slow (about 25 ms a frame).
- **Keep per-frame and per-answer work tiny.** Nothing in a frame may allocate or paint a full-size bitmap; an answer may paint only small sprites.
- **`StaffRenderer.stop()` must stay synchronous with `surfaceDestroyed`.** It returns only after the render thread has destroyed its EGL surface and context. Never draw or post to the surface after it. `device.py crashes` catches a regression (a native SIGSEGV on teardown).

## Tests

- Unit tests are in `app/src/test/...` and mirror the packages: domain, `QuizSessionTest`, and staff layout and animation math. Add tests here when you change domain or quiz logic.
- Instrumented tests are in `app/src/androidTest/...`: `AppFlowTest`, `AudioDecoderTest`, `PublicationReadinessTest`, and `StoreCompatibilityTest`.

## Testing on a device

Use the **[validate-android-pr skill](.claude/skills/validate-android-pr/SKILL.md)** for smoke tests, PR validation, and performance traces. Device control goes through `.claude/skills/validate-android-pr/scripts/device.py` (run with the sandbox disabled); the skill has a map of every screen with exact labels, and the quiz can be played from text alone. For animation smoothness, follow the skill's "Measuring animation smoothness" section: a physical device, a release build, and `device.py trace`.
