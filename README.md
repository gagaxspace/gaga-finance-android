# GaGa Finance Android

Standalone native Android project for GaGa Finance, using Kotlin and Jetpack Compose.

## Development status

**Not a production financial service.** This project now has a local manual-bookkeeping foundation on a review branch. It does not connect to banks, move money, pay bills, manage crypto, create custodial balances or provide investment advice.

### Available in the offline foundation
- Home dashboard with monthly income, expenses and net cashflow derived from local entries.
- Add and view categorized manual BDT income/expense entries, search and filter history, edit entries and delete with confirmation.
- Private app-sandbox SQLite storage across restarts (not encrypted at rest). Non-destructive migrations from v1/v2/v3/v4 to v5 preserve earlier finance records and People Ledger history.
- Export separate transaction and People Ledger CSV files through Android's document picker, without broad storage permission.
- Searchable **People Ledger** with named contacts, amounts lent and borrowed, partial/full repayments, per-person statuses and detailed history. No actual transfers occur.
- Local **Budgets & Goals** screens reached through Home or More: recurring monthly category limits, spending progress and overspending notices calculated from actual manual expense entries, goal targets and manually recorded additions/withdrawals.
- Separate CSV exports for transactions, people and budget/goal records (including savings history).
- Offline **Spending Insights** tab shows category spending, month-to-date cashflow, previous full month's expenses and budget alerts without third-party AI or network permissions.
- **Person details and all-time summary:** optional phone, email, address, relationship and notes, plus gross/returned/outstanding figures.
- **Loans & Borrow:** separate dated manual loan plans, annual simple-interest estimate, repayment entry, due/overdue/settled statuses.
- **Bill Reminders:** create due dates, inspect pending/overdue, mark paid/unpaid, and delete. No scheduled system notifications yet.
- **Overall budget:** one limit spanning all expense categories, alongside category budgets.
- **Complete JSON backup/restore:** all local records export to one versioned JSON file; import previews counts and uses an atomic transaction with validation; reset requires typing RESET.
- **Instructions** and explicit Markets/Mobile Banking provider-status pages.
- AI/cloud services remain disabled until a privacy-reviewed integration exists.
- Unit tests, Android lint and automated debug APK builds.

**Important:** All three manual CSV exports may include personal or financial details; store them securely. The JSON export is a supported manual local backup for this native version; CSV files remain exports without import. These files are not encrypted and are not automatically synchronized. People Ledger amounts and statuses are locally recorded by the user; they are not bank-verified debts. Values are user-entered bookkeeping records, *not verified bank or wallet balances*. There is no login or automatic cloud backup. If you uninstall/clear app data, records may be permanently lost. Do not enter banking passwords, identity documents or financial secrets.

## Requirements

- Android Studio recent stable version
- JDK 17
- Android SDK 35

## Build and test

Open the repository in Android Studio, sync Gradle and run the app configuration.

With Gradle installed, from the repository root:

    gradle :app:testDebugUnitTest :app:lintDebug :app:assembleDebug

For downloadable debug artifacts, use the Actions workflow run for a successful commit or pull request. Debug APKs use a separate .debug application ID and are not a signed production release.

**Safe preview install:** Version 0.4.0 debug uses package `app.gagafinance.android.preview04`, labeled **GaGa Finance Preview**. It installs separately from earlier `.debug` builds and starts with its own empty local database. It does not migrate or synchronize the old app's records. **Upgrade warning:** GitHub-hosted debug APKs may be signed with different temporary debug keys. Updating a previously installed debug APK can fail with a signature mismatch. Never uninstall an existing installation containing important data before exporting a complete JSON backup; uninstalling can delete all local records. A stable signing strategy is required before reliable upgrades can be tested.

## Security and configuration

- Do **not** commit Firebase Admin credentials, payment secrets, private keys, customer data or a release keystore.
- No bank/payment provider is integrated. Do not use the Android app as an authoritative ledger.
- No Android Internet permission is currently requested.
- Only implement actual money movement with a trusted backend and appropriate licensed provider/partner, fraud protection, reconciliation, compliance and auditable server-side ledger.
- No Firebase billing upgrade or paid service should be enabled without approval.

See [implementation and remaining blockers](docs/IMPLEMENTATION_STATUS.md), plus the [screenshot feature-parity audit](docs/SCREENSHOT_PARITY.md). The Firebase-hosted web app is a separate data source; screenshots and web exports do not automatically populate this Android app.
