#!/usr/bin/env python3
"""Drive the booted Android emulator. Coordinates are device pixels."""

import argparse
import os
import re
import subprocess
import sys
import time
from pathlib import Path


def adb_bin() -> str:
    home = os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT")
    if home:
        candidate = Path(home) / "platform-tools" / "adb"
        if candidate.exists():
            return str(candidate)
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


def dump_xml() -> str:
    last = ""
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
            }
        )
    return found


def print_nodes(items: list[dict]) -> None:
    for item in items:
        kind = "click" if item["clickable"] else "text"
        print(f"{kind}\t{item['x']}\t{item['y']}\t{item['label']}")


def cmd_dump(_args: argparse.Namespace) -> None:
    print_nodes(nodes(dump_xml()))


def cmd_tap(args: argparse.Namespace) -> None:
    matches = [item for item in nodes(dump_xml()) if item["label"] == args.label]
    if not matches:
        print(f"not found: {args.label}", file=sys.stderr)
        cmd_dump(args)
        raise SystemExit(1)
    clickable = [item for item in matches if item["clickable"]]
    chosen = clickable[0] if clickable else matches[0]
    run(["shell", "input", "tap", str(chosen["x"]), str(chosen["y"])])
    print(f"tapped\t{chosen['x']}\t{chosen['y']}\t{chosen['label']}")


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


def cmd_install(args: argparse.Namespace) -> None:
    run(["install", "-r", args.apk], timeout=180)
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
    tap.add_argument("label")
    tap.set_defaults(func=cmd_tap)

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
