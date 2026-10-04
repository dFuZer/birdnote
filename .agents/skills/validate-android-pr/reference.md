# Device command crib

`DEVICE` is `python3` and the absolute path of `scripts/device.py`. Export `ANDROID_HOME` from `sdk.dir` in the main checkout's `local.properties` before any `adb` or emulator command.

| Need | Command |
|---|---|
| List controls | `DEVICE dump` |
| Tap a label | `DEVICE tap "Settings"` |
| Scroll a menu | `DEVICE swipe x1 y1 x2 y2 duration`, using centers from `dump` |
| Screenshot | `DEVICE shot /tmp/birdnote-prN/step.png` |
| Back key | `DEVICE key BACK` |
| Wait for boot | `DEVICE wait-boot` |
| Install | `DEVICE install path/to/app-debug.apk` |
| Launcher component | `DEVICE resolve com.dfuzer.birdnote` |
| Open | `DEVICE start com.dfuzer.birdnote/.MainActivity` |
| Fresh state | `DEVICE clear com.dfuzer.birdnote` |
| Phone language | `DEVICE locale es-MX` |
| Read phone language | `DEVICE locale` |

`resolve` and `clear` take the `applicationId` from the PR, which may not be `com.dfuzer.birdnote`.

Screenshots are device pixels in the file. The on-screen window is scaled to 0.4. Ignore the window size when tapping.

## When something stalls

| What you see | What to do |
|---|---|
| `adb: no devices` and the log ends at `Loading snapshot` | Kill that emulator. Start again with `-no-snapshot -no-snapshot-save`. |
| Dump lists Phone, Messages, Chrome | The app is not in front. `DEVICE start` the component from `resolve`, then dump again. |
| `tap` says `not found` | Dump again. Try `content-desc`. Swipe the open menu. The string may be in another locale than the one you assumed. |
| `uiautomator dump` fails once | Run `dump` again. It already retries three times. |
| Gradle cannot see the SDK | The worktree has no `local.properties`. Copy it from the main checkout. |
| `gh` returns 401 | Read the PR with the public API and still do the device pass. The run is incomplete until `gh pr comment` succeeds. Say that the comment was not posted. |
| `FATAL EXCEPTION` in logcat | Fail the check only when the stack names this applicationId. Ignore `uiautomator` and `android.hardware`. |
