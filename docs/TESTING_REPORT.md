# FixupXer Testing Report

## Test Execution Date
July 2, 2026

## Executive Summary
140/140 unit tests and 165/165 instrumentation tests pass for v1.6.0 on the `Pixel_API_35_Play` emulator. v1.6.0 adds user-defined custom Instagram proxies (add/select/delete from the proxy chooser dialog, backed by the new `InstagramProxyStore` and a `custom_instagram_proxies` pref), reinstates `kkinstagram.com` as an active Backup proxy, and lands a broad bug-fix pass (fb.com routing, vxtwitter.com conversion, Instagram cleaning of legacy/custom proxy links, "Nothing to do!" action-URL separation, history classification, lifecycle hygiene, browser-mode double-decoding). FixupXer v1.6.0 is **READY FOR PRODUCTION RELEASE**.

## Test Environment
- **Device**: Android Emulator - Pixel API 35 Play (Android 15)
- **Android Version**: API 35
- **Test Runner**: Android JUnit4
- **Build Variant**: Debug
- **Gradle**: 8.11.1

## Test Results Summary

### Unit Tests (`./gradlew test`)
**Status**: [x] PASSED  
**Total Tests**: 140 unit tests  
**Pass Rate**: 100%  
**Test Classes (highlights)**:
- `CustomInstagramProxyTest` (NEW — 18 cases: `InstagramProxyStore` state, input normalization, hostname/reserved/duplicate validation, custom-proxy conversions and detection, `InstagramCleaner` matching)
- `InstagramProxySelectionTest` (updated — kk active Backup, ee legacy; `InstagramProxyStore.reset()` in `@Before`/`@After`)
- `UrlProcessorMatrixTest` (updated — kk/ee reclassified, new vxtwitter.com and Facebook/fb.com matrix rows)
- `UrlProcessorTest`, `UrlLogicTest`, cleaner implementation tests (all `CleanerService` constructions updated to the 2-arg signature)

### Android Instrumentation Tests (`./gradlew connectedAndroidTest`)
**Status**: [x] PASSED  
**Total Tests**: 165 tests (152 v1.5.1 baseline + 5 new `CustomProxyDialogTest` cases + 8 new/updated `InstagramProxyPreferenceTest` cases)  
**Pass Rate**: 100%  
**Passed**: 165 (100%)  
**Failed**: 0  
**Execution Time**: ~7 min 17s on `Pixel_API_35_Play`

## Detailed Test Results by Feature

### 1. Custom Instagram Proxies (NEW in v1.6.0) [x]
**Test Files**: `CustomInstagramProxyTest.kt` (unit), `CustomProxyDialogTest.kt` (instrumentation), `InstagramProxyPreferenceTest.kt` (instrumentation)
- [x] Store state: set/get/reset; `activeProxies()` = fixed + custom; `allKnownProxies()` adds legacy
- [x] Input normalization: protocol, `www.`, path/query/fragment, whitespace, case
- [x] Hostname format validation (subdomains allowed; spaces/missing TLD/illegal chars/>253 chars rejected)
- [x] Reserved-domain rejection in both containment directions (Instagram + Twitter/X + Facebook families)
- [x] Duplicate rejection
- [x] Conversions: instagram.com → custom, custom → instagram.com (toggle OFF), custom ↔ fixed cross-swaps, no-op on target
- [x] Detection: `isInstagramUrl` recognizes custom proxy only while registered; `InstagramCleaner` matches and strips `igshid`
- [x] Prefs: add persists + syncs store; duplicate add no-op; custom selectable; unknown domain not selectable; remove syncs store; removing selected proxy falls back to default; list survives PreferencesManager recreation
- [x] Dialog UI: full roster + "Add custom proxy…" row listed; invalid input shows inline error and keeps dialog open; reserved domain rejected; add → select → delete flow with fallback to default

### 2. Instagram Proxy Selection (kk active again in v1.6.0) [x]
**Test Files**: `InstagramProxySelectionTest.kt` (unit), `InstagramProxyPreferenceTest.kt`, `MainActivityProxyLabelTest.kt`, `ShareActivityProxyLabelTest.kt`, `CustomProxyDialogTest.kt`
- [x] Forward conversion: `instagram.com` → each of `toinstagram.com`, `adamlikes.men`, `instagram7.com`, `kkinstagram.com`
- [x] Cross-proxy swaps among the active set (incl. kk both directions)
- [x] Bare-hostname: `www.instagram.com` → `<proxy>` (no `www.`); `business.instagram.com` → `<proxy>` (sub-prefix stripped)
- [x] Legacy auto-migration: `eeinstagram.com` URLs convert to the selected active proxy
- [x] No-op: bare proxy already matches target (incl. kk)
- [x] Backward conversion (toggle OFF): any known proxy (fixed/custom/legacy) → `instagram.com`
- [x] `PreferencesManager`: kk stored value stays valid (inverted vs v1.4.8 migration test); ee still migrates; invalid values fall back to default
- [x] Selecting kkinstagram.com from the dialog updates the "Active:" label

### 3. v1.6.0 Bug-Fix Regressions [x]
- [x] `fb.com` → `facebookez.com` conversion (toggle ON) and no-op passthrough (toggle OFF) — `UrlProcessorMatrixTest`
- [x] `vxtwitter.com` → `fixupx.com` (ON) / → `x.com` (OFF) — `UrlProcessorMatrixTest`
- [x] Legacy/custom proxy links receive Instagram tracking cleanup (`igshid`) — `CustomInstagramProxyTest`
- [x] "Nothing to do!" display preserved in UI tests (`BidirectionalConversionTest`, `ShareActivityTest`, `UrlValidationImprovementsTest` unchanged and passing) while action buttons use the separated `actionUrl`

### 4. History Feature [x]
**Test File**: `HistoryDatabaseTest.kt` — all cases pass; history classification refactor (`classifyConversion` helper) covered indirectly by Share/Main history tests.

### 5. Share Activity [x]
**Test Files**: `ShareActivityTest.kt`, `ShareActivityProxyLabelTest.kt`, `ShareActivityNoDuplicatesTest.kt` — all pass with the `actionUrl` UiState refactor.

### 6. Bidirectional URL Conversions [x]
**Test File**: `BidirectionalConversionTest.kt` — all Instagram / Twitter / Facebook bidirectional scenarios pass.

### 7. Browser Mode Integration [x]
**Test File**: `BrowserModeTest.kt` — all pass; VIEW intent validation now gates without double-decoding.

### 8. Main Activity History UI [x]
**Test File**: `MainActivityHistoryTest.kt` — all pass; `HistoryDialogHelper` collector-job fix verified (no duplicate collectors).

### 9. URL Validation [x]
**Test Files**: `UrlValidationImprovementsTest.kt`, `UrlInputValidationTest.kt` — all security and validation tests pass.

### 10-18. UI / Platform Suites [x]
`AccessibilityTest`, `ResponsiveDesignTest`, `TouchTargetTest`, `KeyboardNavigationTest`, `OfflinePerformanceTest`, `ApiCompatibilityTest`, `ReleaseTestSuite`, `SmartFooterTest`, `BrowserAliasIntentResolutionTest` — all pass unchanged.

## Coverage Analysis

### Feature Coverage
- **URL Cleaning**: 100%
- **Instagram Proxy Selection (v1.6.0 active set + custom)**: 100% — forward, reverse, cross-proxy, custom add/select/delete, validation, www. stripping, legacy auto-migration all covered
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

## GITHUB (F-Droid) Variant
- Source parity: all v1.6.0 source changes are fully synced into the GITHUB tree; only the standing intentional differences remain (`dependenciesInfo = false`, Linux JDK paths, F-Droid metadata).
- Instrumentation tests: run from the root tree on Windows (see above); F-Droid CI builds from the tag on Linux.

## Known Issues
- None blocking for v1.6.0 release.
- Pre-existing occasional Espresso flakes (`SettingsTest.testAboutDialog`, `KeyboardNavigationTest.testKeyboardInputAndDismissal`) did **not** reproduce in the v1.6.0 run (165/165 first-pass green).

## Performance Observations
- Unit suite: ~20s
- Instrumentation suite: ~7m 17s on the Pixel API 35 emulator
- No memory leaks or ANRs detected

## Security Testing
- [x] URL injection attacks prevented
- [x] Zero-width character attacks blocked
- [x] Control character attacks handled
- [x] Unicode normalization working correctly
- [x] Multiple URL detection preventing bypass attempts
- [x] Custom proxy input validated (format, reserved-domain, duplicates) — a custom entry cannot hijack Twitter/Facebook/Instagram detection
- [x] Stored proxy preference validated against unknown values (falls back to default)

## Conclusion
**Production Readiness: YES** [x]

FixupXer v1.6.0 passes 140/140 unit tests and 165/165 instrumentation tests on `Pixel_API_35_Play`. The custom proxy feature, the reinstated kkinstagram.com Backup proxy, and all bug fixes are covered by new and updated suites (`CustomInstagramProxyTest`, `CustomProxyDialogTest`, `InstagramProxyPreferenceTest`, `InstagramProxySelectionTest`, `UrlProcessorMatrixTest`).

**The app is ready for production deployment.**
