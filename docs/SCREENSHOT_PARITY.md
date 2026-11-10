# GaGa Finance Android: screen-parity review

Reference: nine screenshots shared on 2026-11-10 from the `gaga-fintech.web.app` web client.
Last reviewed: 2026-11-10.

**Important separation:** The web client data shown (99 people, borrowing and lending totals) is NOT present in the Android SQLite app by default. Never seed personal financial records from screenshots or hardcode them as live user data. Import requires the original source file/schema and explicit user confirmation.

| Web feature | Native Android 0.4.0 review-branch implementation | Gaps |
| --- | --- | --- |
| Dashboard monthly income/expenses/net | Real totals from local entries, recent activity, budget goal links | No verified bank balance/portfolio or connected user account |
| Transactions | Create/edit/delete, category, search/filter, export CSV | Scheduled/recurring entry support |
| People Ledger | Search people, lent/borrowed/returned, outstanding, status, history | No account sync or import from web dataset |
| Person Detail | Edit local phone, email, address, relationship, notes | Contact details are NOT encrypted at rest |
| Summary All | Gross lent/borrowed, returned/paid, total/active people, outstanding and net | No cross-device sync |
| Budget Tracker | Overall and category monthly limits and over-budget indicators | Not a bank spending-limit enforcement system |
| Savings Goals | Target and manually recorded goal progress | No money reserved/held in an account |
| Loans & Borrow | Dated standalone loan plans, direction, principal, annual simple interest estimate, repayment history, overdue flag | Loan plans are separate from People Ledger totals; no contractual interest engine or real payments |
| Bill Reminders | Create, view due/overdue/paid, mark paid/unpaid, delete | No background Android push alarms yet |
| Settings / Export All Data | Versioned full JSON backup + previewed, confirmed, atomic restore | Plaintext manual export; native schema only, not the web client's JSON format |
| Settings / Export CSV | Separate transactions, people and planning CSV exports | CSV import not implemented |
| Settings / Reset | Confirmation requiring typed RESET before deleting local data | Web Excel seed reset not possible without original Excel source |
| Markets / Portfolio | No live market data | Requires a sourced feed, legal/licensing check and separate user consent |
| Mobile Banking | No real banking provider connection | Requires official licensed partner/API, security and compliance |
| AI Advisor | Offline Spending Insights, no API calls | Cloud AI advice intentionally disabled |
| Settings / Theme / Notifications | Dark-mode UI and in-app due/overdue counts | Light theme, push scheduling, auth/profile not implemented |
| Instructions | README and help copy | In-app step-by-step guide to be expanded |

## Financial calculation boundaries

- BDT money is stored as integer minor units (paisa), not floating-point values.
- People Ledger contains manual debt events; loan contracts are separately tracked and not added into People Ledger totals. A later reconciliation workflow is required if users wish to link the two; don't silently double count.
- Simple loan interest is an estimate for the period from creation to due date. Loan repayments reduce principal only and do not collect or settle interest.
- Due/overdue is computed on device. It is **not** a scheduled notification or provider verification.
- All portfolio/market/banking provider figures should remain disabled until actual authorized external data is wired end-to-end.

## Verification checklist

- [ ] Fresh install creates all schema v5 columns and tables.
- [ ] Migration from SQLite v1–v4 retains all earlier entries, people, budgets and goals.
- [ ] Person profile fields persist after app restart.
- [ ] Summary gross vs net calculations match a controlled ledger test dataset.
- [ ] Loans reject repayments over principal, show overdue, and use strict valid dates.
- [ ] Bill status reflects due dates, mark-paid, undo and deletion.
- [ ] Overall monthly budget uses all expense categories but excludes income and other months.
- [ ] Full backup export, preview, import, and rollback on malformed/overpaying backup.
- [ ] Reset needs typed RESET and does not execute on Cancel.
- [ ] Data persists across process death and device rotation; small screens and TalkBack tested.
- [ ] Android 0.4 preview uses a separate `app.gagafinance.android.preview04` package. Verify fresh standalone install; previous debug data remains under the old package and is not automatically imported.
- [ ] Build production APK with stable signing certificate for genuine upgrades; avoid reinstall/uninstall data loss.
- [ ] Public release security/privacy review, encryption at rest and account/recovery design.

## Release blockers

No production money movement is authorized. The database is private to the app sandbox but **not encrypted at rest**; plaintext JSON/CSV exports may contain names and financial information. No Play Store publishable signed AAB is produced here.
