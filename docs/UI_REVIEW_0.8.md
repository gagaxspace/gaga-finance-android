# GaGa Finance Android 0.8 — compact UI design audit

Reference: two user screenshots of version 0.7.0, provided 10 November 2026 (Today and More screens on Android).

## Observed problems and code changes

| Screen issue | Change in 0.8.0 |
| --- | --- |
| Status bar clock/camera overlay obscures branding | Call AndroidX `enableEdgeToEdge`, add `Modifier.statusBarsPadding()` inside shared top-bar surface; no hard-coded status bar pixels |
| Header occupies too much usable height | 34dp GaGa logo, min-height 54dp row plus actual system status inset; single-line 15sp name, compact subtitle |
| Tall navigation bar and oversized lavender selected pill | Replace default 80dp Material NavigationBar content with accessible compact bottom tabs: 58dp minimum target, 21dp icons, 11sp labels, 44×27dp teal selection indicator; account for `navigationBarsPadding()` |
| Oversized Today cashflow & attention cards | Reduce vertical spacing from 15 to 10dp, padding on main card from 18 to 14dp, 26sp hero amount; compact attention action row |
| Duplicate GaGa logo and very tall card on More | Remove repeated 60dp header-logo card, use simple 24sp More title and version badge; original branding still in shared app bar |
| Large gap and weak action affordance between settings | Make rows fully tappable, 64dp minimum height, chevron affordance, and group frequently used tools before unavailable services |
| Small, low contrast secondary labels | Increase muted-text contrast from #B7C5D7 to #C9D6E4 on audited screens, mostly use 12sp secondary labels; keep large money amounts legible |
| Different gaps and corner radius across screens | Normalize 16dp page horizontal gutters, approximately 12dp vertical padding, 13–14dp card corners across Today, People, Insights, Goals, Loans and Summary |

## Android 16 technical grounding
- Target API 36 is edge-to-edge. Status and camera cutout areas must not be covered by important app controls. Use actual WindowInsets, never static pixel offsets.
- Header background extends behind status bar while the row uses `statusBarsPadding`.
- Bottom navigation background extends behind gesture area while interactive tabs stay above `navigationBarsPadding`.
- On older Android versions, `enableEdgeToEdge` gives a consistent arrangement.

## Acceptance tests pending on device
- [ ] Status clock, notch/camera cutout, app logo and title never overlap on Android 16.
- [ ] Bottom nav and gesture/navigation controls do not overlap; all five tabs are tappable.
- [ ] 320dp narrow phone: no offscreen tabs or clipped financial amounts.
- [ ] Accessibility font scale 1.0×, 1.3×, 2.0×: no hidden call-to-action, Voice Access / TalkBack labels useful.
- [ ] Keyboard open, rotated phone, gesture / 3-button nav, split screen and display cutouts.
- [ ] Today, Transactions, People, Insights and More can scroll fully; final action never obscured by bottom bar.
- [ ] Both fresh 0.8 install and direct 0.7 → 0.8 migration plan are verified (the debug packages are deliberately separate).
- [ ] Screenshot comparison with the 0.7 reference; no claims of successful phone QA until verified.

## Data safety
No finance data migration, backend sync, payments, biometric auth, or Play production release is implied by this UI update. Preview 0.8 has a separate debug package and starts with empty local data. A dedicated stable signing key is still required to offer real in-place upgrades later.
