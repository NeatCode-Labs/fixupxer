# Android Test Coverage for FixupXer v2.3.0

## Overview
This document is the canonical test inventory (unit, instrumentation, and URL conversion matrix) covering all major features through v2.3.0. v2.3.0 adds selective Bilibili and Yahoo/Guce cleaning plus GeoRiot/Geniuslink and LinkSynergy/Rakuten offline redirect unwrapping while retaining the keep-unknown, zero-permission offline model.

Test-suite delta vs v2.2.0 (380 unit / 201 instrumentation, both 100% passing):
- **Updated**: `CatalogParameterCleanerTest.kt` — Bilibili removes only `vd_source`, `seid`, `share_source`, and `copy_link`; `from`, unknown/duplicate raw tokens, fragments, valid subdomains, and lookalike hosts are covered.
- **Updated**: `GeneralTrackingCleanerContractTest.kt` — exact case-insensitive removal of `guccounter`, `guce_referrer`, and `guce_referrer_sig`, with near-match names preserved.
- **Updated**: `OfflineRedirectCleanerTest.kt` — exact GeoRiot/LinkSynergy hosts and paths, single-decode behavior, registry multi-pass destination cleaning, fragment pseudo-query rejection, invalid ports/escapes/whitespace, unsafe duplicate semantics, case-sensitive destination keys, and raw `+`/query/fragment preservation.
- **Unchanged**: 201 instrumentation tests exercise the full existing UI, Browser, Share, Process Text, conversion, history, theme, accessibility, responsive-layout, and zero-permission regressions; v2.3.0 adds no UI or manifest surface.

Test-suite delta vs v2.1.0 (370 unit / 201 instrumentation, both 100%):
- **Added**: `LinkLeakAnalyzerTest.kt` — credential/e-mail/JWT/token-parameter/coordinate detection across URL components, one-time percent-decode semantics, false-positive guards, and no raw values in findings.
- **Added**: `LinkGuardRepositoryTest.kt` — ephemeral processing: sensitive inputs bypass the cleaner cache, history writes are skipped on input/output findings, and cache keys created during a run are evicted on output-only findings (including keys created after PRE_CLEAN custom rules).
- **Added**: `HostBoundaryRegressionTest.kt` + `GeneralTrackingCleanerContractTest.kt` — label-boundary host matching (lookalike domains, domains inside paths/queries) and the narrowed universal-tracker contract.
- **Added**: `CatalogParameterCleanerTest.kt` + `GoogleMapsCleanerTest.kt` — the 14 data-driven platform rules and Google Maps coordinate canonicalization.
- **Added**: `OfflineRedirectCleanerTest.kt` — six wrapper families, strict single percent-decode, HTTP(S)-only targets, non-wrapper URLs untouched.
- **Added**: `BlueskyConversionTest.kt` — bsky.app ↔ fxbsky.app post conversion (path/query/fragment preserved), profile URLs excluded, go.bsky.app unwrapping, history classification.
- **Added**: `ChangeOperationTraceTest.kt` — internal operation trace: ordering, cold/warm cache equality, `MAX_CHANGE_OPERATIONS` cap, non-sensitivity (no URLs/values in operations).
- **Added**: `ProcessTextViewModelTest.kt` (unit) + `ProcessTextActivityTest.kt` (instrumentation) — editable single-URL inline replace (`RESULT_OK` + `EXTRA_PROCESS_TEXT`), read-only/prose/multi-URL forward to Share preview, cancel on empty input, conversion-toggle on/off behavior.
- **Added**: `RuleVectorRunnerTest.kt` — isolated vector evaluation (no history/cache/prefs), exact raw comparison, evaluation-profile selection.
- **Added**: `RuleExampleInferenceTest.kt` — RemoveParams and ExtractRedirect inference, all-or-nothing duplicate handling, ambiguity/encoding-change/no-op rejection reasons.
- **Updated**: `CustomRuleRepositoryTest.kt` — activation gate on save/toggle/import, failing imports become disabled drafts, `vectorFailures` scoping per import mode.
- **Updated**: `CustomRulesUiTest.kt` — test-vector editor UI (add/run-all/limit), activation-gate dialogs, Teach-from-example card inference flow.
- **Updated**: cleaner/processor/matrix suites for the keep-unknown contract (unknown parameters survive), `CleanerCatalog` as the canonical test cleaner list, and host-boundary conversions.

Test-suite delta vs v2.0.0 (273 unit / 194 instrumentation, both 100%):
- **Added**: `UrlPipelineDifferentialTest.kt` — frozen master-off baseline, raw URL preservation, intended-difference allow-list, redirect hop/cycle limits.
- **Added**: `CustomRuleEngineTest.kt`, `RuleBundleCodecTest.kt`, `CustomRuleRepositoryTest.kt`, and `CustomRulesPreferenceTest.kt` — scopes/actions/phases, RE2/J validation, versioned bundles, atomic import/rollback, deterministic ordering, and default-off behavior.
- **Added**: `CustomRuleMigrationTest.kt` — Room schema 1→2 migration preserves history and creates custom-rule tables.
- **Added**: `CustomRulesUiTest.kt` — Settings entry, add/save, templates, empty/list states, editor back behavior, and navigation-safe Add rule positioning.
- **Added**: `CustomRulesPerformanceTest.kt` — 200-rule cold/warm Share-profile performance budget.
- **Added**: `ManifestPrivacyTest.kt` — merged app manifest must declare zero permissions.
- **Updated**: `SettingsTest.kt`, `MainActivityHistoryTest.kt`, `OfflinePerformanceTest.kt`, and existing responsive/accessibility suites for the redesigned Rules, Rule Editor, History, Settings, and dialog layouts.

v2.0.0 test-suite delta vs v1.7.2 (252 unit / 190 instrumentation, both 100%):
- **Added**: `MainViewModelTest.kt` (unit) — result statuses incl. `CLEANED_AND_CONVERTED`, `onUrlChanged` stale-result invalidation vs. retention, validation-error reason mapping (`MULTIPLE_URLS` vs `OTHER`), loading/action-URL state.
- **Added**: `ShareViewModelTest.kt` (unit) — platform flag detection, duplicate identical share-text guard, `reprocessAfterProxyChange()`, `setNoSharedText()` error surfacing, error-branch state reset.
- **Added**: `ResultStatusTest.kt` (unit) — status resolution: `ALREADY_CLEAN` vs `CLEANED` under subdomain normalization, `CONVERTED` with lookalike proxy domains, `CLEANED_AND_CONVERTED`, malformed URL fallback.
- **Added**: `UrlDiffHelperTest.kt` (unit) — exact parameter-set strikethrough comparison, fragment handling, `applyStrikesInPlace` scenarios.
- **Added**: `ThemePreferenceTest.kt` (unit, Robolectric) — theme default/round-trip, corrupted-value fallback to System, `ThemeHelper` → `AppCompatDelegate` mapping.
- **Added**: `ThemePickerTest.kt` (instrumentation) — default selection, persisting a selection, picker state restored from a pre-seeded preference.
- **Updated**: `MainActivityProxyLabelTest.kt` (result-card placeholder assertions), `UrlInputValidationTest.kt` (reason-aware error messages), `ReleaseTestSuite.kt` (async timing), plus layout-driven assertion updates across the UI suites for the new flow design.

Test-suite delta vs v1.7.1 (211 unit / 186 instrumentation, both 100%):
- **Updated**: `UpdatedCleanersTest.kt` (+4 `RedditCleaner` cases) — `out.reddit.com` destination extraction from `url=` (%-encoded and plain), wrapper without `url=` returned intact (server-side redirect token preserved), ordinary reddit.com post cleaning unaffected.
- **Updated**: `InputValidatorTest.kt` (now 22 cases) — host-agnostic redirect acceptance: Reddit `out.reddit.com` and Facebook `l.facebook.com/l.php?u=` wrappers, generic single URLs with nested query URLs on any host; second protocol glued into the *path* still rejected; obsolete "nested URL on non-Google host rejected" case removed (behaviour intentionally generalized).
- **Updated**: `UrlProcessorTest.kt` — new end-to-end `reddit outbound wrapper is unwrapped and destination cleaned` case.

Test-suite delta vs v1.7.0 (v1.7.1, 202 unit / 186 instrumentation, both 100%):
- **Added**: `InputValidatorTest.kt` (unit, 18 cases) — first dedicated suite for `InputValidator`. Google-redirect acceptance (plain nested URL, %-encoded destination, no-www, regional `google.co.uk`, nested `www.` destination, encoded-space destination), exemption integrity (whitespace-separated URL pairs still rejected — even when the first URL is a Google redirect; glued URLs still rejected; nested URL on a non-Google host still rejected), and baseline behaviour (plain/tracking/Google-Search URLs accepted, encoded control chars rejected, raw control chars + zero-width chars stripped, encoded-dot attack rejected, overlong input rejected).
- **Updated**: `UrlProcessorTest.kt` — new `google url wrapper cannot smuggle extra urls` case: `GoogleSearchCleaner` extracts only the single `url=`/`q=` destination, so URLs smuggled into other wrapper params are dropped (this property is what makes the validator exemption safe).

Test-suite delta vs v1.6.0 (v1.7.0, 183 unit / 186 instrumentation, both 100%):
- **Added**: `TikTokProxySelectionTest.kt` (unit, 28 cases) — forward conversion tiktok.com → each of the four fixed proxies with host-prefix preservation (`www.`/`vm.`/`vt.`/`m.` kept in both directions), cross-proxy swaps, legacy `vxtiktok.com`/`tiktxk.com` auto-migration (toggle ON → active proxy, OFF → tiktok.com), no-op when already on target, backward conversion for all known proxies, dirty-URL cleaning + conversion, `isTikTokUrl` detection incl. the kktiktok/vxtiktok "contains tiktok.com" substring edge.
- **Added**: `CustomTikTokProxyTest.kt` (unit, 15 cases) — `TikTokProxyStore` state (set/reset/active/allKnown), normalization/format validation (delegated to the shared logic), reserved-domain rejection in both containment directions incl. **cross-platform** reservations (TikTok store rejects Instagram/Twitter/Facebook families; `InstagramProxyStore` now rejects TikTok domains), duplicate detection, conversions with a custom proxy (forward, reverse, cross-swap with fixed proxies, detection only while registered), `TikTokCleaner` matching + tracking-param stripping on custom proxy links.
- **Added**: `TikTokProxyPreferenceTest.kt` (instrumentation, 15 cases) — default is `tnktok.com`; all four fixed proxies persist; invalid stored value falls back to default; legacy `vxtiktok.com`/`tiktxk.com` stored values migrate to default; custom proxy add/duplicate/select/remove with store sync and fallback-on-delete; custom list survives PreferencesManager recreation; TikTok and Instagram custom rosters are independent.
- **Updated**: `BidirectionalConversionTest.kt` — new TikTok section (6 scenarios): tiktok.com → tnktok.com with `www.` kept, tnktok.com → tiktok.com reversion (toggle OFF), `vm.` short-link conversion, legacy vxtiktok migration, dirty URL cleanup + conversion, "Nothing to do!" when toggle OFF on a clean tiktok.com link.
- **Updated**: `UrlProcessorMatrixTest.kt` — 12 TikTok rows (toggle ON/OFF × clean/dirty × tiktok.com/proxy/legacy, vm. prefix row); `UrlProcessorTest.kt` — 4 TikTok cases adapted from community PR #5.

Test-suite delta vs v1.5.1 (v1.6.0):
- **Added**: `CustomInstagramProxyTest.kt` (unit, 18 cases) — `InstagramProxyStore` state (set/reset/active/allKnown), input normalization (protocol/`www.`/path/query/fragment stripping), hostname-format validation, reserved-domain rejection (both containment directions), duplicate detection, and `UrlProcessor` conversions with a custom proxy (forward, reverse, cross-swap with fixed proxies, no-op, detection, `InstagramCleaner` matching + `igshid` stripping).
- **Added**: `CustomProxyDialogTest.kt` (instrumentation, 5 cases) — fixed roster + "Add custom proxy…" row present in the chooser; selecting `kkinstagram.com` updates the label; invalid input shows the inline error and keeps the input dialog open; reserved domains rejected; full add → select → delete flow including fallback to the default proxy after deleting the selected custom proxy.
- **Updated**: `InstagramProxyPreferenceTest.kt` — `kkinstagram.com` stored value now *stays valid* (test inverted vs v1.4.8; `eeinstagram.com` still migrates); new custom-proxy cases (add persists + syncs store, duplicate add is a no-op, custom selectable, unknown domain not selectable, remove syncs store, removing the selected proxy falls back to default, custom list survives PreferencesManager recreation).
- **Updated**: `InstagramProxySelectionTest.kt` — kk is an active Backup target (forward conversion, no-op, cross-swap, revert), ee remains the only legacy proxy; `InstagramProxyStore.reset()` in `@Before`/`@After` (the store is global state — every test touching it must reset).
- **Updated**: `UrlProcessorMatrixTest.kt` — kk rows reclassified as active backup, ee legacy rows added, new `vxtwitter.com` rows (→ fixupx ON / → x.com OFF) and Facebook rows including `fb.com` (v1.6.0 fix) and `m.facebook.com`.
- **Updated**: all unit test setups constructing `CleanerService` — the unused `PreferencesManager` constructor parameter was removed.

v1.4.8 refreshed the Instagram proxy roster and introduced bare-hostname conversion; v1.6.0's active roster is `toinstagram.com`, `adamlikes.men` (Primary) + `instagram7.com`, `kkinstagram.com` (Backup) + custom proxies. `eeinstagram.com` remains detected for auto-migration.

## v1.4.9 Test Stabilization
- [x] `SettingsTest` conversion-defaults tests now use deterministic `NestedScrollView` scrolling instead of flaky Espresso `scrollTo()`.
- [x] `BrowserModeTest.testActionModeSelection` checks effective visibility state for the priority section instead of requiring the section to be inside the current viewport.
- [x] `ApiCompatibilityTest` closes `ActivityScenario` instances deterministically to avoid emulator/process lifecycle crashes.
- [x] Full `connectedAndroidTest` completed with 0 failures on `Pixel_API_35_Play`.

## Instagram Proxy Selection Tests (v1.4.8, extended v1.6.0)

### InstagramProxyPreferenceTest.kt (instrumentation)
- [x] Default proxy is `toinstagram.com` (= `Constants.INSTAGRAM_DEFAULT_PROXY`)
- [x] Setting toinstagram.com persists
- [x] Setting adamlikes.men persists
- [x] Setting instagram7.com persists
- [x] Setting kkinstagram.com persists (active again since v1.6.0)
- [x] Invalid stored value falls back to default
- [x] `kkinstagram.com` stored value stays valid (v1.6.0 — inverted from the old migration test)
- [x] Legacy `eeinstagram.com` stored value migrates to default
- [x] Custom proxy: add persists + syncs `InstagramProxyStore`
- [x] Custom proxy: duplicate add is a no-op
- [x] Custom proxy: can be selected; unknown domains cannot
- [x] Custom proxy: remove syncs store; removing the selected proxy falls back to default
- [x] Custom proxies survive PreferencesManager recreation (app-restart simulation)

### CustomProxyDialogTest.kt (instrumentation, v1.6.0)
- [x] Chooser lists all fixed proxies (incl. kkinstagram.com) + "Add custom proxy…" row
- [x] Selecting kkinstagram.com updates the "Active:" label
- [x] Invalid domain shows inline error; input dialog stays open
- [x] Reserved domain (e.g. fixupx.com) rejected with inline error
- [x] Add → select → delete custom proxy flow; deletion of the selected proxy falls back to the default

### CustomInstagramProxyTest.kt (unit, v1.6.0)
- [x] Store state: set/get/reset, `activeProxies()` = fixed + custom, `allKnownProxies()` adds legacy
- [x] Normalization: strips protocol, `www.`, path/query/fragment, whitespace, uppercase
- [x] Format validation: valid hostnames/subdomains accepted; spaces, missing TLD, illegal chars, >253 chars rejected
- [x] Reserved domains rejected in both containment directions (instagram/twitter/facebook families)
- [x] Duplicate detection against current custom list
- [x] Conversions: instagram.com → custom, custom → instagram.com (toggle OFF), custom ↔ fixed cross-swaps, no-op when already on target
- [x] Detection: `isInstagramUrl` recognizes custom proxy only while registered; `InstagramCleaner` matches + strips `igshid`

### MainActivityProxyLabelTest.kt (instrumentation)
- [x] Instagram URL shows "Active: toinstagram.com." by default
- [x] Changing pref to adamlikes.men updates label on relaunch
- [x] Changing pref to instagram7 updates label on relaunch
- [x] Facebook URL keeps toggle container visible but hides proxy row
- [x] **v1.5.1**: Clicking "Change." opens the inline dialog (not Settings) and updates the label in place; MainActivity stays in the foreground; Processed URL field stays empty when no Process tap preceded
- [x] **v1.5.1**: When a Processed URL already exists for an Instagram input, picking a different proxy auto-refreshes the Processed URL field with the new proxy (parity with Share)

### ShareActivityProxyLabelTest.kt (instrumentation)
- [x] Instagram share shows "Active: toinstagram.com." by default
- [x] Share uses adamlikes.men when preference is set
- [x] Clicking "Change." opens inline dialog (not Settings) and updates the label in place

### InstagramProxySelectionTest.kt (unit)
Forward / reverse / cross-proxy / no-op / dirty / `www.` stripping / sub-prefix stripping / legacy auto-migration / `isInstagramUrl` coverage. All assertions explicit via `Constants.*_DOMAIN` references — no string literals duplicated across tests.

## Prior Coverage (unchanged)

## Running the Tests

To run all Android instrumentation tests, use:
```bash
# With emulator running (Pixel_API_35_Play)
./gradlew connectedAndroidTest

# Or specifically for debug variant
./gradlew connectedDebugAndroidTest
```

## Test Files and Coverage

### 1. **HistoryDatabaseTest.kt** [x] ACTIVE
Tests the Room database functionality for URL history:
- [x] Insert and retrieve history entries
- [x] Multiple entries with proper ordering
- [x] Delete individual entries by ID
- [x] Delete all history
- [x] Trim history to keep only newest entries
- [x] Facebook prefix removal verification

### 2. **ShareActivityTest.kt** [x] ACTIVE
Tests the Share Activity functionality:
- [x] Instagram URL conversion (toggle ON/OFF)
- [x] Twitter/X URL conversion (toggle ON/OFF)
- [x] Facebook URL conversion with prefix removal
- [x] "Nothing to do" message for already clean URLs
- [x] Dirty URL cleaning while preserving domain preference
- [x] Share and Copy button functionality
- [x] Toggle visibility based on URL type
- [x] Multiple URLs rejection
- [x] Invalid URL handling
- [x] History recording on share

### 3. **BidirectionalConversionTest.kt** [x] ACTIVE
Comprehensive tests for bidirectional URL conversions:
- [x] **Instagram**: clean/dirty × instagram.com / active proxy / legacy proxy × toggle ON/OFF (v1.4.8)
- [x] **Twitter/X**: clean/dirty × x.com/fixupx.com × toggle ON/OFF
- [x] **Facebook**: clean/dirty × facebook.com/facebookez.com × toggle ON/OFF
- [x] **TikTok** (v1.7.0): clean/dirty × tiktok.com / tnktok.com / legacy vxtiktok.com × toggle ON/OFF, `www.`/`vm.` prefix preservation
- [x] **"Nothing to do"** scenarios for all platforms
- [x] **Edge cases**: mixed case, fragments, fxtwitter.com
- [x] **All prefix removals**: m., web., www. for Facebook

### 4. **MainActivityHistoryTest.kt** [x] ACTIVE
Tests the History dialog and related UI:
- [x] History button visibility
- [x] History dialog opening
- [x] RecyclerView display with entries
- [x] Long press to delete entries
- [x] Clear all history functionality
- [x] Copy/Share buttons in history items
- [x] Donate button in footer
- [x] History dialog dismissal
- [x] URL validation with history feature

### 5. **UrlValidationImprovementsTest.kt** [x] ACTIVE
Tests the improved URL validation logic:
- [x] Facebook story.php URLs not falsely rejected
- [x] URLs with multiple query parameters accepted
- [x] URLs with dots in query parameters accepted
- [x] Actual multiple URLs still rejected
- [x] Glued URLs without space rejected
- [x] URLs with file extensions accepted
- [x] URLs with port numbers accepted
- [x] Alternative domains (facebookez, fixupx, toinstagram, adamlikes, instagram7, legacy kkinstagram for migration) recognized

### 6. **SettingsTest.kt** [x] ACTIVE
All tests now active:
- [x] About dialog display
- [x] History toggle in settings dialog
- [x] Max history entries dialog and validation  
- [x] Back navigation (close dialog)
- [x] Max entries validation (min/max values)
- [x] History disabled stops recording

### 7. **UrlInputValidationTest.kt** [x] ACTIVE
Tests URL input validation:
- [x] Glued URLs rejection
- [x] Zero-width space attack prevention
- [x] URL-encoded dot attack prevention
- [x] Control character attack prevention
- [x] Valid URL acceptance
- [x] Multiple protocols rejection
- [x] Unicode normalization handling
- [x] Process button functionality
- [x] Empty input handling

### 8. **BrowserModeTest.kt** [x] ACTIVE
Tests browser integration functionality:
- [x] Browser mode toggle functionality
- [x] Action mode selection (Ask vs Priority)
- [x] Action priority configuration
- [x] Browser mode independent from main toggles
- [x] Conversion defaults handling
- [x] VIEW intent handling
- [x] Menu items order
- [x] Settings activity browser mode toggle

### 9. **AccessibilityTest.kt** [x] ACTIVE
Tests accessibility compliance:
- [x] Content descriptions for all buttons
- [x] Main activity accessibility
- [x] Color contrast verification

### 10. **ResponsiveDesignTest.kt** [x] ACTIVE
Tests responsive design implementation:
- [x] Portrait orientation layout
- [x] Landscape orientation layout
- [x] Orientation changes
- [x] Small screen size adaptation

### 11. **TouchTargetTest.kt** [x] ACTIVE
Tests touch target compliance:
- [x] Button touch targets meet 48dp minimum
- [x] Clickable text views accessibility

### 12. **KeyboardNavigationTest.kt** [x] ACTIVE
Tests keyboard navigation:
- [x] Input field keyboard handling
- [x] Paste button functionality
- [x] Navigation flow between elements
- [x] Empty state validation

### 13. **OfflinePerformanceTest.kt** [x] ACTIVE
Tests performance and offline functionality:
- [x] Offline functionality verification
- [x] App startup time testing
- [x] URL processing performance
- [x] Memory usage testing
- [x] History performance testing

### 14. **ApiCompatibilityTest.kt** [x] ACTIVE
Tests API compatibility:
- [x] Current API level compatibility
- [x] Minimum SDK compatibility
- [x] Configuration changes handling
- [x] Material Design components
- [x] Theme compatibility

### 15. **ReleaseTestSuite.kt** [x] ACTIVE
Tests release build functionality:
- [x] App launches successfully
- [x] Core URL processing
- [x] Release configuration
- [x] All platform conversions
- [x] History feature
- [x] Copy/Share buttons

### 16. **SmartFooterTest.kt** [x] ACTIVE
Tests smart footer functionality:
- [x] Footer visibility
- [x] Footer content verification
- [x] Footer clickability
- [x] Scroll view positioning

### 17. **ShareActivityNoDuplicatesTest.kt** [x] ACTIVE
Tests duplicate prevention in share activity:
- [x] Share activity launches and processes URL
- [x] Instagram URL processed without duplicates
- [x] Facebook URL processed without duplicates
- [x] Toggle changes don't cause errors

## Test Dependencies Added
```kotlin
androidTestImplementation("androidx.test.espresso:espresso-contrib:3.5.1") // RecyclerView
androidTestImplementation("androidx.test.ext:junit:1.1.5")
androidTestImplementation("androidx.test:runner:1.5.2")
androidTestImplementation("androidx.room:room-testing:2.6.1") // Room testing
```

## Features Covered

### History Feature [x]
- Room database operations
- History dialog UI
- Long press to delete
- Clear all functionality
- Max entries enforcement (via trimHistory)

### ShareActivity [x]
- All toggle combinations
- URL reprocessing on toggle change
- "Nothing to do" logic
- History integration
- Facebook prefix removal

### URL Validation Improvements [x]
- Query parameter handling
- Domain-only dot counting
- Alternative domain support

### Bidirectional URL Conversions [x] (COMPLETE)
All possible combinations tested:
- Clean → Converted (toggle ON)
- Converted → Original (toggle OFF)
- Dirty → Clean + Converted
- Already correct → "Nothing to do"

### Settings [x]
- History toggle functionality
- History disabled stops recording
- Max entries dialog and validation
- About dialog display
- Back navigation from dialogs

### Facebook Conversion [x]
- Bidirectional conversion
- Prefix removal (m., web., www.)
- Clean URL preservation

## Running Individual Test Classes

```bash
# Run specific test class
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.fixupxer.HistoryDatabaseTest

# Run with specific test method
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.fixupxer.ShareActivityTest#testFacebookUrlConversionWithToggleOn

# Run the new bidirectional tests
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.fixupxer.BidirectionalConversionTest
```

## Current Test Status
- **Unit Tests**: 211/211 passing
- **Instrumentation Tests**: 186/186 passing (v1.7.2 run on `Pixel_API_35_Play`, ~7m 32s)
- **Test Success Rate**: 100%

## URL Conversion Test Matrix

(Merged from the former `URL_CONVERSION_TEST_COVERAGE.md`; covered primarily by `UrlProcessorMatrixTest.kt`, `BidirectionalConversionTest.kt`, `InstagramProxySelectionTest.kt`, `UrlProcessorTest.kt`, `UrlValidationImprovementsTest.kt`.)

### Instagram Conversions (active + custom + legacy proxy set)
| Original URL | Toggle / Proxy | Expected Result | Test Status |
|-------------|----------------|-----------------|-------------|
| instagram.com | ON + toinstagram | toinstagram.com (default, no www.) | [x] Tested |
| instagram.com | ON + adamlikes | adamlikes.men (no www.) | [x] Tested |
| instagram.com | ON + instagram7 | instagram7.com | [x] Tested |
| instagram.com | ON + kkinstagram | kkinstagram.com (active Backup, v1.6.0) | [x] Tested |
| instagram.com | ON + custom proxy | custom proxy domain | [x] Tested |
| instagram.com + tracking | ON + any proxy | selected proxy (clean) | [x] Tested |
| toinstagram.com | OFF | instagram.com | [x] Tested |
| adamlikes.men | OFF | instagram.com | [x] Tested |
| instagram7.com | OFF | instagram.com | [x] Tested |
| kkinstagram.com | OFF | instagram.com | [x] Tested |
| custom proxy | OFF | instagram.com | [x] Tested |
| kkinstagram.com | ON + other proxy | selected proxy (cross-swap; kk active since v1.6.0) | [x] Tested |
| eeinstagram.com | ON + active proxy | selected active proxy (legacy migration) | [x] Tested |
| toinstagram.com | ON + adamlikes | adamlikes.men (cross-swap) | [x] Tested |
| adamlikes.men | ON + instagram7 | instagram7.com (cross-swap) | [x] Tested |
| instagram7.com | ON + toinstagram | toinstagram.com (cross-swap) | [x] Tested |
| custom proxy | ON + fixed proxy | fixed proxy (cross-swap) | [x] Tested |
| fixed proxy | ON + custom proxy | custom proxy (cross-swap) | [x] Tested |
| toinstagram.com | ON + toinstagram | unchanged (no-op) | [x] Tested |
| adamlikes.men | ON + adamlikes | unchanged (no-op) | [x] Tested |
| instagram7.com | ON + instagram7 | unchanged (no-op) | [x] Tested |
| kkinstagram.com | ON + kkinstagram | unchanged (no-op) | [x] Tested |
| custom proxy | ON + same custom | unchanged (no-op) | [x] Tested |
| www.instagram.com | ON + any proxy | selected proxy, bare hostname (www. stripped) | [x] Tested |
| business.instagram.com | ON + any proxy | selected proxy, bare hostname (sub-prefix stripped) | [x] Tested |

### Twitter/X Conversions
| Original URL | Toggle State | Expected Result | Test Status |
|-------------|--------------|-----------------|-------------|
| x.com | ON | fixupx.com | [x] Tested |
| twitter.com | ON | fixupx.com | [x] Tested |
| x.com + tracking | ON | fixupx.com (clean) | [x] Tested |
| twitter.com + tracking | ON | fixupx.com (clean) | [x] Tested |
| fixupx.com | OFF | x.com | [x] Tested |
| fixupx.com + tracking | OFF | x.com (clean) | [x] Tested |
| fxtwitter.com | ON | fixupx.com | [x] Tested |
| fxtwitter.com | OFF | x.com | [x] Tested |
| vxtwitter.com | ON | fixupx.com (v1.6.0 fix) | [x] Tested |
| vxtwitter.com | OFF | x.com (v1.6.0 fix) | [x] Tested |

### Facebook Conversions
| Original URL | Toggle State | Expected Result | Test Status |
|-------------|--------------|-----------------|-------------|
| facebook.com | ON | facebookez.com | [x] Tested |
| fb.com | ON | facebookez.com (v1.6.0 fix — short domain now converts) | [x] Tested |
| m.facebook.com | ON | facebookez.com (prefix removed) | [x] Tested |
| web.facebook.com | ON | facebookez.com (prefix removed) | [x] Tested |
| www.facebook.com | ON | facebookez.com (prefix removed) | [x] Tested |
| facebook.com + tracking | ON | facebookez.com (clean) | [x] Tested |
| facebookez.com | OFF | facebook.com | [x] Tested |
| facebookez.com + tracking | OFF | facebook.com (clean) | [x] Tested |

### TikTok Conversions (v1.7.0 — active + custom + legacy proxy set)
| Original URL | Toggle / Proxy | Expected Result | Test Status |
|-------------|----------------|-----------------|-------------|
| tiktok.com | ON + tnktok | tnktok.com (default) | [x] Tested |
| tiktok.com | ON + tfxktok | tfxktok.com | [x] Tested |
| tiktok.com | ON + tiktokez | tiktokez.com | [x] Tested |
| tiktok.com | ON + kktiktok | kktiktok.com | [x] Tested |
| tiktok.com | ON + custom proxy | custom proxy domain | [x] Tested |
| tiktok.com + tracking | ON + any proxy | selected proxy (clean) | [x] Tested |
| www.tiktok.com | ON + any proxy | www.<proxy> (prefix KEPT) | [x] Tested |
| vm.tiktok.com / vt.tiktok.com / m.tiktok.com | ON + any proxy | same prefix + <proxy> | [x] Tested |
| tnktok.com / tfxktok.com / tiktokez.com / kktiktok.com | OFF | tiktok.com (prefix kept) | [x] Tested |
| custom proxy | OFF | tiktok.com | [x] Tested |
| active proxy | ON + other proxy | selected proxy (cross-swap) | [x] Tested |
| vxtiktok.com / tiktxk.com | ON + active proxy | selected active proxy (legacy migration) | [x] Tested |
| vxtiktok.com / tiktxk.com | OFF | tiktok.com (legacy revert) | [x] Tested |
| target proxy | ON + same proxy | unchanged (no-op) | [x] Tested |
| kktiktok.com (contains "tiktok.com") | any | detected as proxy, not plain tiktok.com | [x] Tested |

### URL Validation Edge Cases
1. **Facebook Story URLs** — no false positive "Multiple URLs detected" (`https://m.facebook.com/story.php?story_fbid=123`) [x]
2. **Case Sensitivity** — post IDs preserve case (e.g. `DLRNJjEx45S`) [x]
3. **Glued URL Detection** — legitimate URLs with dots not flagged [x]
4. **Query Parameters** — complex queries handled correctly [x]
5. **Google/Gmail Redirect Extraction** — nested destination URLs accepted and cleaned (`https://www.google.com/url?q=...`) [x]

## Notes
- Tests require Android emulator or device; final v1.4.9 verification used `Pixel_API_35_Play`.
- Emulator animations should be disabled for stable Espresso runs (`window_animation_scale`, `transition_animation_scale`, `animator_duration_scale` = `0`).
- Wait times in tests account for async UI processing where idling resources are not available.
- SettingsActivity tests are active and passing for browser conversion defaults. Instagram proxy selection coverage moved to `MainActivityProxyLabelTest` + `ShareActivityProxyLabelTest` after v1.5.1 removed the Settings entry.
- All core v1.4.9 browser-mode fixes and v1.4.8 Instagram proxy behavior are covered by active tests.
- Bidirectional URL conversions are comprehensively tested.
