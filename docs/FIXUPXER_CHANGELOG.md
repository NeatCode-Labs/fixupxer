# FixupXer App - Development Summary
## Version Progression: v2.5.1 → v1.2.1 (Latest to Oldest)

**Total Versions Released:** 35 (v2.5.1 through v1.2.1)
**Current Version:** v2.5.1 (versionCode: 41)
**Development Period:** v1.2.1 (Initial) → v2.5.1 (Current)

---

## 🎯 Executive Summary

This document summarizes all modifications made to the FixupXer Android app since v1.2.1, culminating in v2.5.1: selective host-bound cleaning across 26 domain cleaners plus a universal cleaner, Private Link Guard, curated offline redirect unwrapping, social embed conversion with a vetted frontend catalog reachable from Settings, Browser-mode privacy readers with saved per-host app choices, local settings backup/restore, Process Text, and a tested no-code custom-rule engine — all while retaining the zero-permission offline model.

### Key Achievements:
- ✅ **Frontend Safety & Settings Access** - Retired compromised frontend domains (facebookez.com, kkinstagram.com) with automatic settings/backup migration and a permanent denylist; every platform's frontend picker reachable from Settings > Alternative frontends
- ✅ **Browser Privacy Readers & Saved App Choices** - Dedicated Browser mode settings hub with per-platform privacy reader conversions (X, Bluesky, Reddit, Pinterest), exact-host saved app routing, and a read-only Configuration status overview
- ✅ **Alternative Frontend Catalog** - Embed/Privacy frontend picker on Main/Share across nine platforms with selectable readers (xcancel, Nitter, SkyLib, Redlib, Invidious, …) and custom domains for every platform
- ✅ **Local Settings Backup** - Validated JSON export/restore of settings, custom rules, and saved app choices with atomic apply, automatic rollback, and crash-safe recovery
- ✅ **Private Link Guard** - Offline detection of credentials, e-mails, JWT/auth tokens and precise coordinates left in links; ephemeral (no-history, no-cache) processing of sensitive URLs
- ✅ **Keep-Unknown Cleaning Contract** - Only known tracking keys are removed; unknown functional parameters survive, host-boundary matching kills lookalike-domain false positives
- ✅ **Custom URL Rule Engine** - Ordered scopes/actions/phases/contexts, excludes, keep-only, redirects, templates, Test Lab, test vectors with activation gate, Teach-from-example inference and portable bundles
- ✅ **TikTok Conversion Support** - Dedicated Embed? toggle + full proxy picker (tnktok.com, tfxktok.com, tiktokez.com, kktiktok.com + custom), subdomain-preserving conversion
- ✅ **Custom Instagram Proxies** - Users can add/select/delete their own embed proxy domains, validated and persisted locally
- ✅ **Browser Mode Integration** - Optional system-wide URL filtering as default browser with configurable action priorities
- ✅ **Selectable Instagram Embed Proxy** - User-chosen, persistent proxy for Instagram embeds with cross-proxy swap, legacy auto-migration, and bare-hostname conversion
- ✅ **Cross-OEM Default-Browser Support** - `MAIN + APP_BROWSER` filter so Xiaomi/Redmi/HyperOS (and other non-AOSP pickers) list FixupXer as a browser candidate
- ✅ **Professional UI/UX** - Polished Material Design 3 interface with perfect text formatting and typography
- ✅ **Selective Cleaner Catalog** - 26 host-bound domain cleaners plus one universal cleaner preserve unknown and functional parameters
- ✅ **Offline Redirect Unwrapping** - Curated HTTP(S) target extraction with exact host/path checks, strict single decoding and multi-pass destination cleaning
- ✅ **Efficient Dispatch** - O(1) domain dispatch and bounded smart caching
- ✅ **International Support** - Full IDN support and zero-width character handling
- ✅ **Comprehensive Verification** - 877 automated tests plus release lint, REUSE and reproducible-build checks
- ✅ **Thread-Safe Design** - Immutable processing snapshots and concurrency-safe state
- ✅ **Security Hardening** - Comprehensive protection against malicious input attacks
- ✅ **Professional Architecture** - Clean, maintainable, and extensible codebase

---

## 📋 Version History

### v2.5.0 → v2.5.1
- **Focus:** Google Play compliance release — raise `targetSdk`/`compileSdk` to Android 16 (API 36) before the Aug 31, 2026 deadline; no user-facing feature or behavior changes.
- **Target API:** `compileSdk` and `targetSdk` bumped 35 → 36; `tools:targetApi` in the manifest updated to match.
- **Build tooling:** Android Gradle Plugin 8.3.2 → 8.9.3 (official API 36 support line); removed obsolete `android.suppressUnsupportedCompileSdk=35` from `gradle.properties`. Gradle wrapper 8.11.1 and JDK 17 unchanged.
- **Tests:** new `robolectric.properties` pins default emulated SDK to 35 because Robolectric 4.14.1 has no SDK 36 runtime (upgrade deferred).
- **Runtime:** edge-to-edge (`enableEdgeToEdge` in `BaseActivity`) and predictive back (`enableOnBackInvokedCallback` + `OnBackInvokedCallback`) were already in place — no Kotlin changes required.

### v2.4.1 → v2.5.0
- **Focus:** Frontend safety release — two bundled frontend domains retired after a security review (community report on Mastodon/PieFed), plus a new Settings screen that makes every platform's frontend picker reachable without pasting or sharing a link.
- **Retired frontends:** `facebookez.com` (redirects to an advertising network; blocked by Quad9/security DNS and ad-block lists) and `kkinstagram.com` (flagged by multiple reputation services) removed from `AlternativeFrontendCatalog`; Facebook now ships with no built-in target (`defaultTargetId` nullable). Both domains live in `Constants.RETIRED_UNSAFE_FRONTEND_DOMAINS` — a denylist enforced in the picker UI, `PreferencesManager.addCustomProxy`, `ProxyRoster` reserved domains, and backup validation, so they cannot return as custom frontends. Retired domains intentionally keep receiving generic tracking cleaning (treated as ordinary unknown hosts) but are no longer generated, detected as platform URLs, or offered anywhere.
- **Migration:** new `utils/RetiredFrontendMigration` shared by `PreferencesManager` (init, idempotent) and `LocalBackupCodec` (snapshot decode before validation): kkinstagram selections move to the first active Instagram proxy, retired custom-proxy CSV entries are purged, retired disabled-built-in markers dropped, and `convert_facebook` is forced off only when a retired facebookez selection was actually removed with no custom Facebook proxies remaining.
- **Alternative frontends in Settings:** new `FrontendSettingsActivity` under Settings > Link processing lists all nine platforms with live frontend/conversion state (`FrontendDisplayHelper`) and opens the existing `ProxyPickerDialogHelper` per platform. `PlatformToggleHelper` gains an empty-state: platforms with zero active frontends show a neutral title with the switch off and disabled.
- **Verification:** 642/642 unit + 235/235 instrumentation tests on `Pixel_API_35_Play`; `lintRelease` clean; zero-permission manifest unchanged. Four-stage subagent review (implementation + two read-only reviews + fixer) with final main-agent verification; emulator environment issues (wipe-data GMS churn, kernel-time storm) diagnosed and excluded from code findings.

### v2.4.0 → v2.4.1
- **Focus:** Fast patch release — one privacy fix and three reliability fixes found in a post-v2.4.0 audit, plus the Play-side fix for the intermittent PairIP "Something went wrong" dialog on shares.
- **Privacy — fragment leak:** `PlatformDomainConverter.extractUrlSuffix` no longer promotes a `?` that lives inside a URL fragment into a real query. Previously `…#client?access_token=…` produced a genuine `?access_token=…` query sent to reader/embed frontends during conversion. The fix covers all conversion paths (X readers forward/reverse, Reddit host swaps, youtu.be, Farside, generic host swaps).
- **Open self-interception:** `UrlActionHelper.openUrl` resolves the implicit `ACTION_VIEW` first; only when it would land back in FixupXer (app set as default browser) it delegates to the external-browser chooser. Native App Links keep working unchanged.
- **Facebook retarget:** `convertToFacebookTarget` now recognizes URLs already on any known Facebook frontend (`ProxyRoster.allKnownDomains`) and swaps them to the newly selected built-in/custom target — previously Facebook was the only platform without frontend-to-frontend retargeting.
- **Browser VIEW dedup:** Browser-mode processing moved into `MainViewModel.viewModelScope` with an in-flight cache keyed by URL, and the completed transaction (original/processed URL + routing host) is stored in `SavedStateHandle`. Activity recreation (rotation, uiMode change, process death) replays the post-clean step from the stored result instead of re-processing and re-inserting a duplicate history row; new VIEW intents invalidate the stored transaction. `BrowserViewGate` checks stay active on the replay path.
- **Play-side:** the intermittent PairIP/`LicenseActivity` "Something went wrong" dialog on shares was traced to Google Play Automatic protection's installer check (not app code); the installer check was disabled in Play Console.
- **Verification:** 628/628 unit + 229/229 instrumentation tests on `Pixel_API_35_Play`; `lintRelease` clean (0 errors); zero-permission manifest unchanged. Four-stage subagent review (implementation + two read-only reviews + fixer) with final main-agent verification.

### v2.3.0 → v2.4.0
- **Focus:** Browser-mode maturity release — dedicated Browser mode settings hub, alternative reader frontends across nine platforms on Main/Share, privacy-reader conversions decoupled from embed toggles, exact-host saved app choices, read-only Configuration status, local settings backup/restore with crash-safe rollback, and handed action layouts.
- **Browser mode hub:** New `BrowserSettingsActivity` gathers **Enable Browser mode**, **After processing an opened link** (Ask what to do / Try actions automatically), the reorderable **Action order**, **Saved app choices**, **Configure privacy readers**, and Browser status. Main Settings shows a compact status line via new `BrowserSettingsState`/`BrowserStatusTextHelper` resolvers. Browser alias changes are transactional (`BrowserModeUtils.updateBrowserMode` with verify + rollback), and `reconcileBrowserAlias` aligns the alias with preferences at every app start.
- **Alternative frontends on Main/Share:** New `utils/AlternativeFrontendCatalog` (EMBED / READER / AUTOMATIC / EXPERIMENTAL roles) + `utils/ProxyRoster` power a unified per-platform conversion row and full picker on Main/Share for nine platforms (X, Instagram, TikTok, Facebook, Bluesky, Reddit, YouTube, Pinterest, Threads). X/Bluesky gain selectable readers (xcancel, nitter.net, community Nitter instances, twiiit/farside automatic pickers, SkyLib); Reddit/YouTube/Pinterest/Threads gain new reader conversions (Redlib roster incl. SafeReddit, Invidious, pinterest.bunk.im, Shoelace — off by default); custom domains supported on every platform via `ProxyRoster`. Conversion executed by `processing/PlatformDomainConverter`.
- **Privacy readers:** `ProcessingProfile.BROWSER` converts only platforms with built-in READER targets (X/Twitter, Bluesky, Reddit, Pinterest) through per-platform `browser_privacy_target_<platform>` preferences and a saved-if-active → first-active → cleaning-only resolver; decision logic centralized in `processing/BrowserConversionPolicy`. Old inert browser embed toggles removed. Full proxy picker gains Embed/Privacy section headers and a selection-only mode with draft Save/Cancel semantics; **Restore built-in readers** revives platforms whose Readers were all removed.
- **Saved app choices:** With **Ask what to do**, "Always use app for this host" persists a per-exact-host route (`RememberedRoute`, capped at 100) checked before the action picker; invalid, disabled, or incompatible choices self-heal by deletion with a one-time fallback to the picker. Management UI lists and deletes saved choices; routes stay saved but inactive outside Browser mode + Ask.
- **Configuration status:** Read-only dialog summarizes cleaning, Custom rules (enabled/disabled counts via `SettingsStatusResolver`), Browser alias, default-browser tri-state (`getDefaultBrowserStatus` incl. *Unable to verify*), active privacy readers (none/active/mixed/broken), after-clean behavior, and App Links caveat.
- **Local backup:** `LocalBackupCodec`/`LocalBackupManager` export a versioned JSON (settings snapshot, custom rules bundle, saved app choices) through SAF; restore validates everything up front (`SettingsSnapshotValidator`), applies under a mutex with `NonCancellable`, rolls back settings/rules/alias on failure, and persists an fsynced `restore_rollback.json` marker so a process death mid-restore is recovered at next launch. History entries are never exported; retained entries are trimmed to the restored limit. Deterministic post-restore theme application without timers (CAS acknowledgement in `SettingsBackupViewModel`).
- **History limits:** Supported range 1–10,000 enforced (`MIN/MAX_HISTORY_ENTRIES`); out-of-range legacy limits get a guided migration prefill in the dialog while pending state trims to the raw legacy limit, so history never grows unbounded and never silently loses entries.
- **Hardening:** Tracking cleaning became a non-optional invariant (preference forced true, repository toggle API removed). New `BrowserViewGate` revision/pause gate invalidates in-flight Browser VIEW work on any routing-relevant settings change (browser mode, action mode/priority, privacy targets, saved routes, custom-rules master switch); `MainActivity` serializes rapid consecutive VIEW intents and dismisses stale Ask dialogs. Manifest `<queries>` expanded for Android 11+ package visibility. Handed action layouts (`DominantHandLayoutHelper`) mirror action buttons for left/right-handed use.
- **Verification:** 607/607 unit + 228/228 instrumentation tests on `Pixel_API_35_Play`; `lintRelease` clean; zero-permission manifest unchanged. Four-stage subagent review (implementation + two read-only reviews + fixer) with final main-agent verification.

### v2.2.0 → v2.3.0
- **Focus:** Expand useful international cleaning and safe affiliate-wrapper extraction without adopting whole-query deletion or adding permissions.
- **Bilibili:** New host-bound catalog rule removes only `vd_source`, `seid`, `share_source`, and `copy_link`; generic `from`, unknown keys, duplicate ordering, raw encoding, and fragments remain intact.
- **Yahoo/Guce:** The universal cleaner now removes only the exact case-insensitive keys `guccounter`, `guce_referrer`, and `guce_referrer_sig`; near-match names remain untouched.
- **Offline wrappers:** GeoRiot/Geniuslink (`target.georiot.com/Proxy.ashx`, `GR_URL`) and LinkSynergy/Rakuten (`click.linksynergy.com/link`, `murl`) join the curated offline unwrapper roster. Both require exact hosts and paths, decode once, validate the complete HTTP(S) target, and then pass the destination through the existing multi-pass cleaner pipeline.
- **Redirect hardening:** Query-like text after fragments is ignored; endpoint subpaths, invalid ports, malformed percent escapes, boundary whitespace, relative/non-HTTP targets, unsafe duplicate bypasses, and double-encoded targets remain wrapped. Valid target `+`, encoding, query order, and fragments are preserved.
- **Verification:** 380/380 unit + 201/201 instrumentation tests on `Pixel_API_35_Play`; `lintRelease` and REUSE 3.3 clean; zero-permission manifest unchanged.

### v2.1.0 → v2.2.0
- **Focus:** Competitive-audit adoption release — bring the existing scope to its peak (cleaner correctness + catalog breadth) and add two first-of-their-kind features: Private Link Guard and Teach-from-example.
- **Cleaning engine hardening:** `UrlNormalizer` gained static host helpers (`extractAsciiHost`, `hostMatchesDomain`, `urlMatchesDomain`, `urlMatchesAnyDomain`); all cleaners and `UrlProcessor` conversions moved from substring `contains()` to label-boundary host checks anchored to `^https?://`. New **keep-unknown contract**: platform cleaners remove only known tracking keys and preserve unknown/functional parameters and raw encoding. `GeneralTrackingCleaner` narrowed to proven universal trackers. New `CleanerCatalog` as the canonical production cleaner list; explicit `priority`/`displayName` on the `UrlCleaner` interface; frozen master-off differential corpus guards behaviour.
- **Cleaner catalog expansion:** data-driven `ParameterRuleCatalog` + generic `CatalogParameterCleaner` add 14 platforms (Wikipedia, Threads, Twitch, Spotify, Pinterest, Snapchat, WhatsApp, Medium, Bing, DuckDuckGo, Google Store, eBay, Netflix, AliExpress); new `GoogleMapsCleaner` canonicalizes coordinate URLs; Facebook (`sfnsn`, `fb_` prefix, `_rdr`), LinkedIn (`rcm`) and global keys (`mkt_tok`, `sfmc_activityid`, Webtrekk `wt_`, `#Echobox=` fragment) expanded. Provenance of all adopted behaviours audited against Léon in `docs/THIRD_PARTY_PROVENANCE.md`.
- **Offline redirect unwrapping:** new `OfflineRedirectCleaner` (extraction priority) unwraps Facebook `l.php`, LinkedIn `/safety/go`, YouTube `/redirect`, `go.bsky.app`, Google Adservices `pagead/aclk` and Reddit Mail wrappers — strict single percent-decode, HTTP(S) targets only. `CleanerRegistry.getCleanersFor` now accumulates cleaners across the host suffix hierarchy (unwrapper + domain cleaner in one pass).
- **Bluesky conversion:** `bsky.app` ↔ `fxbsky.app` post conversion with its own Embed? toggle on Main/Share and an independent Browser-mode default in Conversion defaults.
- **Process Text:** new translucent `ProcessTextActivity` + ViewModel handle `ACTION_PROCESS_TEXT` — editable single-URL selections are cleaned and replaced inline (`RESULT_OK`); read-only/prose/multi-URL input forwards to the Share preview.
- **Private Link Guard:** new `LinkLeakAnalyzer` detects credentials in userinfo, e-mails (path/query/fragment), JWTs, sensitive token parameters (`access_token`, `otp`, `sig`, …) and precise lat/lon pairs — one-time percent-decode, no raw values in results or logs. Sensitive URLs are processed ephemerally: no history entry, no cache entry (cache keys of the run are evicted on output-only findings). Warning row + dialog with Continue / Back / Remove parameter. All processing logs sanitized (no full URLs in logcat).
- **Custom rules:** per-rule **test vectors** (≤ 20) with isolated `RuleVectorRunner` evaluation and Run-all UI; **activation gate** — a rule can be saved/toggled enabled only while all vectors pass; imports with failing vectors become disabled drafts (existing enabled rules are never auto-disabled). **Teach from example**: `RuleExampleInference` conservatively infers a disabled `RemoveParams`/`ExtractRedirect` draft (exact-host scope, auto test vector) from one before/desired pair; ambiguous examples are rejected with a reason; redundant examples (pipeline already produces the desired output) are reported without creating a rule.
- **UI:** "What's new?" overflow-menu entry (Main + Share) opens the GitHub release notes. (A "What changed?" per-result trace dialog was implemented and later removed at the maintainer's request; the internal `ChangeOperation` trace remains as a test-verification layer.)
- **Verification:** 370/370 unit + 201/201 instrumentation tests on `Pixel_API_35_Play`; `lintRelease` and REUSE 3.3 clean; frozen differential baseline byte-identical across the release; zero-permission manifest enforced by test.

### v2.0.0 → v2.1.0
- **Focus:** Full offline custom URL rule system requested in issue #6, plus the agreed advanced scope.
- **Pipeline:** New raw-preserving validator/extractor/normalizer/orchestrator shared by Main, Share and Browser. Custom phases run before built-ins, after built-ins and after domain conversion; immutable snapshots prevent mid-request rule changes.
- **Rules:** all URLs, exact host, domain+subdomains, host list and RE2/J URL regex scopes; shared excludes; remove-all, remove-selected, keep-only, regex replace, redirect extraction and component-template actions; per-context filtering, stop-after-match and deterministic order.
- **Storage/portability:** Room v2 migration preserves history; versioned JSON bundles, atomic conflict policies, last-three rollback snapshots and Storage Access Framework import/export without storage permissions.
- **UI:** Custom rules are opt-in with one master switch in Settings; the rule library/editor and Conversion History now share the redesigned Material 3 cards, hierarchy and controls. A full-app visual pass also aligns Settings and dialogs, keeps fixed actions clear of lists/system bars, preserves 48dp touch targets, and remains readable at 320dp width with 130% font scaling.
- **Documentation:** Browser integration and Custom URL rules expose **How to Use** links to dedicated GitHub Markdown guides; the embedded Browser instructions dialog was removed.
- **Safety/privacy:** linear-time RE2/J 1.8, strict output validation, raw query preservation, cycle/hop/resource limits, custom rules excluded from automatic backup, zero merged permissions enforced by test.
- **Verification:** frozen master-off differential corpus, generated raw-query corpus, compiler/engine/codec/repository/migration/UI/performance/manifest tests. 273/273 unit + 194/194 instrumentation tests pass; `lintRelease` and REUSE 3.3 lint pass.

### v1.7.2 → v2.0.0
- **Focus:** Complete UI redesign — the largest visual change in the app's history. Main and Share screens rebuilt as a **before/after flow layout** on a hand-tuned Material 3 DayNight theme with full dark mode. All URL-processing logic, proxy systems, browser mode, and privacy guarantees unchanged.
- **Key Changes — layout (`activity_main.xml`, `activity_share.xml`, new includes):**
  - Both screens restructured into a vertical flow: input node (link icon) → original-URL card → `Process URL` CTA (Main only) → result node (check icon) → result card → platform toggle card → action row. Shared markup extracted into `include_processed_url_card.xml` and `include_platform_toggles.xml` (IG/FB/X/TT rows with monogram, Embed toggle, `Active: <proxy>. Change.` sub-row).
  - **New `domain/model/ResultStatus`** + `ui/helpers/ResultStatusHelper` — result card shows a status chip: `Already clean` / `Tracking removed` / `Converted for embedding` / `Tracking removed and converted`; subdomain-normalization and lookalike-proxy edge cases covered by unit tests (`ResultStatusTest`).
  - **New `ui/helpers/UrlDiffHelper`** — tracking params removed by cleaning are struck through in the original URL (Main input + Share card); exact parameter-set comparison, fragment-aware (no substring false positives).
  - **History is a bottom sheet** (`dialog_history.xml` via `BottomSheetDialog`), launched from a pinned `History` extended FAB; entry tap reloads it into the screen, per-entry delete with Snackbar undo; collector cancelled while history is disabled.
  - **New `ui/helpers/SmartFooterHelper`** — footer + FAB re-anchor into the scroll content on small screens so nothing overlaps; `ui/helpers/UrlActionHelper`/`SnackbarHelper` centralize Open/Copy/Share actions and feedback (Toasts → Snackbars).
  - Action row `Open / Copy / Share` (M3 tonal buttons) disabled until a result exists; paste is an end-icon inside the input field; result card shows a placeholder before first processing.
- **Key Changes — theming:**
  - Full **M3 DayNight** theme: `values-night/colors.xml` palette, per-API `themes.xml` overlays (v23/v27/v35 + night variants), theme-attr drawables (`?attr/colorSurfaceContainer` etc.) so every surface adapts; `DynamicColors` intentionally NOT applied (hand-tuned palette preserved on OEM skins).
  - **Theme picker in Settings** (System/Light/Dark, `ThemeHelper` → `AppCompatDelegate`); preference validated with fallback to System on corrupt values.
  - Edge-to-edge on API 21–35 with correct scrim handling for old nav-bar button versions (API 21–25).
  - **New launcher icon** — wizard-wand glyph fitted to the 72dp adaptive safe zone, with monochrome (themed icon) variant; all densities regenerated.
- **Key Changes — behaviour/hardening:**
  - `InputValidator.validate()` returns a **reason-aware `ValidationResult`** (`MULTIPLE_URLS` vs `OTHER`) → distinct user-facing error messages (`error_multiple_urls` / `error_invalid_input`); multi-URL heuristic hardened for glued URLs vs nested query URLs.
  - Main/Share ViewModels: `isProcessing` + `pendingReprocess` concurrency guards (toggle changes during processing queue exactly one reprocess); `onUrlChanged` invalidates stale results; UiState split into `processedUrl` (display) vs `actionUrl` (actions); Share VM ignores duplicate identical share texts, exposes `reprocessAfterProxyChange()` and `setNoSharedText()`.
  - `ShareActivity`: `onNewIntent` sets the intent, `onPause` no longer finishes on config change, `ClipData` fallback when `EXTRA_TEXT` is missing, empty share intents surface an error state.
  - Auto-reprocess on toggle/proxy change on both screens; input preserved across pause; predictive back; accessibility pass (content descriptions, live regions, keyboard navigation).
- **Cleanup:** dead resources removed (wave backgrounds, legacy button/edit-text drawables, `dialog_about.xml`, `dialog_action_priority.xml`, `styles.xml`, unused switch selectors, `ic_menu`/`ic_search`), unused `androidx-core-testing` dependency dropped, orphan `drawable-v24/ic_launcher_foreground.xml` deleted.
- **Tests added:** `MainViewModelTest` + `ShareViewModelTest` (new suites — statuses, invalidation, validation-error mapping, duplicate-share guard, proxy-change reprocess), `ResultStatusTest`, `UrlDiffHelperTest`, `ThemePreferenceTest` (Robolectric), `ThemePickerTest` (instrumentation); existing suites updated for the new layout/flow.
- **Tests pass rate:** 252/252 unit (100%, +41 vs v1.7.2) + 190/190 instrumentation (100%, +4) on `Pixel_API_35_Play`.
- **Impact:** the app looks completely different — clearer before/after mental model, full dark mode, modern M3 components — while the cleaning engine, proxy rosters, browser mode, and zero-permission privacy posture are byte-for-byte the same as v1.7.2.

### v1.7.1 → v1.7.2
- **Focus:** Root-cause fix for redirect-wrapper links breaking in browser mode. Reported symptom: opening a link from the Reddit app (FixupXer as default browser) landed on `reddit.com/invalid_token…` instead of the destination.
- **Root cause (two layers):**
  1. `RedditCleaner` applied its aggressive "remove unknown params" cleaning to Reddit's outbound wrapper `out.reddit.com/…?url=<dest>&token=…`, stripping the functional `url=` and `token=` params → the browser received a redirect with no destination, which Reddit rejects with `/invalid_token`.
  2. `InputValidator` (v1.7.1) only exempted Google's `google.com/url?` wrapper from the multiple-URL check via a per-service allow-list, so every other wrapper (Reddit `out.reddit.com`, Facebook `l.facebook.com/l.php?u=`, LinkedIn, YouTube, newsletters) was still rejected before processing — the same class of bug the Gmail fix addressed for one host only.
- **Fix (`RedditCleaner.kt`):** detect the `out.reddit.com` wrapper and extract the destination from the `url=` parameter (URL-decoded, http(s)-validated); `CleanerService.deepClean` then cleans the destination with its own cleaner (same contract as `GoogleSearchCleaner`). Wrappers with no extractable `url=` are returned untouched so the server-side redirect (and its token) still works.
- **Fix (`InputValidator.kt`) — host-agnostic generalization:** replaced the `GOOGLE_REDIRECT_WRAPPER` allow-list with a general rule — a single whitespace-free `http(s)://` URL (`SINGLE_URL_TOKEN`) has the multiple-URL heuristic run on its **authority+path only** (`substringBefore('?').substringBefore('#')`), so a nested `https://` in the query string is not counted as a second pasted URL, on any host. Whitespace-separated pastes and glued host names still fall through to rejection.
- **Fix (`InputValidator.kt`) — performance/timeout:** all detection regexes (protocol/www/domain/TLD-glue/glued-URL patterns with ~250 TLD alternatives, plus control-char and combining-mark patterns) are now compiled once as fields instead of being rebuilt on every `hasMultipleUrls()`/`detectGluedUrls()` call. On the emulator the per-call recompile was exceeding the 50 ms anti-DoS timeout, which is treated as "multiple URLs" and silently rejected valid single URLs ("URL detection timed out, assuming multiple URLs"). This was a latent bug affecting ordinary URLs on slower hardware.
- **Security review:** only the multiple-URL check is relaxed; length, control-character, combining-accent and encoded-dot checks unchanged. Multi-URL pastes and glued hosts still rejected. Only the single `url=`/`q=` destination is ever extracted downstream, so smuggled extra URLs are dropped (regression-tested).
- **Verification:** end-to-end on `Pixel_API_35_Play` via simulated VIEW intents with logcat — `out.reddit.com/…?url=…&token=…` → `URL cleaned … -> https://example.com/article` → `PostCleanRunner` browser handoff; Gmail redirect still unwraps; multi-URL attack input still rejected. User confirmed real Reddit-app links open correctly on device.
- **Tests added:** `UpdatedCleanersTest` +4 (`RedditCleaner` outbound extraction, plain + %-encoded destination, wrapper-without-`url=` kept intact, ordinary reddit.com post still cleaned); `InputValidatorTest` host-agnostic redirect cases (Reddit, Facebook, generic host, plain nested) + path-glued rejection, obsolete "non-Google host rejected" case removed; `UrlProcessorTest` +1 end-to-end `out.reddit.com` unwrap-and-clean case.
- **Tests pass rate:** 211/211 unit (100%, +9 vs v1.7.1) + 186/186 instrumentation (100%) on `Pixel_API_35_Play`.
- **Impact:** links opened/shared from the Reddit app (and any other redirect-wrapper app) now clean to their real destination instead of failing; valid single URLs no longer spuriously rejected on slower devices. No new permissions, no security-posture change.

### v1.7.0 → v1.7.1
- **Focus:** Regression fix — Gmail links in browser mode. Every link clicked in Gmail (Google redirect wrapper `google.com/url?q=<destination>`) failed with "Error processing URL" since v1.6.0.
- **Root cause:** v1.6.0 added `InputValidator.validateAndSanitizeInput()` as a gate in `MainActivity.handleViewIntentIfPresent()` (part of the double-decode fix). The validator's `hasMultipleUrls()` counts `https?://` occurrences after URL-decoding; a Google redirect legitimately contains a second protocol in the `q=` parameter, so `protocolCount=2` → rejected before `UrlProcessor` (whose own v1.4.9 Gmail fix was intact but never reached). Diagnosis: `docs/reports/GMAIL_BROWSER_MODE_DIAGNOSIS.md`.
- **Fix (`InputValidator.kt`):** new `GOOGLE_REDIRECT_WRAPPER` regex (`^https?://(www.)?google(.tld){1,2}/url?\S+$`, full-string match). When the sanitized *or* decoded input matches, the multiple-URL check is skipped; all other checks (length, control chars, combining accents, `%2E`) still apply. Whitespace anywhere breaks the match, so genuine multi-URL pastes — even ones starting with a Google redirect — are still rejected. The same exemption automatically heals pasting a Google redirect into Main (previously "Please paste one URL at a time").
- **Security review of the exemption:** `GoogleSearchCleaner.extractRedirectUrl()` deterministically extracts only the single `url=`/`q=` destination, so URLs smuggled into other wrapper parameters are dropped with the wrapper — covered by the new `UrlProcessorTest.google url wrapper cannot smuggle extra urls` test.
- **Audit:** all other v1.6.0/v1.7.0 changes re-checked against the v1.4.9 browser-mode fix list — `PostCleanRunner` routing (loop guard, ask dialog, priority chain, native-app fallback, browser discovery) untouched and verified live on the emulator (Gmail redirect → clean → post-clean action flow).
- **Tests added:** new `InputValidatorTest` (18 cases — first dedicated suite for the validator: Gmail/regional/encoded redirect acceptance, whitespace/glued/non-Google-host rejections, control-char/encoded-dot/length/zero-width behaviour) + 1 new `UrlProcessorTest` smuggling case. Emulator verification: Gmail-style VIEW intents (plain + %-encoded), direct URLs, and multi-URL attack input.
- **Tests pass rate:** 202/202 unit (100%) + 186/186 instrumentation (100%) on `Pixel_API_35_Play`.
- **Impact:** Gmail (and Google Search result) links work again in browser mode; no security posture change.

### v1.6.0 → v1.7.0
- **Focus:** TikTok conversion support — dedicated Embed? toggle plus a full Primary/Backup/Custom proxy picker mirroring the Instagram system. Idea from community PR #5 (@gautamnabin5), re-implemented on the v1.6.0 architecture and expanded from a single hardcoded proxy (kktiktok.com) to a four-proxy roster with custom entries and legacy migration.
- **Key Changes — proxy roster (`Constants.kt`):**
  - `TIKTOK_DOMAIN` moved from "Other services" into a new TikTok block. New: `TNKTOK_DOMAIN`, `TFXKTOK_DOMAIN` (`TIKTOK_PRIMARY_PROXIES` — embed videos + slideshows + stats), `TIKTOKEZ_DOMAIN`, `KKTIKTOK_DOMAIN` (`TIKTOK_BACKUP_PROXIES`), `TIKTOK_PROXY_DOMAINS = PRIMARY + BACKUP`, `TIKTOK_DEFAULT_PROXY = tnktok.com`, `TIKTOK_LEGACY_PROXIES = [vxtiktok.com, tiktxk.com]` (dead services, detected for auto-migration only).
  - **New `utils/TikTokProxyStore`** — twin of `InstagramProxyStore` (`@Volatile` custom snapshot, `activeProxies()`/`allKnownProxies()`, normalization/format/reserved/duplicate validation delegating to the Instagram store where identical). Both stores now treat each other's domains as reserved so the two custom rosters cannot hijack each other's substring-based detection.
- **Key Changes — conversion engine (`UrlProcessor.kt`):**
  - New `isTikTokUrl()` (tiktok.com + all known proxies), `convertToTikTokProxy()` and `convertFromTikTokProxy()`; new `tiktokProxy` parameter on `processUrl()` / `processUrlForSharing()`; TikTok branch appended to `applyDomainConversions()`.
  - **Subdomain preservation** — unlike Instagram (www. stripped), TikTok conversions keep the host prefix (`vm.tiktok.com` → `vm.tnktok.com`) because TikTok short links live on subdomains (vm./vt.) and the proxies mirror them. Regex is anchored to the protocol with an explicit label-prefix group; kktiktok.com/vxtiktok.com containing "tiktok.com" as a substring is handled by combined proxy-first checks.
- **Key Changes — plumbing:**
  - `PreferencesManager`: new `convert_tiktok`, `tiktok_proxy_domain`, `custom_tiktok_proxies` (CSV), `browser_convert_tiktok` prefs + accessors; init mirrors custom list into `TikTokProxyStore`; `getTikTokProxy()` validates against active proxies with silent fallback to default.
  - `UrlRepository`/`UrlRepositoryImpl`: `isTikTokUrl()`, `isTikTokConversionEnabled()`/`setTikTokConversionEnabled()`; TikTok branches in `processUrl`, `processUrlWithoutHistory`, `processUrlForBrowser` (browser pref, default off); platform detection + history `classifyConversion()` extended (proxy-first substring handling as for Instagram).
  - `TikTokCleaner.matches()` extended with `TikTokProxyStore.allKnownProxies()` so proxy links get TikTok tracking cleanup (`_r`, `_t`, `tt_from`, …); `CleanerRegistry` associates fixed + legacy TikTok proxies for O(1) dispatch; `PostCleanRunner` recognizes proxy links as TikTok content for native-app forwarding.
- **Key Changes — UI:**
  - New `tiktokToggleContainer` row in `activity_main.xml` / `activity_share.xml` (icon + Embed? + switch + "Active: <proxy>. Change." row), identical structure to the Instagram row.
  - **New `ui/dialogs/TikTokProxyDialogHelper`** — twin of the Instagram dialog helper; reuses the generic picker layouts/strings (`item_instagram_proxy_option`, `dialog_add_custom_proxy`, badge/validation strings); TikTok-specific title/info strings. `tiktokProxyInfoIcon` added to `ids.xml`.
  - ViewModels: `isTikTokUrl`/`isTikTokConversionEnabled` UiState fields, `onTikTokConversionToggled()`; `MainViewModel.reprocessAfterProxyChange()` now also fires for TikTok URLs.
  - Settings → Conversion Defaults dialog: new **Convert TikTok links** switch (browser mode).
  - Strings: `convert_tiktok_toggle(_desc)`, `tiktok_proxy_dialog_title`, `tiktok_proxy_info_text`, `change_tiktok_proxy_link_desc`, `convert_tiktok_browser`; disclaimer/trademark lists updated with the four TikTok proxies.
- **Tests added:** `TikTokProxySelectionTest` (28 unit cases — forward/backward/cross-proxy/legacy/no-op/prefix preservation/detection), `CustomTikTokProxyTest` (15 unit cases — store state, reserved domains incl. cross-platform, custom conversions, cleaner matching), TikTok rows in `UrlProcessorMatrixTest` + 4 `UrlProcessorTest` cases, `TikTokProxyPreferenceTest` (15 instrumentation cases — persistence, validation fallback, custom roster independence from Instagram), 6 TikTok scenarios in `BidirectionalConversionTest`.
- **Tests pass rate:** 183/183 unit (100%) + 186/186 instrumentation (100%) on `Pixel_API_35_Play`, first-pass green.
- **Impact:** TikTok links finally embed properly when shared to Discord/Telegram, with the same resilience story as Instagram — four independent proxy services, user-supplied custom proxies, and automatic migration off dead services (vxtiktok.com shut down 11/2025).

### v1.5.1 → v1.6.0
- **Focus:** User-defined custom Instagram proxies, kkinstagram.com reinstated as an active Backup proxy, and a broad bug-fix / code-hygiene pass.
- **Key Changes — custom proxies (new feature):**
  - **New `utils/InstagramProxyStore`** — process-wide `object` holding the custom proxy list behind a `@Volatile` immutable snapshot. `activeProxies()` = fixed roster + custom; `allKnownProxies()` = active + legacy. Stateless consumers (`UrlProcessor`, `InstagramCleaner`, `PostCleanRunner`, `UrlRepositoryImpl`, `CleanerService.describeAction`, ViewModels) read from the store instead of static `Constants` lists.
  - **`PreferencesManager`** — new `custom_instagram_proxies` CSV pref with `getCustomInstagramProxies()` / `addCustomInstagramProxy()` / `removeCustomInstagramProxy()`; the `init` block and every mutation mirror the list into the store. `getInstagramProxy()` validates against active proxies, so deleting the selected custom proxy transparently falls back to `toinstagram.com`. `FixupXerApplication` injects `PreferencesManager` so the store is seeded before any URL processing.
  - **Chooser dialog rebuilt** (`InstagramProxyDialogHelper`) — dynamic `BaseAdapter` list built from Constants + store: radio + domain + badge (Primary/Backup/Custom), delete icon on custom rows, trailing **Add custom proxy…** row that opens a Material `TextInputLayout` input dialog with inline validation errors. New layouts `item_instagram_proxy_option.xml`, `dialog_add_custom_proxy.xml`, drawable `ic_delete.xml`.
  - **Validation** — input normalized (lowercase, strip protocol/`www.`/path/query/fragment), hostname regex (subdomains allowed), duplicate rejection, and rejection of every domain the app already routes (Instagram + Twitter/X + Facebook families, both containment directions) so a custom proxy can never corrupt substring-based platform detection.
- **Key Changes — kkinstagram.com returns:**
  - `Constants`: `KKINSTAGRAM_DOMAIN` joins `INSTAGRAM_BACKUP_PROXIES`; `INSTAGRAM_PROXY_DOMAINS = PRIMARY + BACKUP` (4 fixed proxies); `INSTAGRAM_LEGACY_PROXIES = [eeinstagram.com]` only.
  - Stored kk selections from ≤ v1.4.7 are valid again (no longer migrated to default); `eeinstagram.com` still migrates.
  - Dialog roster, `instagram_proxy_info_text`, toggle description and `disclaimer_text` updated (Backup = instagram7.com, kkinstagram.com; new Custom section).
- **Key Changes — bug fixes:**
  - `fb.com` now routes through `isFacebookUrl()` / `convertToFacebookez()` (previously only `FacebookCleaner` knew it); `FB_SHORT_DOMAIN` added to Constants.
  - `vxtwitter.com` conversion branch added (→ `fixupx.com` toggle ON, → `x.com` toggle OFF), parity with fxtwitter.
  - `InstagramCleaner.matches()` now uses `allKnownProxies()` so legacy/custom proxy links get `igsh`/`igshid` cleaning.
  - "Nothing to do!" display text separated from the actionable URL (`processedUrl` vs new `actionUrl` in both UiStates) — Share/Open/Copy no longer act on the literal message.
  - `MainViewModel.clearInput()` / `ShareViewModel.clearState()` reset `isTwitterUrl`/`isLoading` (Twitter toggle no longer lingers).
  - History classification unified into `UrlRepositoryImpl.classifyConversion()` + `saveHistoryEntry()` helpers; legacy/custom proxy conversions now record "Domain converted"; substring pitfalls (toinstagram⊃instagram.com, fixupx⊃x.com) handled explicitly.
  - Browser VIEW intent: `InputValidator` used only as a gate; original URI string passed to `processUrlForBrowser` (fixes double URL-decoding of %-encoded parameters).
  - Lifecycle: `OnGlobalLayoutListener`s removed in `onDestroy` (Main + Share), TextWatcher validation job cancelled before relaunch, `HistoryDialogHelper` cancels the previous history collector before starting a new one, `ShareViewModel.processUrlInternal` no longer nests a second `viewModelScope.launch`.
  - ShareActivity copy-error toast now uses its own string instead of `error_browser`.
- **Key Changes — rules/hygiene:**
  - All hardcoded UI strings moved to `strings.xml` (PostCleanRunner chooser titles/toasts, Settings toast, MainActivity toast, ClipData labels); duplicate `please_enter_url` consolidated.
  - All domain literals centralized in `Constants.kt` (cleaners, registry, ViewModels, repository, PostCleanRunner).
  - Dead code removed: `PostCleanRunner.showSystemChooser/runFollow/runPriority(list)` + orphaned `AfterCleanAction.kt`, unused `CleanerService.preferencesManager` inject (AppModule + 7 test call sites updated), `MainUiState.showErrorToast`, empty `SettingsActivity.updateBrowserModeDescription()`. `HistoryDialogHelper.showDeleteConfirmation()` renamed `deleteHistoryEntry()` (it never confirmed).
  - Full GPL headers added to 4 test files that only carried the SPDX line.
- **Tests updated/added:** `InstagramProxySelectionTest` (kk active, ee legacy, store reset in `@Before`/`@After`), `UrlProcessorMatrixTest` (kk/ee cases + new vxtwitter and Facebook/fb.com matrix rows), new `CustomInstagramProxyTest` (store state, normalization, format/reserved/duplicate validation, custom-proxy conversions, detection, cleaner matching), `InstagramProxyPreferenceTest` (kk-selection persists, custom proxy add/remove/select/fallback/restart persistence), new `CustomProxyDialogTest` instrumentation suite (roster + add/select/delete flow, inline validation errors, kk selection).
- **Tests pass rate:** 140/140 unit (100%) + 165/165 instrumentation (100%) on `Pixel_API_35_Play`, first-pass green.
- **Impact:** Users are no longer stranded when the fixed proxy roster goes stale — any embed proxy can be added on the spot, with kkinstagram.com back in the box as a known-good backup.

### v1.5.0 → v1.5.1
- **Focus:** Unified Instagram proxy selector (Main + Share use the same dialog; Settings entry removed) plus auto-reprocess parity on Main when a Processed URL already exists.
- **Key Changes:**
  - **Main screen** — `MainActivity.onChangeProxyClick()` now opens `InstagramProxyDialogHelper` directly (parity with Share). Previously it launched `SettingsActivity`. Settings is no longer the source of truth for picking the proxy.
  - **Auto-reprocess on Main** — `MainViewModel.reprocessAfterProxyChange()` calls `urlRepository.processUrl(url, false, previousProcessedUrl)` only when (a) the input is an Instagram URL, and (b) a Processed URL already exists (i.e. the user has tapped Process at least once). Fresh inputs still require an explicit Process tap. The repository's `previousProcessedUrl` parameter prevents duplicate history entries when the result is identical.
  - **Share screen** — unchanged behaviour; already used the dialog inline because `ShareActivity` is `android:noHistory="true"`. Re-processes the shared URL after a new proxy is picked.
  - **Settings** — the entire **Instagram embed proxy** `MaterialCardView` is removed from `activity_settings.xml`. `SettingsActivity.kt` drops the radio-group listener, the info-icon click handler, the `loadSettings()` proxy block, and the duplicated `showInstagramProxyInfoDialog()` (the dialog helper already owns the info icon). Settings now opens straight to Browser Integration / Conversion Defaults / Action Mode.
  - **Persistence + conversion logic** — unchanged. `PreferencesManager.getInstagramProxy()` / `setInstagramProxy()` and `instagram_proxy_domain` are kept; existing users keep their selection. Default remains `toinstagram.com`.
  - **Strings cleanup** — removed `settings_instagram_proxy_title`, `settings_instagram_proxy_summary`, `instagram_proxy_to`, `instagram_proxy_adamlikes`, `instagram_proxy_7` (referenced only by the removed Settings card). Kept `instagram_proxy_primary_label`, `instagram_proxy_backup_label`, `instagram_proxy_dialog_title`, and the three `instagram_proxy_info_*` strings (still used by `InstagramProxyDialogHelper`).
  - **New `res/values/ids.xml`** declaring `instagramProxyInfoIcon`. The dialog helper builds the title row programmatically and assigns this ID to the info icon; declaring it in `ids.xml` keeps the ID available now that no layout XML defines it.
  - **F-Droid mirror parity** — all touched files copied 1:1 to `GITHUB/fixupxer/` (no diff fence touched). `SettingsActivityProxyTest.kt` deleted in both trees.
- **Tests updated:**
  - **Removed** `SettingsActivityProxyTest` (5 cases) — tested radio buttons that no longer exist.
  - **Added** `MainActivityProxyLabelTest.changeProxyShowsDialogAndUpdatesLabelInPlace` — taps **Change.** in the Main screen and verifies (a) the dialog appears, (b) selecting `instagram7.com` updates the label in place, (c) the Main screen stays in the foreground, (d) the Processed URL field stays empty (no auto-reprocess for fresh inputs). Mirrors the existing Share-screen test.
  - **Added** `MainActivityProxyLabelTest.processedInstagramUrlReprocessesAfterProxyChange` — types an Instagram URL, taps Process to populate Processed URL with the default proxy, opens **Change.**, picks `instagram7.com`, and verifies the Processed URL field is automatically refreshed (no extra Process tap required) and no longer contains the previous proxy domain.
- **Tests pass rate:** 119/119 unit (100%) + 152/152 instrumentation (100%) on `Pixel_API_35_Play`. One pre-existing `KeyboardNavigationTest.testKeyboardInputAndDismissal` flake (Espresso `typeText()` swallowing the first character before the soft keyboard is fully attached) was observed once during a full-suite run; passes reliably (4/4) when re-run in isolation. Unrelated to v1.5.1.
- **Impact:** One chooser, two screens, identical flow. Settings stops carrying redundant state; Main + Share are now the single point of interaction for the proxy. Already-processed Instagram URLs re-render with the new proxy in place — fresh inputs still belong to the Process button.

### v1.4.9 → v1.5.0
- **Focus:** Xiaomi/Redmi/HyperOS default-browser list compatibility (manifest fix + regression test).
- **Key Changes:**
  - **Root cause:** MIUI/HyperOS picker for the default browser scans apps via `Intent.CATEGORY_APP_BROWSER` (the same mechanism Chrome, Firefox, Brave, and Edge use). FixupXer's `BrowserAlias` declared only the AOSP-minimum `VIEW + BROWSABLE + DEFAULT + http/https` filter, which works on stock Android (Pixel) but is ignored by Xiaomi's picker. Direct parallel: Mozilla Bugzilla 1204655 / Fenix #16780, fixed there in 2021.
  - **Fix:** Added a second `<intent-filter>` to the existing `BrowserAlias` declaring `MAIN + DEFAULT + APP_BROWSER`. The filter is paired with the existing `VIEW` filter inside the same alias, so `BrowserModeUtils.setBrowserAliasEnabled` toggles both atomically. `CATEGORY_DEFAULT` is required because OEM pickers use `PackageManager.queryIntentActivities(..., MATCH_DEFAULT_ONLY)`.
  - **Why not LAUNCHER:** Mozilla put `APP_BROWSER` in their existing `MAIN+LAUNCHER` filter; we cannot follow that route because `BrowserAlias` is a separate component and adding `LAUNCHER` would create a duplicate launcher icon. Bypassing this with a dedicated `DEFAULT+APP_BROWSER` filter keeps the home-screen surface unchanged.
  - **Privacy-by-default preserved:** `BrowserAlias` remains `android:enabled="false"` in the manifest, so users who never toggle Browser mode on are never treated as browser candidates by any OS picker.
  - **No UI / behaviour changes:** `MainActivity.handleViewIntentIfPresent` is a no-op for `ACTION_MAIN` (it only acts when `intent.data != null`), so when MIUI invokes the alias through `MAIN+APP_BROWSER` the app simply opens its main UI.
- **New test:** `BrowserAliasIntentResolutionTest` (4 instrumentation `@Test` methods):
  1. `testAppBrowserCategoryHiddenWhenAliasDisabled` — privacy-by-default guard.
  2. `testAppBrowserCategoryVisibleWhenAliasEnabled` — verifies discoverability after toggle on.
  3. `testHttpViewIntentFilterStillDeclaredAndAliasEnabled` — AOSP regression guard: confirms BrowserAlias is still enabled and reachable through PackageManager after the manifest edit (deliberately avoids `queryIntentActivities(VIEW+http, ...)` because emulator pre-set defaults can collapse URI-aware results to the preferred handler).
  4. `testBrowserAliasDoesNotAppearInLauncher` — guards against accidentally adding a duplicate launcher icon.
- **Tests pass rate:** 119/119 unit (100%) + 155/155 instrumentation (100%) on `Pixel_API_35_Play`.
- **Impact:** Xiaomi Redmi (e.g. Redmi 15) and other MIUI/HyperOS users can now select FixupXer in **Settings → Apps → Default apps → Browser app** after enabling Browser mode (a phone restart may be required to flush MIUI's cached browser list).

### v1.4.8 → v1.4.9
- **Focus:** Browser-mode stability, reliable post-clean actions, and Android routing correctness
- **Key Changes:**
  - Fixed the default-browser loop/crash when FixupXer was selected as Android's browser and post-clean behavior was set to **Ask every time** or **Follow action order**. Browser fallback now excludes FixupXer itself instead of allowing the system to route the cleaned URL back into the app.
  - Replaced chooser-dependent **Ask every time** behavior with a FixupXer-owned action dialog, so the app asks consistently even when Android has only one external handler.
  - Made **Open in native app** fail cleanly when no native app accepts the cleaned URL, allowing priority mode to fall through to browser/share/clipboard instead of showing Android's generic share popup.
  - Refactored browser launch to discover installed browsers dynamically via `CATEGORY_APP_BROWSER`, avoiding hardcoded browser preference order.
  - Added direct launch coverage for common YouTube package variants before falling back.
  - Improved URL extraction so Gmail/Google redirect links with nested destination URLs are accepted and cleaned correctly.
  - Removed temporary debug instrumentation used during physical-device verification.
- **Tests updated:** `UrlProcessorTest` covers Gmail/Google redirect acceptance and extraction; browser-mode routing was verified on emulator and physical device with YouTube, GLS/Gmail, and Instagram flows.
- **Tests pass rate:** 117 unit (100%) + 151 instrumentation (100%) on `Pixel_API_35_Play`. The previous Settings/BrowserMode `scrollTo` flakes were fixed by using deterministic nested scrolling, checking visibility state instead of viewport display where appropriate, and closing `ActivityScenario` instances in API compatibility tests.
- **Impact:** Browser mode now behaves predictably as a system-wide cleaner: links are cleaned once, the ask dialog appears when FixupXer receives the link, native app failures fall back safely, and generic browser handling remains browser-agnostic.

### v1.4.7 → v1.4.8
- **Focus:** Refreshed Instagram proxy list, bare-hostname conversion, inline info tooltip
- **Key Changes:**
  - **New active proxy set**: `toinstagram.com` (Primary, default), `adamlikes.men` (Primary), `instagram7.com` (Backup). `kkinstagram.com` and `eeinstagram.com` are retired from the chooser but still recognised in incoming URLs (`Constants.INSTAGRAM_LEGACY_PROXIES`); pasted legacy links auto-convert to the active default. Existing prefs pointing at a retired proxy silently migrate (`PreferencesManager.getInstagramProxy()` guard now uses `INSTAGRAM_DEFAULT_PROXY = TOINSTAGRAM_DOMAIN` as fallback).
  - **Bare-hostname conversion**: `UrlProcessor.convertToInstagramProxy(...)` now strips the `www.` prefix (and any host-level sub-prefix such as `business.instagram.com`) when converting to a proxy. The proxies render best at the bare hostname. Reverse-conversion (toggle OFF) is unchanged and preserves the original prefix.
  - **Primary / Backup labels** + **inline `i` tooltip** in both Settings ▸ Instagram embed proxy and the `InstagramProxyDialogHelper` chooser. The tooltip describes which proxies provide rich embeds (media + title + description) vs. media-only, and notes the no-www. behaviour. New string `instagram_proxy_info_text` (HTML) plus a custom dialog title view that hosts the info icon next to the title.
  - **Visual order** in Settings + chooser: `toinstagram.com → adamlikes.men → instagram7.com`. New `radioProxyTo` and `radioProxyAdamlikes` IDs replace `radioProxyKk` / `radioProxyEe` in `activity_settings.xml`.
  - **Default proxy**: changed from `kkinstagram.com` (v1.4.7 first release) → `adamlikes.men` (post-rebase) → `toinstagram.com` (final v1.4.8 release).
  - `MainViewModel` and `ShareViewModel` now use `Constants.INSTAGRAM_ALL_KNOWN_PROXIES` (active + legacy) for `isInstagramUrl` detection so legacy URLs still trigger the Instagram toggle.
  - `disclaimer_text` and the embed-toggle description updated to list the new active proxies.
- **Tests updated:** `InstagramProxySelectionTest` rewritten for the new active set (forward / reverse / cross-proxy / `www.` stripping / legacy-migration coverage), `UrlProcessorTest` and `UrlProcessorMatrixTest` updated for the new default and bare-hostname behaviour, `InstagramProxyPreferenceTest` covers legacy-migration paths, `SettingsActivityProxyTest`/`MainActivityProxyLabelTest`/`ShareActivityProxyLabelTest` use the new IDs and labels, `BidirectionalConversionTest` updated for bare-hostname output, `HistoryDatabaseTest` and `UrlValidationImprovementsTest` cover the new domains.
- **Tests pass rate:** 117 unit (100%) + 147 of 151 instrumentation; the 4 instrumentation failures are pre-existing `scrollTo`/visibility flakes in `SettingsTest.testConversionDefaults*` and `BrowserModeTest.testActionModeSelection` that are unrelated to v1.4.8 changes (button is reachable manually but Espresso `scrollTo()` reports zero visible area on the Pixel API 35 emulator). Tracked separately.
- **Impact:** Users get an up-to-date proxy roster the day v1.4.7's proxies became unreliable; bare-hostname output works around proxy quirks transparently; primary/backup distinction lets users pick their preferred fallback at a glance.

### v1.4.6 → v1.4.7
- **Focus:** Selectable Instagram embed proxy + F-Droid Settings parity + fastlane metadata compliance
- **Key Changes:**
  - Added two new Instagram proxies alongside `kkinstagram.com`: `eeinstagram.com` and `instagram7.com`
  - New `KEY_INSTAGRAM_PROXY` string preference in `PreferencesManager` (default: `kkinstagram.com`)
  - `UrlProcessor.processUrl(...)` and `processUrlForSharing(...)` accept a new `instagramProxy` parameter (default-kept for backward-compatible tests)
  - Generic `convertToInstagramProxy(url, targetProxy)` and `convertFromInstagramProxy(url)` replace the old `kkinstagram`-specific helpers; cross-proxy swaps are supported
  - `InstagramCleaner.matches()`, `CleanerService.describeAction()`, `PostCleanRunner` native-app mapping and `UrlRepositoryImpl` history classification all extended to cover the three proxies
  - ViewModel split: `isFacebookUrl` now a dedicated UI-state field, so only Instagram URLs expose the proxy row
  - New layout inside `instagramToggleContainer`: single horizontal row with `[icon] Embed? [switch]` on the left and `Active: <proxy>. Change.` on the right (Instagram only)
  - New `InstagramProxyDialogHelper` (radio `AlertDialog`) hosts the proxy chooser:
    - `MainActivity.onChangeProxyClick()` launches `SettingsActivity` when `Intent.resolveActivity()` succeeds (primary path) and falls back to the dialog otherwise
    - `ShareActivity.onChangeProxyClick()` always uses the dialog. `ShareActivity` is declared `android:noHistory="true"` in the manifest, so the system calls `finish()` the moment the activity loses focus — launching `SettingsActivity` from Share would therefore destroy the share context mid-flow. An earlier attempt with an `isNavigatingAway` flag could not work around `noHistory`. Showing the dialog keeps the share context alive and re-processes the URL in place so the preview updates immediately. The one-shot share contract (`onPause → clearState → finish`) is preserved unchanged
  - New Settings section "Instagram embed proxy" with three `MaterialRadioButton` choices positioned above Browser Integration
  - Browser mode reads the same `getInstagramProxy()` preference — both entry points honour the user's choice
  - `disclaimer_text` updated to list all three proxies as third-party services
  - **Issue #3 fix:** F-Droid build now ships with the same Settings menu (and therefore browser mode) as Google Play. Root-of-truth sync direction is now strict: all source changes go root → GITHUB; F-Droid-only files (metadata, proguard rules, gradle.properties) are the sole exceptions. The only intentional build-config difference is `dependenciesInfo.includeInBundle/includeInApk = false` in the GITHUB `app/build.gradle.kts`, plus the omission of Google's `adi-registration.properties`.
  - **Issue #4 fix:** fastlane metadata trimmed to respect F-Droid limits: `short_description.txt` 82→72 chars (≤80 limit), `changelogs/23.txt` 1324→440 chars (≤500 limit); older oversize changelogs (12, 13, 15, 21) also trimmed; new `changelogs/25.txt` added for v1.4.7.
- **Tests added:** `InstagramProxySelectionTest` (17 unit cases covering forward/backward/cross-proxy/no-op/dirty/subdomain), `InstagramProxyPreferenceTest`, `SettingsActivityProxyTest`, `MainActivityProxyLabelTest`, `ShareActivityProxyLabelTest`; extensions in `UrlValidationImprovementsTest` and `HistoryDatabaseTest` for the two new proxies
- **Impact:** Users can recover from proxy outages in a single tap; `kkinstagram` remains the default so upgrades are invisible for existing installs; F-Droid users get parity with Google Play build

### v1.4.5 → v1.4.6
- **Focus:** Browser mode, UI/UX polish, and ReVanced YouTube support
- **Key Changes:**
  - Added optional browser mode with activity alias for system-wide URL cleaning
  - Implemented comprehensive Settings screen with Material Design 3 compliance
  - Added configurable post-clean actions (native app, browser, share, clipboard)
  - Implemented drag-and-drop priority list for action ordering
  - **Added ReVanced YouTube support with priority over official YouTube app**
  - **Enhanced native app detection to prefer ReVanced YouTube (app.revanced.android.youtube) when available**
  - Added detailed "How to Use" instructions dialog with professional formatting
  - Fixed text formatting issues with proper HTML line breaks and Unicode quotes
  - Added Unicode symbols (ⓘ) for better visual clarity in instructions
  - Enhanced menu structure with dedicated Settings and Instructions options
  - Improved instruction text with proper grammar, punctuation, and typography
  - Maintained edge-to-edge display compliance for Android 15
  - Comprehensive lint issue resolution and code quality improvements
- **Impact:** Complete browser integration solution with professional-grade UI, comprehensive user guidance, and enhanced YouTube app compatibility

### v1.4.4 → v1.4.5
- **Focus:** Multi-subdomain URL support and MailChimp compatibility
- **Key Changes:**
  - Fixed MailChimp false positive detection by increasing domain dots threshold
  - Enhanced enterprise URL support (AWS, Azure, Google Cloud services)
  - Improved legitimate multi-subdomain URL handling (3-4 dots allowed)
  - Maintained security with proper glued URL detection
  - Single-line code change with comprehensive test verification
- **Impact:** Better compatibility with email marketing and enterprise cloud services

### v1.4.3 → v1.4.4
- **Focus:** Android 15 edge-to-edge compliance and navigation bar fixes
- **Key Changes:**
  - Removed deprecated `window.statusBarColor` and `window.navigationBarColor` APIs
  - Implemented modern `enableEdgeToEdge()` with `SystemBarStyle` configuration
  - Fixed navigation bar icon visibility on light backgrounds
  - Added API-level specific handling for all Android versions (5.0-15+)
  - Enhanced backward compatibility with semi-transparent scrim for older versions
- **Impact:** Full Android 15 compliance with proper edge-to-edge implementation

### v1.4.2 → v1.4.3
- **Focus:** Test coverage expansion and quality assurance
- **Key Changes:**
  - Expanded test suite to 112 comprehensive test cases
  - Added bidirectional conversion testing for all major platforms
  - Enhanced URL validation and edge case coverage
  - Improved test automation and CI/CD pipeline
  - Added comprehensive regression testing
- **Impact:** 100% test coverage ensuring reliability and preventing regressions

### v1.4.1 → v1.4.2
- **Focus:** Engine performance optimization and caching implementation
- **Key Changes:**
  - Implemented LRU cache system for cleaner performance optimization
  - Added smart caching with 1-hour TTL for repeated URL patterns
  - Enhanced domain dispatch with O(1) lookup performance
  - Optimized memory usage and garbage collection
  - Added cache hit ratio monitoring and debugging
- **Impact:** 5x performance improvement for repeated URL cleaning operations

### v1.4.0 → v1.4.1
- **Focus:** Production stability and comprehensive testing
- **Key Changes:**
  - Comprehensive test suite with 86 unit tests and 38 instrumentation tests
  - Production-ready architecture with enterprise-grade error handling
  - Enhanced thread safety and concurrent processing support
  - Improved logging and debugging capabilities
  - Final quality assurance and release preparation
- **Impact:** Production-ready app with 100% test coverage and enterprise stability

### v1.3.5 → v1.4.0
- **Focus:** Complete engine overhaul with modular architecture
- **Key Changes:**
  - Revolutionary modular cleaner system with 11 specialized cleaners
  - Industry-leading 964 unique tracking parameter coverage
  - O(1) domain dispatch performance with smart caching
  - Complete thread-safe, stateless design
  - International URL support with IDN and zero-width character handling
  - Comprehensive JSON-based test cases with 100% coverage
  - Professional architecture ready for enterprise deployment
- **Impact:** Complete transformation from basic cleaner to professional-grade URL processing engine

### v1.3.4 → v1.3.5
- **Focus:** Enhanced UI/UX and feature completeness
- **Key Changes:**
  - Material Design 3 interface improvements
  - Enhanced accessibility and responsive design
  - Improved user workflow and interaction patterns
  - Advanced settings and configuration options
  - Polished user experience across all device sizes
- **Impact:** Professional-grade user interface meeting modern Android standards

### v1.3.3 → v1.3.4
- **Focus:** Advanced URL processing and feature expansion
- **Key Changes:**
  - Enhanced URL processing algorithms
  - Expanded domain coverage and tracking parameter detection
  - Improved conversion logic for social media platforms
  - Advanced validation and security enhancements
  - Performance optimizations and memory management
- **Impact:** Significantly expanded platform support and processing capabilities

### v1.3.2 → v1.3.3
- **Focus:** Critical bug fixes for glued URL detection and UI improvements
- **Key Changes:**
  - Fixed Instagram URL false positive detection as "Multiple URLs"
  - Fixed case sensitivity issue breaking Instagram post IDs
  - Enhanced glued URL detection to avoid false positives on legitimate URLs
  - Added "Open" button to ShareActivity for better user experience
  - Improved TLD-based glued URL detection with comprehensive TLD list
  - Fixed detection logic to check for complete domain boundaries
- **Impact:** Resolved critical false positive issues and improved user experience

### v1.3.1 → v1.3.2
- **Focus:** F-Droid release preparation and compliance
- **Key Changes:**
  - Updated build configurations for F-Droid compliance
  - Enhanced open-source distribution compatibility
  - Minor bug fixes and stability improvements
  - Documentation updates for F-Droid submission
- **Impact:** Enabled F-Droid distribution for broader user accessibility

### v1.3.0 → v1.3.1
- **Focus:** Comprehensive security hardening and attack vector protection
- **Key Changes:**
  - Added comprehensive InputValidator utility class with multi-layer security validation
  - Enhanced MainActivity with real-time input validation and crash prevention
  - Improved ShareActivity with security hardening for shared content
  - Implemented protection against 8 major attack vector categories
  - Added timeout protection and DoS prevention mechanisms
  - Enhanced error handling with user-friendly feedback
  - Improved performance with sub-100ms validation times
- **Impact:** Enterprise-grade security with comprehensive attack prevention

### v1.2.5 → v1.3.0
- **Focus:** Major URL logic overhaul, toggle and UI improvements, lifecycle and build enhancements
- **Key Changes:**
  - Extensive URL detection logic overhaul for all supported domains
  - Toggle functionality fixes and improvements for Twitter/X and Instagram
  - UI updates: white Share screen background, improved toggle labels, better accessibility
  - ShareActivity now always finishes on losing focus ("one-shot" share)
  - About dialog version is now always correct and automatic
  - Main screen continues to clear fields on losing focus
  - All bug fixes and improvements from previous versions included
- **Impact:** Major architectural improvements with enhanced UI/UX and functionality

### v1.2.4 → v1.2.5
- **Focus:** Code quality improvements and static analysis fixes
- **Key Changes:**
  - Comprehensive lint fixes and code quality improvements
  - Dependency updates and build optimization
  - Enhanced static analysis compliance
  - Improved code maintainability and readability
- **Impact:** Professional code quality standards and improved maintainability

### v1.2.3 → v1.2.4
- **Focus:** Test suite implementation and validation
- **Key Changes:**
  - Comprehensive behavioral matrix testing implementation
  - Automated test case generation and validation
  - Enhanced quality assurance processes
  - Improved regression testing capabilities
- **Impact:** Robust testing foundation ensuring reliability and preventing regressions

### v1.2.2 → v1.2.3  
- **Focus:** URL processing logic refinement and bug fixes
- **Key Changes:**
  - Fixed tracking parameter removal and domain conversion logic
  - Enhanced URL detection accuracy
  - Improved error handling and edge case management
  - Refined conversion algorithms
- **Impact:** More accurate and reliable URL processing

### v1.2.1 → v1.2.2
- **Focus:** Initial behavioral matrix implementation
- **Key Changes:**
  - Basic URL processing logic updates
  - Initial framework for behavioral testing
  - Foundation for future feature expansion
- **Impact:** Established foundation for systematic feature development

---

## 🔧 Core URL Processing Logic Changes

### 1. Behavioral Matrix Implementation
**File:** `app/src/main/java/com/fixupxer/UrlProcessor.kt`

#### New Processing Logic:
- **Return Type Change:** `processUrl()` now returns `Pair<String, Boolean>` (URL + wasAlreadyClean flag)
- **Robust URL Validation:** Enhanced input validation with proper exception handling
- **Manual Query Parameter Processing:** Replaced unreliable Android Uri parsing with custom implementation
- **Domain-Specific Processing:** Separate logic paths for Instagram, Twitter/X, and general URLs

#### Key Algorithm Changes:
```kotlin
// Before: Simple string replacement
// After: Comprehensive behavioral matrix implementation
fun processUrl(url: String, cleanTracking: Boolean, convertTwitter: Boolean): Pair<String, Boolean> {
    // 1. Input validation
    // 2. URL decoding and normalization  
    // 3. Domain detection (Instagram, Twitter/X, General)
    // 4. Tracking parameter removal (if enabled)
    // 5. Domain conversion (if enabled)
    // 6. Cleanliness detection
    // 7. Return processed URL + cleanliness flag
}
```

#### Domain-Specific Processing:

**Instagram URLs:**
- `instagram.com` ↔ `kkinstagram.com` conversion
- Toggle ON: Convert to kkinstagram.com
- Toggle OFF: Convert back to instagram.com

**Facebook URLs:**
- `facebook.com` ↔ `facebookez.com` conversion (with prefix removal)
- Toggle ON: Convert to facebookez.com (removes m., www., mobile., touch., web. prefixes)
- Toggle OFF: Convert back to facebook.com

**Twitter/X URLs:**
- `x.com`/`twitter.com` ↔ `fixupx.com` conversion  
- `fxtwitter.com` ↔ `fixupx.com` conversion
- Toggle ON: Convert to fixupx.com
- Toggle OFF: Convert back to x.com

**General URLs:**
- Tracking parameter removal only
- No domain conversion

### 2. Tracking Parameter Removal Enhancement
**File:** `app/src/main/java/com/fixupxer/UrlProcessor.kt`

#### Improvements:
- **Manual Implementation:** Replaced Android Uri parsing with custom regex-based approach
- **Comprehensive Parameter List:** 50+ tracking parameters from ClearURLs database
- **Robust Detection:** Handles edge cases and malformed URLs
- **Performance Optimization:** Efficient regex patterns and string operations

#### Key Methods:
```kotlin
private fun hasTrackingParameters(url: String): Boolean
private fun removeTrackingParameters(url: String): String
private fun findFirstValidUrl(input: String): String?
```

### 3. URL Validation and Error Handling
**File:** `app/src/main/java/com/fixupxer/UrlProcessor.kt`

#### Enhanced Validation:
- **Empty URL Detection:** Throws `IllegalArgumentException` for empty inputs
- **Invalid URL Detection:** Comprehensive URL format validation
- **Exception Handling:** Proper error messages and logging
- **Edge Case Handling:** @ prefixes, URL encoding, malformed URLs

---

## 🔧 Core Architecture Changes

### 1. Modular Cleaner System
**New Files Created:**
- `cleaners/UrlCleaner.kt` - Base interface for all cleaners
- `cleaners/CleanerRegistry.kt` - O(1) domain dispatch system
- `cleaners/CleanerService.kt` - Deep-clean orchestration
- `cleaners/cache/CleanerCache.kt` - LRU cache with TTL
- `cleaners/config/CleanerConfig.kt` - Priority and configuration
- `cleaners/model/CleanerResult.kt` - Result data model
- `cleaners/utils/CleanerUtils.kt` - Shared utilities

### 2. Domain-Specific Cleaners
**Each cleaner is a Kotlin object (singleton) with:**
- Stateless, pure functions
- Domain-specific parameter sets
- Optimized matching logic
- Comprehensive test coverage

### 3. Deep-Clean Algorithm
```kotlin
// Multi-pass cleaning with stabilization detection
fun deepClean(url: String, maxPasses: Int = 5): CleanerResult {
    // 1. Priority-based cleaner execution
    // 2. Iterative cleaning until stable
    // 3. Cache results for performance
    // 4. Track which cleaners were applied
}
```

### 4. Performance Optimizations
- **Domain Dispatch:** O(1) lookup using ConcurrentHashMap
- **Smart Caching:** LRU with size limit and TTL
- **Lazy Evaluation:** Kotlin sequences for efficiency
- **Early Exit:** Stabilization detection prevents unnecessary passes

---

## 🧪 Testing Infrastructure Overhaul

### 1. Behavioral Matrix Test Suite
**File:** `app/src/test/java/com/fixupxer/UrlProcessorMatrixTest.kt`

#### Comprehensive Test Coverage:
- **24 Test Cases:** Covering all behavioral matrix scenarios
- **Domain Coverage:** Instagram, Twitter/X, Fixupx, FxTwitter, General URLs
- **Toggle States:** Both ON and OFF for each domain type
- **Cleanliness States:** Clean and dirty (with tracking parameters) URLs
- **Expected Outcomes:** URL transformations, cleanliness flags, "nothing to do" states

#### Test Matrix Structure:
```kotlin
data class Case(
    val desc: String,
    val url: String, 
    val cleanTracking: Boolean,
    val convertSpecial: Boolean,
    val expectedUrl: String,
    val expectAlreadyClean: Boolean,
    val expectNothingToDo: Boolean
)
```

#### Test Categories:
1. **Non-special URLs:** General website links
2. **Instagram URLs:** instagram.com ↔ kkinstagram.com
3. **Twitter/X URLs:** x.com/twitter.com ↔ fixupx.com
4. **FxTwitter URLs:** fxtwitter.com ↔ fixupx.com

### 2. Individual Unit Test Updates
**File:** `app/src/test/java/com/fixupxer/UrlProcessorTest.kt`

#### Test Fixes:
- **Return Type Updates:** All tests updated to handle `Pair<String, Boolean>` return type
- **Exception Handling:** Updated tests to expect proper exceptions for invalid inputs
- **Behavioral Alignment:** Tests updated to match new URL processing logic
- **Edge Case Coverage:** Enhanced test coverage for boundary conditions

#### Key Test Updates:
- Empty URL handling (now throws exception)
- Invalid URL handling (now throws exception)  
- Non-status Twitter URL conversion (now converts when toggle ON)
- Encoded URL handling (proper exception handling)

---

## 🏗️ Build System and Dependency Updates

### 1. Dependency Modernization
**File:** `gradle/libs.versions.toml`

#### Updated Dependencies:
```toml
[versions]
agp = "8.4.2"                    # Android Gradle Plugin
kotlin = "1.9.23"               # Kotlin compiler
coreKtx = "1.13.1"              # Android Core KTX
lifecycleRuntimeKtx = "2.8.4"   # Lifecycle components
activityCompose = "1.9.1"       # Compose Activity
composeBom = "2024.10.00"       # Compose BOM
material = "1.12.0"             # Material Design
compose-compiler = "1.5.11"     # Compose Compiler
coroutines = "1.8.1"            # Kotlin Coroutines
hilt = "2.51.1"                 # Dependency Injection
room = "2.6.1"                  # Database
navigation = "2.7.7"            # Navigation
timber = "5.0.1"                # Logging
leakcanary = "2.14"             # Memory leak detection
```

### 2. Kapt to KSP Migration
**Files:** `app/build.gradle.kts`, `build.gradle.kts`, `gradle/libs.versions.toml`

#### Migration Benefits:
- **Build Performance:** 20-30% faster build times
- **Memory Efficiency:** Reduced memory usage during compilation
- **Modern Tooling:** KSP is the recommended replacement for Kapt
- **Better IDE Support:** Improved IntelliJ/Android Studio integration

#### Changes:
```kotlin
// Before: kapt
kapt(libs.hilt.compiler)
kapt(libs.room.compiler)

// After: ksp  
ksp(libs.hilt.compiler)
ksp(libs.room.compiler)
```

### 3. Plugin Updates
**File:** `gradle/libs.versions.toml`

#### New Plugin Configuration:
```toml
[plugins]
ksp = { id = "com.google.devtools.ksp", version = "1.9.23-1.0.19" }
```

---

## 🎨 Code Quality and Static Analysis Fixes

### 1. Lint Warning Resolution
**Total Fixes:** 102 warnings addressed

#### Redundancy Fixes:
- **AndroidManifest.xml:** Removed redundant `android:label` from MainActivity
- **Duplicate Icons:** Removed duplicate drawable resources (`icon_share.xml`, `icon_copy.xml`, `icon_back.xml`)

#### Performance Optimizations:
- **Overdraw Fixes:** Optimized layout hierarchy in `activity_main.xml`
- **Resource Cleanup:** Removed unused drawable resources
- **Layout Optimization:** Reduced redundant background layers

#### Usability Improvements:
- **RTL Support:** Verified proper RTL-aware padding and margins
- **Icon Consistency:** Standardized icon naming conventions
- **Resource Organization:** Cleaned up drawable resource structure

### 2. Layout Optimizations
**File:** `app/src/main/res/layout/activity_main.xml`

#### Overdraw Fix:
```xml
<!-- Before: Redundant background ImageView -->
<ImageView android:background="@drawable/subtle_wave_background" />

<!-- After: Direct background on CoordinatorLayout -->
<androidx.coordinatorlayout.widget.CoordinatorLayout 
    android:background="@drawable/subtle_wave_background" />
```

### 3. Resource Cleanup
**Files:** Various drawable resources

#### Removed Duplicates:
- `icon_share.xml` → `ic_share.xml`
- `icon_copy.xml` → `ic_copy.xml` 
- `icon_back.xml` → `ic_back.xml`

---

## 📱 Version Management

### Version Progression:
| Version | Version Code | Key Focus | Status |
|---------|-------------|-----------|---------|
| v1.2.1 | 10 | Initial state | ✅ Released |
| v1.2.2 | 11 | URL logic fixes | ✅ Released |
| v1.2.3 | 12 | Test implementation | ✅ Released |
| v1.2.4 | 13 | Lint fixes | ✅ Released |
| v1.2.5 | 13 | Final optimization | ✅ Released |
| v1.3.0 | 9 | Major overhaul | ✅ Released |
| v1.3.1 | 10 | Security hardening | ✅ Released |
| v1.3.2 | 11 | F-Droid preparation | ✅ Released |
| v1.3.3 | 12 | Critical bug fixes | ✅ Released |
| v1.3.4 | 13 | Facebook support | ✅ Released |
| v1.3.5 | 14 | Release preparation | ✅ Released |
| v1.4.0 | 16 | Engine overhaul | ✅ Released |
| v1.4.1 | 17 | Android 15 compliance | ✅ Released |
| v1.4.2 | 18 | History feature | ✅ Released |
| v1.4.3 | 20 | UI polish & production ready | ✅ Released |
| v1.4.4 | 21 | Android 15 edge-to-edge compliance | ✅ Released |
| v1.4.5 | 22 | Multi-subdomain URL support | ✅ Released |
| v1.4.6 | 23 | Browser mode integration | ✅ Released |
| v1.4.7 | 25 | Selectable Instagram embed proxy | ✅ Released |
| v1.4.8 | 26 | Instagram proxy refresh | ✅ Released |
| v1.4.9 | 27 | Browser mode stability & routing fixes | ✅ Released |
| v1.5.0 | 28 | Xiaomi/Redmi/HyperOS default-browser compatibility | ✅ Released |
| v1.5.1 | 29 | Unified Instagram proxy chooser (Settings entry removed) | ✅ Released |
| v1.6.0 | 30 | Custom Instagram proxies + kkinstagram.com returns | ✅ Released |
| v1.7.0 | 31 | TikTok conversion support + TikTok proxy picker | ✅ Released |
| v1.7.1 | 32 | Gmail browser-mode regression fix (Google redirect validator exemption) | ✅ Released |
| v1.7.2 | 33 | Reddit/redirect-wrapper fix; host-agnostic validator | ✅ Released |
| v2.0.0 | 34 | Complete UI redesign: before/after flow, M3 DayNight + dark mode, theme picker | ✅ Released |
| v2.1.0 | 35 | Complete opt-in custom URL rule system with Test Lab, templates and import/export | ✅ Released |
| v2.2.0 | 36 | Private Link Guard, keep-unknown cleaning, 14 new cleaners, redirect unwrapping, Bluesky, Process Text, test vectors, Teach from example | ✅ Released |
| v2.3.0 | 37 | Bilibili cleaning, Yahoo referrer keys, GeoRiot/LinkSynergy offline unwrapping and redirect validation hardening | ✅ Released |
| v2.4.0 | 38 | Browser mode hub, privacy readers, saved app choices, local backup/restore, alternative frontend catalog | ✅ Released |
| v2.4.1 | 39 | Privacy fix (fragment query leak) + Open self-interception, Facebook retarget, Browser VIEW history dedup | ✅ Released |
| v2.5.0 | 40 | Retired unsafe frontends (facebookez.com, kkinstagram.com) with automatic migration; Alternative frontends screen in Settings | ✅ Released |
| v2.5.1 | 41 | Android 16 (API 36) target compliance; AGP 8.9.3; no user-facing changes | ✅ Current |

### Build Artifacts (v2.5.1):
- **Google APK:** `app/build/outputs/apk/release/app-release.apk`
- **Google AAB:** `app/build/outputs/bundle/release/app-release.aab`
- **GITHUB / F-Droid APK:** `FixupXer-v2.5.1-release.apk`

For per-build SHA-256 fingerprints, signing details, and the full release checklist, see [BUILD_REPORT.md](BUILD_REPORT.md).
