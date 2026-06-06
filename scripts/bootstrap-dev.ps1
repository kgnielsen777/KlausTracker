$ErrorActionPreference = 'Stop'

Write-Host 'Bootstrapping local dev tooling for KlausTracker...'

winget install --id EclipseAdoptium.Temurin.17.JDK -e --source winget
winget install --id Gradle.Gradle -e --source winget

Write-Host 'Tooling installed. Open a fresh terminal, then run:'
Write-Host '  gradle wrapper --gradle-version 8.10.2'
Write-Host 'Then open in Android Studio and Sync Project with Gradle Files.'
