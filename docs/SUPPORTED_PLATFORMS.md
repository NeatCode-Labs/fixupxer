# Supported Platforms for Tracking Parameter Removal

FixupXer removes tracking parameters from URLs using a revolutionary modular cleaning engine with industry-leading coverage.

## 🚀 Engine Architecture

### Modular Design
- **11 Specialized Cleaners** - Each platform has its own optimized cleaner
- **964 Unique Tracking Parameters** - Most comprehensive tracking removal available
- **Deep-Clean Technology** - Multi-pass cleaning ensures nothing gets missed
- **Lightning Fast** - O(1) domain lookup with intelligent caching

## URL Conversion Platforms

These platforms support both tracking parameter removal AND URL conversion to alternative domains:

### Social Media & Communication
- **Facebook** - Removes 119 tracking parameters (fbclid, mibextid, __cft__, __tn__, _branch_match_id, epa, _gl, fb_action_ids, fb_action_types, fb_source, fb_ref, fb_comment_id, etc.) and converts to facebookez.com for better embedding
- **Instagram** - Removes 67 tracking parameters (igshid, ig_cache_key, ig_mid, ig_share_sheet, utm_*, fbclid, etc.) and converts to a user-selectable proxy for better embedding: `toinstagram.com` (default, Primary), `adamlikes.men` (Primary), `instagram7.com` / `kkinstagram.com` (Backup), or any user-added custom proxy. Converted links are sent without the `www.` prefix because these proxies render best at the bare hostname. The legacy proxy `eeinstagram.com` is still recognised in pasted URLs and auto-converted to the active proxy.
- **Twitter/X** - Removes 99 tracking parameters (s, t, ref_src, ref_url, via, twclid, utm_*, etc.) and converts to fixupx.com for better embedding
- **TikTok** - Removes 124 tracking parameters (_t, _r, checksum, sec_uid, share_app_id, share_link_id, tt_from, u_code, etc.) and converts to a user-selectable proxy for better embedding: `tnktok.com` (default, Primary), `tfxktok.com` (Primary), `tiktokez.com` / `kktiktok.com` (Backup), or any user-added custom proxy. Subdomains are preserved (`vm.tiktok.com` → `vm.tnktok.com`) so short links keep working. The dead services `vxtiktok.com` and `tiktxk.com` are still recognised in pasted URLs and auto-converted to the active proxy.

## Tracking Parameter Removal Platforms

These platforms support comprehensive tracking parameter removal:

### Social Media & Communication
- **LinkedIn** - 117 parameters removed (trackingId, lipi, licu, trk, trkEmail, trkInfo, midToken, midSig, eBP, tscp, refId, veh, _l, _e, originalReferer, sessionRedirect, etc.)
- **Reddit** - 91 parameters removed (context, correlator, feature, rdt_cid, ref, ref_campaign, ref_source, $deep_link, $original_url, _branch_match_id, etc.)
- **Pinterest** - Removes e_t, e_t_s, e_t_cs, ouuid, cid, sfo, sfo_s, nic, nic_v, pin_unauth, dpi, i, w, m, n and pin tracking
- **Snapchat** - Removes share_id, locale, attachment_url and snap-specific tracking
- **Discord** - Removes Discord campaign tracking and UTM parameters
- **Telegram** - Removes tgme, start, startgroup, game, voicechat and Telegram tracking
- **WhatsApp** - Removes app_absent, link_medium, link_source and WhatsApp tracking

### Video & Streaming
- **YouTube** - 139 parameters removed (si, pp, feature, embeds_referring_euri, embeds_referring_origin, source_ve_path, embeds_euri, app, attribution_link, ytclid, cbr, cbrver, cos, cosver, gclid, utm_*, fbclid, etc.)
- **Twitch** - Removes tt_medium, tt_content and Twitch tracking
- **Netflix** - Removes trackId, tctx, jb, jbv, dgs and Netflix analytics

### Content & Publishing
- **Substack** - 87 parameters removed (token, r, isFreemail, utm_*, ref, source, gi, triedSigningIn, utm_source, utm_medium, utm_campaign, utm_content, action, component, etc.) while preserving publication_id and post_id
- **Medium** - Removes source, sk, source_user_id, source_post_link and Medium tracking

### E-commerce & Shopping
- **Amazon** - 147 parameters removed (tag, linkCode, camp, creative, ascsubtag, ref_, pf_rd_m, pf_rd_s, pf_rd_r, pf_rd_t, pf_rd_p, pf_rd_i, qid, sr, srs, keywords, ie, rh, pd_rd_w, pd_rd_wg, pd_rd_r, psc, aaxitk, hsa_cr_id, etc.)
- **AliExpress** - 100+ parameters removed (spm, scm, scm_id, scm-url, pvid, algo_expid, algo_pvid, ns, abbucket, acm, utparam, pos, cv, af, mall_affr, sk, dp, terminal_id, aff_fcid, aff_fsk, aff_platform, aff_trace_key, shareId, platform, businessType, title, srcSns, image, sourceType, spreadType, templateId, etc.)
- **eBay** - Removes mkrid, siteid, mkcid, mkevt, mkpid, trksid, campid, toolid, customid, amdata, var, selected and eBay tracking
- **Shopify** - Removes variant, omnisendContactID, sca_ref, mc_cid, mc_eid, _pos, _sid, _ss, _s, _shopify_s, _shopify_sa_t, _shopify_sa_p, _shopify_y, cart_sig and Shopify analytics

### Music & Audio
- **Spotify** - Removes si, dl_branch, dl_mobileapp, go, nd, uri and Spotify share tracking

### Developer & Tech
- **GitHub** - Removes email_token, email_source and GitHub tracking
- **Stack Overflow** - Removes rq, rv, lq, md and SO tracking

### Search & Discovery
- **Google Search** - 140 parameters removed and URL extraction from redirects (ved, ei, usg, source, ust, q, oq, gs_lcp, gs_lp, sclient, utm_*, gclid, dclid, gbv, sei, bih, biw, sa, vet, etc.)
- **Bing** - Removes cvid, FORM, qpvt, qs, sc, sp, sk, ghsh, ghacc, ghpl, ghpr, ghc, ghhc, ghpos and Bing tracking
- **DuckDuckGo** - Removes t, ia, iax, atb, v7-it and DDG tracking

## General Tracking Cleaner

The GeneralTrackingCleaner handles 106 universal tracking parameters for any website:

### UTM Parameters
- All UTM variants: utm_source, utm_medium, utm_campaign, utm_term, utm_content, utm_id, utm_reader, utm_name, utm_referrer, utm_social, utm_social-type, utm_brand, utm_cid, utm_viz_id, utm_pubreferrer

### Click IDs
- gclid (Google), fbclid (Facebook), msclkid (Microsoft), twclid (Twitter), dclid (DoubleClick), yclid (Yandex), gbraid, wbraid, gclsrc

### Analytics & Attribution  
- ref, source, referrer, origin, share_id, si, __a, __d, _rdr, hl, from_ad, from_source, feature, mkt_tok

### Session & Campaign Tracking
- session_id, user_id, timestamp, ts, campaign_id, ad_id, placement, creative, keyword, partner

## Performance Features

### Intelligent Processing
- **Priority-Based Execution** - Extraction → Conversion → Parameter Removal → General
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