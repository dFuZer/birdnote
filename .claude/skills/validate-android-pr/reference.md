# When something stalls

| What you see | What to do |
|---|---|
| `D boot` times out | Read `/tmp/birdnote-emulator.log`. If it stops at `Loading snapshot`, kill the emulator with `adb emu kill` and run `D boot` again. |
| `adb: not found`, or `Permission denied` for `/dev/kvm` | The command ran inside the sandbox. Run it again with the sandbox disabled. |
| The dump lists Phone, Messages, or Chrome | The app isn't in front. Run `D launch`. |
| `tap` prints `not found` along with a dump | Choose the exact label from that dump. If it's below the visible part of a menu, swipe inside the menu using coordinates from the dump. The text may be in a different locale than you expected. |
| `uiautomator dump failed` | `D dump` already retries and stops mobile-mcp's server first. If it still fails, check `adb logcat -d \| grep -A2 UiAutomation`. |
| Gradle can't find the SDK in a worktree | Copy `local.properties` over from the main checkout. |
| `gh` returns 401 | Read the PR through the public API and still do the device pass. Say that the comment wasn't posted. |
