# GaGa Finance Android

Standalone native Android project for GaGa Finance, using Kotlin and Jetpack Compose.

## Development status

**Not a production financial service.** This project now has a local manual-bookkeeping foundation on a review branch. It does not connect to banks, move money, pay bills, manage crypto, create custodial balances or provide investment advice.

### Available in the offline foundation
- Home dashboard with monthly income, expenses and net cashflow derived from local entries.
- Add and view categorized manual BDT income/expense entries, search and filter history, edit entries and delete with confirmation.
- Private app-sandbox SQLite storage across restarts (not encrypted at rest). Non-destructive migrations from v1/v2 to v3 preserve earlier finance records.
- Export transaction CSV via Android's document picker, without broad storage permission.
- Searchable **People Ledger** with named contacts, amounts lent and borrowed, partial/full repayments, per-person statuses and detailed history. No actual transfers occur.
- AI and More sections that distinguish unavailable features from working ones.
- Unit tests, Android lint and automated debug APK builds.

**Important:** People Ledger amounts and statuses are locally recorded by the user; they are not bank-verified debts. Values are user-entered bookkeeping records, *not verified bank or wallet balances*. There is no login or cloud backup. If you uninstall/clear app data, records may be permanently lost. Do not enter banking passwords, identity documents or financial secrets.

## Requirements

- Android Studio recent stable version
- JDK 17
- Android SDK 35

## Build and test

Open the repository in Android Studio, sync Gradle and run the app configuration.

With Gradle installed, from the repository root:

    gradle :app:testDebugUnitTest :app:lintDebug :app:assembleDebug

For downloadable debug artifacts, use the Actions workflow run for a successful commit or pull request. Debug APKs use a separate .debug application ID and are not a signed production release.

## Security and configuration

- Do **not** commit Firebase Admin credentials, payment secrets, private keys, customer data or a release keystore.
- No bank/payment provider is integrated. Do not use the Android app as an authoritative ledger.
- No Android Internet permission is currently requested.
- Only implement actual money movement with a trusted backend and appropriate licensed provider/partner, fraud protection, reconciliation, compliance and auditable server-side ledger.
- No Firebase billing upgrade or paid service should be enabled without approval.

See [implementation and remaining blockers](docs/IMPLEMENTATION_STATUS.md).
