# GaGa Finance — Google Play release checklist (10 November 2026)

## What is ready
- Android 16 API 36 target configured for Google Play's current minimum submission requirement.
- Offline bookkeeping, People Ledger, budgets, goals, separate loan plans, bills, in-app insights and opt-in WorkManager due-item reminders.
- User-controlled encrypted JSON export/import (native schema only) and local reset.
- Public draft policy: docs/privacy.html. **GitHub Pages must be enabled and URL verified before submitting this as a Play privacy URL.**
- Separate debug APK preview package; production package is `app.gagafinance.android`.

## Mandatory steps not automatically done
- [ ] Choose a permanent application ID before first Play upload; it cannot later change for the same listing. Confirm `app.gagafinance.android` matches the intended finance listing (not GaGa Chat).
- [ ] Generate a brand-new, durable **finance-only upload keystore** on a trusted offline device, back it up privately. Do not reuse or disclose GaGa Chat keys.
- [ ] Configure private GitHub Actions repository secrets `GAGA_FINANCE_KEYSTORE_BASE64`, `GAGA_FINANCE_KEYSTORE_PASSWORD`, `GAGA_FINANCE_KEY_ALIAS` and `GAGA_FINANCE_KEY_PASSWORD`.
- [ ] After merging and verifying source, use Actions > "Build privately signed Play Store bundle" > Run workflow. Signed AAB remains a GitHub Actions artifact, not a public GitHub release.
- [ ] Verify AAB signature/certificate, version code, API 36 and Play App Signing settings.
- [ ] Enable GitHub Pages from `/docs`, or host the policy on another public HTTPS site, confirm live non-editable browser URL, and provide a real private developer privacy contact.
- [ ] Prepare 512x512 logo (app icon), 1024x500 feature graphic, real in-app screenshots with no fictional personal records, and truthful store listing text.
- [ ] Complete App content: Privacy policy, Data safety, Financial features declaration, content rating, target audience, ads declaration, app access and any relevant country requirements.
- [ ] No fabricated "banking", "payment", "wallet" or "interest earning" claims; finance bookkeeping is not licensed real-money services. Disclosure must match actual permissions and any bundled SDKs.
- [ ] Complete local Android functional QA and migration: API 23–36, font scaling, orientation, network off, 100+ people, 10k entries, backup recovery, cancellation and malformed backup rollback.
- [ ] Review whether this Play developer account requires 12 closed testers for 14 continuous days. If so, satisfy the test before applying for production access.
- [ ] Publish a signed AAB first to **Internal testing**, test with fresh installs, then closed testing or production access based on Play Console.
- [ ] Do not ship sensitive financial data under a false claim of encryption at rest: app-private SQLite is not independently encrypted, although password-protected exports are available. A separate database encryption review is pending.

## Build commands
```bash
gradle :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
gradle :app:bundleRelease
```
A `bundleRelease` run without the signing environment creates an unsigned bundle **not suitable for upload**. Use the guarded signed GitHub workflow with stable private secrets.

## Cost-first approach
The offline app requires no hosting subscription for ledger data and uses no paid SMS or AI API. A Play Console developer account, if not already owned, has its own Google fees; avoid new banking and market subscriptions until confirmed necessary and lawful.

## Potential further product work
- Encrypted-at-rest local database plus migration safety
- Secure optional cloud sync and account recovery
- Bangla translations and light-theme accessibility audit
- Recurring transactions/bills and exact-time scheduling where policy permits
- Full real-device testing and regression suite
