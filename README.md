# GaGa Finance Android

Standalone native Android project for GaGa Finance, using Kotlin and Jetpack Compose.

## Development status

**Not a production financial service.** This project now has a local manual-bookkeeping foundation on a review branch. It does not connect to banks, move money, pay bills, manage crypto, create custodial balances or provide investment advice.

### Available in the offline foundation
- Home dashboard with monthly income, expenses and net cashflow derived from local entries.
- Add and view categorized manual BDT income/expense entries, search and filter history, edit entries and delete with confirmation.
- Private app-sandbox SQLite storage across restarts (not encrypted at rest). Non-destructive migrations from v1/v2/v3 to v4 preserve earlier finance records and People Ledger history.
- Export separate transaction and People Ledger CSV files through Android's document picker, without broad storage permission.
- Searchable **People Ledger** with named contacts, amounts lent and borrowed, partial/full repayments, per-person statuses and detailed history. No actual transfers occur.
- Local **Budgets & Goals** screens reached through Home or More: recurring monthly category limits, spending progress and overspending notices calculated from actual manual expense entries, goal targets and manually recorded additions/withdrawals.
- Separate CSV exports for transactions, people and budget/goal records (including savings history).
- AI remains disabled until a privacy-reviewed service exists.
- Unit tests, Android lint and automated debug APK builds.

**Important:** All three manual CSV exports may include personal or financial details; store them securely. They are exports, not an automated restore/backup system. People Ledger amounts and statuses are locally recorded by the user; they are not bank-verified debts. Values are user-entered bookkeeping records, *not verified bank or wallet balances*. There is no login or cloud backup. If you uninstall/clear app data, records may be permanently lost. Do not enter banking passwords, identity documents or financial secrets.

## Requirements

- Android Studio recent stable version
- JDK 17
- Android SDK 35

## Build and test

Open the repository in Android Studio, sync Gradle and run the app configuration.

With Gradle installed, from the repository root:

    gradle :app:testDebugUnitTest :app:lintDebug :app:assembleDebug

For downloadable debug artifacts, use the Actions workflow run for a successful commit or pull request. Debug APKs use a separate .debug application ID and are not a signed production release.

**Upgrade warning:** GitHub-hosted debug APKs may be signed with different temporary debug keys. Updating a previously installed debug APK can fail with a signature mismatch. Never uninstall an existing installation containing important data before exporting both CSV files; uninstalling can delete all local records. A stable signing strategy is required before reliable upgrades can be tested.

## Security and configuration

- Do **not** commit Firebase Admin credentials, payment secrets, private keys, customer data or a release keystore.
- No bank/payment provider is integrated. Do not use the Android app as an authoritative ledger.
- No Android Internet permission is currently requested.
- Only implement actual money movement with a trusted backend and appropriate licensed provider/partner, fraud protection, reconciliation, compliance and auditable server-side ledger.
- No Firebase billing upgrade or paid service should be enabled without approval.

See [implementation and remaining blockers](docs/IMPLEMENTATION_STATUS.md).
