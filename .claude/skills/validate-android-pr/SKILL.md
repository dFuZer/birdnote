---
name: validate-android-pr
description: >-
  Validate a BirdNote pull request, or just smoke-test the app, on the Android
  emulator. Builds and installs the debug APK, drives the app through adb from
  the PR's test plan, and posts a GitHub comment with the result. Use when the
  user asks to test, verify, or validate a pull request, or to check the app
  end to end on a device or emulator.
---

# Test BirdNote on the emulator

## Tooling: one script, no setup

`D` stands for this exact command; paste it literally every time:

    python3 /home/dfuzer/Desktop/birdnote/.claude/skills/validate-android-pr/scripts/device.py

Don't put it in a shell variable. In zsh, `$D …` fails with exit 127, and variables don't carry over between tool calls anyway. Chain several commands in one call with `&&`.

Run every `D`, `adb`, emulator, and Gradle command **with the sandbox disabled**, because they need `/dev/kvm`, the display, and the SDK. The script finds the SDK through `local.properties`, so you don't need to export `ANDROID_HOME` or change `PATH`.

| Command | What it does |
|---|---|
| `D boot` | Reuses a booted device, or starts the AVD and waits (about 20 s). Prints `booted`. |
| `D launch [--apk [PATH]] [--fresh]` | Installs, force-stops, clears data (`--fresh`), starts the app, waits 2 s, then **dumps**. A bare `--apk` installs the main checkout's debug APK. Relative paths resolve from the repo root. |
| `D dump` | Prints one line per label: `text\|click  centerX  centerY  label` (device pixels) |
| `D tap "A" "B" …` | Taps exact labels in order, about 1 s apart, then **dumps the final screen**. `--settle S` sets the wait before that dump; `--settle 0` skips the dump. |
| `D wait "Label" [--gone] [--timeout 60]` | Polls until the label appears (or disappears), then dumps |
| `D shot /tmp/birdnote-x/name.png` | Saves a screenshot (read it with the Read tool) |
| `D key BACK`, `D swipe x1 y1 x2 y2` | Key press and swipe |
| `D crashes` | Shows Java and native crashes of the app since the last `launch --fresh`, with the first 40 lines for each crashed process, and exits 1. If there are none it prints `no crashes for …`. It ignores system noise such as `uwb-service`. |
| `D locale [tag]`, `D clear PKG`, `D resolve PKG`, `D start COMPONENT` | Lower-level helpers. `launch` covers the usual case. |

`launch`, `tap`, and `wait` already print the next screen, so **don't call `dump` after them**. Every step is one call. Take tap coordinates only from the dump, never from a screenshot, because screenshots are scaled when shown. Look at a screenshot only when what you're checking is visual (staff drawing, layout, colors).

mobile-mcp is set up in `.mcp.json` and is optional. Use `device.py` instead; it's about 5× faster per read and prints only the app's own labels. If you do use mobile-mcp:
- The device id is `Medium_Phone`.
- Element `at=` values are top-left corners, so add half of `size`.
- It wrongly reports portrait, so its screenshot scale factors are wrong.
- `mobile_list_crashes` is mostly system noise.
- It leaves a device server running that blocks raw `uiautomator dump`. `D dump` stops that server itself.

## Map of the app

en-US, landscape 2400x1080. Labels are exact.

1. **Fresh install only** (after `--fresh`): the welcome screen with clef cards, then `Let's go!`, then `Thank you!`. `Treble` and `Bass` start out selected, and tapping a card toggles it. The dump **does not show which cards are selected**. Check the `Clefs: …` line in Settings instead, or take a screenshot.
2. **Home**: `Practice`, `My scores`, `Settings`.
3. **Practice**: `Notes`, `Intervals`, `Chords`, and `Back`.
4. **Setup**: difficulty chips `1` to `4` and `Let's go!`. Notes and Chords also have a `Clef` selector; Intervals has none. Positions differ between modes, so always tap by label.
5. **Quiz**: `Stop`, `Time remaining`, the score (the number at the top right), the staff description (for example `Treble, Line 2 from the bottom`), and the answer labels (`C` `D` `E` `F` `G` `A` `B` for notes). From the bottom, treble lines are E G B D F and treble spaces are F A C E; bass lines are G B D F A and bass spaces are A C E G. A right answer adds 1 to the score. A wrong answer takes off 3 seconds. Both move to the next question. Taps during the quiz can use `--settle 0.3`.
   `Stop` ends the round early. It sometimes goes to Result and sometimes to Home, but the score is saved either way. For a full-round check, don't tap Stop.
   Changing the language in Settings resets a note-naming scheme you chose by hand. Pick the naming again after any language change.
6. **Result**: a round lasts 45 seconds, so use `D wait "Try again" --timeout 60`. The screen shows `Great job!`, `Your score:`, `N note(s)`, `Your first score!` (only for a first score), `Try again`, and `Menu`.

## Smoke test (no PR)

From the main checkout:

Five calls in total, each with the sandbox disabled:

```
1. cd /home/dfuzer/Desktop/birdnote && ./gradlew -q :app:assembleDebug && D boot && D launch --fresh --apk
2. D tap "Let's go!" "Thank you!" Practice Notes "Let's go!"     → ends in the quiz
3. D tap <right answer> --settle 0.3                             → score goes up by 1
4. D tap <wrong answer> --settle 0.3                             → score stays the same
5. D wait "Try again" --timeout 60 && D crashes                  → result screen, no crashes
```

The first question is random, so read the answer from the staff description in the step 2 dump. Each later answer comes from the dump of the step before. A smoke test posts nothing to GitHub. Report each step and what the screen showed. Leave the emulator running.

## Validating a pull request

Derive every check from that PR's diff, test plan, and linked issue ("Done when"). Don't reuse an earlier PR's script. A unit-test run doesn't replace a device pass when the change is visible. **Never merge.** The run is unfinished until a GitHub comment with the result exists.

1. **Read the PR.** Run `gh auth status`; if it fails, say so right away and keep testing. Read the title, body, head branch, and SHA with `gh pr view N`. If that fails, use `curl -sS https://api.github.com/repos/dFuZer/birdnote/pulls/N`. Sort the checks into three groups:
   - **Device:** something a person can see or tap.
   - **Unit:** pure logic.
   - **Skip:** needs a Play account, a second device, or similar. Say why.

   Docs-only or test-only PRs get the unit suite but no device pass.
2. **Check out the head.** Never edit the main checkout. Run `git fetch origin <branch>` and then `git worktree list`. Reuse a worktree whose `HEAD` matches the PR's SHA. If the branch is checked out but dirty or at a different commit, stop and report it, and don't reset it. Otherwise run `git worktree add` from `origin/<branch>` and copy `local.properties` into the new worktree (don't commit it). Read `applicationId` from `app/build.gradle.kts`. If it isn't `com.dfuzer.birdnote`, pass `--package` to `launch` and `crashes`.
3. **Build** in the worktree: `./gradlew -q :app:assembleDebug :app:testDebugUnitTest`. A failure makes the overall result fail. If an APK was still produced, do the device pass anyway.
4. **Device pass:** `D boot`, then `D launch --fresh --apk <worktree>/app/build/outputs/apk/debug/app-debug.apk`. Then work through the checks with `tap` and `wait`. Compare the labels you see against the strings the PR promises.
   - For persistence checks, run `D launch` without `--fresh`.
   - For locale checks, run `D locale xx-YY` and then `D launch --fresh`. Restore `en-US` afterwards.
   - Finish with `D crashes`.

   Any crash, missing control, or wrong text fails that check. Finish the whole list anyway, and never pass a check just because the code looks right.
5. **Comment** with `gh pr comment N --body-file /tmp/birdnote-prN/comment.md`, and give the same report in chat:

   ```markdown
   ## Device pass - pass|fail

   Tested `<sha>` on the Medium Phone emulator.

   Build: assembleDebug pass|fail. Unit tests: pass|fail (counts).

   | Check | Result | What the device showed |
   |---|---|---|
   | … | pass, fail, or skipped | quoted label, or why it was skipped |

   Not merged.
   ```

   Confirm the comment URL. If `gh` fails, paste the comment in chat, quote the error, and say it wasn't posted.

When something stalls, see [reference.md](reference.md).
