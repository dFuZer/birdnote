# Code cleanup handoff

This is one coordinated refactor of the existing application, preserving its routes, preference files and keys, quiz rules, layout values, animation calculations, and audio samples.

- One typed quiz session and shared quiz/setup composition replace the repeated mode implementations.
- Animated setup previews are the sole Compose notation path. Shared layout and glyph specifications live in `ui/staff/StaffLayout.kt`; chord preview grid motion lives beside its setup screen.
- Quiz notation remains on the independent hardware surface render thread. Ordered updates carry notes and configuration together, and explicit cache keys replace parallel invalidation fields.
- Concrete stores live in `data`, playback and PCM utilities in `audio`, and reusable controls in `ui/components`.
- Unused WAV parsing, the nonanimated renderer, diagnostic sound switch, redundant quiz state, queue wrappers, and unused artwork/tooling dependency are removed.

Glyph, plate, and notation strip caches remain. Physical-device performance measurements were unavailable, so no cache removal meets the agreed performance gate.

## Checks collected before testing was stopped

The developer subsequently requested no further tests or app runs and will perform acceptance testing.

- Original source/APKs, seeded queue fingerprints, and 47 baseline screenshots were captured under `/tmp/birdnote-cleanup-baseline` on the implementation host. These temporary artifacts are not part of the repository.
- All 127 refactored unit tests passed, including seeded sequence compatibility, parameterized quiz timing/answer acceptance, ordered rendering updates, layout, interpolation, and audio math.
- Debug, release, and Android test APK builds succeeded.
- Android lint reported 19 `MissingTranslation` errors in existing string resources. Translation resources were not changed by this refactor.
- The refactored emulator flow run stopped at a native HWUI RenderThread SIGSEGV during surface teardown in the chords flow. Its cause has not been established. It must be investigated before release; a passing device-flow result is not claimed.
- Baseline screenshot capture succeeded. Refactored screenshot comparisons, animation frame comparisons, lifecycle/persistence instrumented checks, and physical-device performance measurements remain incomplete.

The existing flow tests and added `CleanupParityTest` / `StoreCompatibilityTest` are available for manual acceptance work. Full visual, lifecycle, audio, storage-upgrade, translation/RTL, and large-text parity still requires that acceptance work.
