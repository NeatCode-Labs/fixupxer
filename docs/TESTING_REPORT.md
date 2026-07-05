# FixupXer Testing Report

## Test Execution Date
July 5, 2026

## Executive Summary
211/211 unit tests and 186/186 instrumentation tests pass for v1.7.2 on the `Pixel_API_35_Play` emulator. v1.7.2 is a root-cause bug-fix release: opening links from the Reddit app (FixupXer as default browser) landed on `reddit.com/invalid_token…` because (1) `RedditCleaner` stripped the functional `url=`/`token=` params from Reddit's outbound wrapper `out.reddit.com`, and (2) the v1.7.1 Gmail fix was a Google-only allow-list, so all other redirect wrappers still failed the validator's multiple-URL check. The validator is now host-agnostic (multiple-URL heuristic runs on authority+path of a single whitespace-free URL), `RedditCleaner` extracts the outbound destination, and the validator's regexes are precompiled (per-call recompilation was exceeding the 50 ms anti-DoS timeout on slower hardware and wrongly rejecting valid single URLs). Verified end-to-end on the emulator and user-confirmed with the real Reddit app on a physical device. FixupXer v1.7.2 is **READY FOR PRODUCTION RELEASE**.

## Test Environment
- **Device**: Android Emulator - Pixel API 35 Play (Android 15)
- **Android Version**: API 35
- **Test Runner**: Android JUnit4
- **Build Variant**: Debug
- **Gradle**: 8.11.1

## Test Results Summary

### Unit Tests (`./gradlew test`)
**Status**: [x] PASSED  
**Total Tests**: 211 unit tests (+9 vs v1.7.1)  
**Pass Rate**: 100%  
**Test Classes (highlights)**:
- `UpdatedCleanersTest` (updated — 4 new `RedditCleaner` cases): `out.reddit.com` destination extraction (%-encoded and plain `url=`), wrapper without `url=` kept intact (server-side redirect and its token preserved), ordinary reddit.com post cleaning unaffected
- `InputValidatorTest` (updated — host-agnostic redirect handling): Reddit `out.reddit.com` and Facebook `l.facebook.com/l.php?u=` wrappers accepted, generic single URLs with nested query URLs accepted on any host, second protocol glued into the *path* still rejected; obsolete "non-Google host rejected" case removed
- `UrlProcessorTest` (updated — 1 new end-to-end case: `reddit outbound wrapper is unwrapped and destination cleaned`)
- `TikTokProxySelectionTest`, `CustomTikTokProxyTest`, `InstagramProxySelectionTest`, `CustomInstagramProxyTest`, `UrlProcessorMatrixTest`, cleaner implementation tests — all unchanged and passing

### Android Instrumentation Tests (`./gradlew connectedAndroidTest`)
**Status**: [x] PASSED  
**Total Tests**: 186 tests (unchanged vs v1.7.1)  
**Pass Rate**: 100%  
**Passed**: 186 (100%)  
**Failed**: 0  
**Execution Time**: ~7 min 30s on `Pixel_API_35_Play`

### Manual Emulator Verification (browser-mode VIEW intents)
Simulated VIEW intents fired at `MainActivity` on `Pixel_API_35_Play` with logcat inspection:
- [x] Reddit outbound wrapper (`out.reddit.com/…?url=https%3A%2F%2F…&token=…`) → validator accepts, `RedditCleaner` extracts the destination, destination cleaned, `PostCleanRunner` runs (pre-fix: stripped wrapper → `reddit.com/invalid_token`)
- [x] Gmail-style Google redirect (plain and %-encoded `q=`) → still unwrapped and cleaned correctly
- [x] Direct URL with tracking params → cleaned as before (no behaviour change)
- [x] Multi-URL attack input (`…/a%20https://attacker.com/b`) → still rejected by the validator
- [x] Physical-device confirmation by the maintainer: links from the real Reddit app open correctly through the browser picker

## Detailed Test Results by Feature

### 0. Redirect Wrappers in Browser Mode (generalized in v1.7.2; Gmail-only in v1.7.1) [x]
**Test Files**: `InputValidatorTest.kt` (unit), `UpdatedCleanersTest.kt` (unit), `UrlProcessorTest.kt` (unit)
- [x] Any single whitespace-free URL with a nested destination in its query passes validation — Reddit `out.reddit.com`, Facebook `l.facebook.com/l.php`, Google `google.com/url`, generic hosts
- [x] `RedditCleaner` extracts the `url=` destination from `out.reddit.com`; wrappers without `url=` are left intact
- [x] Exemption requires the whole input to be one URL — any whitespace voids it; a second protocol glued into the path is still rejected
- [x] All other validator checks (length, control chars, combining accents, %2E) still apply to redirects
- [x] Smuggled URLs in non-`q=`/`url=` wrapper params are dropped by single-destination extraction
- [x] Validator regexes precompiled — no spurious 50 ms timeout rejections (verified in emulator logcat)
- [x] End-to-end emulator + physical-device verification (see Manual Emulator Verification above)

### 1. TikTok Conversion + Proxy Picker (NEW in v1.7.0) [x]
**Test Files**: `TikTokProxySelectionTest.kt` (unit), `CustomTikTokProxyTest.kt` (unit), `TikTokProxyPreferenceTest.kt` (instrumentation), `BidirectionalConversionTest.kt` (instrumentation)
- [x] Forward conversion: `tiktok.com` → each of `tnktok.com`, `tfxktok.com`, `tiktokez.com`, `kktiktok.com`
- [x] Host-prefix preservation: `www.`/`vm.`/`vt.`/`m.` kept on conversion in BOTH directions (vm.tiktok.com ↔ vm.tnktok.com)
- [x] Cross-proxy swaps among the active set
- [x] Legacy auto-migration: `vxtiktok.com` and `tiktxk.com` URLs convert to the selected active proxy (toggle ON) or back to tiktok.com (toggle OFF)
- [x] No-op: proxy already matches target (bare and prefixed hosts)
- [x] Backward conversion (toggle OFF): any known proxy (fixed/custom/legacy) → `tiktok.com`
- [x] Substring safety: kktiktok.com/vxtiktok.com contain "tiktok.com" — combined proxy-first checks verified
- [x] Custom proxies: store state, add/select/delete, conversions custom ↔ fixed, detection only while registered
- [x] Reserved-domain validation both ways: TikTok store rejects Instagram/Twitter/Facebook/TikTok families; Instagram store now also rejects TikTok domains (custom rosters cannot hijack each other)
- [x] Prefs: default = tnktok.com; all four fixed proxies persist; invalid/legacy stored values fall back to default; custom roster survives PreferencesManager recreation; TikTok and Instagram custom rosters independent
- [x] Share-flow E2E (Espresso): tiktok→proxy with www kept, proxy→tiktok reversion, vm. short link, legacy vxtiktok migration, dirty-URL cleanup+conversion, "Nothing to do!" with toggle OFF
- [x] `TikTokCleaner` matches proxy links and strips TikTok params (`_r`, `_t`, `tt_from`, …)

### 2. Custom Instagram Proxies (v1.6.0) [x]
**Test Files**: `CustomInstagramProxyTest.kt` (unit), `CustomProxyDialogTest.kt` (instrumentation), `InstagramProxyPreferenceTest.kt` (instrumentation) — all passing unchanged.

### 3. Instagram Proxy Selection [x]
**Test Files**: `InstagramProxySelectionTest.kt` (unit), `InstagramProxyPreferenceTest.kt`, `MainActivityProxyLabelTest.kt`, `ShareActivityProxyLabelTest.kt`, `CustomProxyDialogTest.kt` — all passing unchanged.

### 4. History Feature [x]
**Test File**: `HistoryDatabaseTest.kt` — all cases pass; `classifyConversion` extended for TikTok covered via processor-level tests.

### 5. Share Activity [x]
**Test Files**: `ShareActivityTest.kt`, `ShareActivityProxyLabelTest.kt`, `ShareActivityNoDuplicatesTest.kt` — all pass with the new TikTok toggle present.

### 6. Bidirectional URL Conversions [x]
**Test File**: `BidirectionalConversionTest.kt` — all Instagram / Twitter / Facebook / **TikTok (new)** bidirectional scenarios pass.

### 7. Browser Mode Integration [x]
**Test File**: `BrowserModeTest.kt` — all pass; new `browser_convert_tiktok` pref defaults to off (no behavior change for existing flows).

### 8. Main Activity History UI [x]
**Test File**: `MainActivityHistoryTest.kt` — all pass.

### 9. URL Validation [x]
**Test Files**: `UrlValidationImprovementsTest.kt`, `UrlInputValidationTest.kt` — all security and validation tests pass.

### 10-18. UI / Platform Suites [x]
`AccessibilityTest`, `ResponsiveDesignTest`, `TouchTargetTest`, `KeyboardNavigationTest`, `OfflinePerformanceTest`, `ApiCompatibilityTest`, `ReleaseTestSuite`, `SmartFooterTest`, `BrowserAliasIntentResolutionTest` — all pass unchanged.

## Coverage Analysis

### Feature Coverage
- **URL Cleaning**: 100%
- **Instagram Proxy Selection (active set + custom)**: 100%
- **TikTok Proxy Selection (v1.7.0 active set + custom + legacy)**: 100% — forward, reverse, cross-proxy, custom add/select/delete, validation, subdomain preservation, legacy auto-migration all covered
- **Bidirectional Conversions**: 100%
- **Browser Mode**: 100% (Google variant)
- **History Management**: 100%
- **Input Validation**: 100%
- **Settings/Preferences**: 100%
- **Share Functionality**: 100%

### Platform Coverage
- **Instagram + active proxies (toinstagram.com / adamlikes.men / instagram7.com / kkinstagram.com) + custom proxies + legacy (eeinstagram.com)**: [x] Complete
- **Twitter/X/FixupX/FxTwitter/VxTwitter**: [x] Complete
- **Facebook (facebook.com / fb.com) / FacebookEZ**: [x] Complete
- **TikTok + active proxies (tnktok.com / tfxktok.com / tiktokez.com / kktiktok.com) + custom proxies + legacy (vxtiktok.com / tiktxk.com)**: [x] Complete

## GITHUB (F-Droid) Variant
- Source parity: all v1.7.2 source changes are fully synced into the GITHUB tree; only the standing intentional differences remain (`dependenciesInfo = false`, Linux JDK paths, F-Droid metadata).
- Instrumentation tests: run from the root tree on Windows (see above); F-Droid CI builds from the tag on Linux.

## Known Issues
- None blocking for v1.7.2 release.
- Pre-existing occasional Espresso flakes (`SettingsTest.testAboutDialog`, `KeyboardNavigationTest.testKeyboardInputAndDismissal`) did **not** reproduce in the v1.7.2 run (186/186 first-pass green).

## Performance Observations
- Unit suite: ~25s
- Instrumentation suite: ~7m 32s on the Pixel API 35 emulator
- No memory leaks or ANRs detected

## Security Testing
- [x] URL injection attacks prevented
- [x] Zero-width character attacks blocked
- [x] Control character attacks handled
- [x] Unicode normalization working correctly
- [x] Multiple URL detection preventing bypass attempts (v1.7.2: heuristic runs on authority+path of a single whitespace-free URL — whitespace pastes and glued hosts still rejected, scope regression-tested)
- [x] Custom proxy input validated (format, reserved-domain, duplicates) for BOTH rosters — a custom TikTok entry cannot hijack Instagram/Twitter/Facebook detection and vice versa
- [x] Stored proxy preferences validated against unknown values (fall back to default)

## Conclusion
**Production Readiness: YES** [x]

FixupXer v1.7.2 passes 211/211 unit tests and 186/186 instrumentation tests on `Pixel_API_35_Play`. The Reddit outbound-wrapper fix, the host-agnostic validator generalization, and the regex precompilation fix are covered by new unit cases in `UpdatedCleanersTest`, `InputValidatorTest`, and `UrlProcessorTest`, plus live emulator VIEW-intent verification and the maintainer's physical-device confirmation with the real Reddit app.

**The app is ready for production deployment.**
