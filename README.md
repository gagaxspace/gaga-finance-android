# GaGa Finance Android

Native Android project for GaGa Finance, built with Kotlin and Jetpack Compose.

## Project status

This repository is being initialized as a clean native Android codebase. The first milestone establishes the app shell, visual design system, navigation, and build configuration. It is **not yet a production financial application**.

Until server-side services and licensed payment integrations are implemented and tested, all financial data must be treated as sample/demo data. Do not use the client app as an authoritative ledger or store secrets in the APK.

## Requirements

- Android Studio (recent stable release)
- JDK 17
- Android SDK 35

## Build

Open this repository in Android Studio and allow Gradle sync, then run the `app` configuration on an emulator or device.

## Configuration and security

- No Firebase project credentials or payment-provider secrets are committed.
- Add environment-specific configuration only through secure local configuration or a trusted backend.
- Never embed service-account JSON, private keys, or payment secrets in the Android app.
- Firebase Spark-compatible services should be preferred unless a billing upgrade is explicitly approved.

## Planned implementation milestones

1. Native app shell, theme, dashboard and navigation.
2. Transactions, budgets, savings goals and people screens.
3. Local persistence and repository abstractions.
4. Authentication and backend integration.
5. Server-authoritative ledger, audit trail and transaction safeguards.
6. Payment-provider integration, only after provider credentials, compliance and settlement flows are verified.
7. Automated Android build, tests and signed release process.

See `docs/IMPLEMENTATION_STATUS.md` for the current implementation boundary.
