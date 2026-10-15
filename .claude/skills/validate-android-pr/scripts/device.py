#!/usr/bin/env python3
"""Drive the booted Android emulator. Coordinates are device pixels."""

import argparse
import os
import re
import subprocess
import sys
import time
from pathlib import Path


DEFAULT_PACKAGE = "com.dfuzer.birdnote"


def sdk_home() -> Path | None:
    home = os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT")
    if home:
        return Path(home)
    for start in (Path.cwd(), Path(__file__).resolve().parent):
        for folder in (start, *start.parents):
            props = folder / "local.properties"
            if props.exists():
                for line in props.read_text().splitlines():
                    if line.startswith("sdk.dir="):
                        return Path(line.split("=", 1)[1].strip())
    return None


def adb_bin() -> str:
    home = sdk_home()
    if home and (home / "platform-tools" / "adb").exists():
        return str(home / "platform-tools" / "adb")
    return "adb"


def run(args: list[str], timeout: int = 30, check: bool = True, binary: bool = False):
    completed = subprocess.run(
        [adb_bin(), *args],
        capture_output=True,
        timeout=timeout,
    )
    if check and completed.returncode != 0:
        sys.stderr.write(completed.stderr.decode(errors="replace"))
        sys.stderr.write(completed.stdout.decode(errors="replace"))
        raise SystemExit(completed.returncode)
    if binary:
        return completed.stdout
    return completed.stdout.decode(errors="replace")


def stop_mobile_mcp_server() -> None:
    # mobile-mcp leaves a DeviceServer holding UiAutomation for 30 minutes,
    # which makes every `uiautomator dump` fail with "already registered".
    subprocess.run(
        [adb_bin(), "shell", "pkill", "-f", "com.mobilenext.mobilecli.DeviceServer"],
        capture_output=True,
    )


def dump_xml() -> str:
    last = ""
    stop_mobile_mcp_server()
    for _ in range(3):
        completed = subprocess.run(
            [adb_bin(), "shell", "uiautomator", "dump", "/sdcard/ui.xml"],
            capture_output=True,
            text=True,
        )
        last = completed.stdout + completed.stderr
        if completed.returncode == 0:
            xml = run(["exec-out", "cat", "/sdcard/ui.xml"], check=False)
            if "<hierarchy" in xml:
                return xml
        time.sleep(0.6)
    raise SystemExit(f"uiautomator dump failed\n{last}")


def nodes(xml: str) -> list[dict]:
    found = []
    for tag in re.findall(r"<node\b[^>]*>", xml):
        def attr(name: str) -> str:
            match = re.search(fr'{name}="([^"]*)"', tag)
            return match.group(1) if match else ""

        bounds = re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', tag)
        if not bounds:
            continue
        x1, y1, x2, y2 = (int(value) for value in bounds.groups())
        label = attr("text") or attr("content-desc")
        if not label.strip():
            continue
        found.append(
            {
                "label": label,
                "x": (x1 + x2) // 2,
                "y": (y1 + y2) // 2,
                "clickable": attr("clickable") == "true",
                "on": attr("checked") == "true" or attr("selected") == "true",
            }
        )
    return found


def print_nodes(items: list[dict]) -> None:
    for item in items:
        kind = "click" if item["clickable"] else "text"
        mark = "\t[on]" if item["on"] else ""
        print(f"{kind}\t{item['x']}\t{item['y']}\t{item['label']}{mark}")


def cmd_dump(_args: argparse.Namespace) -> None:
    print_nodes(nodes(dump_xml()))


def settle_and_dump(seconds: float) -> None:
    time.sleep(seconds)
    print("--")
    items = nodes(dump_xml())
    for _ in range(5):
        if items:
            break
        time.sleep(0.5)
        items = nodes(dump_xml())
    print_nodes(items)


def cmd_tap(args: argparse.Namespace) -> None:
    for index, label in enumerate(args.labels):
        if index:
            time.sleep(max(args.settle, 1.0))
        matches = [item for item in nodes(dump_xml()) if item["label"] == label]
        if not matches:
            print(f"not found: {label}", file=sys.stderr)
            cmd_dump(args)
            raise SystemExit(1)
        clickable = [item for item in matches if item["clickable"]]
        chosen = clickable[0] if clickable else matches[0]
        run(["shell", "input", "tap", str(chosen["x"]), str(chosen["y"])])
        print(f"tapped\t{chosen['x']}\t{chosen['y']}\t{chosen['label']}")
    if args.settle > 0:
        settle_and_dump(args.settle)


def cmd_wait(args: argparse.Namespace) -> None:
    deadline = time.time() + args.timeout
    while True:
        items = nodes(dump_xml())
        present = any(item["label"] == args.label for item in items)
        if present != args.gone or time.time() > deadline:
            print_nodes(items)
            if present == args.gone:
                raise SystemExit(f"timeout waiting for {args.label}")
            return
        time.sleep(1)


def device_booted() -> bool:
    devices = run(["devices"], check=False)
    if not re.search(r"\tdevice$", devices, re.M):
        return False
    return run(["shell", "getprop", "sys.boot_completed"], check=False).strip() == "1"


def cmd_boot(args: argparse.Namespace) -> None:
    if not device_booted():
        home = sdk_home()
        emulator = str(home / "emulator" / "emulator") if home else "emulator"
        avd = args.avd or subprocess.run(
            [emulator, "-list-avds"], capture_output=True, text=True
        ).stdout.split()[0]
        env = dict(os.environ, QT_QPA_PLATFORM="xcb", DISPLAY=os.environ.get("DISPLAY", ":0"))
        log = open("/tmp/birdnote-emulator.log", "w")
        subprocess.Popen(
            [emulator, "-avd", avd, "-no-snapshot", "-no-snapshot-save", "-no-boot-anim",
             "-gpu", "host", "-accel", "on", "-scale", "0.4"],
            stdout=log, stderr=log, env=env, start_new_session=True,
        )
        args.timeout = args.timeout or 240
        cmd_wait_boot(args)
    else:
        print("booted")


def cmd_launch(args: argparse.Namespace) -> None:
    if args.apk:
        cmd_install(args)
    run(["shell", "am", "force-stop", args.package])
    if args.fresh:
        cmd_clear(args)
        run(["logcat", "-b", "crash", "-c"], check=False)
    component = run(["shell", "cmd", "package", "resolve-activity", "--brief", args.package]).split()[-1]
    run(["shell", "am", "start", "-n", component])
    print(f"started\t{component}")
    settle_and_dump(args.settle)


def cmd_crashes(args: argparse.Namespace) -> None:
    # Group the crash buffer by pid and keep processes that name the package:
    # Java "FATAL EXCEPTION … Process: pkg" and native "Fatal signal … >>> pkg <<<".
    by_pid: dict[str, list[str]] = {}
    for line in run(["logcat", "-d", "-b", "crash", "-v", "threadtime"], check=False).splitlines():
        fields = line.split()
        if len(fields) > 5 and fields[2].isdigit():
            by_pid.setdefault(fields[2], []).append(line)
    hits = [lines for lines in by_pid.values() if any(args.package in line for line in lines)]
    if not hits:
        print(f"no crashes for {args.package}")
        return
    for lines in hits:
        print("\n".join(lines[: args.lines]))
        print("--")
    raise SystemExit(1)


def cmd_swipe(args: argparse.Namespace) -> None:
    run(
        [
            "shell",
            "input",
            "swipe",
            str(args.x1),
            str(args.y1),
            str(args.x2),
            str(args.y2),
            str(args.duration),
        ]
    )


def cmd_shot(args: argparse.Namespace) -> None:
    path = Path(args.path)
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(run(["exec-out", "screencap", "-p"], binary=True))
    print(f"{path}\t{path.stat().st_size}")


def cmd_key(args: argparse.Namespace) -> None:
    run(["shell", "input", "keyevent", args.code])


def cmd_wait_boot(args: argparse.Namespace) -> None:
    subprocess.run([adb_bin(), "wait-for-device"], check=True)
    deadline = time.time() + args.timeout
    while time.time() < deadline:
        state = run(["shell", "getprop", "sys.boot_completed"], check=False).strip()
        if state == "1":
            print("booted")
            return
        time.sleep(2)
    raise SystemExit("boot timeout")


def repo_root() -> Path:
    return Path(__file__).resolve().parents[4]


def cmd_install(args: argparse.Namespace) -> None:
    apk = Path(args.apk)
    if not apk.is_absolute() and not apk.exists():
        apk = repo_root() / apk
    run(["install", "-r", str(apk)], timeout=180)
    print("installed")


def cmd_clear(args: argparse.Namespace) -> None:
    run(["shell", "pm", "clear", args.package])
    print("cleared")


def cmd_start(args: argparse.Namespace) -> None:
    run(["shell", "am", "start", "-n", args.component])


def cmd_resolve(args: argparse.Namespace) -> None:
    output = run(["shell", "cmd", "package", "resolve-activity", "--brief", args.package])
    lines = [line.strip() for line in output.splitlines() if line.strip()]
    if not lines:
        raise SystemExit(f"no launcher activity for {args.package}")
    print(lines[-1])


def cmd_locale(args: argparse.Namespace) -> None:
    if args.tag:
        run(["shell", "cmd", "locale", "set-device-locale", args.tag])
        time.sleep(2)
    print(run(["shell", "cmd", "locale", "get-device-locale"]).strip())


def main() -> None:
    parser = argparse.ArgumentParser(description="Drive the booted Android emulator")
    sub = parser.add_subparsers(dest="command", required=True)

    sub.add_parser("dump").set_defaults(func=cmd_dump)

    tap = sub.add_parser("tap")
    tap.add_argument("labels", nargs="+", help="one or more exact labels, tapped in order")
    tap.add_argument("--settle", type=float, default=1.0, help="seconds before the follow-up dump; 0 skips it")
    tap.set_defaults(func=cmd_tap)

    wait = sub.add_parser("wait", help="poll until a label appears (or --gone)")
    wait.add_argument("label")
    wait.add_argument("--gone", action="store_true")
    wait.add_argument("--timeout", type=int, default=60)
    wait.set_defaults(func=cmd_wait)

    boot_any = sub.add_parser("boot", help="reuse a booted device or start the emulator")
    boot_any.add_argument("--avd")
    boot_any.add_argument("--timeout", type=int, default=240)
    boot_any.set_defaults(func=cmd_boot)

    launch = sub.add_parser("launch", help="[install], force-stop, [clear], start, dump")
    launch.add_argument("--apk", nargs="?", const="app/build/outputs/apk/debug/app-debug.apk",
                        help="APK to install; bare --apk means the main checkout's debug APK")
    launch.add_argument("--fresh", action="store_true", help="pm clear first")
    launch.add_argument("--package", default=DEFAULT_PACKAGE)
    launch.add_argument("--settle", type=float, default=2.0)
    launch.set_defaults(func=cmd_launch)

    crashes = sub.add_parser("crashes", help="FATAL EXCEPTIONs naming the package")
    crashes.add_argument("--package", default=DEFAULT_PACKAGE)
    crashes.add_argument("--lines", type=int, default=40, help="lines shown per crashed process")
    crashes.set_defaults(func=cmd_crashes)

    swipe = sub.add_parser("swipe")
    swipe.add_argument("x1", type=int)
    swipe.add_argument("y1", type=int)
    swipe.add_argument("x2", type=int)
    swipe.add_argument("y2", type=int)
    swipe.add_argument("duration", type=int, nargs="?", default=300)
    swipe.set_defaults(func=cmd_swipe)

    shot = sub.add_parser("shot")
    shot.add_argument("path")
    shot.set_defaults(func=cmd_shot)

    key = sub.add_parser("key")
    key.add_argument("code", help="Android key name, for example BACK or ENTER")
    key.set_defaults(func=cmd_key)

    boot = sub.add_parser("wait-boot")
    boot.add_argument("--timeout", type=int, default=180)
    boot.set_defaults(func=cmd_wait_boot)

    install = sub.add_parser("install")
    install.add_argument("apk")
    install.set_defaults(func=cmd_install)

    clear = sub.add_parser("clear")
    clear.add_argument("package")
    clear.set_defaults(func=cmd_clear)

    start = sub.add_parser("start")
    start.add_argument("component", help="package/activity from resolve")
    start.set_defaults(func=cmd_start)

    resolve = sub.add_parser("resolve")
    resolve.add_argument("package")
    resolve.set_defaults(func=cmd_resolve)

    locale = sub.add_parser("locale")
    locale.add_argument("tag", nargs="?", help="BCP 47 tag to set, for example en-US")
    locale.set_defaults(func=cmd_locale)

    args = parser.parse_args()
    args.func(args)


if __name__ == "__main__":
    main()
