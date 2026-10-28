#!/usr/bin/env bash
# Runs on a booted emulator (CI): installs the debug APK, makes Dokey the keyboard and takes screenshots.
set -euo pipefail
PKG=com.dhtoan.dokey
OUT=docs/screenshots
mkdir -p "$OUT"

adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell ime enable "$PKG/.ime.DokeyIme"
adb shell ime set "$PKG/.ime.DokeyIme"
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0

shot() {
  sleep 4
  adb exec-out screencap -p > "$OUT/$1.png"
  echo "captured $1"
}

demo() {
  local name=$1; shift
  adb shell am start -W -S -n "$PKG/.DemoActivity" "$@" > /dev/null
  shot "$name"
}

adb shell cmd uimode night no
for t in material_you light sakura lavender coffee; do demo "theme-$t" --es theme "$t"; done
demo one-hand --es theme light --es one_hand right
demo vni-number-row --es theme light --es method vni

adb shell cmd uimode night yes
for t in dark amoled ocean sunset forest midnight; do demo "theme-$t" --es theme "$t"; done

adb shell cmd uimode night no
adb shell am start -W -S -n "$PKG/.settings.SettingsActivity" > /dev/null
shot settings-1
adb shell input swipe 540 1800 540 700 600
shot settings-2

# Leave the shared preferences as a fresh install would have them.
adb shell am force-stop "$PKG"
ls -l "$OUT"
