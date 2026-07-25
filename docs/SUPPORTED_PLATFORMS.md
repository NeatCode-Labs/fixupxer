# Registered Platforms for Tracking Parameter Removal

FixupXer removes known tracking parameters from URLs using registered, host-bound cleaners.

## 🚀 Engine Architecture

### Modular Design
- **26 domain-specific cleaners + 1 universal cleaner** - each registered platform has its own cleaner
- **Selective parameter removal** - known tracking keys are removed; unknown and functional keys are preserved
- **Deep-Clean Technology** - Multi-pass cleaning ensures nothing gets missed
- **Lightning Fast** - O(1) domain lookup with intelligent caching
- **Private Link Guard** - locally warns when e-mails, tokens, or precise coordinates remain visible in a link

## URL Conversion Platforms

These platforms support both tracking parameter removal AND URL conversion to
alternative frontends. The in-app picker separates **Embed frontends** (better
previews in chat apps) from **Privacy frontends** (read without an account;
grouped as Recommended, Automatic picker, Community instances, or
Experimental), and every platform accepts user-added custom domains. Each
platform remembers its own selection; conversion toggles are off by default.

### Social Media & Communication
- **Facebook** - Removes known Facebook tracking keys. No embed frontend is bundled — the former `facebookez.com` was retired after it began redirecting to an advertising network — but conversion to a user-added custom frontend is supported
- **Instagram** - Removes known Instagram tracking keys and converts to a user-selectable frontend. Embed proxies: `toinstagram.com` (default), `adamlikes.men`, `instagram7.com`, or any user-added custom proxy. Experimental readers: `kittygr.am`, `kg.meowing.de`, `kittygram.kareem.one`. Converted links are sent without the `www.` prefix because these proxies render best at the bare hostname. The legacy proxy `eeinstagram.com` is still recognised in pasted URLs and auto-converted to the active frontend.
- **Twitter/X** - Removes known Twitter/X tracking keys and converts to a user-selectable frontend. Embed: `fixupx.com` (default). Readers: `xcancel.com`, `nitter.net`, `twitterviewer.net`; automatic instance pickers `twiiit.com` and `farside.link/nitter`; community Nitter instances `nitter.catsarch.com`, `nitter.tiekoetter.com`, `nitter.kareem.one`, `nitter.privacyredirect.com`, `nuku.trabun.org`. The legacy proxies `fxtwitter.com` and `vxtwitter.com` are still recognised and auto-converted to the active frontend.
- **Bluesky** - Converts supported `bsky.app` post URLs to a user-selectable frontend. Embed: `fxbsky.app` (default). Readers: `skylib.coffee`, `skylib.catsarch.com`.
- **TikTok** - Removes known TikTok tracking keys and converts to a user-selectable proxy for better embedding: `tnktok.com` (default, Primary), `tfxktok.com` (Primary), `tiktokez.com` / `kktiktok.com` (Backup), or any user-added custom proxy. Subdomains are preserved (`vm.tiktok.com` → `vm.tnktok.com`) so short links keep working. The dead services `vxtiktok.com` and `tiktxk.com` are still recognised in pasted URLs and auto-converted to the active proxy.
- **Reddit** - Removes known Reddit tracking keys, extracts `out.reddit.com` redirect destinations, and converts to a user-selectable reader: `redlib.catsarch.com` (default), `redlib.privacyredirect.com`, `redlib.nadeko.net`, `redlib.privadency.com`, `safereddit.com` (SFW only); community Redlib instances `red.artemislena.eu`, `redlib.r4fo.com`, `redlib.cow.rip`.
- **YouTube** - Removes known YouTube tracking keys and converts to experimental Invidious readers: `inv.nadeko.net` (default), `invidious.nerdvpn.de`.
- **Pinterest** - Removes `e_t`, `e_t_s`, `e_t_cs`, `ouuid`, and `pin_unauth`, and converts to the `pinterest.bunk.im` reader.
- **Threads** - Removes `igshid` and `xmt`, and converts to the experimental Shoelace reader `shoelace.mint.lgbt`.

Browser mode uses its own separate Reader-only selections for X/Twitter,
Bluesky, Reddit, and Pinterest — see the Browser Mode Guide.

## Tracking Parameter Removal Platforms

These registered platforms support host-bound tracking parameter removal:

### Social Media & Communication
- **LinkedIn** - Removes known LinkedIn tracking keys
- **Snapchat** - Removes `share_id`
- **WhatsApp** - Removes `link_source` and `link_medium`

### Video & Streaming
- **Twitch** - Removes `tt_medium` and `tt_content`
- **Spotify** - Removes `si`, `dl_branch`, and `dl_mobileapp`; keeps `context` and `uri`
- **Netflix** - Removes `trkid` and `tctx`
- **Bilibili** - Removes `vd_source`, `seid`, `share_source`, and `copy_link`; keeps `from` and unknown keys

### Content & Publishing
- **Substack** - Removes known Substack tracking keys while preserving `publication_id` and `post_id`
- **Wikipedia** - Removes `wprov`
- **Medium** - Removes `source` and `sk`

### E-commerce & Shopping
- **Amazon** - Removes known Amazon tracking keys and canonicalizes recognised product links
- **Google Store** - Removes `hl` and `selections` on `store.google.com`
- **eBay** (`ebay.com`, `ebay.co.uk`, `ebay.de`) - Removes `mkevt`, `mkcid`, `mkrid`, `campid`, `customid`, `toolid`, `ssspo`, `sssrc`, `ssuid`, `widget_ver`, `media`, `_trkparms`, and `_trksid`; keeps `var`, `selected`, `hash`, and `epid`
- **AliExpress** - Removes `spm`, `srcSns`, `businessType`, `templateKey`, `aff_fcid`, `aff_fsk`, `aff_platform`, `aff_trace_key`, `terminal_id`, `afSmartRedirect`, `utparam-url`, `pdp_npi`, `afTraceInfo`, `algo_pvid`, `algo_exp_id`, `algo_expid`, `curPageLogUid`, `scm`, `scm_id`, `scm-url`, `utparam`, `aff_short_key`, `aff_request_id`, `gps-id`, `ws_ab_test`, `btsid`, and `mall_affr`; keeps `sku_id`, `currency`, `language`, and `gatewayAdapt`

### Search & Discovery
- **Google Search** - Removes known Google tracking keys and extracts `/url` redirect destinations
- **Google Maps** - Canonicalizes supported `@latitude,longitude,zoom` map URLs to `www.google.com/maps`, dropping their query and fragment
- **Bing** - Removes `cvid` while keeping `q`
- **DuckDuckGo** - Removes `t` and `atb` while keeping `q`

### Offline Redirect Unwrapping
- **Facebook Link Shim** - Extracts the `u` destination from `l.facebook.com/l.php` and `lm.facebook.com/l.php`
- **LinkedIn** - Extracts the `url` destination from `/safety/go`
- **YouTube** - Extracts the `q` destination from `/redirect`
- **Google Ads** - Extracts the `adurl` destination from `googleadservices.com/pagead/aclk`
- **Reddit Mail** - Extracts the encoded destination from `click.redditmail.com`
- **Bluesky Go** - Extracts the `u` destination from `go.bsky.app/redirect`
- **GeoRiot / Geniuslink** - Extracts the `GR_URL` destination from `target.georiot.com/Proxy.ashx`
- **LinkSynergy / Rakuten** - Extracts the `murl` destination from `click.linksynergy.com/link`

## General Tracking Cleaner

The GeneralTrackingCleaner removes 71 exact universal tracking keys and 10
tracking-key prefixes from any host. It runs after a matching domain cleaner,
so universal keys such as `utm_source` are removed on supported platforms too.
It intentionally preserves non-universal keys such as `ref`, `source`, `si`,
`from`, and unknown flag parameters.

## Performance Features

### Intelligent Processing
- **Priority-Based Execution** - Extraction → Conversion → Domain → General
- **Multi-Pass Cleaning** - Up to 5 passes ensure complete cleaning
- **Stabilization Detection** - Stops when URL is fully clean
- **Cache Performance** - LRU cache with 1-hour TTL reduces redundant processing

### International Support
- **IDN Support** - Full Internationalized Domain Names handling
- **Unicode Normalization** - Handles all character encodings
- **Zero-Width Character Removal** - Removes invisible tracking characters

## Important Notes

### URL Conversion Behavior
- **Facebook URLs** are converted only to a user-added custom frontend by user decision (with automatic prefix removal); no built-in Facebook frontend is bundled
- **Instagram URLs** are converted to a user-selected embed proxy, experimental reader, or custom domain by user decision
- **Twitter/X URLs** are converted to the fixupx.com embed frontend, a user-selected reader (xcancel, Nitter instances, automatic pickers), or a custom domain by user decision
- **Bluesky post URLs** are converted to the fxbsky.app embed frontend, a SkyLib reader, or a custom domain by user decision
- **TikTok URLs** are converted to a user-selected embed proxy or custom domain by user decision
- **Reddit, YouTube, Pinterest, and Threads URLs** are converted to user-selected reader frontends or custom domains by user decision (off by default)
- All other platforms only have tracking parameters removed
- Conversions are pure local string transformations; FixupXer never contacts any frontend service

### Parameter Preservation
The app preserves essential parameters needed for functionality:
- Post IDs, product IDs, and content identifiers
- Navigation and pagination parameters
- User-specific content parameters (when not tracking-related)
- Essential query parameters for proper URL functionality

### Privacy Excellence
- Host-bound cleaners remove documented tracking keys while preserving unknown and functional parameters
- Registered platform coverage with multi-pass cleaning
- Future-proof modular architecture