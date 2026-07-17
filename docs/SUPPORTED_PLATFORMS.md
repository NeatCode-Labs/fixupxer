# Registered Platforms for Tracking Parameter Removal

FixupXer removes known tracking parameters from URLs using registered, host-bound cleaners.

## 🚀 Engine Architecture

### Modular Design
- **25 domain-specific cleaners + 1 universal cleaner** - each registered platform has its own cleaner
- **Selective parameter removal** - known tracking keys are removed; unknown and functional keys are preserved
- **Deep-Clean Technology** - Multi-pass cleaning ensures nothing gets missed
- **Lightning Fast** - O(1) domain lookup with intelligent caching
- **Private Link Guard** - locally warns when e-mails, tokens, or precise coordinates remain visible in a link

## URL Conversion Platforms

These platforms support both tracking parameter removal AND URL conversion to alternative domains:

### Social Media & Communication
- **Facebook** - Removes known Facebook tracking keys and converts to facebookez.com for better embedding
- **Instagram** - Removes known Instagram tracking keys and converts to a user-selectable proxy for better embedding: `toinstagram.com` (default, Primary), `adamlikes.men` (Primary), `instagram7.com` / `kkinstagram.com` (Backup), or any user-added custom proxy. Converted links are sent without the `www.` prefix because these proxies render best at the bare hostname. The legacy proxy `eeinstagram.com` is still recognised in pasted URLs and auto-converted to the active proxy.
- **Twitter/X** - Removes known Twitter/X tracking keys and converts to fixupx.com for better embedding
- **Bluesky** - Converts supported `bsky.app` post URLs to fxbsky.app for better embedding
- **TikTok** - Removes known TikTok tracking keys and converts to a user-selectable proxy for better embedding: `tnktok.com` (default, Primary), `tfxktok.com` (Primary), `tiktokez.com` / `kktiktok.com` (Backup), or any user-added custom proxy. Subdomains are preserved (`vm.tiktok.com` → `vm.tnktok.com`) so short links keep working. The dead services `vxtiktok.com` and `tiktxk.com` are still recognised in pasted URLs and auto-converted to the active proxy.

## Tracking Parameter Removal Platforms

These registered platforms support host-bound tracking parameter removal:

### Social Media & Communication
- **LinkedIn** - Removes known LinkedIn tracking keys
- **Reddit** - Removes known Reddit tracking keys and extracts `out.reddit.com` redirect destinations
- **Threads** - Removes `igshid` and `xmt`
- **Pinterest** - Removes `e_t`, `e_t_s`, `e_t_cs`, `ouuid`, and `pin_unauth`
- **Snapchat** - Removes `share_id`
- **WhatsApp** - Removes `link_source` and `link_medium`

### Video & Streaming
- **YouTube** - Removes known YouTube tracking keys
- **Twitch** - Removes `tt_medium` and `tt_content`
- **Spotify** - Removes `si`, `dl_branch`, and `dl_mobileapp`; keeps `context` and `uri`
- **Netflix** - Removes `trkid` and `tctx`

### Content & Publishing
- **Substack** - Removes known Substack tracking keys while preserving `publication_id` and `post_id`
- **Wikipedia** - Removes `wprov`
- **Medium** - Removes `source` and `sk`

### E-commerce & Shopping
- **Amazon** - Removes known Amazon tracking keys and canonicalizes recognised product links
- **Google Store** - Removes `hl` and `selections` on `store.google.com`
- **eBay** (`ebay.com`, `ebay.co.uk`, `ebay.de`) - Removes `mkevt`, `mkcid`, `mkrid`, `campid`, `customid`, `toolid`, `ssspo`, `sssrc`, `ssuid`, `widget_ver`, `media`, `_trkparms`, and `_trksid`; keeps `var`, `selected`, `hash`, and `epid`
- **AliExpress** - Removes `spm`, `srcSns`, `businessType`, `templateKey`, `aff_fcid`, `aff_fsk`, `aff_platform`, `aff_trace_key`, `terminal_id`, `afSmartRedirect`, and `utparam-url`; keeps `sku_id`, `currency`, `language`, and `gatewayAdapt`

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

## General Tracking Cleaner

The GeneralTrackingCleaner removes 68 exact universal tracking keys and 10
tracking-key prefixes from any host. It runs after a matching domain cleaner,
so universal keys such as `utm_source` are removed on supported platforms too.
It intentionally preserves non-universal keys such as `ref`, `source`, `si`,
and unknown flag parameters.

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
- **Facebook URLs** are converted to facebookez.com for better embedding and privacy by user decision (with automatic prefix removal)
- **Instagram URLs** are converted to a user-selected proxy (toinstagram.com / adamlikes.men / instagram7.com / kkinstagram.com / custom) for better embedding and privacy by user decision
- **Twitter/X URLs** are converted to fixupx.com for better embedding and privacy by user decision
- **Bluesky post URLs** are converted to fxbsky.app for better embedding and privacy by user decision
- **TikTok URLs** are converted to a user-selected proxy (tnktok.com / tfxktok.com / tiktokez.com / kktiktok.com / custom) for better embedding and privacy by user decision
- All other platforms only have tracking parameters removed

### Parameter Preservation
The app preserves essential parameters needed for functionality:
- Post IDs, product IDs, and content identifiers
- Navigation and pagination parameters
- User-specific content parameters (when not tracking-related)
- Essential query parameters for proper URL functionality

### Privacy Excellence
- Most comprehensive tracking removal available
- Industry-leading parameter coverage per platform
- Continuous multi-pass cleaning
- Future-proof modular architecture 