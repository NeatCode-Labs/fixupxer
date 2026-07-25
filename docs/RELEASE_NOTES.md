# FixupXer v2.6.1 - AliExpress Cleaning and Share Reliability

## What's New

### AliExpress links are actually cleaned now
Cleaning an AliExpress product link removed `spm` but left `pdp_npi` untouched,
and that single parameter is often longer than the rest of the URL put together.
The result looked like nothing had happened. This release strips `pdp_npi` and
15 further confirmed tracking keys from AliExpress links.

Parameters that can carry functional data are deliberately kept: `pdp_ext_f`
and `pvid` survive byte for byte, as does `gatewayAdapt`, which selects the
regional gateway.

### Sharing from apps that use a wildcard text type
`ACTION_SEND` intents were only accepted when the sending app declared exactly
`text/plain`. Apps that share as `text/*` still match the manifest filter, so
their links reached FixupXer and were then rejected with "No URL found in
shared text". The share handler now accepts any text type it is offered, and
falls back to the intent's character sequence extra before reading `ClipData`.

### Links containing a space are no longer discarded
A single-line link whose query contained a literal space, which is what happens
when a browser address bar hands over a partly decoded URL, was dropped
entirely. Spaces in the query tail are percent-encoded instead. Whitespace
before the query still rejects the input, so the host can never shift to a
different domain.

### Technical Details
- Minimum Android: 5.0 (API 21)
- Target Android: 16 (API 36)
- Version Code: 43
- versionName: 2.6.1

---

# FixupXer v2.6.0 - Redacted History for Sensitive Links

## What's New

### History entries for fully cleaned sensitive links
Previously, any link whose **input** contained sensitive-looking data (auth
tokens, e-mail addresses, JWTs, precise coordinates) was kept out of history
entirely — even when cleaning removed every sensitive part. This silently
dropped common redirect wrappers such as Reddit's `out.reddit.com` links
(whose functional `token=` parameter trips the Private Link Guard) and Google
Ads `sig=` wrappers, so clicking an external article from Reddit in Browser
mode left no history trace.

Now, when the Private Link Guard flags the input but the **final cleaned URL
is verifiably safe** (no remaining findings, structurally valid HTTP(S)),
FixupXer saves a **redacted history entry**:

- Only the safe final URL is stored — the original wrapper, its token, and
  its host are never written to the database.
- The history card shows **"Sensitive input" / "Original URL was not saved"**
  with an **"Input redacted for privacy"** badge instead of the original URL.
- Copy, Share, open, and delete work on the safe final URL as usual.

Nothing changes for links whose cleaned result **still** contains sensitive
data: they continue to stay out of history and the cleaner cache completely.
Sensitive inputs also still never enter the cleaner cache. The Room database
schema is unchanged — no migration required.

### Technical Details
- Minimum Android: 5.0 (API 21)
- Target Android: 16 (API 36)
- Version Code: 42
- versionName: 2.6.0

---

# FixupXer v2.5.1 - Android 16 Target Compliance

## What's New

### Google Play Target API Compliance
FixupXer now targets **Android 16 (API level 36)** to comply with Google Play
target API requirements (deadline Aug 31, 2026). Edge-to-edge display and
predictive back support was already in place, so this release required no
related app-code changes.

### Build Tooling
Android Gradle Plugin updated **8.3.2 → 8.9.3** (minimum AGP line for API 36
support). Gradle wrapper and JDK 17 are unchanged.

There are **no user-facing feature changes** in this release. The app remains
100% offline with zero permissions.

### Technical Details
- Minimum Android: 5.0 (API 21)
- Target Android: 16 (API 36)
- Version Code: 41
- versionName: 2.5.1

---

# FixupXer v2.5.0 - Frontend Safety & Settings Access

## What's New

### Alternative Frontends in Settings
Every platform's frontend picker is now always reachable from **Settings >
Link processing > Alternative frontends**. The new screen lists all nine
platforms (X/Twitter, Instagram, TikTok, Facebook, Bluesky, Reddit, YouTube,
Pinterest, Threads) with their active frontend and conversion state, and each
row opens the familiar full picker — no need to paste or share a link first.

## What's Fixed

### Unsafe Frontends Retired
Two bundled frontend domains were retired after a security review prompted by
a community report:

- **`facebookez.com`** (Facebook embed) now redirects visitors to an
  advertising network and is blocked by security DNS providers and ad-block
  filter lists.
- **`kkinstagram.com`** (Instagram backup proxy) is flagged as potentially
  malicious by multiple reputation services.

Both domains are removed from the built-in catalog and can no longer be added
as custom frontends. Existing selections migrate automatically: Instagram
users on kkinstagram move to the first active proxy, and Facebook conversion
turns off unless a custom frontend is configured. Old links on these domains
still get generic tracking cleaning but are no longer generated or specially
handled. Settings backups referencing the retired domains restore cleanly with
the same migration.

Facebook currently ships with **no built-in frontend** — conversion works when
you add your own custom domain in the picker. Platform toggles without any
active frontend now show a neutral label and stay disabled instead of
referencing a missing domain.

### Technical Details
- Minimum Android: 5.0 (API 21)
- Target Android: 15 (API 35)
- Version Code: 40
- versionName: 2.5.0
- Unit tests: 642 / 642 passing. Instrumentation: 235 / 235 passing on
  `Pixel_API_35_Play`.

---

# FixupXer v2.4.1 - Privacy & Reliability Patch

## What's Fixed

### Privacy: Fragment Data No Longer Leaks Into Queries
URL fragments (the part after `#`) stay on your device — servers never see
them. When a link such as `…/status/1#section?access_token=…` was converted
to an alternative frontend, the `?…` inside the fragment could be promoted
into a real query and sent to the destination server. Conversions now keep
fragment content strictly inside the fragment for all platforms (X/Twitter
readers, Reddit, youtu.be, Farside links, and host swaps).

### Open Button No Longer Reopens FixupXer
When FixupXer is set as the default browser, tapping **Open** on the Main or
Share screen could route the link straight back into FixupXer instead of
handing it out. The Open action now detects this self-interception and opens
the link in your real external browser, while links that belong to native
apps (App Links) keep opening in those apps.

### Facebook Frontend Retargeting
Selecting a different Facebook frontend now also converts links that are
already on another known Facebook frontend (for example `facebookez.com` →
your custom domain), matching how every other platform behaves. Path, query,
and fragment are preserved.

### No More Duplicate History From Browser Mode
Opening a link through Browser mode and then rotating the screen (or any
other configuration change, including mid-processing) no longer records the
same conversion twice in History. Processing now survives screen recreation
and the post-clean dialog is restored from the completed result instead of
re-running the conversion.

### Play Store Note
The intermittent "Something went wrong — check that Google Play is enabled"
message some Google Play users saw when sharing into FixupXer was caused by
Google Play's Automatic integrity protection (installer check), not by the
app itself. The installer check has been disabled in the Play Console for
this and future releases.

### Technical Details
- Minimum Android: 5.0 (API 21)
- Target Android: 15 (API 35)
- Version Code: 39
- versionName: 2.4.1
- Unit tests: 628 / 628 passing. Instrumentation: 229 / 229 passing on
  `Pixel_API_35_Play`.

---

# FixupXer v2.4.0 - Browser Mode Hub, Privacy Readers & Local Backup

## What's New

### Dedicated Browser Mode Screen
All Browser-mode settings now live on their own screen under **Settings >
Configure Browser mode**: the **Enable Browser mode** switch, the **After
processing an opened link** choice (**Ask what to do** or **Try actions
automatically** with a reorderable **Action order**), **Saved app choices**,
and **Configure privacy readers**. The main Settings screen stays compact and
shows a live status line for the whole Browser feature.

### Alternative Frontends on Main and Share
The conversion picker on the Main and Share screens now separates **Embed
frontends** (better previews in chat apps) from **Privacy frontends** (read
without an account) and covers more platforms. X/Twitter and Bluesky gain
selectable reader frontends (xcancel, Nitter instances, automatic instance
pickers, SkyLib) next to their embed defaults, while Reddit, YouTube,
Pinterest, and Threads gain new reader conversions (Redlib, SafeReddit,
Invidious, and more — off by default, some marked Experimental). Custom
domains can now be added for every platform, and each platform remembers its
own selection.

### Browser Privacy Readers
Browser mode no longer shares frontend choices with the Main/Share conversion
toggles. Instead, four platforms — X/Twitter, Bluesky, Reddit, and Pinterest —
can be converted to privacy-oriented reader frontends through their own
per-platform switches and Reader pickers. Platforms without an active Reader
fall back to cleaning only; embed domains are never used in Browser mode.

### Saved App Choices
While using **Ask what to do**, pick **Always use app for this host** to save
which native app or browser should open links from an exact website host.
Saved choices are applied before the action picker, can be reviewed and
deleted on the Browser mode screen, and stay inactive outside Browser mode.

### Configuration Status
A read-only **Configuration status** dialog at the top of Settings summarizes
link cleaning, Custom rules, the Browser alias, the Android default-browser
role (with an explicit *Unable to verify* state), active privacy readers, and
after-clean behavior — including attention markers when something needs a fix.

### Local Settings Backup
**Settings > Backup & restore** exports a versioned JSON file with settings,
custom rules, and saved app choices through Android's system file picker.
Restore validates the whole file first, applies everything atomically with
automatic rollback on failure, and even recovers cleanly if the process is
killed mid-restore. URL history entries are never included.

### Handed Action Layouts
A new **Dominant hand** setting mirrors main action buttons and footer
controls for left- or right-handed use.

### History Limits
The history size dialog now enforces a supported range of 1–10,000 entries,
and history is always trimmed to your limit. Users upgrading with an
out-of-range legacy limit get a guided one-tap migration to the nearest
supported value.

### Reliability and Privacy Hardening
- Tracking cleaning is now an invariant: it is always on and no longer
  exposed as a toggle anywhere in the app.
- Browser alias changes are transactional with automatic rollback, and the
  alias is reconciled with preferences at every app start.
- In-flight Browser VIEW handling is invalidated whenever routing-related
  settings change, and rapid consecutive links can no longer stack dialogs.
- Theme restore after a backup import is deterministic — no timers, no
  duplicate confirmation messages.

### Technical Details
- Minimum Android: 5.0 (API 21)
- Target Android: 15 (API 35)
- Version Code: 38
- versionName: 2.4.0
- Unit tests: 607 / 607 passing. Instrumentation: 228 / 228 passing on
  `Pixel_API_35_Play`.

---

# FixupXer v2.3.0 - International Cleaning & Affiliate Redirects

## What's New

### Bilibili Cleaning
FixupXer now removes four documented Bilibili share-tracking keys:
`vd_source`, `seid`, `share_source`, and `copy_link`. The generic `from`
parameter and every unknown or functional key remain intact under the
keep-unknown contract.

### Yahoo Referrer Cleanup
Three Yahoo/Guce referrer keys are now removed on any destination host:
`guccounter`, `guce_referrer`, and `guce_referrer_sig`. Matching is
case-insensitive but exact, so similarly named parameters are preserved.

### More Offline Redirect Unwrapping
- **GeoRiot / Geniuslink** links on
  `target.georiot.com/Proxy.ashx` unwrap their `GR_URL` destination.
- **LinkSynergy / Rakuten** links on
  `click.linksynergy.com/link` unwrap their `murl` destination.

Both wrappers require the exact host and path, decode the destination exactly
once, and accept only structurally valid HTTP(S) targets. Invalid ports,
malformed percent escapes, relative or non-HTTP destinations, unsafe duplicate
parameters, lookalike hosts, subpaths, and query-like fragment text remain
wrapped.

### Privacy and Compatibility
- No new permissions or network access: all changes are deterministic string
  processing on the device.
- Unknown parameters, raw encoding, duplicate ordering, target fragments, and
  literal `+` characters remain preserved.
- Existing wrapper rules were hardened to ignore `?` characters that appear
  after a URL fragment and to require exact endpoint paths.

### Technical Details
- Minimum Android: 5.0 (API 21)
- Target Android: 15 (API 35)
- Version Code: 37
- versionName: 2.3.0
- Unit tests: 380 / 380 passing. Instrumentation: 201 / 201 passing on
  `Pixel_API_35_Play`.

---

# FixupXer v2.2.0 - Private Link Guard & Expanded Cleaning

## What's New

### Private Link Guard
FixupXer now warns you when a link still carries data that could identify or
compromise you — even after cleaning. Detection is fully offline and covers
login credentials embedded in the URL, e-mail addresses, auth tokens (JWT),
sign-in/reset/invite token parameters, and precise GPS coordinates.

- A warning row appears under the result; tapping it lists what was found and
  where (no values are ever displayed, logged, or stored).
- Choose **Continue anyway**, go **Back**, or **Remove parameter** directly
  from the dialog.
- Links with detected sensitive data are processed ephemerally: they are kept
  out of Conversion History and the processing cache, so no trace remains on
  the device either.

### Smarter, More Careful Cleaning
- **Keep-unknown contract** — platform cleaners now remove only *known*
  tracking keys and keep unknown functional parameters, so cleaned links are
  far less likely to break.
- **Host-boundary matching** — cleaners and conversions only trigger when a
  domain actually terminates the URL's hostname, eliminating false positives
  from domains appearing in paths, queries, or lookalike hosts.
- **14 new platform cleaners** — Wikipedia, Threads, Twitch, Spotify,
  Pinterest, Snapchat, WhatsApp, Medium, Bing, DuckDuckGo, Google Store, eBay,
  Netflix, and AliExpress, plus Google Maps coordinate-URL canonicalization.
- **Expanded existing cleaners** — additional Facebook, LinkedIn, and global
  marketing keys (`mkt_tok`, Webtrekk `wt_` prefix, `#Echobox=` fragments, …).

### Offline Redirect Unwrapping
Known redirect wrappers are unwrapped locally, with strict single decoding and
HTTP(S)-only targets: Facebook `l.php`, LinkedIn `/safety/go`, YouTube
`/redirect`, Google Ads `pagead/aclk`, Reddit Mail click tracking, and
Bluesky `go.bsky.app`.

### Bluesky Post Conversion
A new **Embed?** toggle converts Bluesky post links to `fxbsky.app` for better
embedding (and back when disabled). Browser mode has its own independent
Bluesky default under **Conversion defaults**.

### Clean Link from Selected Text
Select a URL in any app and choose **Clean link** from the text-selection menu.
In editable fields the link is cleaned and replaced inline without leaving the
app; elsewhere FixupXer opens a share-style preview.

### Custom Rules: Test Vectors and Teach from Example
- Save up to 20 input → expected-output **test vectors** per rule and run them
  all with one tap. A rule can only be enabled while every saved vector passes;
  imports with failing vectors are kept as disabled drafts.
- **Teach from example** — give one *before* URL and the exact *desired*
  result, and FixupXer conservatively infers a disabled draft rule (with an
  auto-generated test vector). Ambiguous examples are rejected with a reason.

### Other Changes
- New **What's new?** entry in the overflow menu opens the release notes on
  GitHub.
- All processing logs were sanitized so full URLs never reach the logcat.
- Documentation refreshed: supported platforms, custom rules guide, browser
  mode guide, and third-party provenance notes.

### Technical Details
- Minimum Android: 5.0 (API 21)
- Target Android: 15 (API 35)
- Version Code: 36
- versionName: 2.2.0
- Unit tests: 370 / 370 passing. Instrumentation: 201 / 201 passing on
  `Pixel_API_35_Play`.

---

# FixupXer v2.1.0 - Custom URL Rules

## What's New

### Build Your Own Offline URL Rules
FixupXer now includes a complete custom URL rule system under **Settings →
Custom URL rules**. Rules remain fully local and can remove every parameter,
remove or keep selected names, run safe regex replacements, extract offline
redirect targets, or rewrite URL components with templates.

- Scope rules to all URLs, one exact host, a domain and its subdomains, a host
  group, or an RE2/J URL pattern.
- Choose before-cleaning, after-cleaning, or after-conversion phases.
- Limit rules to Main, Share, or Browser mode; add excludes and stop-after-match.
- Enable, duplicate, delete, drag-reorder, or move rules with accessible buttons.
- Preview unsaved changes through Test Lab with before/after output and a bounded
  execution trace.
- Custom rules are opt-in and remain off after a new installation or an update
  from v2.0 until enabled in Settings.

### Templates and Portable Rule Bundles
- Two independently authored offline template bundles are included.
- Versioned JSON import/export uses Android's system file picker and requires no
  storage permission.
- Import offers Add new, Update matching, and Replace all conflict policies,
  shows counts before mutation, and keeps the last three rollback snapshots.
- Invalid, oversized, unknown, or partly broken bundles are rejected atomically.

### GitHub How-to Guides
- Browser integration and Custom URL rules now have dedicated, maintainable
  Markdown guides on GitHub.
- The in-app **How to Use** actions open those guides in an external browser;
  the previous embedded Browser mode instruction dialog has been removed.

### Interface Polish
- The rule library and editor now use the same grouped Material 3 cards,
  typography, dropdowns, and surface hierarchy as the redesigned Main screen.
- The Add rule button respects gesture and three-button navigation insets.
- Conversion History now has a modern full-height bottom sheet, clearer empty
  states, before/after cards, visible copy/share/delete actions, and refreshed
  on-device history controls.
- A full-app visual pass aligns Settings and dialogs, preserves 48dp touch
  targets, and keeps fixed actions readable at 320dp width with 130% font scaling.

### Privacy, Safety, and Compatibility
- User regex uses linear-time RE2/J only; unsupported backreferences/lookaround
  fail validation instead of falling back to Java regex.
- Raw URL components are preserved: `+`, duplicate parameters, ordering,
  percent-encoding, and untouched fragments are not rebuilt or whole-decoded.
- Redirect cycle, hop, URL-length, regex-complexity, rule-count, trace, and bundle
  limits prevent abusive rules from blocking processing.
- Room migration 1→2 preserves existing history. Rules and rollback snapshots are
  excluded from automatic cloud backup and leave the device only through an
  explicit export.
- A manifest regression test now enforces the zero-permission privacy model.

### Technical Details
- Minimum Android: 5.0 (API 21)
- Target Android: 15 (API 35)
- Version Code: 35
- versionName: 2.1.0
- Unit tests: 273 / 273 passing. Instrumentation: 194 / 194 passing on
  `Pixel_API_35_Play`.

---

# FixupXer v2.0.0 - Complete UI Redesign

## What's New

### A Brand-New Look — the Before/After Flow
FixupXer 2.0 is a complete visual overhaul. Both the Main and Share screens now present your link as a **before → after flow**: the original URL sits in the top card (with its tracking parameters ~~struck through~~ once processed), a vertical flow line leads to the result card below, and a status chip tells you exactly what happened — **Already clean**, **Tracking removed**, **Converted for embedding**, or **Tracking removed and converted**. The functionality you know is unchanged; it's just far clearer what FixupXer did to your link.

- **Strikethrough diff** — removed tracking parameters are shown struck through in the original URL, so you can see at a glance what was cleaned away.
- **Platform toggle cards** — the Embed toggle now lives in a per-platform card (IG / FB / X / TT monogram) with the active proxy and a **Change.** link right there.
- **Action row** — Open / Copy / Share as Material 3 tonal buttons, disabled until there's a result to act on.
- **History** — now a Material bottom sheet, pinned to a floating **History** button; tap an entry to reload it, delete with undo.
- **Modern dialogs** — Instructions, About, proxy pickers, and all other dialogs restyled to the app's design language; Snackbars replace Toasts.

### Full Dark Mode + Theme Picker
The app now ships a hand-tuned Material 3 **DayNight** palette. Choose **System / Light / Dark** in Settings — the default follows your system setting. Every screen, dialog, card, and status chip is theme-aware, including proper status/navigation bar treatment from Android 5 all the way to Android 15 edge-to-edge.

### New App Icon
A new wizard-wand launcher icon (with a proper monochrome/themed-icon variant for Android 13+), fitted to the adaptive-icon safe zone.

### Under the Hood (production hardening)
- Validation errors are now reason-aware: pasting multiple URLs and pasting invalid input give distinct error messages.
- Processing state is properly guarded against races: toggling switches mid-processing queues a single reprocess instead of overlapping work; changing the input invalidates a stale result immediately.
- Share screen: empty share intents surface a proper error, `ClipData` fallback for apps that share without `EXTRA_TEXT`, config changes no longer kill the share context.
- URL diff comparison is now exact (parameter-set based, fragment-aware) — no more false strikethrough matches.
- Numerous small fixes: theme-aware drawables in dark mode, corrupted theme preference fallback, tablet dimension polish, dead resource cleanup.

### Technical Details
- Minimum Android: 5.0 (API 21)
- Target Android: 15 (API 35)
- Version Code: 34
- versionName: 2.0.0
- Unit tests: 252 / 252 passing. Instrumentation: 190 / 190 passing on `Pixel_API_35_Play`.

## Download
- [FixupXer-v2.0.0-release.apk](https://github.com/NeatCode-Labs/fixupxer/releases/download/v2.0.0/FixupXer-v2.0.0-release.apk)

---

# FixupXer v1.7.2 - Reddit (and every redirect-wrapper) Links Fixed

## What's New

### Bug Fix: Links Opened From the Reddit App Now Work
Opening a link from a Reddit post while FixupXer was the default browser landed on a `reddit.com/invalid_token…` error page instead of the real destination. The Reddit app wraps every outbound link in a redirect (`out.reddit.com/…?url=<destination>&token=…`), and FixupXer's aggressive parameter cleaning stripped the functional `url=` and `token=` parameters, handing the browser a redirect with no destination.

This release fixes the **root cause**, not just Reddit:

- **`RedditCleaner`** now recognizes the `out.reddit.com` wrapper and extracts the real destination from the `url=` parameter (then cleans that destination with its own cleaner), exactly like the existing Gmail/Google redirect handling. Wrappers without an extractable destination are left untouched so their server-side redirect keeps working.
- **`InputValidator` is now host-agnostic.** v1.7.1 fixed Gmail links with a Google-only allow-list (`google.com/url?`), which meant every other service that wraps outbound links — Reddit, Facebook (`l.facebook.com/l.php?u=`), LinkedIn, YouTube, newsletters — still failed the "multiple URLs" check. The validator now treats **any** single whitespace-free URL as one navigable URL and only runs the multiple-URL heuristic on its authority+path, so a nested destination in the query string is never mistaken for a multi-URL paste.

### Bug Fix: Valid Single URLs Rejected on Slower Devices
The multiple-URL detector rebuilt several large regular expressions (≈250 TLD alternatives) on **every** call. On slower devices and emulators that alone could exceed the 50 ms anti-DoS timeout, which is treated as "multiple URLs" — silently rejecting perfectly valid single links. All patterns are now compiled once, eliminating the timeout.

**Security is unchanged:**
- Only the multiple-URL check is relaxed — length limits, control-character, Unicode-normalization, and encoded-dot checks still apply.
- Genuine multi-URL pastes (two URLs separated by whitespace) and glued host names (`instagram.comwww.x.com`) are still rejected.
- Downstream, only the single `url=`/`q=` destination is ever extracted and opened, so a second URL smuggled into another wrapper parameter is dropped (covered by regression tests).

### Internal
- `InputValidatorTest`: added host-agnostic redirect cases (Reddit `out.reddit.com`, Facebook `l.facebook.com`, generic hosts) and a path-glued rejection case; removed the now-obsolete "non-Google host rejected" case.
- `UpdatedCleanersTest`: 4 new `RedditCleaner` cases (outbound extraction, plain + encoded destination, wrapper-without-url kept intact, ordinary reddit.com post still cleaned).
- `UrlProcessorTest`: new end-to-end case unwrapping a real-shaped `out.reddit.com` link and cleaning its destination.

### Technical Details
- Minimum Android: 5.0 (API 21)
- Target Android: 15 (API 35)
- Version Code: 33
- versionName: 1.7.2
- Unit tests: 211 / 211 passing. Instrumentation: 186 / 186 passing on `Pixel_API_35_Play`.

## Download
- [FixupXer-v1.7.2-release.apk](https://github.com/NeatCode-Labs/fixupxer/releases/download/v1.7.2/FixupXer-v1.7.2-release.apk)

---

# FixupXer v1.7.1 - Gmail Links Fixed in Browser Mode

## What's New

### Bug Fix: Gmail Links Work in Browser Mode Again
A v1.6.0 regression broke **every link clicked in Gmail** when FixupXer was set as the default browser — the app showed "Error processing URL" instead of cleaning the link. Gmail wraps every link in a Google redirect (`google.com/url?q=<destination>`), and the security validator added in v1.6.0 counted the nested destination URL as a "multiple URLs" attack and rejected the input before URL processing ever started.

The validator now recognizes legitimate Google redirect wrappers (`google.com/url?…`, including regional domains like `google.co.uk`) and lets them through — the existing v1.4.9 redirect extraction then unwraps the real destination as before. Verified end-to-end on the emulator: Gmail-style redirects (plain and %-encoded) clean correctly and continue into the post-clean action flow.

**Security is not weakened by this exemption:**
- Only the multiple-URL check is skipped for these wrappers — length limits, control-character, Unicode-normalization, and encoded-dot checks still apply.
- The exemption requires the *entire input* to be a single Google redirect URL; two URLs separated by whitespace are still rejected, even if the first one is a Google redirect.
- The downstream extractor only ever takes the single `url=`/`q=` destination, so extra URLs smuggled into other wrapper parameters are dropped (covered by a new regression test).
- The same fix also applies to pasting a Google redirect link into the Main screen, which used to be rejected with "Please paste one URL at a time".

### Internal
- New `InputValidatorTest` unit suite (18 cases) — the validator previously had no dedicated tests; covers the Gmail regression, smuggling attempts, and all the pre-existing attack-vector rejections.
- New `UrlProcessorTest` case proving the redirect extractor cannot be abused to smuggle a second URL.

### Technical Details
- Minimum Android: 5.0 (API 21)
- Target Android: 15 (API 35)
- Version Code: 32
- versionName: 1.7.1
- Unit tests: 202 / 202 passing. Instrumentation: 186 / 186 passing on `Pixel_API_35_Play`.

## Download
- [FixupXer-v1.7.1-release.apk](https://github.com/NeatCode-Labs/fixupxer/releases/download/v1.7.1/FixupXer-v1.7.1-release.apk)

---

# FixupXer v1.7.0 - TikTok Support with Full Proxy Picker

## What's New

### TikTok Joins the Embed Family
TikTok links now get the same first-class treatment as Instagram and Twitter/X. Paste or share any TikTok link and a dedicated **Embed?** toggle appears, converting the link to an embed-friendly proxy domain for platforms like Discord and Telegram. Like every FixupXer conversion, this is a pure local string swap — the app never contacts any proxy.

The idea came from community [PR #5](https://github.com/NeatCode-Labs/fixupxer/pull/5) by @gautamnabin5 — thank you! The feature was re-implemented on the current architecture and expanded from a single hardcoded proxy into the full proxy-picker system below.

### Full TikTok Proxy Picker (Primary + Backup + Custom)
The same chooser UI known from Instagram (**Active: \<proxy\>. Change.**) is available for TikTok on both Main and Share screens:

- **Primary** — `tnktok.com` (fxTikTok, default) and `tfxktok.com` (FxTikTok): embed videos *and* multi-image slideshows, with like/comment/share counts.
- **Backup** — `tiktokez.com` (EmbedEZ): embeds media like the primaries; `kktiktok.com` (kkScript): embeds the video only.
- **Custom** — add your own TikTok embed proxy via the **Add custom proxy…** row, with the same validation as Instagram custom proxies (hostname format, duplicates, reserved domains).
- **Legacy detection** — links on the dead `vxtiktok.com` (shut down by legal request 11/2025) and `tiktxk.com` services are still recognized and auto-migrate to your selected active proxy.

### Subdomains Are Preserved
Unlike Instagram (where `www.` is stripped), TikTok conversions keep the host prefix: `vm.tiktok.com/…` → `vm.tnktok.com/…`, `www.tiktok.com/…` → `www.tnktok.com/…`. TikTok short links live on subdomains (`vm.`, `vt.`) and the proxies mirror them, so stripping would break short links.

### Everything Else TikTok
- Own preference (`convert_tiktok`) — independent from the Twitter and Instagram toggles.
- Browser mode: new **Convert TikTok links** switch in Settings → Conversion Defaults (off by default, like the others).
- Tracking cleanup: proxy links get the same TikTok-specific parameter cleaning (`_r`, `_t`, `tt_from`, …) as tiktok.com links.
- History classification records TikTok domain conversions; native-app forwarding recognizes proxy links as TikTok content.

### Technical Details
- Minimum Android: 5.0 (API 21)
- Target Android: 15 (API 35)
- Version Code: 31
- versionName: 1.7.0
- Unit tests: 183 / 183 passing. Instrumentation: 186 / 186 passing on `Pixel_API_35_Play`.

## Download
- [FixupXer-v1.7.0-release.apk](https://github.com/NeatCode-Labs/fixupxer/releases/download/v1.7.0/FixupXer-v1.7.0-release.apk)

---

# FixupXer v1.6.0 - Custom Instagram Proxies + kkinstagram.com Returns

## What's New

### Bring Your Own Instagram Proxy
The proxy chooser (Main and Share screens → **Change.**) now has an **Add custom proxy…** row. Type any Instagram embed proxy domain — full URLs are accepted and normalized to the bare hostname — and it appears in the list with a *Custom* badge and a delete icon. You can add as many as you want, select any of them, and remove them at any time. Deleting the currently selected proxy silently falls back to the default (`toinstagram.com`).

- Custom domains are validated on entry: hostname format check, duplicate check, and rejection of every domain the app already routes specially (`instagram.com`, `x.com`, `fixupx.com`, `facebook.com`, …) so a custom entry can never corrupt platform detection.
- Custom proxies participate everywhere the fixed ones do: forward/backward conversion, cross-proxy swaps, paste detection, tracking cleanup (`igsh`, `igshid`, …), history classification, browser mode, and native-app forwarding.
- Stored locally in `SharedPreferences` (`custom_instagram_proxies`) — like everything else in FixupXer, the list never leaves your device. The app never contacts any proxy; conversions remain a pure string swap.

### kkinstagram.com Is Back
`kkinstagram.com` (retired in v1.4.8) is active again and joins `instagram7.com` under the **Backup** label (embeds media only, no title/description). Users upgrading from ≤ v1.4.7 with `kkinstagram.com` still saved keep their selection instead of being migrated to the default. The active roster is now: toinstagram.com, adamlikes.men (Primary) + instagram7.com, kkinstagram.com (Backup) + your custom proxies. `eeinstagram.com` remains recognized-but-legacy.

### Bug Fixes
- **fb.com links** now get the facebookez.com conversion and the Facebook toggle (previously only the cleaner knew the short domain).
- **vxtwitter.com links** now convert — to `fixupx.com` when Embed is ON, to `x.com` when OFF (parity with fxtwitter.com).
- **Legacy/custom proxy links get Instagram cleaning** — `igsh`/`igshid` and friends are now stripped from e.g. `eeinstagram.com` links, not just the active roster.
- **"Nothing to do!" is no longer treated as a URL** — Share/Open/Copy buttons now act on the actual clean URL instead of the literal message text.
- **Twitter toggle no longer lingers** after clearing the input field (Main and Share).
- **History classification** — conversions involving legacy/custom proxies are now correctly recorded as "Domain converted"; the duplicated classification logic was unified into one helper.
- **Browser mode double-decoding fixed** — VIEW intents with %-encoded URLs were URL-decoded twice (once by the input validator, once by the processor); the validator is now used only as a gate.
- **Share screen copy-error toast** showed the browser error message; now shows the correct one.
- **Lifecycle hygiene** — layout listeners are removed in `onDestroy`, stale text-validation jobs are cancelled before starting new ones, and the history dialog no longer stacks duplicate database collectors.

### Internal
- All remaining hardcoded UI strings moved to `strings.xml`; all domain names centralized in `Constants.kt`.
- New `InstagramProxyStore` — process-wide registry of fixed + custom + legacy proxies backing every detection site.
- Dead code removed (`AfterCleanAction`, unused `PostCleanRunner` chooser paths, unused `CleanerService` dependency on `PreferencesManager`).
- Full GPL license headers added to 4 test files that only had the SPDX short form.

### Technical Details
- Minimum Android: 5.0 (API 21)
- Target Android: 15 (API 35)
- Version Code: 30
- versionName: 1.6.0
- Unit tests: 140 / 140 passing. Instrumentation: 165 / 165 passing on `Pixel_API_35_Play`.

## Download
- [FixupXer-v1.6.0-release.apk](https://github.com/NeatCode-Labs/fixupxer/releases/download/v1.6.0/FixupXer-v1.6.0-release.apk)

---

# FixupXer v1.5.1 - Unified Instagram Proxy Selector

## What's New

### One Selector, Two Screens
v1.5.1 cleans up Instagram proxy management. The chooser now lives on the screens where you actually use it — Main and Share — and is **gone from Settings**. Both screens open the same dialog when you tap **Change.**, so the flow is identical regardless of where you start.

### What changed
- **Main screen — Change. now opens the proxy dialog directly** (previously it launched Settings). No more screen switch, no lost context.
- **Auto-reprocess parity with Share** — if a Processed URL already exists for an Instagram input on the Main screen (i.e. you've already tapped Process), picking a different proxy now refreshes the Processed URL field automatically. Fresh inputs that haven't been processed yet still go through the explicit **Process URL** button — typing a new link does not auto-process.
- **Share screen — unchanged behaviour** (already used the dialog inline because Share is `noHistory="true"`); still re-processes the shared URL after you pick a different proxy.
- **Settings — Instagram embed proxy section removed.** That whole card is gone, along with its info icon and three radio buttons. Settings now opens straight to Browser Integration / Conversion Defaults / Action Mode.
- **Persistence and conversion logic — unchanged.** The selected proxy still lives under `instagram_proxy_domain` in `SharedPreferences`. Existing users keep their selection. Default remains `toinstagram.com`.

### Why
Two surfaces with the same chooser were redundant. Settings used to be the "source of truth", but it was an unnecessary detour when Main and Share already render the **Active: \<proxy\>. Change.** row inline.

### Cleanup details
- Removed: `MaterialCardView` for Instagram proxy in `activity_settings.xml`, the radio listener and `loadSettings` proxy block in `SettingsActivity.kt`, the duplicated `showInstagramProxyInfoDialog()` (the dialog helper already owns the info icon).
- Removed strings: `settings_instagram_proxy_title`, `settings_instagram_proxy_summary`, `instagram_proxy_to`, `instagram_proxy_adamlikes`, `instagram_proxy_7`.
- Kept strings (still used by `InstagramProxyDialogHelper`): `instagram_proxy_primary_label`, `instagram_proxy_backup_label`, `instagram_proxy_dialog_title`, `instagram_proxy_info_*`.
- Added `res/values/ids.xml` declaring `instagramProxyInfoIcon` (the dialog helper builds its title row programmatically and needs the ID to outlive the layout removal).

### Tests
- Removed: `SettingsActivityProxyTest` (tested radio buttons that no longer exist).
- Added: `MainActivityProxyLabelTest.changeProxyShowsDialogAndUpdatesLabelInPlace` — clicking **Change.** opens the dialog and updates the label in place; Processed URL field stays empty when no Process tap preceded.
- Added: `MainActivityProxyLabelTest.processedInstagramUrlReprocessesAfterProxyChange` — covers the auto-reprocess parity (Process → change proxy → Processed URL refreshes with the new proxy).
- Unit tests: 119 / 119 passing. Instrumentation: 152 / 152 passing on `Pixel_API_35_Play`.

### Technical Details
- Minimum Android: 5.0 (API 21)
- Target Android: 15 (API 35)
- Version Code: 29
- versionName: 1.5.1

## Download
- [FixupXer-v1.5.1-release.apk](https://github.com/NeatCode-Labs/fixupxer/releases/download/v1.5.1/FixupXer-v1.5.1-release.apk)

---

# FixupXer v1.5.0 - Xiaomi/Redmi/HyperOS Default Browser Compatibility

## What's New

### MIUI/HyperOS Default Browser Fix
On some Xiaomi and Redmi devices running MIUI/HyperOS, FixupXer was missing from the system **Default browser app** list even with Browser mode enabled. The OEM picker scans for apps that explicitly declare themselves as browsers via the `MAIN + APP_BROWSER` intent category — a mechanism used by Chrome, Firefox, Brave, and Edge — while FixupXer's `BrowserAlias` only declared the AOSP-minimum `VIEW + http/https` filter (sufficient on stock Pixel but ignored by MIUI's picker).

v1.5.0 adds a second intent-filter to the existing `BrowserAlias`:

```xml
<intent-filter>
    <action android:name="android.intent.action.MAIN" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.APP_BROWSER" />
</intent-filter>
```

This is the same fix Mozilla shipped for Firefox in 2021 (Bugzilla 1204655 / Fenix #16780). The filter only activates when the user explicitly enables Browser mode (the alias is `enabled="false"` by default), so privacy-by-default is preserved.

### Compatibility Matrix
- Pixel / stock Android — already worked (AOSP path), no change in behaviour.
- Samsung One UI, Oppo ColorOS, OnePlus OxygenOS — likely also benefit (their pickers use the same `APP_BROWSER` mechanism).
- Xiaomi MIUI / HyperOS (Redmi 15 and similar) — now appear in the default-browser list.

### Manual Step on MIUI/HyperOS
Some MIUI builds cache the browser list. If FixupXer still doesn't appear after the update, **toggle Browser mode off and back on, then restart the phone** to flush the cache.

### No UI Changes
This release is a manifest-only fix plus a regression test. No new screens, no removed features, no permission changes. Browser mode behaviour, post-clean actions, and the rest of the app are unchanged.

### Technical Details
- Minimum Android: 5.0 (API 21)
- Target Android: 15 (API 35)
- Version Code: 28
- versionName: 1.5.0
- New instrumentation test: `BrowserAliasIntentResolutionTest` (4 cases)
- Tests: 119 unit + 155 instrumentation = **274 tests passing**
- 0 permissions, 0 network calls, 0 hardcoded secrets (verified)

## Download
- [FixupXer-v1.5.0-release.apk](https://github.com/NeatCode-Labs/fixupxer/releases/download/v1.5.0/FixupXer-v1.5.0-release.apk)

---

# FixupXer v1.4.9 - Browser Mode Stability & Routing Fixes

## What's New

### Browser Mode Loop Fix
FixupXer no longer re-launches itself when it is set as the default browser and post-clean behavior is set to **Ask every time** or **Follow action order**. Browser fallback now excludes FixupXer and only targets external handlers.

### Reliable Ask Every Time Dialog
**Ask every time** now uses a FixupXer-owned action dialog instead of depending on Android's system chooser. This keeps the ask step visible even when Android would otherwise auto-select a single target.

### Safer Native App and Browser Routing
The **Open in native app** action now reports failure when no native app can handle the cleaned URL, allowing priority mode to fall through to browser/share/clipboard instead of opening a generic Android share popup. Browser launching is browser-agnostic and discovers installed browsers dynamically.

### Gmail / Google Redirect Handling
Complex Google redirect links, including Gmail tracking redirects with nested destination URLs, are now accepted and cleaned correctly instead of being rejected as invalid URL formats.

### YouTube and Instagram Forwarding Improvements
YouTube native launch attempts now cover common package variants before falling back. Instagram links tested through browser mode are cleaned before being forwarded to Instagram when Android supported-link settings route them through FixupXer.

### Technical Details
- Minimum Android: 5.0 (API 21)
- Target Android: 15 (API 35)
- Version Code: 27
- versionName: 1.4.9
- Google Play AAB: 5.52 MB
- GITHUB/F-Droid APK: 4.40 MB
- Tests: 117 unit + 151 instrumentation = **268 tests passing**
- 0 permissions, 0 network calls, 0 hardcoded secrets (verified)

## Download
- [FixupXer-v1.4.9-release.apk](https://github.com/NeatCode-Labs/fixupxer/releases/download/v1.4.9/FixupXer-v1.4.9-release.apk)

---

# FixupXer v1.4.8 - Instagram Proxy Refresh (toinstagram.com / adamlikes.men)

## 🎯 What's New

### Refreshed Instagram Proxy List
The two proxies shipped in v1.4.7 (`kkinstagram.com`, `eeinstagram.com`) have been retired in favour of two currently active ones. The full v1.4.8 set:

- **toinstagram.com** — Primary (default), rich embed (media + post / reel title and description)
- **adamlikes.men** — Primary, same rich embed as toinstagram.com
- **instagram7.com** — Backup, embeds media only (no title or description)

`kkinstagram.com` and `eeinstagram.com` remain *recognised* in pasted/shared URLs so existing links keep working — they are auto-converted to the user's currently selected active proxy. Stored preferences pointing at a retired proxy silently migrate to the default (`toinstagram.com`).

### Bare-Hostname Conversion (no www.)
Converted links are now sent to the proxy *without* the `www.` prefix or any host-level sub-prefix (e.g. `business.instagram.com`). The active proxies render best at the bare hostname; this change applies on every forward conversion (Main, Share, Browser mode). Reverse-conversion (toggle OFF) preserves the user's original prefix unchanged.

### Primary / Backup Distinction with Inline Help
Both the Settings ▸ Instagram embed proxy chooser and the inline Share-screen dialog group the proxies under **Primary** and **Backup** labels and include a small "i" info icon next to the title. Tapping it opens an explanation of which proxies embed rich content vs. media only, and a one-line note about the bare-hostname behaviour. This makes it obvious which proxy to switch to when one goes offline.

### Visual Order
Proxies are listed in the order toinstagram.com → adamlikes.men → instagram7.com in both the Settings radio group and the inline chooser dialog. The default selection on a clean install is the first Primary entry (`toinstagram.com`).

### Technical Details
- Minimum Android: 5.0 (API 21)
- Target Android: 15 (API 35)
- Version Code: 26
- versionName: 1.4.8
- APK Size: ~4.34 MB (Google), ~4.2 MB (F-Droid)
- Tests: 117 unit + 147 instrumentation = **264 tests passing** (4 pre-existing scrollTo flakes in `SettingsTest`/`BrowserModeTest` are unrelated to v1.4.8 and tracked separately)
- 0 permissions, 0 network calls, 0 hardcoded secrets (verified)

## Download
- [FixupXer-v1.4.8-release.apk](https://github.com/NeatCode-Labs/fixupxer/releases/download/v1.4.8/FixupXer-v1.4.8-release.apk)

---

# FixupXer v1.4.7 - Selectable Instagram Embed Proxy + F-Droid Settings Menu

## 🎯 What's New

### Choose Your Instagram Proxy
Instagram embed proxies occasionally go offline. Previously FixupXer only used `kkinstagram.com`, so when it went down your Instagram embeds broke. v1.4.7 lets you pick the active proxy and persists that choice.

- **3 Instagram proxies supported** — `kkinstagram.com`, `eeinstagram.com`, `instagram7.com`.
- **Persistent user choice** — whichever proxy you select stays in effect across Main, Share, and Settings. Stored in `SharedPreferences`, survives app restarts and upgrades.
- **"Active: &lt;proxy&gt;. Change." row** — visible next to the `Embed?` toggle on Main and Share screens when the URL is an Instagram link. One-tap access to change the proxy.
- **Settings section** — `Settings ▸ Instagram embed proxy` shows a three-option radio group (both builds). On the Main screen, *Change.* jumps to Settings. On the Share screen, *Change.* opens the same chooser as an inline dialog — required because the share flow is declared `android:noHistory="true"` and must not be destroyed mid-share.
- **Cross-proxy swap** — paste an `eeinstagram.com` link, pick `instagram7.com`, and the URL is swapped directly without round-tripping through `instagram.com`.
- **Browser mode parity** — the optional browser mode now reads the same Instagram proxy preference, so cleaned links land on your preferred proxy regardless of entry point.

### Settings Menu Now Available on F-Droid (Issue #3)
Previously the F-Droid build shipped without a Settings menu entry, so browser mode was inaccessible to F-Droid users (reported by @Milliw on /e/OS 3.1.1). v1.4.7 closes this gap:

- **Settings menu item** now present in the overflow menu on both Google and F-Droid builds.
- **Browser mode integration** (default-browser alias, priority actions, conversion defaults) fully available in F-Droid.
- Codebase now uniformly sync'd root → GITHUB; the only remaining difference is `dependenciesInfo = false/false` in the F-Droid `build.gradle.kts` (required for F-Droid reproducibility) and the omission of `adi-registration.properties` (Google-only marketing asset).

### Fastlane Metadata Fixes (Issue #4)
Feedback from @IzzySoft (F-Droid):
- `short_description.txt` trimmed from 82 → 72 chars (≤80 limit).
- `changelogs/23.txt` trimmed from 1324 → 440 chars (≤500 limit).
- Older oversize changelogs (12, 13, 15, 21) also trimmed to respect the 500-char cap.
- New `changelogs/25.txt` added for v1.4.7 (440 chars).

### UI / UX Improvements
- Toggle label shortened from "Create embeddable link?" to **Embed?** (single horizontal row).
- Proxy row auto-hides for Facebook URLs (single-proxy platform).
- Disclaimer text now lists all three Instagram proxies as third-party services.
- All accessibility attributes preserved, including `contentDescription` on the new "Change." link.

### Technical Details
- Minimum Android: 5.0 (API 21)
- Target Android: 15 (API 35)
- Version Code: 25
- APK Size: 4.33 MB (Google), 4.19 MB (F-Droid)
- Tests: 113 unit + 144 instrumentation = **257 tests, 100% pass rate**
- 0 permissions, 0 network calls, 0 hardcoded secrets (verified)

## Download
- [FixupXer-v1.4.7-release.apk](https://github.com/NeatCode-Labs/fixupxer/releases/download/v1.4.7/FixupXer-v1.4.7-release.apk)

---

# FixupXer v1.4.6 - Browser Mode, Professional UI & ReVanced YouTube Support

## 🎯 What's New - Complete Browser Integration, Professional UI & Enhanced YouTube Experience

### Revolutionary Browser Mode 🌐
- **System-Wide URL Cleaning** - Set FixupXer as your default browser to automatically clean ALL links
- **Configurable Action Priorities** - Drag-and-drop interface to customize what happens after cleaning
- **Smart Native App Integration** - Properly handles app-specific links while cleaning tracking
- **ReVanced YouTube Priority** - Automatically prefers ReVanced YouTube over official YouTube app
- **Comprehensive Instructions** - Built-in "How to Use" guide with step-by-step browser setup
- **Flexible Post-Clean Actions** - Choose from native app, browser, share menu, or clipboard
- **Zero-Configuration Option** - Works out-of-the-box with sensible defaults

### Enhanced YouTube Experience 📺
- **ReVanced YouTube Support** - Prioritizes ReVanced YouTube (app.revanced.android.youtube) when available
- **Smart Fallback System** - Falls back to official YouTube if ReVanced isn't installed
- **Ad-Free Integration** - Seamlessly works with community-built ad-free YouTube alternatives
- **Intelligent App Detection** - Automatically detects and prioritizes the best YouTube app available

### Professional UI/UX Improvements ✨
- **Perfect Text Formatting** - All instruction text now displays with proper line breaks and spacing
- **Unicode Typography** - Professional quotation marks ("") and symbols (ⓘ) for clarity
- **Material Design 3 Compliance** - Polished interface following Google's latest design guidelines
- **Enhanced Settings Screen** - Comprehensive configuration options with drag-and-drop priority lists
- **Improved Menu Structure** - Dedicated Settings and Instructions sections for better organization
- **Edge-to-Edge Support** - Full Android 15 compliance with proper system bar handling

### Technical Excellence 🔧
- **HTML Text Rendering** - Proper `<br/>` tags and CDATA sections for perfect formatting
- **Lint-Free Codebase** - All code quality issues resolved for professional standards
- **API Compatibility** - Proper deprecated API handling for Android SDK transitions
- **Thread-Safe Operations** - Enterprise-grade architecture for reliable performance
- **Comprehensive Testing** - All 212 tests passing with 100% success rate

### Browser Mode Setup (Step-by-Step):

1. **Enable Browser Integration:**
   - Open FixupXer → Menu → Settings → Browser integration → Turn ON

2. **Set as Default Browser:**
   - Go to System Settings → Apps → Default apps → Browser → Choose FixupXer

3. **Configure Native Apps (Optional):**
   - For apps you want FixupXer to clean first: System Settings → Apps → [App Name] → "Open supported links" → Turn OFF
   - For apps you want to open directly: Leave "Open supported links" ON

4. **Customize Actions:**
   - In FixupXer Settings → Action priority → Drag to reorder your preferred actions

### New Features Summary:
- 🌐 **Browser Mode** - System-wide automatic URL cleaning
- 📺 **ReVanced YouTube Support** - Priority for ad-free YouTube alternatives
- ⚙️ **Settings Screen** - Comprehensive configuration interface  
- 📋 **Action Priorities** - Customizable post-clean behavior
- 📖 **Instructions Dialog** - Built-in step-by-step guidance
- ✨ **UI Polish** - Professional typography and formatting
- 🔧 **Code Quality** - Zero lint issues and improved architecture

## Technical Details
- Minimum Android: 5.0 (API 21)
- Target Android: 15 (API 35)
- Version Code: 23
- Material Design 3 with full edge-to-edge support

## Download
- [FixupXer-v1.4.6-release.apk](https://github.com/NeatCode-Labs/fixupxer/releases/download/v1.4.6/FixupXer-v1.4.6-release.apk)

---

# FixupXer v1.4.5 - Multi-Subdomain URL Support

## 🎯 What's New - Fixed MailChimp & Enterprise URL Support

### Multi-Subdomain URL Support
- **MailChimp Compatibility** - Fixed false positive detection of MailChimp tracking links
- **Enterprise Services** - Now supports AWS, Azure, Google Cloud, and other enterprise domains
- **Legitimate Multi-Subdomain URLs** - Allows URLs with 3-4 dots (e.g., `customer.us14.list-manage.com`)
- **Maintained Security** - Glued URLs still properly detected and blocked
- **Future-Proof** - Supports modern web architecture with multiple subdomain levels

### URL Detection Improvements
- **Increased Threshold** - Changed domain dots limit from >2 to >5 for better accuracy
- **Smart Detection** - Glued URLs still caught by `tldGluePattern` and `detectGluedUrls()`
- **False Positive Reduction** - Eliminates incorrect blocking of legitimate services
- **Comprehensive Coverage** - Works with email marketing, cloud platforms, CDNs, and enterprise services

### Technical Details
- **Minimal Code Change** - Single line modification in InputValidator.kt
- **Backward Compatible** - All existing functionality preserved
- **Test Verified** - All 112 tests pass, confirming no regressions
- **Performance Unchanged** - No impact on app speed or memory usage

## Technical Details
- Minimum Android: 5.0 (API 21)
- Target Android: 15 (API 35)
- Version Code: 22
- Material Design 3 with full edge-to-edge support

## Download
- [FixupXer-v1.4.5-release.apk](https://github.com/NeatCode-Labs/fixupxer/releases/download/v1.4.5/FixupXer-v1.4.5-release.apk)

---

# FixupXer v1.4.4 - Android 15 Edge-to-Edge Compliance

## 🎯 What's New - Full Android 15 Compliance & Enhanced Responsive Design

### Android 15 Edge-to-Edge Compliance
- **Removed Deprecated APIs** - Eliminated use of `window.statusBarColor` and `window.navigationBarColor`
- **Modern Edge-to-Edge Implementation** - Uses `enableEdgeToEdge()` with proper `SystemBarStyle` configuration
- **API-Level Specific Handling** - Smart navigation bar configurations for all Android versions
- **Navigation Bar Contrast** - Proper `isNavigationBarContrastEnforced` handling for Android 10+
- **Backward Compatible** - Maintains support back to Android 5.0 (API 21)

### Navigation Bar Icon Visibility Fix
- **Android 15 Compliant Solution** - Uses `SystemBarStyle.light()` for Android 8.1+ 
- **Semi-Transparent Scrim** - Adds subtle background for older Android versions (5-7)
- **Visible Icons** - Navigation bar icons remain visible on light backgrounds
- **No Deprecated APIs** - Clean implementation following Google's latest guidelines

### 100% Responsive Design
- **Zero Hardcoded Values** - All dimensions moved to resource files
- **Multi-Screen Support** - Optimized for small (sw320dp), regular, and tablet (sw600dp) screens  
- **Dynamic Sizing** - Button padding, corner radius, and stroke width adapt to screen size
- **Future-Proof** - Ready for any new screen sizes or form factors

### Code Quality & Testing
- **198 Tests Passing** - 86 unit tests + 112 instrumented tests = 100% coverage
- **Clean Code** - Fixed ObsoleteSdkInt lint warning, centralized edge-to-edge handling
- **Performance Verified** - 297ms startup time, ~41MB memory usage
- **Emulator Tested** - Verified on Pixel 9 Pro API 36

### Technical Improvements
- **BaseActivity Centralization** - All edge-to-edge logic in one place
- **Removed Legacy Code** - Eliminated deprecated `applyNavigationBarFix()` method
- **Clean Architecture** - Follows Google's Android 15 design guidelines
- **Professional Build** - Signed APK (4.3MB) and AAB (5.2MB) ready for distribution

## Technical Details
- Minimum Android: 5.0 (API 21)
- Target Android: 15 (API 35)
- Version Code: 21
- Material Design 3 with full edge-to-edge support

## Download
- [FixupXer-v1.4.4-release.apk](https://github.com/NeatCode-Labs/fixupxer/releases/download/v1.4.4/FixupXer-v1.4.4-release.apk)

---

# FixupXer v1.4.3 - UI Polish & Production Ready

## 🎯 What's New - Final UI Refinements & 100% Test Coverage

### UI Polish & Consistency
- **Fixed Button Text Alignment** - "Open" button text now properly aligned with icon in both screens
- **Material Design 3 Consistency** - All dialog buttons now use proper M3 styles
- **Icon Color Consistency** - History item buttons now match primary theme color
- **Enhanced Visual Hierarchy** - Improved button styles throughout the app

### Production Ready
- **100% Test Coverage** - All 198 tests passing (86 unit, 112 instrumentation)
- **Zero Build Warnings** - Clean build with no lint errors or warnings
- **Comprehensive Testing** - Touch targets, accessibility, responsive design all verified
- **Performance Verified** - Startup < 3s, URL processing < 100ms
- **API Compatibility** - Tested on API 21-36 (Android 5.0 to 15)

### Technical Improvements
- **Button Gravity Fix** - Added `app:iconGravity="textStart"` to all MaterialButtons with icons
- **Dialog Button Styles** - Updated to use `Widget.Material3.Button.TextButton.Dialog`
- **Icon Tinting** - Applied consistent `app:tint="@color/primary"` to ImageButtons
- **Code Quality** - Removed redundant attributes and improved consistency

### Test Suite Highlights
- **TouchTargetTest** - All interactive elements meet 48dp minimum
- **AccessibilityTest** - Proper content descriptions verified
- **ResponsiveDesignTest** - Works perfectly on all screen sizes
- **OfflinePerformanceTest** - Fast startup and processing confirmed
- **KeyboardNavigationTest** - Full keyboard navigation support
- **ReleaseTestSuite** - Production build functionality verified

## Technical Details
- Minimum Android: 5.0 (API 21)
- Target Android: 15 (API 35)
- Version Code: 20
- Material Design 3 with Room database

## Download
- [FixupXer-v1.4.3-release.apk](https://github.com/NeatCode-Labs/fixupxer/releases/download/v1.4.3/FixupXer-v1.4.3-release.apk)

---

# FixupXer v1.4.2 - History Feature & Critical Bug Fixes

## 🎯 What's New - Conversion History, Bug Fixes & Better UX

### Conversion History Feature (NEW!)
- **History Tracking** - Now keeps a local history of all your URL conversions
- **Smart Management** - Enable/disable history and set max entries (50-1000)
- **Quick Actions** - Copy or share any past conversion with one tap
- **Privacy First** - History is stored locally only, never leaves your device
- **Easy Cleanup** - Clear all history with one button or long-press to delete individual entries

### Critical Bug Fixes
- **Facebook Prefix Removal** - Fixed m., web., www. prefixes not being removed properly
- **URL Validation** - Fixed false "Multiple URLs detected" errors on legitimate Facebook URLs
- **Bidirectional Conversion** - Fixed facebookez.com not converting back to facebook.com
- **Share Activity** - Fixed duplicate history entries when toggling conversion options
- **App Stability** - Fixed startup crash due to missing UI resources
- **Toggle Functionality** - Restored proper toggle behavior in Share Activity

### UI/UX Improvements
- **Better Navigation** - Donate button moved to footer links for cleaner UI
- **History Button** - New prominent History button where Donate used to be
- **Consistent Dialogs** - All dialogs now follow Material Design 3 guidelines
- **Time Display** - Shows relative time for each conversion (e.g., "2 min ago")
- **Platform Labels** - History shows which platform URLs were from (Twitter, Instagram, etc.)
- **Button Spacing** - Fixed spacing issues in dialog buttons

### Privacy Enhancements
- **Local Storage Only** - History data never leaves your device
- **Opt-in by Default** - History is enabled but can be disabled anytime
- **Updated Disclaimer** - Added information about local history storage
- **No Permissions** - No new permissions required for history feature

### Technical Improvements
- **Room Database** - Uses Android's Room persistence library for reliable storage
- **Efficient Storage** - Automatic cleanup keeps database size minimal
- **Thread-Safe** - All database operations run on background threads
- **Material Design 3** - History UI follows latest design guidelines
- **Comprehensive Testing** - Added 7 test files with ~85 tests for complete coverage

## Technical Details
- Minimum Android: 5.0 (API 21)
- Target Android: 15 (API 35)
- Version Code: 18
- Material Design 3 with Room database

## Download
- [FixupXer-v1.4.2-release.apk](https://github.com/NeatCode-Labs/fixupxer/releases/download/v1.4.2/FixupXer-v1.4.2-release.apk)

---

# FixupXer v1.4.1 - Android 15 Compliance Update

## 🎯 What's New - Full Android 15 Edge-to-Edge Support

### Android 15 Compliance
- **Edge-to-Edge Implementation** - Removed deprecated status/navigation bar APIs for full Android 15 compatibility
- **Modern API Usage** - Implemented WindowCompat.setDecorFitsSystemWindows for proper edge-to-edge display
- **Android 15 Theme** - Added dedicated values-v35 theme configuration
- **Updated Material Design** - Upgraded to Material Design 1.13.0-alpha10 for latest improvements

### UI/UX Improvements
- **Fixed Black Background** - Resolved edge-to-edge black background issue with proper white background
- **Better Title Alignment** - Corrected title and back button positioning in ShareActivity
- **Simplified App Bar** - Clean single-color light blue (#E3F2FD) wave background
- **Enhanced Visual Hierarchy** - Increased AppBarLayout elevation to 4dp for better separation
- **Improved Visual Polish** - Cleaner, more modern appearance throughout the app

### Responsive Design
- **100% Responsive** - All hardcoded dimensions replaced with screen-adaptive resources
- **Multi-Screen Support** - Optimized layouts for small (sw320dp), regular, and tablet (sw600dp) screens
- **Dynamic Padding** - Smart padding adjustments: 16dp (regular), 12dp (small), 24dp (tablets)
- **Dialog Responsiveness** - All dialogs now adapt to different screen sizes
- **Future-Proof Design** - Ready for any screen size or orientation

### Bug Fixes
- Fixed missing string resources for donate dialog
- Resolved build errors from resource dependencies
- Corrected layout spacing inconsistencies
- Fixed dimension resource usage in all layouts

## Technical Details
- Minimum Android: 5.0 (API 21)
- Target Android: 15 (API 35)
- Version Code: 17
- Material Design 3 with edge-to-edge support

## Download
- [FixupXer-v1.4.1-release.apk](https://github.com/NeatCode-Labs/fixupxer/releases/download/v1.4.1/FixupXer-v1.4.1-release.apk)

---

# FixupXer v1.4.0 - Major Engine Overhaul

## 🚀 What's New - Complete Engine Redesign

### Revolutionary Modular Architecture
- **All-New Cleaning Engine** - Replaced monolithic processor with lightning-fast modular architecture
- **11 Specialized Cleaners** - Each platform gets dedicated, optimized cleaning logic
- **964 Unique Tracking Parameters** - Industry-leading coverage across all platforms
- **Deep-Clean Technology** - Multi-pass cleaning ensures nothing gets missed
- **Lightning Fast** - O(1) domain lookup with intelligent caching

### New Platform Support
- **Substack Support Added** - Comprehensive tracking removal while preserving article links
- **Enhanced Coverage** - Every major platform now has dedicated, optimized cleaning

### Performance Breakthroughs
- **5x Faster Processing** - Optimized domain dispatch and parallel cleaning
- **Smart Caching** - LRU cache with 1-hour TTL reduces redundant processing
- **Thread-Safe Design** - Built for modern multi-core devices
- **Memory Efficient** - Stateless cleaners with minimal footprint

### Enhanced URL Detection
- **International Domain Support** - Full IDN (Internationalized Domain Names) support
- **Zero-Width Character Protection** - Removes invisible tracking characters
- **Improved Glued URL Detection** - Better accuracy with fewer false positives
- **Unicode Normalization** - Handles all character encodings properly

### Technical Excellence
- **100% Kotlin** - Modern, null-safe implementation
- **Comprehensive Testing** - Every cleaner thoroughly tested
- **Future-Proof Design** - Easy to add new platforms and parameters
- **Clean Architecture** - Maintainable, extensible, professional-grade code

## Cleaner Capabilities

### Social Media
- **Facebook** - 119 parameters removed (fbclid, fb_action_ids, mibextid, etc.)
- **Instagram** - 67 parameters removed (igshid, ig_mid, ig_cache_key, etc.)
- **Twitter/X** - 99 parameters removed (s, t, ref_src, twclid, etc.)
- **TikTok** - 124 parameters removed (_t, sec_uid, share_app_id, etc.)
- **LinkedIn** - 117 parameters removed (trackingId, lipi, midToken, etc.)
- **Reddit** - 91 parameters removed (context, rdt_cid, feature, etc.)

### E-commerce
- **Amazon** - 147 parameters removed (tag, ref_, ascsubtag, etc.)
- **AliExpress** - 100+ parameters removed (spm, algo_pvid, aff_trace_key, etc.)

### Content Platforms
- **YouTube** - 139 parameters removed (si, pp, feature, embeds_euri, etc.)
- **Substack** - 87 parameters removed (token, r, utm_*, etc.) - **NEW!**

### Search & Utilities
- **Google Search** - URL extraction plus 140 parameters removed
- **General Tracking** - 106 universal parameters for any website

## Why This Matters
- **Superior Privacy** - Most comprehensive tracking removal available
- **Faster Than Ever** - Optimized for instant results
- **Future Ready** - Built to grow with new tracking methods
- **Professional Grade** - Enterprise-quality architecture and testing

## Technical Details
- Minimum Android: 5.0 (API 21)
- Target Android: 15 (API 35)  
- Version Code: 16
- Light theme with Material Design 3

## Download
- [FixupXer-v1.4.0-release.apk](https://github.com/NeatCode-Labs/fixupxer/releases/download/v1.4.0/FixupXer-v1.4.0-release.apk)

---

# FixupXer v1.3.5

## What's New
- **Improved URL Detection** - Refined glued URL detection to reduce false positives
- **Enhanced User Experience** - Error messages now appear in "Processed URL" field instead of temporary toasts
- **Better Domain Boundary Checking** - More accurate detection of URL boundaries

## Technical Changes
- Enhanced InputValidator with improved glued URL detection logic
- Updated UI to display error messages in result field for better visibility
- Improved domain boundary checking for more accurate URL detection
- Updated automated tests to expect error messages in result field

## Bug Fixes
- Fixed false positive "Multiple URLs detected" errors on legitimate URLs
- Improved error message visibility by moving from toast to result field
- Enhanced glued URL detection accuracy

## Compatibility
- Minimum Android: 5.0 (API 21)
- Target Android: 15 (API 35)
- Light theme with Material Design 3

## Download
- [FixupXer-v1.3.5-release.apk](https://github.com/NeatCode-Labs/fixupxer/releases/download/v1.3.5/FixupXer-v1.3.5-release.apk)

---

# FixupXer v1.3.4

## What's New
- **Facebook URL Conversion** - Convert Facebook URLs to facebookez.com for better embedding and privacy
- **Facebook Prefix Removal** - Automatically removes prefixes like m., www., mobile., touch., web. when converting
- **Enhanced Toggle Functionality** - "Create embeddable link?" toggle now supports Facebook, Instagram, and Twitter/X
- **Platform-Agnostic Toggle** - Single toggle controls all supported platforms (Facebook, Instagram, Twitter/X)
- **Improved URL Processing** - Enhanced regex patterns for Facebook URL transformation
- **Consistent Behavior** - Facebook conversion works in both main screen and share screen

## Platform Support
- **Facebook**: facebook.com ↔ facebookez.com (with prefix removal)
- **Instagram**: instagram.com ↔ kkinstagram.com
- **Twitter/X**: x.com/twitter.com ↔ fixupx.com

## Technical Changes
- Added Facebook domain detection and processing scenarios
- Implemented comprehensive Facebook prefix removal regex
- Updated both MainActivity and ShareActivity with Facebook support
- Enhanced URL processing logic for Facebook URLs
- Improved toggle text to be platform-agnostic

## Bug Fixes
- Fixed toggle text specificity (now generic "Create embeddable link?" for all platforms)
- Enhanced Facebook URL processing with proper prefix handling
- Improved consistency between main screen and share screen functionality

## Compatibility
- Minimum Android: 5.0 (API 21)
- Target Android: 15 (API 35)
- Light theme with Material Design 3

## Download
- [FixupXer-v1.3.4-release.apk](https://github.com/NeatCode-Labs/fixupxer/releases/download/v1.3.4/FixupXer-v1.3.4-release.apk)

---

# FixupXer v1.3.3

## What's New
- **Fixed Instagram URL Detection** - Instagram URLs no longer incorrectly flagged as "Multiple URLs detected"
- **Fixed Case Sensitivity** - Instagram post IDs now preserve case (e.g., DLRNJjEx45S won't become lowercase)
- **Enhanced Glued URL Detection** - Improved detection to avoid false positives on legitimate URLs
- **Added Open Button to Share Screen** - Can now open processed URLs directly from the share screen
- **Improved Accessibility** - Fixed hardcoded text and added proper content descriptions
- **Better RTL Support** - Fixed padding issues for right-to-left languages
- **Minor UI Improvements** - Added autofill hints and fixed various layout issues

## Bug Fixes
- Fixed Instagram URLs being rejected with "Multiple URLs detected" error
- Fixed case-sensitive URLs (like Instagram posts) being converted to lowercase
- Fixed glued URL detection triggering on legitimate URLs containing common TLDs
- Fixed missing Open button in ShareActivity
- Fixed hardcoded content descriptions for better accessibility
- Fixed RTL padding symmetry issues
- Fixed missing autofill hints on URL input field

## Technical Changes
- Enhanced InputValidator with TLD-based glued URL detection
- Added comprehensive TLD list for accurate domain boundary detection
- Improved URL validation logic to handle edge cases
- Reduced lint warnings from 97 to 94
- Added proper string resources for all UI text
- Synced all changes to GITHUB folder for F-Droid builds

## Compatibility
- Minimum Android: 5.0 (API 21)
- Target Android: 15 (API 35)
- Light theme with Material Design 3

## Download
- [FixupXer-v1.3.3-release.apk](https://github.com/NeatCode-Labs/fixupxer/releases/download/v1.3.3/FixupXer-v1.3.3-release.apk)

---

# FixupXer v1.3.2

## What's New
- **F-Droid Release Preparation** - Updated build configurations for F-Droid compliance
- **Build System Updates** - Enhanced build configuration for reproducible builds
- **Minor Improvements** - Various small fixes and optimizations

## Technical Changes
- Added dependenciesInfo block to GITHUB build configuration
- Updated version metadata for F-Droid submission
- Improved build reproducibility

## Compatibility
- Minimum Android: 5.0 (API 21)
- Target Android: 15 (API 35)
- Light theme with Material Design 3

## Download
- [FixupXer-v1.3.2-release.apk](https://github.com/NeatCode-Labs/fixupxer/releases/download/v1.3.2/FixupXer-v1.3.2-release.apk)

---

# FixupXer v1.3.1

## What's New
- **Enhanced Security Protection** - Comprehensive input validation to protect against malicious URL attacks
- **Improved Input Handling** - Better detection and rejection of glued URLs, malformed inputs, and attack vectors
- **Crash Prevention** - App now gracefully handles invalid input without freezing or crashing
- **Real-time Validation** - Instant feedback when typing problematic URLs or content
- **Better Error Messages** - Clear, user-friendly notifications for different types of invalid input
- **Share Screen Security** - Enhanced protection for content shared from other apps
- **Performance Optimization** - Faster validation with timeout protection against DoS attacks

## Security Improvements
- **Glued URL Protection** - Prevents attacks like "www.instagram.comwww.x.com"
- **Invisible Character Filtering** - Removes zero-width spaces and control characters
- **URL Encoding Safety** - Handles encoded attacks like "www%2Einstagram.com"
- **Multiple URL Detection** - Rejects input containing multiple URLs
- **Length Limits** - Prevents DoS attacks with overly long inputs
- **Unicode Normalization** - Protects against homograph attacks using lookalike characters

## Technical Changes
- Added comprehensive InputValidator utility class
- Enhanced MainActivity with real-time input validation
- Improved ShareActivity with security hardening
- Added timeout protection for regex operations
- Implemented proper error handling and user feedback
- Updated input sanitization pipeline

## Bug Fixes
- Fixed app crashes when pasting malformed URLs
- Fixed input field issues with special characters
- Fixed share screen handling of invalid content
- Improved error message clarity and consistency

## Compatibility
- Minimum Android: 5.0 (API 21)
- Target Android: 15 (API 35)
- Light theme with Material Design 3

## Download
- [FixupXer-v1.3.1-release.apk](https://github.com/NeatCode-Labs/fixupxer/releases/download/v1.3.1/FixupXer-v1.3.1-release.apk)

---

# FixupXer v1.3.0

## What's New
- **Updated to Android API 35** - Latest Android 15 support
- **Enhanced tracking parameter coverage** - Added comprehensive tracking parameter removal for all supported platforms
- **Improved documentation** - Completely updated SUPPORTED_PLATFORMS.md with accurate platform coverage details
- **Better URL conversion clarity** - Clear distinction between URL conversion (Instagram/Twitter) and tracking removal (all platforms)
- **F-Droid compatibility** - Added dependenciesInfo configuration for F-Droid builds
- **Build system modernization** - Migrated from KAPT to KSP for better performance
- **Enhanced privacy focus** - Comprehensive removal of tracking, analytics, and advertising parameters

## Platform Support
- **URL Conversion**: Instagram (→ kkinstagram.com) and Twitter/X (→ fixupx.com) by user decision
- **Tracking Removal**: 25+ platforms including Facebook, LinkedIn, YouTube, TikTok, Amazon, eBay, and more
- **General Tracking**: All UTM parameters, click IDs, analytics, advertising, and session tracking

## Technical Changes
- Updated compileSdk and targetSdk to 35 (Android 15)
- Migrated from KAPT to KSP annotation processing
- Added F-Droid-specific build configuration
- Enhanced tracking parameter definitions with platform-specific categories
- Improved build performance and reliability

## Bug Fixes
- Fixed build system compatibility issues
- Resolved dependency metadata conflicts for F-Droid
- Improved platform detection accuracy

## Compatibility
- Minimum Android: 5.0 (API 21)
- Target Android: 15 (API 35)
- Light theme with Material Design 3

## Download
- [FixupXer-v1.3.0-release.apk](https://github.com/NeatCode-Labs/fixupxer/releases/download/v1.3.0/FixupXer-v1.3.0-release.apk)

---

# FixupXer v1.2.2

## What's New
- **Fixed paste button freeze issue** - App no longer freezes when pasting large text
- **Enhanced URL detection** - Shows "URL is already clean" or "No action necessary" messages appropriately
- **Added Twitter/X toggle** - Convert between x.com and fixupx.com (appears only for Twitter URLs)
- **Added FxTwitter support** - Now supports fxtwitter.com domain for Twitter embeds
- **Improved Instagram toggle logic** - Better handling of clean/dirty URLs with kkinstagram conversion
- **Fixed share screen layout** - Shared text field now has proper fixed height
- **Added button icons** - Share, Copy, and Open buttons now have visual icons
- **Better user feedback** - Shows "No URL found in clipboard" when appropriate
- **UI improvements** - Removed URL input underline, changed Process button to blue outline style

## Technical Changes
- Migrated to View Binding (removed all findViewById calls)
- Moved all hardcoded strings to resources
- Added dimension resources for responsive design
- Optimized URL finding with 500ms timeout to prevent freezes
- Fixed extra closing brace in UrlProcessor.kt
- Improved coroutine usage for better performance

## Bug Fixes
- Fixed app freeze when pasting large clipboard content
- Fixed share screen text field expanding with content
- Fixed Instagram toggle behavior for all 8 scenarios
- Fixed button color inconsistencies
- Fixed layout issues on different screen sizes

## Compatibility
- Minimum Android: 5.0 (API 21)
- Target Android: 14 (API 35)
- Light theme with Material Design 3

## Download
- [FixupXer-v1.2.2-release.apk](https://github.com/NeatCode-Labs/fixupxer/releases/download/v1.2.2/FixupXer-v1.2.2-release.apk)

---

# FixupXer v1.2.1

## What's New
- Added clickable footer link in main screen
- Added disclaimer dialog for transparency
- Improved UI consistency and fixed minor bugs
- Cleaned up codebase structure

## Technical Changes
- Removed legacy Java code directories
- Updated to latest Android build tools
- Enhanced link processing reliability
- Improved error handling

## Compatibility
- Minimum Android: 5.0 (API 21)
- Target Android: 14 (API 35)
- Light theme with Material Design 3

## Download
- [FixupXer-v1.2.1-release.apk](https://github.com/NeatCode-Labs/fixupxer/releases/download/v1.2.1/FixupXer-v1.2.1-release.apk)

---

Made with ❤️ by NeatCode Labs 