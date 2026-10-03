---
name: validate-android-pr
description: >-
  Validate a BirdNote pull request on the Android emulator. Check out the PR
  in a worktree, build and install the debug APK, drive the on-screen
  emulator from that PR's test plan, and post a GitHub comment with the full
  result. Use when the user asks to test, verify, or validate a pull request,
  or to check the app end to end on a device or emulator.
---

# Validate an Android pull request

Test the pull request the user named. Derive every tap from that PR's diff and its test plan. Do not reuse a previous PR's script.

The emulator window must stay on the user's screen. A unit-test run does not replace a device pass when the change is visible.

Do not merge, and do not run `gh pr merge`. Post the full result as a GitHub comment on that pull request. The run is unfinished until that comment exists.

## Tooling

Run emulator, `adb`, and Gradle outside the sandbox so they can use the display, `/dev/kvm`, and the SDK.

Read `sdk.dir` from the main checkout's `local.properties`. Export it as `ANDROID_HOME` and put `$ANDROID_HOME/emulator` and `$ANDROID_HOME/platform-tools` on `PATH`.

`mobile-mcp` is already configured in `.cursor/mcp.json`. If this session has its tools (`mobile_list_elements_on_screen`, `mobile_take_screenshot`, `mobile_click_on_screen_at_coordinates`, `mobile_swipe_on_screen`), use them to see and tap. It does not boot the emulator. If those tools are absent, do not stop to reload Cursor. Use [scripts/device.py](scripts/device.py) instead. Same rule either way: tap coordinates come from the element list, never from a scaled screenshot.

`DEVICE` below is `python3` plus the absolute path of `scripts/device.py` in this skill directory (the main checkout, not the PR worktree).

## 1. Read the PR

Check `gh auth status` first. The result comment needs it. If it fails, say so immediately and keep testing. Read the title, body, head branch, and head SHA with `gh pr view N`. If that fails, use the public API:

```bash
curl -sS -H "Accept: application/vnd.github+json" \
  "https://api.github.com/repos/dFuZer/birdnote/pulls/N"
```

The checklist is the PR test plan. Also read the linked issue's "Done when" list. Turn those into checks. Split them:

- **Device**: something a person can see or tap.
- **Unit**: pure logic. Run `:app:testDebugUnitTest` from the PR worktree.
- **Skip**: needs a Play account, a second physical device, or a translation that is not actually missing. Say why.

A docs-only or test-only PR still gets the unit suite when code or tests changed. Skip the emulator and say so.

## 2. Check out the head

Do not edit the main checkout at `/home/dfuzer/Desktop/birdnote`.

```bash
git fetch origin <head-branch>
git worktree list
```

Use a worktree already on that branch when its `HEAD` equals the PR SHA. If the branch is checked out and dirty, or `HEAD` differs, stop and report that. Do not reset it.

Otherwise add a worktree from `origin/<head-branch>` and copy `local.properties` from the main checkout into it. Do not commit that file.

Read `applicationId` from that worktree's `app/build.gradle.kts`. Do not assume `com.example.birdnote`.

## 3. Build

From the worktree, always run both:

```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest
```

A failed build or a failed unit test makes the overall result fail. If the APK was still produced, run the device pass anyway so the comment covers it. The APK is `app/build/outputs/apk/debug/app-debug.apk`.

## 4. Boot the emulator

Reuse an emulator that `adb devices` already lists and whose `sys.boot_completed` is `1`. `DEVICE wait-boot` blocks until that property is `1`. Do not start a second emulator.

Otherwise pick the AVD named `Medium_Phone` when `emulator -list-avds` shows it, otherwise the only AVD. Start it as its own long-running command, with `exec`, so the window stays up:

```bash
export QT_QPA_PLATFORM=xcb
export DISPLAY="${DISPLAY:-:0}"
exec emulator -avd "$AVD" -no-snapshot -no-snapshot-save \
  -no-boot-anim -gpu host -accel on -scale 0.4
```

Then `DEVICE wait-boot`. A saved snapshot has hung this emulator before; `-no-snapshot` avoids that. If the log sits on `Loading snapshot` and no device appears, kill that process and start again with the same flags.

Leave the window open when you finish.

The launcher activity is landscape (`sensorLandscape`). A wide dump is expected. Do not rotate the device to portrait unless this PR changes orientation.

## 5. Install and open

```bash
DEVICE install app/build/outputs/apk/debug/app-debug.apk
DEVICE resolve "$APPLICATION_ID"
DEVICE start "$COMPONENT"
```

`$COMPONENT` is the `package/activity` line from `resolve`.

`pm clear` (`DEVICE clear "$APPLICATION_ID"`) before a check that needs a fresh install. Do not clear in the middle of a check that is about something being remembered.

After every cold start, dump the UI and read a screenshot. A crash counts only when the stack names this `applicationId`. `uiautomator` and `android.hardware` crash lines do not fail the pull request. `AndroidRuntime` alone is not a crash; the process start logs that tag.

```bash
adb logcat -d -t 400 | rg "FATAL EXCEPTION" 
```

## 6. Walk the test plan

For each device check, in order:

1. Dump the UI. `DEVICE dump` prints `click` or `text`, then center x, y, then the label. Labels include `content-desc`. Icon buttons often have no `text`.
2. Tap with `DEVICE tap "exact label"`. One exact string. If the label is off the bottom of a menu, swipe inside that menu using coordinates from the dump, then dump again. Do not reuse a fixed swipe from another screen size.
3. Save a screenshot under `/tmp/birdnote-prN/` with `DEVICE shot`. Read that image before deciding. Staff, noteheads, and other canvas drawing do not appear in the dump.
4. Wait after a navigation or an activity recreate, then dump again. Match the string the PR said the player should see, in the locale this step is using.

Change the phone locale only when the PR's behavior depends on it:

```bash
DEVICE locale sv-SE
DEVICE clear "$APPLICATION_ID"
DEVICE start "$COMPONENT"
```

Restore with `DEVICE locale en-US` before you finish.

For a check that something survives leaving the app, force-stop and open it again:

```bash
adb shell am force-stop "$APPLICATION_ID"
DEVICE start "$COMPONENT"
```

A crash in this app, a missing control, or copy that does not match the test plan is a fail for that check. Keep going through the rest of the list so the comment is complete. The overall result is fail if any check failed. Skipped checks need a reason. Do not mark a check passed because the code looks right.

## 7. Comment on the pull request

Write the same report in the chat and in a GitHub comment. The comment has to stand on its own: quote the strings the device showed. Local screenshot paths are only for the chat.

```markdown
## Device pass — pass|fail

Tested `<sha>` on the Medium Phone emulator.

Build: assembleDebug pass|fail. Unit tests: pass|fail (counts).

| Check | Result | What the device showed |
|---|---|---|
| … | pass, fail, or skipped | quoted label, or the reason it was skipped |

Not merged.
```

Post it with:

```bash
gh pr comment N --body-file /tmp/birdnote-prN/comment.md
```

Confirm the comment URL. If `gh` returns 401, the run is incomplete: paste the comment in the chat, include the `gh` error, and say it was not posted. Do not claim the pull request was updated.

Command names and the usual stalls are in [reference.md](reference.md).
