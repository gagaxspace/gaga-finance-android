# GaGa Finance Android implementation status

Last reviewed: 2026-11-03

## Scope of the offline foundation (proposed PR)

- Native Kotlin + Jetpack Compose application with five destinations.
- Working categorized local manual income/expense entry; search and filter, in-place editing and deletion with confirmation.
- Exact amounts stored as integer BDT minor units, not floating-point currency.
- SQLite app-private persistence for manual records (survives app restarts). A v1→v2 non-destructive migration adds the category field, preserving existing entries.
- Current-month cashflow calculations from entered data.
- CSV export through Android's system document picker, with spreadsheet-formula escaping.
- Unit tests for parsing, cashflow and CSV export; GitHub Actions test/lint/debug APK pipeline.
- No INTERNET permission, no financial provider SDK and no committed service credentials.

This is a **personal bookkeeping prototype**, not a bank, wallet, remittance service, or investment platform. No displayed value is a bank-verified balance. The SQLite database is private to the Android app sandbox but **is not encrypted at rest**. Do not enter account credentials or other sensitive banking information. Backups are disabled and uninstalling/clearing app data can delete records permanently.

## Production blockers (not implemented)

### P0 — decide the product and safeguard money
1. Decide whether GaGa Finance is **manual personal finance** only, or a regulated custodial/payment product. Do not connect real-money functions until a compliance and partnership model is signed off.
2. Establish threat model, data classification, encryption-at-rest strategy, secure key management and device/session security.
3. Design backend boundaries, authentication, privacy policy, account deletion and recovery; no secret should be built into the APK.
4. For any actual money movement: regulated provider partnership and legal assessment, verified identity as applicable, server-authoritative double-entry ledger, idempotent commands, payment reconciliation, fraud handling, refunds/reversals, audit logs, and dispute procedures.

### P1 — core bookkeeping product
5. Budget management, savings goals, merchants/people details, recurring entries and more granular search/filter options.
6. Undo/recovery for deletion, historical date editing, and complete data portability.
7. Database schema migrations, test fixtures, full accessibility and small-screen QA.
8. Optional authenticated encrypted sync and backup only after privacy/security review.

### P2 — optional extensions
9. Permissioned analytics and insights; AI disabled until a suitable privacy contract and data-use controls exist.
10. Optional investment market data only from licensed, trustworthy sources; distinguish quotes from holdings.
11. Releases: signing material kept outside GitHub, proper Play App Signing, release minification, provenance, device testing, internal track and compliance declarations.

## Verification checklist
- [x] GitHub Actions debug assembly succeeded on main before this PR.
- [ ] New PR build, tests and lint pass.
- [ ] Install debug APK on multiple Android devices / Android versions.
- [ ] Enter income, expense and category, invalid amount, restart app, edit and delete; verify exact values.
- [ ] Install v1 APK, create entries, upgrade to v2 and verify records were preserved.
- [ ] Export CSV and inspect contents, including formulas and escaped quotation marks.
- [ ] Test uninstall/clear-data warnings, rotation, TalkBack and long descriptions.
- [ ] Define Android production application ID, package ownership and release key custody.
- [ ] Sign release AAB and review Google Play policies for the final declared product.
- [ ] Confirm security/compliance before enabling any real money feature.

## Cost posture
Keep the offline MVP on free Android/GitHub tooling. Do not add Firebase paid services, payment processors, premium AI APIs, or SMS until the specific use case, regulatory path and recurring costs are approved.
