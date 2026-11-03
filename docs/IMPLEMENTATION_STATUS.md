# Implementation status

## Committed foundation

- Native Kotlin Android application module.
- Jetpack Compose app shell and dark navy / turquoise visual baseline.
- Five-item bottom navigation: Home, Txn, People, AI and More.
- Initial dashboard layout with clearly labeled sample values.
- Gradle configuration targeting Android SDK 35 and minimum SDK 23.

## Not implemented yet

- Authentication, account recovery and persistent sessions.
- Live transaction storage, budgets, savings goals, portfolio or market data.
- Cloud synchronization, Firebase integration or offline conflict handling.
- AI service integration.
- Real deposits, withdrawals, transfers, bill payment or settlement.
- Server-authoritative double-entry ledger, idempotency, fraud controls and audit logs.
- Automated tests, CI build verification, signing and release APK.

## Financial safety

The displayed zero values are placeholders. No balance or transaction in this starter app represents actual funds. Do not connect real payment rails until a trusted backend validates identity, authorizes operations, records immutable ledger entries, handles reversals and reconciles provider settlement. Payment secrets and Firebase Admin credentials must remain server-side.
