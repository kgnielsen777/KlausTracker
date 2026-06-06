# Technical Implementation Checklist

## Scope Guardrails
- [ ] Confirm personal-only mode is enforced.
- [ ] Confirm no social or sharing features in scope.
- [ ] Confirm mandatory automatic address lookup and POI/hotel enrichment.

## Android App Foundation
- [ ] Create Android app project structure (Kotlin, modern Android stack).
- [ ] Define package name, app id, min/target SDK.
- [ ] Configure build variants: dev and prod.
- [ ] Set up logging and crash monitoring strategy (privacy-aware, minimal diagnostics).

## Permissions and Background Tracking
- [ ] Implement foreground location permission flow.
- [ ] Implement background location permission flow with rationale screen.
- [ ] Add user setting to pause/resume tracking.
- [ ] Implement recurring capture scheduler for 30-minute cadence.
- [ ] Handle reboot/device restart so scheduler resumes.
- [ ] Handle battery optimization constraints and user guidance.

## Capture Pipeline
- [ ] Persist each sample with timestamp, lat/lng, accuracy, speed, and source metadata.
- [ ] Validate incoming location accuracy before enrichment.
- [ ] Mark low-confidence samples for later review/filtering.
- [ ] Add local queue state for pending enrichment.

## Transit vs Stay Detection
- [ ] Implement transit detection based on speed and movement.
- [ ] Store driving captures as transit points.
- [ ] Do not open or extend a place stay during transit.
- [ ] Implement stay clustering with radius and dwell thresholds.
- [ ] Exclude transit time from place-duration totals.
- [ ] Surface transit segments distinctly in timeline/map.

## Enrichment (Mandatory)
- [ ] Integrate reverse geocoding provider for automatic address lookup.
- [ ] Integrate POI lookup provider for automatic place names.
- [ ] Ensure hotel names are enriched when returned by provider.
- [ ] Implement fallback to address-only when no POI/hotel exists.
- [ ] Add retry policy for timeouts/network failures.
- [ ] Record enrichment confidence and provider metadata.

## Place Management
- [ ] Suggest recurring places from repeated stays.
- [ ] Implement confirm/edit/dismiss flow for suggestions.
- [ ] Provide built-in labels: Home, Work, Friend, Family.
- [ ] Support custom labels.
- [ ] Implement duplicate place merge flow.

## UI and Reporting
- [ ] Build map view with points, stays, and transit segments.
- [ ] Build timeline/list view with chronological visit entries.
- [ ] Build place list with cumulative duration by place.
- [ ] Build day/week/month summary reports.
- [ ] Build visit detail screen with coordinates, address, POI/hotel, label, duration.

## Local Data Layer
- [ ] Define schema for captures, enrichment, stays, places, and backup manifest.
- [ ] Add migration strategy for schema upgrades.
- [ ] Encrypt local sensitive data at rest.
- [ ] Add retention/cleanup job policy.

## Azure Backup and Restore
- [ ] Create Azure Storage Account and private Blob container.
- [ ] Define backup snapshot format and manifest metadata.
- [ ] Implement app-side encryption before blob upload.
- [ ] Implement secure SAS-token flow via token-issuing backend.
- [ ] Ensure no storage account keys are stored in app.
- [ ] Implement offline queue and retry for failed backup jobs.
- [ ] Implement restore flow from latest valid snapshot.
- [ ] Validate checksums before restore import.
- [ ] Define retention policy (example: 30 daily snapshots).

## Security and Compliance
- [ ] Enforce least-privilege SAS permissions and expiry.
- [ ] Secure transport with HTTPS/TLS.
- [ ] Add integrity checks and tamper detection for backup files.
- [ ] Document data handling and consent UX text.

## Testing Plan
- [ ] Unit tests for detection logic (transit vs stay thresholds).
- [ ] Unit tests for enrichment fallback and retry behavior.
- [ ] Integration tests for background capture reliability.
- [ ] Integration tests for Azure backup upload/download.
- [ ] Restore tests across app reinstall and new device scenario.
- [ ] Battery impact tests over 24-hour realistic movement.
- [ ] Manual acceptance run against PRD acceptance criteria.

## Release Readiness
- [ ] Confirm no-sharing requirement still enforced in UI and API.
- [ ] Freeze default thresholds and document rationale.
- [ ] Verify all critical edge cases are covered.
- [ ] Prepare post-v1 backlog for adaptive sampling and advanced analytics.
