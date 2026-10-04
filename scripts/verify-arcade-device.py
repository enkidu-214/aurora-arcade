#!/usr/bin/env python3
"""Non-destructive navigation smoke test on an explicitly selected development emulator.

Usage: python3 scripts/verify-arcade-device.py emulator-5558 [output-directory]
Requires an already installed Aurora Arcade 0.3 APK. Saves screenshots and a report.
"""
import json
from pathlib import Path
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
ADB = ROOT / ".toolchain/android-sdk/platform-tools/adb"
PACKAGE = "com.aurora.arcade"


class Device:
    def __init__(self, serial, output):
        if not serial.startswith("emulator-"):
            raise ValueError("This smoke test only operates on an explicitly selected emulator")
        self.command = [str(ADB if ADB.exists() else "adb"), "-s", serial]
        self.output = Path(output)
        self.output.mkdir(parents=True, exist_ok=True)

    def run(self, *args, timeout=40):
        return subprocess.run(self.command + list(args), check=True, capture_output=True, timeout=timeout).stdout

    def tree(self):
        self.run("shell", "uiautomator", "dump", "/sdcard/aurora-games-test.xml")
        return ET.fromstring(self.run("exec-out", "cat", "/sdcard/aurora-games-test.xml"))

    @staticmethod
    def bounds(node):
        return tuple(map(int, re.findall(r"\d+", node.attrib["bounds"])))

    def find(self, text, tree=None):
        root = self.tree() if tree is None else tree
        return next((n for n in root.iter("node") if text in (n.get("text"), n.get("content-desc")) and self.bounds(n)[2] > self.bounds(n)[0]), None)

    def tap(self, text, tree=None):
        node = self.find(text, tree)
        if node is None:
            raise AssertionError(f"Visible control missing: {text}")
        x1, y1, x2, y2 = self.bounds(node)
        self.run("shell", "input", "tap", str((x1+x2)//2), str((y1+y2)//2))

    def open_game(self, title):
        for _ in range(5):
            tree = self.tree()
            if self.find(title, tree) is not None:
                self.tap(title, tree)
                return
            screen = self.run("shell", "wm", "size").decode()
            width, height = map(int, re.findall(r"(\d+)x(\d+)", screen)[-1])
            self.run("shell", "input", "swipe", str(width//2), str(int(height*.8)), str(width//2), str(int(height*.35)), "260")
        raise AssertionError(f"Game missing from home: {title}")

    def screenshot(self, name):
        (self.output / f"{name}.png").write_bytes(self.run("exec-out", "screencap", "-p"))


def main():
    serial = sys.argv[1]
    output = sys.argv[2] if len(sys.argv) > 2 else ROOT / ".toolchain/arcade-device"
    device = Device(serial, output)
    device.run("shell", "am", "force-stop", PACKAGE)
    device.run("shell", "am", "start", "-W", "-n", PACKAGE + "/.MainActivity")
    device.tree()
    device.screenshot("home")
    report = []
    for identity, title in [("2048", "2048"), ("snake", "贪吃蛇"), ("breakout", "打砖块"),
                            ("minesweeper", "扫雷"), ("sokoban", "推箱子"), ("link", "连连看"),
                            ("bubble", "泡泡消除"), ("spider", "蜘蛛纸牌")]:
        device.open_game(title)
        tree = device.tree()
        assert any(title in n.get("text", "") for n in tree.iter("node")), title
        assert device.find("玩法", tree) is not None, title
        device.screenshot(identity)
        device.tap("暂停", tree)
        tree = device.tree()
        assert device.find("继续游戏", tree) is not None, title
        device.tap("继续游戏", tree)
        device.run("shell", "input", "keyevent", "3")
        device.run("shell", "am", "start", "-W", "-n", PACKAGE + "/.MainActivity")
        tree = device.tree()
        assert device.find("继续游戏", tree) is not None, title + " background pause"
        device.tap("返回游乐场", tree)
        report.append({"game": identity, "navigation": "passed", "manual_pause": "passed", "background_pause": "passed"})
        print(title + ": navigation / pause / background passed", flush=True)
    device.screenshot("home-final")
    (device.output / "report.json").write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n")
    print("Report:", device.output / "report.json")


if __name__ == "__main__":
    main()
