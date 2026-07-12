# FixupXer Testing Report

## Test Execution Date
July 12, 2026

## Executive Summary
273/273 unit tests and 194/194 instrumentation tests pass for v2.1.0 on the `Pixel_API_35_Play` emulator. v2.1.0 adds the complete opt-in Custom URL rules system, raw-preserving processing, RE2/J validation, Test Lab, templates, atomic import/export with rollback, and a Room 1→2 migration while retaining the zero-permission offline model. The release also modernizes Rules, Rule Editor, History, Settings, and dialogs and links Browser/Rules help to dedicated GitHub guides. Differential, engine, repository, migration, UI, performance, and merged-manifest privacy tests cover the new surface. FixupXer v2.1.0 is **READY FOR PRODUCTION RELEASE**.

## Test Environment
- **Device**: Android Emulator - Pixel API 35 Play (Android 15)
- **Android Version**: API 35
- **Test Runner**: Android JUnit4
- **Build Variant**: Debug
- **Gradle**: 8.11.1

## Test Results Summary

### Unit Tests (`./gradlew test`)
**Status**: [x] PASSED
**Total Tests**: 273 unit tests (+21 vs v2.0.0)
**Pass Rate**: 100%
**Test Classes (highlights)**:
- `UrlPipelineDifferentialTest` — frozen master-off baseline, intended-difference allow-list, raw URL preservation, redirect hops and cycle limits
- `CustomRuleEngineTest` — scope/action/phase/context behavior, excludes, stop-after-match, invalid output rejection, idempotence, and raw token preservation
- `RuleBundleCodecTest` — versioned bundle round-trip, malformed/unknown input rejection, limits, and stable wire names
- `CustomRuleRepositoryTest` — atomic imports, conflict policies, deterministic order, snapshots, and rollback
- `CustomRulesPreferenceTest` — first-install/update default-off state and persisted opt-in
- Existing ViewModel, cleaner, conversion, validation, theme, history, and proxy suites remain green

### Android Instrumentation Tests (`./gradlew connectedAndroidTest`)
**Status**: [x] PASSED
**Total Tests**: 194 tests (+4 vs v2.0.0)
**Pass Rate**: 100%
**Passed**: 194 (100%)
**Failed**: 0
**Execution Time**: 7 min 55s on `Pixel_API_35_Play`

### Manual Emulator Verification
Manual verification of the v2.1.0 UI on `Pixel_API_35_Play` (release APK):
- [x] Main empty state — placeholder in result card, action buttons disabled
- [x] Main with dirty X/Twitter URL — tracking parameters struck through in the input, "Tracking removed and converted" status chip, fixupx.com result, action buttons enabled
- [x] Share with Instagram URL — same flow layout, struck-through `igsh` parameter, toinstagram.com conversion, proxy row with `Active: … Change.`
- [x] Dark mode — full palette verified on Main (cards, chips, toggles, FAB, status bar)
- [x] Rules library/editor — templates, empty/list states, add/edit/save/back behavior, Test Lab, fixed actions, and navigation insets
- [x] History bottom sheet — optimized list space, per-entry actions, bottom actions, enable toggle, and header settings action
- [x] About dialog — M3 styling, version 2.1.0, GPL notice
- [x] Theme picker in Settings — System/Light/Dark selection persisted and restored (also covered by `ThemePickerTest`)
- [x] Main, Share, Settings, Rules, Rule Editor, History, and dialogs checked in light/dark themes, 100%/130% font scale, and at 320dp width

## Detailed Test Results by Feature

### 0. Custom URL Rules (NEW in v2.1.0) [x]
**Test Files**: `UrlPipelineDifferentialTest.kt`, `CustomRuleEngineTest.kt`, `RuleBundleCodecTest.kt`, `CustomRuleRepositoryTest.kt`, `CustomRulesPreferenceTest.kt`, `CustomRuleMigrationTest.kt`, `CustomRulesUiTest.kt`, `CustomRulesPerformanceTest.kt`, `ManifestPrivacyTest.kt`
- [x] Master-off output matches the frozen pre-rules baseline; raw path/query/fragment tokens remain encoded and ordered
- [x] All scopes, excludes, phases, contexts, actions, stop-after-match, redirect hops, and invalid-output paths are covered
- [x] Bundle imports are bounded, validated, atomic, previewed before mutation, and reversible through snapshots
- [x] Room migration 1→2 preserves history and creates rule/snapshot tables without destructive fallback
- [x] Rules remain disabled by default; templates correctly replace the empty state; opening an unchanged rule does not trigger a discard prompt
- [x] A 200-rule snapshot meets the cold/warm processing budget
- [x] The merged app manifest declares zero permissions

### 0b. UI State & Theming Regression [x]
Existing `MainViewModelTest`, `ShareViewModelTest`, `ResultStatusTest`, `UrlDiffHelperTest`, `ThemePreferenceTest`, `ThemePickerTest`, accessibility, responsive-layout, and touch-target suites remain green after the pipeline and visual changes.

### 1. TikTok Conversion + Proxy Picker (v1.7.0) [x]
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
- [x] Share-flow E2E (Espresso): tiktok→proxy with www kept, proxy→tiktok reversion, vm. short link, legacy vxtiktok migration, dirty-URL cleanup+conversion
- [x] `TikTokCleaner` matches proxy links and strips TikTok params (`_r`, `_t`, `tt_from`, …)

### 2. Custom Instagram Proxies (v1.6.0) [x]
**Test Files**: `CustomInstagramProxyTest.kt` (unit), `CustomProxyDialogTest.kt` (instrumentation), `InstagramProxyPreferenceTest.kt` (instrumentation) — all passing on the new UI.

### 3. Instagram Proxy Selection [x]
**Test Files**: `InstagramProxySelectionTest.kt` (unit), `InstagramProxyPreferenceTest.kt`, `MainActivityProxyLabelTest.kt`, `ShareActivityProxyLabelTest.kt`, `CustomProxyDialogTest.kt` — all passing (label assertions updated for the new `Active: … Change.` row and result placeholder).

### 4. History Feature [x]
**Test File**: `HistoryDatabaseTest.kt` + `MainActivityHistoryTest.kt` + `SettingsTest.kt` — entry reload, copy/share/delete, clear all, enable/disable behavior, header max-entry settings, and empty/list states pass on the optimized bottom sheet.

### 5. Share Activity [x]
**Test Files**: `ShareActivityTest.kt`, `ShareActivityProxyLabelTest.kt`, `ShareActivityNoDuplicatesTest.kt` — all pass on the new flow layout.

### 6. Bidirectional URL Conversions [x]
**Test File**: `BidirectionalConversionTest.kt` — all Instagram / Twitter / Facebook / TikTok bidirectional scenarios pass.

### 7. Browser Mode Integration [x]
**Test File**: `BrowserModeTest.kt` — all pass; browser-mode pipeline untouched by the redesign.

### 8. Main Activity History UI [x]
**Test File**: `MainActivityHistoryTest.kt` — all pass.

### 9. URL Validation [x]
**Test Files**: `UrlValidationImprovementsTest.kt`, `UrlInputValidationTest.kt` — all security and validation tests pass (error-message assertions updated for reason-aware messages).

### 10-18. UI / Platform Suites [x]
`AccessibilityTest`, `ResponsiveDesignTest`, `TouchTargetTest`, `KeyboardNavigationTest`, `OfflinePerformanceTest`, `ApiCompatibilityTest`, `ReleaseTestSuite`, `SmartFooterTest`, `BrowserAliasIntentResolutionTest`, `SettingsTest`, `ThemePickerTest`, and `CustomRulesUiTest` — all pass on the v2.1.0 UI.

## Coverage Analysis

### Feature Coverage
- **URL Cleaning**: 100%
- **Result Status + URL Diff (v2.0.0)**: 100% — status resolution, strikethrough diff, placeholder/error states
- **Theming (v2.0.0)**: 100% — persistence, fallback, delegate mapping, picker UI
- **Instagram Proxy Selection (active set + custom)**: 100%
- **TikTok Proxy Selection (active set + custom + legacy)**: 100%
- **Bidirectional Conversions**: 100%
- **Browser Mode**: 100% (Google variant)
- **History Management**: 100%
- **Custom URL Rules**: 100% — engine, storage, migration, import/export, UI, performance, and privacy regression coverage
- **Input Validation**: 100%
- **Settings/Preferences**: 100%
- **Share Functionality**: 100%

### Platform Coverage
- **Instagram + active proxies (toinstagram.com / adamlikes.men / instagram7.com / kkinstagram.com) + custom proxies + legacy (eeinstagram.com)**: [x] Complete
- **Twitter/X/FixupX/FxTwitter/VxTwitter**: [x] Complete
- **Facebook (facebook.com / fb.com) / FacebookEZ**: [x] Complete
- **TikTok + active proxies (tnktok.com / tfxktok.com / tiktokez.com / kktiktok.com) + custom proxies + legacy (vxtiktok.com / tiktxk.com)**: [x] Complete

## GITHUB (F-Droid) Variant
- Source parity: all v2.1.0 source changes are synced into the GITHUB tree; only the standing intentional differences remain (`dependenciesInfo = false`, Linux JDK paths, F-Droid metadata).
- Instrumentation tests: run from the root tree on Windows (see above); F-Droid CI builds from the tag on Linux.

## Known Issues
- None blocking for v2.1.0 release.

## Performance Observations
- Unit suite and release lint pass with no failures/errors
- Final instrumentation suite: 7m 55s on the Pixel API 35 emulator
- UI responsiveness and the dedicated 200-rule cold/warm performance budgets pass
- No memory leaks or ANRs detected

## Security Testing
- [x] URL injection attacks prevented
- [x] Zero-width character attacks blocked
- [x] Control character attacks handled
- [x] Unicode normalization working correctly
- [x] Multiple URL detection preventing bypass attempts (validation surface unchanged from v1.7.2; errors now reason-aware)
- [x] Custom proxy input validated (format, reserved-domain, duplicates) for BOTH rosters — a custom TikTok entry cannot hijack Instagram/Twitter/Facebook detection and vice versa
- [x] Stored proxy preferences validated against unknown values (fall back to default)
- [x] Stored theme preference validated against unknown values (falls back to System)
- [x] User regexes compile only through RE2/J with bounded program size and no Java-regex fallback
- [x] Rule output, redirect hops, bundle size/counts, and raw URL structure are validated
- [x] Merged-manifest test enforces zero declared permissions

## Conclusion
**Production Readiness: YES** [x]

FixupXer v2.1.0 passes 273/273 unit tests and 194/194 instrumentation tests on `Pixel_API_35_Play`. The custom-rule pipeline, local persistence, migration, portable bundles, Test Lab, privacy guarantees, and consolidated Material 3 UI are covered by automated regression tests and a full manual visual walkthrough.

**The app is ready for production deployment.**
