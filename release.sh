#!/bin/bash

# Skript beenden, wenn ein Befehl fehlschlägt
set -e

# Ordner "release" löschen und neu erstellen
rm -rf release
mkdir release

# FOSS Release APKs bauen
./gradlew clean
./gradlew assembleFossRelease -PsplitApks=true
if [ -f "app/build/outputs/apk/foss/release/app-foss-universal-release.apk" ]; then
    cp app/build/outputs/apk/foss/release/app-foss-universal-release.apk release/app-foss-release.apk
elif [ -f "app/build/outputs/apk/foss/release/app-foss-universal-release-unsigned.apk" ]; then
    cp app/build/outputs/apk/foss/release/app-foss-universal-release-unsigned.apk release/app-foss-release.apk
fi
cp app/build/outputs/apk/foss/release/app-foss-*.apk release/

# Play Release APKs bauen
./gradlew clean
./gradlew assemblePlayRelease -PsplitApks=true
if [ -f "app/build/outputs/apk/play/release/app-play-universal-release.apk" ]; then
    cp app/build/outputs/apk/play/release/app-play-universal-release.apk release/app-play-release.apk
elif [ -f "app/build/outputs/apk/play/release/app-play-universal-release-unsigned.apk" ]; then
    cp app/build/outputs/apk/play/release/app-play-universal-release-unsigned.apk release/app-play-release.apk
fi
cp app/build/outputs/apk/play/release/app-play-*.apk release/

# Play Release Bundle (AAB) bauen
./gradlew bundlePlayRelease
cp app/build/outputs/bundle/playRelease/app-play-release.aab release/
