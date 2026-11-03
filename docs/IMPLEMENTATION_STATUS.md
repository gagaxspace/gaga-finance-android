# GaGa Finance Android implementation status

Last reviewed: 2026-11-03

## Scope of the offline foundation (proposed PR)

- Native Kotlin + Jetpack Compose application with five destinations.
- Working categorized local manual income/expense entry; search and filter, in-place editing and deletion with confirmation.
- Exact amounts stored as integer BDT minor units, not floating-point currency.
- SQLite app-private persistence for manual records (survives app restarts). Non-destructive v1→v2→v3→v4 migrations preserve transactions and People Ledger data, adding separate planning tables.
- Searchable People Ledger, lending/borrowing events, partial/full repayments, outstanding per-person balances, status (no activity/active/settled) and detailed history.
- Repayment validation in the database transaction prevents overpayment and stores append-only event history. These records are not bank-confirmed and never initiate payments.
- Monthly category budgets with real recorded-expense progress, over-budget alerts and recurring limits. Manually tracked savings goals with append-only addition/reduction events; savings values are not real-money balances.
- CSV export of both plans and their savings history, in addition to separate transaction and People Ledger CSV files.
- On-device Spending Insights: current-month income, expenses, net cashflow, category rankings, previous full calendar-month expense totals and budget warnings; no AI calls or financial-data uploads.
- Current-month cashflow calculations from entered data.
- Separate transaction and People Ledger CSV exports through Android's system document picker, with spreadsheet-formula escaping. CSV exports may contain personal information and must be stored securely.
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
5. Budgets and goals now have local MVP screens; remaining enhancements include goal target editing, custom categories, people nickname editing, recurring entries, due dates/reminders and richer filters.
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
- [ ] Check People Ledger: create person, search, lend, borrow, partial repayment, complete repayment, inspect history/status.
- [ ] Confirm entering a repayment above the outstanding balance is rejected.
- [ ] Upgrade from v1→v3 and v2→v3 and verify existing transactions are preserved and People Ledger is usable.
- [ ] Verify Insights uses correct month boundaries, excludes income from spending categories and shows descriptive month-to-month results.
- [ ] Verify monthly budget filters correctly reset by month and exclude income entries.
- [ ] Create savings goals, add/reduce progress, restart app, inspect event history and over-withdrawal protection.
- [ ] Verify v1/v2/v3→v4 migration on test devices without clearing local data.
- [ ] Export all three CSV files and inspect contents, including formulas and escaped quotation marks.
- [ ] Verify the development signing certificate is consistent across upgrade tests; GitHub-hosted debug keys may differ, so never uninstall an app containing records without exporting them first.
- [ ] Test uninstall/clear-data warnings, rotation, TalkBack and long descriptions.
- [ ] Define Android production application ID, package ownership and release key custody.
- [ ] Sign release AAB and review Google Play policies for the final declared product.
- [ ] Confirm security/compliance before enabling any real money feature.

## Cost posture
Keep the offline MVP on free Android/GitHub tooling. Do not add Firebase paid services, payment processors, premium AI APIs, or SMS until the specific use case, regulatory path and recurring costs are approved.
