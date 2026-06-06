# KlausTracker

## Local setup

The app now uses OpenStreetMap tiles through `osmdroid`, so no map API key is required for local development.

Personal Android location journal app.

## Current State
This repository includes product docs plus a starter Android project scaffold.

## Prerequisites
- JDK 17+
- Android Studio (latest stable)
- Android SDK Platform 35 and Build-Tools

## Quick Start
1. Open the project in Android Studio.
2. Let Android Studio install missing SDK components.
3. Generate Gradle wrapper if missing:
   - `gradle wrapper --gradle-version 8.10.2`
4. Sync project.
5. Run app on emulator/device.

## Scope Notes
- Personal-only app.
- No sharing/social features in v1.
- Mandatory automatic address and POI/hotel enrichment.

## Docs
- `docs/PRD-Personal-Location-Journal.md`
- `docs/Implementation-Checklist.md`
- `docs/Architecture-and-Data-Model.md`
- `docs/Engineering-Backlog-Issues.md`
