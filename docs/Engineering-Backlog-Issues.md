# Engineering Backlog (Issue-Ready)

This backlog is derived from the PRD and architecture docs and is grouped into milestones for execution.

## Milestone 1: Foundation and Platform

### Issue 1: Initialize Android App Baseline
- Type: Feature
- Goal: Create Kotlin Android project baseline with clean package structure and build variants.
- Scope:
  - Set min/target SDK.
  - Configure dev/prod flavors.
  - Add core modules for app, data, domain.
- Acceptance Criteria:
  - App builds and runs on emulator/device.
  - CI build task executes successfully.
  - Flavor-specific applicationId suffix configured.

### Issue 2: Permission and Consent Flow
- Type: Feature
- Goal: Implement foreground/background location permissions with clear rationale UX.
- Scope:
  - Foreground permission request.
  - Background permission request and rationale screens.
  - Settings entry to re-open permission guidance.
- Acceptance Criteria:
  - User can complete permission flow from cold start.
  - App handles denied and revoked permissions gracefully.
  - Tracking remains disabled until required permissions are granted.

### Issue 3: Local Data Layer and Migrations
- Type: Feature
- Goal: Create local database schema for capture, enrichment, stay, place, visit, backup manifest.
- Scope:
  - Define entities and DAOs.
  - Add migration strategy and tests.
  - Add local encryption strategy for sensitive data.
- Acceptance Criteria:
  - Schema matches architecture doc entities.
  - Migration test passes from v1 to v2 schema mock.
  - Sensitive fields are encrypted at rest.

## Milestone 2: Capture and Movement Classification

### Issue 4: 30-Minute Background Capture Scheduler
- Type: Feature
- Goal: Capture location every 30 minutes with Android-compliant background behavior.
- Scope:
  - Schedule recurring work.
  - Persist timestamp, lat/lng, accuracy, speed, source.
  - Resume scheduling after reboot.
- Acceptance Criteria:
  - Samples recorded on schedule during test window.
  - Scheduler resumes after reboot.
  - Pause/resume toggle works from settings.

### Issue 5: Transit vs Stay Detection Engine
- Type: Feature
- Goal: Ensure captures while driving are classified as transit and not false stays.
- Scope:
  - Speed and movement-based transit classification.
  - Stay clustering by radius and dwell threshold.
  - Exclude transit duration from place totals.
- Acceptance Criteria:
  - Driving captures do not create or extend stays.
  - Stay requires configured dwell and low-movement conditions.
  - Reporting excludes transit minutes from place totals.

## Milestone 3: Enrichment and Place Identity

### Issue 6: Reverse Geocoding Integration
- Type: Feature
- Goal: Automatically resolve address for eligible captured points.
- Scope:
  - Integrate geocoding provider.
  - Retry policy for temporary failures.
  - Persist enrichment status and metadata.
- Acceptance Criteria:
  - Majority of network-available points get address values.
  - Retry behavior handles transient outages.
  - Failed enrichment states are visible in logs/debug UI.

### Issue 7: POI and Hotel Name Enrichment
- Type: Feature
- Goal: Automatically enrich POI names and hotel names where provider data exists.
- Scope:
  - POI lookup integration.
  - Hotel identification mapping.
  - Address fallback when POI unavailable.
- Acceptance Criteria:
  - Nearby POI names are stored when returned by provider.
  - Hotel names appear for hotel POI types.
  - App falls back to address-only display when POI is absent.

### Issue 8: Recurring Place Suggestions and Labels
- Type: Feature
- Goal: Suggest recurring places and support manual label confirmation.
- Scope:
  - Suggest places from repeated stays.
  - Built-in labels: Home, Work, Friend, Family.
  - Custom labels and merge duplicates.
- Acceptance Criteria:
  - User can confirm/edit/dismiss suggestions.
  - Label assignment updates reporting output.
  - Duplicate places can be merged without data loss.

## Milestone 4: UX and Reporting

### Issue 9: Map and Timeline Experience
- Type: Feature
- Goal: Present points, stays, and transit clearly in map and timeline views.
- Scope:
  - Map overlays for points/stays/transit.
  - Chronological timeline entries.
  - Visit detail screen.
- Acceptance Criteria:
  - Timeline reflects captured history in order.
  - Transit and stay visuals are clearly distinct.
  - Visit detail shows coordinates, address, POI/hotel, label, duration.

### Issue 10: Time-Spent Summaries
- Type: Feature
- Goal: Produce day/week/month time allocation summaries by place.
- Scope:
  - Aggregation jobs.
  - Summary UI cards/tables.
  - Filter by period.
- Acceptance Criteria:
  - Day/week/month totals are consistent with visit records.
  - Transit duration is excluded from place totals.
  - Summary refreshes after label edits.

## Milestone 5: Azure Backup and Restore

### Issue 11: Token Broker API for SAS
- Type: Feature
- Goal: Build minimal backend endpoint that issues short-lived SAS tokens.
- Scope:
  - Authenticate app user context.
  - Generate least-privilege SAS (read/write scope as needed).
  - Return token expiry metadata.
- Acceptance Criteria:
  - App obtains SAS token without storing storage account keys.
  - Token expiry is enforced.
  - Permissions are limited to required blob paths.

### Issue 12: Encrypted Backup Upload to Azure Blob
- Type: Feature
- Goal: Export encrypted snapshots and upload to private blob container.
- Scope:
  - Snapshot packaging and manifest.
  - Client-side encryption before upload.
  - Offline queue and retry/backoff.
- Acceptance Criteria:
  - Backups upload successfully with valid SAS.
  - Uploads resume/retry after connectivity loss.
  - Backup manifest includes checksum and schema version.

### Issue 13: Restore from Latest Valid Snapshot
- Type: Feature
- Goal: Download, validate, decrypt, and import latest valid backup snapshot.
- Scope:
  - Retrieve latest manifest.
  - Checksum validation.
  - Safe import with rollback on failure.
- Acceptance Criteria:
  - Restore reproduces capture history and labels.
  - Invalid checksum blocks import.
  - Restore failure leaves local DB consistent.

## Milestone 6: Quality, Hardening, and Release

### Issue 14: Test Coverage for Critical Flows
- Type: Test
- Goal: Add automated tests for capture, classification, enrichment fallback, and backup/restore.
- Scope:
  - Unit tests for transit/stay thresholds.
  - Integration tests for capture and backup flows.
  - Regression tests for no-sharing guarantee.
- Acceptance Criteria:
  - Critical-path tests run in CI.
  - Transit-vs-stay edge cases are covered.
  - Backup/restore integration test passes.

### Issue 15: Battery and Reliability Validation
- Type: Task
- Goal: Validate battery impact and schedule reliability over realistic movement patterns.
- Scope:
  - 24-hour test scenarios.
  - Reliability telemetry for capture success.
  - Parameter tuning recommendations.
- Acceptance Criteria:
  - Capture success meets target rate.
  - Battery impact remains within agreed threshold.
  - Final threshold values documented.

### Issue 16: Release Readiness Checklist
- Type: Task
- Goal: Confirm PRD compliance and ship v1 baseline.
- Scope:
  - Verify mandatory automatic address and POI/hotel enrichment.
  - Verify no-sharing scope enforcement.
  - Verify security controls for SAS and encryption.
- Acceptance Criteria:
  - All PRD must-have items are marked complete.
  - Security checks pass.
  - Go/no-go review approved.

## Suggested Labels for GitHub Issues
- enhancement
- backend
- android
- data
- security
- testing
- milestone:m1
- milestone:m2
- milestone:m3
- milestone:m4
- milestone:m5
- milestone:m6

## Suggested First 5 Issues to Start Immediately
1. Initialize Android App Baseline
2. Permission and Consent Flow
3. Local Data Layer and Migrations
4. 30-Minute Background Capture Scheduler
5. Transit vs Stay Detection Engine
