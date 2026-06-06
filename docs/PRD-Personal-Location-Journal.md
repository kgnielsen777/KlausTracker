# PRD: Personal Location Journal (Android)

## 1. Product Overview
Personal Android app for one user that automatically captures location every 30 minutes, enriches coordinates into readable places (addresses and POIs/hotels), and summarizes where time is spent (home, work, friends, family, hotels, and other places).

## 2. Problem Statement
Raw coordinates are hard to use. The app should automatically convert location history into understandable places and visit summaries, while allowing manual control over labels.

## 3. Goals
- Capture location automatically in background every ~30 minutes.
- Automatically enrich captured points with human-readable addresses.
- Automatically enrich points and stays with nearby POI names, including hotel names when available.
- Distinguish transit from stays and avoid false place visits while driving.
- Let user define and manage important places (Home, Work, Friend, Family, Custom).
- Provide map view, timeline/list view, and time-spent summaries by day/week/month.
- Keep app personal-only (no sharing).
- Support secure cloud backup and restore.

## 4. Non-Goals (V1)
- Multi-user or collaborative usage.
- Social or public sharing features.
- Fully autonomous labeling without user confirmation.
- Advanced ML-first labeling as core mechanism.

## 5. Target User
Single user: app owner.

## 6. Core Use Cases
- Review where time was spent this week.
- Open a visit and see automatic address plus POI/hotel name.
- Mark or adjust labels such as Home, Work, Friend, Family.
- Confirm or correct suggested recurring places.
- Restore all data after reinstall or moving to another phone.

## 7. Functional Requirements

### 7.1 Location Capture
- FR-1: App captures location automatically every 30 minutes while tracking is enabled.
- FR-2: Capture runs in background using Android-compliant mechanisms.
- FR-3: Each sample stores timestamp, latitude, longitude, accuracy, and speed/motion metadata.
- FR-4: User can pause/resume tracking in settings.

### 7.2 Transit vs Stay Detection
- FR-5: If capture occurs while driving/in transit, app stores transit point but does not create a place stay immediately.
- FR-6: Place stay is created or extended only after dwell criteria are met (low movement within radius for minimum time).
- FR-7: Transit time is excluded from place-duration totals.
- FR-8: Transit segments are visible separately in timeline/map.

### 7.3 Mandatory Enrichment
- FR-9: Every eligible captured point is automatically reverse-geocoded to a human-readable address (best effort).
- FR-10: Nearby POI lookup is automatic; hotel names are auto-enriched when available from provider data.
- FR-11: If POI/hotel is unavailable, app falls back to address-only display.
- FR-12: Enrichment retries after temporary network/service failures.

### 7.4 Place Identity and Labeling
- FR-13: App suggests recurring places based on repeated stays.
- FR-14: User can confirm, edit, or remove suggestions.
- FR-15: Built-in labels include Home, Work, Friend, Family.
- FR-16: Custom labels are supported.
- FR-17: Duplicate places can be merged and canonical place details edited.

### 7.5 Views and Insights
- FR-18: Map view shows points, stays, and transit segments.
- FR-19: Timeline/list view shows chronological visits with enrichment.
- FR-20: Place list shows known places and cumulative time spent.
- FR-21: Reports summarize time spent by place for day/week/month.
- FR-22: Visit detail shows coordinates, address, POI/hotel name (if available), label, and duration.

### 7.6 Backup and Restore (Azure Storage)
- FR-23: App supports cloud backup of captured history, labels, and settings to Azure Blob Storage.
- FR-24: Backups work with offline queueing and sync-on-connect.
- FR-25: Restore reconstructs local data on new/reinstalled device from latest valid snapshot.
- FR-26: App uses short-lived SAS tokens for upload/download; no storage account keys are embedded in the app.
- FR-27: Backup uploads are encrypted before upload and validated with checksums during restore.

## 8. Data Requirements
- Capture record: id, timestamp, lat/lng, accuracy, speed/motion, source, enrichment status.
- Enrichment record: formatted address, POI name, POI type, hotel flag, confidence, provider timestamp.
- Stay record: start/end, centroid, radius, duration, transit/stay classification.
- Place record: canonical place id, label type, custom name, metadata, merge history.
- Backup manifest: backup version, createdAt, schemaVersion, checksum, blob path.

## 9. Permissions, Privacy, and Security
- PR-1: Explicit consent flow for foreground/background location permission.
- PR-2: Clear in-app explanation of why background location is needed.
- PR-3: No social sharing in v1.
- PR-4: Data encrypted at rest on device and encrypted in transit for backup.
- PR-5: User can disable tracking at any time.
- PR-6: Azure access is scoped via short-lived SAS token with least privilege.

## 10. Non-Functional Requirements
- NFR-1: Battery impact target: low daily overhead under normal usage.
- NFR-2: Sampling reliability target: high scheduled-capture success rate.
- NFR-3: Enrichment reliability target: high percentage of points resolved to address when network is available.
- NFR-4: App remains usable offline; enrichment and backups retry later.
- NFR-5: Background behavior complies with Android execution limits.

## 11. Suggested Default Detection Parameters (V1)
- Transit candidate speed threshold: >15 km/h.
- Stay clustering radius: 120 m.
- Minimum dwell time to confirm stay: 10 minutes.
- Confidence boost: 2 consecutive low-movement samples in same cluster.

## 12. Acceptance Criteria
- AC-1: During a test day, 30-minute captures appear in timeline with timestamps.
- AC-2: While driving during a trigger, event is marked transit and does not increment place-stay duration.
- AC-3: For network-available captures, address is auto-populated for the majority of points.
- AC-4: Nearby hotel names appear automatically when provider returns hotel POIs.
- AC-5: User can assign Home/Work/Friend/Family/custom to a detected place and edit later.
- AC-6: Day/week/month summaries are visible and align with visit records.
- AC-7: Backup then restore reproduces location history and place labels.
- AC-8: No storage account key appears in app binaries/config; SAS-token flow is used.

## 13. Edge Cases
- GPS unavailable or poor accuracy.
- No network during enrichment.
- Geocoder/POI API timeout or quota errors.
- Permission revoked after onboarding.
- Device reboot, app process death, or battery optimization interruption.
- Timezone or device clock changes affecting grouping.
- Expired SAS token during backup/restore.

## 14. Release Scope
- V1 must-have: FR-1 through FR-27, PR-1 through PR-6, baseline NFRs, and acceptance criteria.
- Post-V1 candidates: adaptive intervals, stronger predictive labeling, advanced trip analytics.

## 15. Open Decisions Before Build
- Confirm backup token issuer approach (lightweight API, function app, or trusted broker).
- Choose reverse-geocode and POI provider(s) and quota plan.
- Finalize numeric thresholds after pilot testing on real travel patterns.
- Decide backup cadence and retention policy (for example daily snapshots, 30-day retention).
