# FixupXer Testing Report

## v2.6.1 verification — July 25, 2026

v2.6.1 fixes a field report about AliExpress links: `pdp_npi` and 15 further
tracking keys are now stripped, wildcard `text/*` share intents are accepted,
and links carrying a literal space in the query are percent-encoded instead of
discarded. The instrumentation suite was also reworked for speed and stability.
Final verification passes:

- **659/659 unit tests** (`./gradlew test`, debug variant) — 7 new tests
- **235/235 instrumentation tests** (`./gradlew connectedAndroidTest`) on
  `Pixel_API_35_Play` / API 35, zero failures in a single full-suite pass
- **Release lint** (`./gradlew lintRelease`) — 0 errors

New coverage pins the exact URL from the bug report: `CatalogParameterCleaner`
tests assert that `pdp_npi` and `spm` are removed while `gatewayAdapt`,
`pdp_ext_f` and `pvid` survive byte for byte and that a second cleaning pass is
idempotent; `UrlProcessorTest` asserts that a decoded URL with spaces in the
query is percent-encoded rather than dropped, and that whitespace before the
query is still rejected so the host cannot shift.

Test-infrastructure changes: fixed sleeps were replaced with the shared
`EspressoSupport.awaitAssertion` poller, per-class duplicate helpers were
consolidated, and a `@Smoke` annotation marks a 52-test development subset
(`-Pandroid.testInstrumentationRunnerArguments.annotation=com.fixupxer.Smoke`,
43 s) that explicitly does **not** replace the full suite as a release gate.
Full-suite runtime went from about 19 minutes to about 10, and the previously
flaky `SettingsTest.testMaxEntriesValidation` now passes consistently.

Environment note: the AVD does not survive several consecutive full suites —
individual tests start taking a minute or more and the run dies with
`INSTRUMENTATION_ABORTED: System has crashed`, most often on the
`ShareActivityTest` action-mode tests that hand a link to an external browser.
The gate run above was made on a cold-booted emulator.

## v2.6.0 verification — July 22, 2026

v2.6.0 fixes Private Link Guard history suppression for fully cleaned
sensitive inputs: redirect wrappers (Reddit `out.reddit.com` `token=`, Google
Ads `pagead/aclk` `sig=`) and cleaner/custom-rule token strips now leave a
redacted history entry holding only the safe final URL. Cache rules are
unchanged. Final verification passes:

- **652/652 unit tests** (`./gradlew test`, debug variant; release variant
  also green) — 10 new tests
- **235/235 instrumentation tests** (`./gradlew connectedAndroidTest`) on
  `Pixel_API_35_Play` / API 35, zero failures in a single full-suite pass
- **Release lint** (`./gradlew lintRelease`) — 0 errors

New coverage: `LinkGuardRepositoryTest` gains redacted-entry assertions for
the Reddit token wrapper, Google Ads sig wrapper, Substack JWT strip and a
custom `RemoveParams` rule (each verifying the raw token/wrapper host never
reaches `insertHistory` and the cleaner cache stays empty), SHARE and BROWSER
profile smokes, `previousProcessedUrl` dedupe, an unchanged-sensitive-URL
guard, and an invalid-final-URL guard via a mocked orchestrator. New
Robolectric `HistoryAdapterTest` verifies redacted labels and full ViewHolder
reset on recycling. On-device verification on the emulator confirmed redacted
Room rows with a binary scan proving no token/sig/wrapper-host bytes were
persisted.

## v2.5.1 verification — July 22, 2026

v2.5.1 is a Google Play compliance release: `targetSdk`/`compileSdk` raised to
Android 16 (API 36) with Android Gradle Plugin 8.3.2 → 8.9.3 (Gradle 8.11.1
and JDK 17 unchanged). No app-code changes; a new
`app/src/test/resources/robolectric.properties` pins the default emulated SDK
to 35 because Robolectric 4.14.1 has no SDK 36 runtime (explicit
`@Config(sdk = ...)` annotations keep precedence). Final verification passes
with the new toolchain:

- **642/642 unit tests** (`./gradlew test`, debug variant; release variant
  also green)
- **235/235 instrumentation tests** (`./gradlew connectedAndroidTest`) on
  `Pixel_API_35_Play` / API 35, zero failures in a single full-suite pass
- **Release lint** (`./gradlew lintRelease`) — 0 errors with the newer lint
  bundled in AGP 8.9.3

No new tests were added — the suite is unchanged from v2.5.0 and re-validated
end-to-end against compileSdk 36 and the upgraded build toolchain.

## v2.5.0 verification — July 22, 2026

v2.5.0 retires two unsafe built-in frontend domains (`facebookez.com`,
`kkinstagram.com`) with automatic preference/backup migration and a custom-
proxy denylist, and adds the **Settings > Link processing > Alternative
frontends** screen with per-platform pickers. Final verification passes:

- **642/642 unit tests** (`./gradlew test`, debug variant; release variant
  also green)
- **235/235 instrumentation tests** (`./gradlew connectedAndroidTest`) on
  `Pixel_API_35_Play` / API 35 — earlier full-suite passes hit environment
  flakes traced to the emulator itself (a `-wipe-data` boot with GMS
  self-update churn, animations re-enabled by the wipe, a `system_server`
  crash, and a guest kernel-time storm); after a clean reboot with network
  off and animations disabled the full suite passed 235/235 with zero
  failures
- **Release lint** (`./gradlew lintRelease`) — 0 errors

New coverage includes retired-domain catalog/roster exclusions and reserved-
domain enforcement, preference migration (kkinstagram → first active proxy,
facebookez → conversion off only when actually migrated, custom-proxy CSV
purge, idempotency), backup snapshot migration and validator acceptance of
legacy custom proxies, `PlatformToggleHelper` empty-state (neutral title,
switch off and disabled), the new `FrontendSettingsActivity` (nine platform
rows, picker launch, state refresh), and updated bidirectional-conversion
expectations for retired domains (generic tracking cleaning only, no
platform-specific handling).

## v2.4.1 verification — July 21, 2026

v2.4.1 is a patch release with one privacy fix (fragment pseudo-query no
longer promoted into a real query during conversions) and three reliability
fixes (Open self-interception when FixupXer is the default browser, Facebook
frontend-to-frontend retargeting, Browser VIEW history dedup across Activity
recreation via `viewModelScope` + `SavedStateHandle` replay). Final
verification passes:

- **628/628 unit tests** (`./gradlew test`, debug variant; release variant
  also green)
- **229/229 instrumentation tests** (`./gradlew connectedAndroidTest`) on
  `Pixel_API_35_Play` / API 35 — one full-suite pass had 5 environment
  flakes (window-focus / `onActivityResult` timeouts on a busy fresh-boot
  emulator, one of them also reproducible on the v2.4.0 baseline); all
  affected classes were re-run green in targeted executions, and
  `SettingsTest.testBackNavigation` was stabilized against the bottom-sheet
  settle animation
- **Release lint** (`./gradlew lintRelease`) — 0 errors

New coverage includes fragment pseudo-query preservation across X reader
forward/reverse, Reddit host swaps, youtu.be, Farside reverse, and real
query + fragment combinations; Facebook built-in↔custom retargeting incl.
lookalike-host boundaries; `shouldRedirectSelfOpen` decision logic; the
`SavedStateHandle` transaction roundtrip incl. ViewModel recreation; the
in-flight `browserViewResult` cache (awaiter cancellation, same-URL reuse,
different-URL replacement); and a `BrowserViewRecreationTest` instrumentation
scenario asserting a restored Ask dialog with exactly one history row after
`ActivityScenario.recreate()`.

## v2.4.0 verification — July 20, 2026

v2.4.0 splits Browser mode into a dedicated settings screen, adds Browser
privacy readers with per-platform targets, exact-host saved app choices, a
read-only Configuration status dialog, crash-safe local settings backup and
restore, handed action layouts, guided legacy history-limit migration, and an
always-on tracking-cleaning invariant. Final verification passes:

- **607/607 unit tests** (`./gradlew test`, debug variant; release variant also
  green)
- **228/228 instrumentation tests** (`./gradlew connectedAndroidTest`) on
  `Pixel_API_35_Play` / API 35
- **Release lint** (`./gradlew lintRelease`)

New coverage includes Browser alias transactions with rollback, view-gate
revision/pause semantics incl. underflow protection, remembered-route and
custom-rules invalidation, serialized VIEW intents without stacked dialogs,
restore rollback-marker recovery (settings, rules, alias, pending legacy
limit), deterministic post-restore theme acknowledgement, legacy history-limit
migration UI and raw-limit trimming, tracking-cleaning invariant, settings
status resolvers, bottom-sheet animation behavior, and saved-app-choices
layout/regression suites. The verification history below is retained.

## Post-v2.3.0 working-tree verification — July 19, 2026

The current working tree adds versioned local settings backup/restore and
exact-host remembered Browser-mode destinations. Final verification passes:

- **546/546 unit tests** (`./gradlew test`)
- **223/223 instrumentation tests** (`./gradlew connectedAndroidTest`) on
  `Pixel_API_35_Play` / API 35
- **Release lint** (`./gradlew lintRelease`)

New coverage includes strict backup schema/size/settings/proxy/route validation,
full custom-rule replacement and rollback, cross-device route persistence,
IDN/exact-host normalization, Reader-native skip-without-delete, incompatible
route removal with normal fallback, compatible app filtering, and Settings
entries for backup and remembered destinations. This is a post-release
working-tree result; the v2.3.0 release baseline remains recorded below.

## Test Execution Date
July 18, 2026

## Executive Summary
380/380 unit tests and 201/201 instrumentation tests pass for v2.3.0 on the `Pixel_API_35_Play` emulator. v2.3.0 adds selective Bilibili cleaning, three exact Yahoo/Guce referrer keys, and offline GeoRiot/Geniuslink plus LinkSynergy/Rakuten redirect unwrapping. Redirect extraction now requires exact hosts and paths, ignores query-like fragment text, decodes once, and rejects invalid ports, malformed escapes, boundary whitespace, unsafe duplicate bypasses, and non-HTTP targets while preserving valid raw destination data. FixupXer v2.3.0 remains fully offline and zero-permission.

## Test Environment
- **Device**: Android Emulator - Pixel API 35 Play (Android 15)
- **Android Version**: API 35
- **Test Runner**: Android JUnit4
- **Build Variant**: Debug
- **Gradle**: 8.11.1

## Test Results Summary

### Unit Tests (`./gradlew test`)
**Status**: [x] PASSED
**Total Tests**: 380 unit tests (+10 vs v2.2.0)
**Pass Rate**: 100%
**Test Classes (expanded in v2.3.0)**:
- `CatalogParameterCleanerTest` — Bilibili exact-key removal with `from`, unknown keys, duplicate raw tokens, fragments, subdomains, and lookalike hosts preserved
- `GeneralTrackingCleanerContractTest` — exact case-insensitive Yahoo/Guce key removal with near-match preservation
- `OfflineRedirectCleanerTest` — GeoRiot and LinkSynergy extraction, registry deep-clean, exact host/path boundaries, strict decoding, structural target validation, duplicate semantics, fragment safety, and raw target preservation

**Carried-over v2.2.0 highlights**:
- `LinkLeakAnalyzerTest` — credentials/e-mail/JWT/token/coordinate detection, false-positive guards, no raw values in findings
- `LinkGuardRepositoryTest` — ephemeral processing: history skip, cache bypass for sensitive inputs, cache-key eviction on output-only findings (incl. PRE_CLEAN custom-rule keying)
- `HostBoundaryRegressionTest` + `GeneralTrackingCleanerContractTest` — label-boundary matching and the keep-unknown contract
- `CatalogParameterCleanerTest`, `GoogleMapsCleanerTest`, `OfflineRedirectCleanerTest`, `BlueskyConversionTest` — new cleaner catalog, canonicalization, redirect unwrapping, Bluesky conversion
- `ChangeOperationTraceTest` — internal operation trace: order, cold/warm cache equality, caps, non-sensitivity
- `ProcessTextViewModelTest` — inline replace vs Share forward vs cancel decision paths
- `RuleVectorRunnerTest` + expanded `CustomRuleRepositoryTest` — vector evaluation, activation gate, import failure handling
- `RuleExampleInferenceTest` — RemoveParams/ExtractRedirect inference, ambiguity and encoding-change rejection
- `UrlPipelineDifferentialTest` — frozen master-off baseline stays byte-identical
- Existing ViewModel, cleaner, conversion, validation, theme, history, and proxy suites remain green

### Android Instrumentation Tests (`./gradlew connectedAndroidTest`)
**Status**: [x] PASSED
**Total Tests**: 201 tests (unchanged vs v2.2.0)
**Pass Rate**: 100%
**Passed**: 201 (100%)
**Failed**: 0
**Execution Time**: ~8 min on `Pixel_API_35_Play` (animations disabled)

### Manual Emulator Verification
Manual verification of the v2.3.0 UI on `Pixel_API_35_Play` (the flow layout is unchanged from v2.2.0):
- [x] Process Text — instrumentation covers inline replace (`RESULT_OK` + `EXTRA_PROCESS_TEXT`), read-only forward to Share preview, and conversion-toggle on/off behavior
- [x] Link Guard warning row + dialog covered by ViewModel/repository tests; a release APK is provided to the maintainer for on-device verification
- [x] Main empty state — placeholder in result card, action buttons disabled
- [x] Main with dirty X/Twitter URL — tracking parameters struck through in the input, "Tracking removed and converted" status chip, fixupx.com result, action buttons enabled
- [x] Share with Instagram URL — same flow layout, struck-through `igsh` parameter, toinstagram.com conversion, proxy row with `Active: … Change.`
- [x] Dark mode — full palette verified on Main (cards, chips, toggles, FAB, status bar)
- [x] Rules library/editor — templates, empty/list states, add/edit/save/back behavior, Test Lab, fixed actions, and navigation insets
- [x] History bottom sheet — optimized list space, per-entry actions, bottom actions, enable toggle, and header settings action
- [x] About dialog — M3 styling, version 2.3.0, GPL notice
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
`AccessibilityTest`, `ResponsiveDesignTest`, `TouchTargetTest`, `KeyboardNavigationTest`, `OfflinePerformanceTest`, `ApiCompatibilityTest`, `ReleaseTestSuite`, `SmartFooterTest`, `BrowserAliasIntentResolutionTest`, `SettingsTest`, `ThemePickerTest`, `CustomRulesUiTest`, and `ProcessTextActivityTest` — all pass on the v2.3.0 build.

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
- **Custom URL Rules**: 100% — engine, storage, migration, import/export, test vectors, Teach-from-example, UI, performance, and privacy regression coverage
- **Private Link Guard (v2.2.0)**: 100% — detection categories, ephemeral history/cache behavior, parameter removal
- **Cleaner catalog + redirect unwrapping (through v2.3.0)**: 100% — keep-unknown contract, host boundaries, 15 catalog platforms, Google Maps, eight wrapper families
- **Bluesky conversion + Process Text (v2.2.0)**: 100%
- **Input Validation**: 100%
- **Settings/Preferences**: 100%
- **Share Functionality**: 100%

### Platform Coverage
- **Instagram + active proxies (toinstagram.com / adamlikes.men / instagram7.com / kkinstagram.com) + custom proxies + legacy (eeinstagram.com)**: [x] Complete
- **Twitter/X/FixupX/FxTwitter/VxTwitter**: [x] Complete
- **Facebook (facebook.com / fb.com) / FacebookEZ**: [x] Complete
- **TikTok + active proxies (tnktok.com / tfxktok.com / tiktokez.com / kktiktok.com) + custom proxies + legacy (vxtiktok.com / tiktxk.com)**: [x] Complete
- **Bluesky posts (bsky.app ↔ fxbsky.app) + go.bsky.app unwrapping**: [x] Complete
- **Catalog platforms (Wikipedia, Threads, Twitch, Spotify, Pinterest, Snapchat, WhatsApp, Medium, Bing, DuckDuckGo, Google Store, eBay, Netflix, AliExpress, Bilibili) + Google Maps**: [x] Complete

## GITHUB (F-Droid) Variant
- Source parity: all v2.3.0 source changes are synced into the GITHUB tree; only the standing intentional differences remain (`dependenciesInfo = false`, Linux JDK paths, F-Droid metadata).
- Instrumentation tests: run from the root tree on Windows (see above); F-Droid CI builds from the tag on Linux.

## Known Issues
- None blocking for v2.3.0 release.
- The API 35 emulator occasionally drops window focus on long runs when animations are enabled (`RootViewWithoutFocusException`); animations are kept disabled on the AVD, after which the full suite is stable.

## Performance Observations
- Unit suite and release lint pass with no failures/errors
- Final instrumentation suite: ~8 min on the Pixel API 35 emulator
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
- [x] Sensitive URLs (Link Guard findings) bypass cache and history; created cache keys are evicted on output-only findings
- [x] Processing logs sanitized — full URLs and parameter values never reach logcat
- [x] Redirect unwrapping requires exact registered hosts/paths and accepts only single-decoded, structurally valid HTTP(S) targets

## Conclusion
**Production Readiness: YES** [x]

FixupXer v2.3.0 passes 380/380 unit tests and 201/201 instrumentation tests on `Pixel_API_35_Play`. The Bilibili and Yahoo/Guce additions plus GeoRiot/LinkSynergy wrappers are covered alongside all existing cleaning, Link Guard, conversion, Process Text, and custom-rule regressions.

**The app is ready for production deployment.**
