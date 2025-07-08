# Supported Platforms for Tracking Parameter Removal

FixupXer removes tracking parameters from URLs of popular platforms and provides URL conversion for enhanced privacy and better embedding.

## URL Conversion Platforms

These platforms support both tracking parameter removal AND URL conversion to alternative domains:

### Social Media & Communication
- **Facebook** - Removes fbclid, mibextid, __cft__, __tn__, _branch_match_id, epa, _gl, fb_action_ids, fb_action_types, fb_source, fb_ref, fb_comment_id, fb_story_location, fb_dtsg_ag, fbid and converts to facebookez.com for better embedding
- **Instagram** - Removes igshid, ig_cache_key, ig_mid, ig_share_sheet and converts to kkinstagram.com for better embedding
- **Twitter/X** - Removes s, t, ref_src, ref_url, via and converts to fixupx.com for better embedding

## Tracking Parameter Removal Platforms

These platforms support tracking parameter removal only:

### Social Media & Communication
- **Facebook** - Removes fbclid, mibextid, __cft__, __tn__, _branch_match_id, epa, _gl, fb_action_ids, fb_action_types, fb_source, fb_ref, fb_comment_id, fb_story_location, fb_dtsg_ag, fbid and other FB tracking (URL conversion available via toggle)
- **LinkedIn** - Removes trackingId, lipi, licu, trk, trkEmail, trkInfo, midToken, midSig, eBP, tscp, refId, veh and other professional network tracking
- **Pinterest** - Removes e_t, e_t_s, e_t_cs, ouuid, cid, sfo, sfo_s, nic, nic_v, pin_unauth, dpi, i, w, m, n and pin tracking
- **Snapchat** - Removes share_id, locale, attachment_url and snap-specific tracking
- **TikTok** - Removes _t, _r, checksum, sec_uid, share_app_id, share_link_id, tt_from, u_code, user_id, preview_pb, language, timestamp, aid, iid and TikTok analytics
- **Discord** - Removes Discord campaign tracking and UTM parameters
- **Telegram** - Removes tgme, start, startgroup, game, voicechat and Telegram tracking
- **WhatsApp** - Removes app_absent, link_medium, link_source and WhatsApp tracking

### Video & Streaming
- **YouTube** - Removes feature, pp, si, embeds_referring_euri, embeds_referring_origin, source_ve_path, embeds_euri, app, attribution_link, ytclid and YouTube analytics
- **Twitch** - Removes tt_medium, tt_content and Twitch tracking
- **Netflix** - Removes trackId, tctx, jb, jbv, dgs and Netflix analytics

### Content & Publishing
- **Medium** - Removes source, sk, source_user_id, source_post_link and Medium tracking
- **Substack** - Removes token, r, isFreemail (keeps publication_id and post_id)
- **Reddit** - Removes context, correlator, feature, rdt_cid and Reddit tracking

### E-commerce & Shopping
- **Amazon** - Removes tag, linkcode, camp, creative, ascsubtag, ref_ and affiliate tracking
- **eBay** - Removes mkrid, siteid, mkcid, mkevt, mkpid, trksid, campid, toolid, customid, amdata, var, selected and eBay tracking
- **AliExpress** - Removes spm, scm, scm_id, scm-url, pvid, algo_expid, algo_pvid, ns, abbucket, acm, utparam, pos, cv, af, mall_affr, sk, dp, terminal_id, aff_fcid, aff_fsk, aff_platform, aff_trace_key, shareId, platform, businessType, title, srcSns and extensive Ali tracking
- **Shopify** - Removes variant, omnisendContactID, sca_ref, mc_cid, mc_eid, _pos, _sid, _ss, _s, _shopify_s, _shopify_sa_t, _shopify_sa_p, _shopify_y, cart_sig and Shopify analytics

### Music & Audio
- **Spotify** - Removes si, dl_branch, dl_mobileapp, go, nd, uri and Spotify share tracking

### Developer & Tech
- **GitHub** - Removes email_token, email_source and GitHub tracking
- **Stack Overflow** - Removes rq, rv, lq, md and SO tracking

### Search Engines
- **Bing** - Removes cvid, FORM, qpvt, qs, sc, sp, sk, ghsh, ghacc, ghpl, ghpr, ghc, ghhc, ghpos and Bing tracking
- **DuckDuckGo** - Removes t, ia, iax, atb, v7-it and DDG tracking

## General Tracking Parameters Removed

### UTM Parameters
All UTM parameters are removed: utm_source, utm_medium, utm_campaign, utm_term, utm_content, utm_id, utm_reader, utm_name, utm_referrer, utm_social, utm_social-type, utm_brand, utm_cid, utm_viz_id, utm_pubreferrer

### Click IDs
- Google Click ID (gclid)
- Facebook Click ID (fbclid)
- Microsoft Click ID (msclkid)
- Twitter Click ID (twclid)
- DoubleClick Click ID (dclid)

### Analytics & Advertising
- Google Analytics (ga_source, ga_medium, ga_term, ga_content, ga_campaign, ga_place)
- Yandex Click ID (yclid)
- Mailchimp tracking (mc_cid, mc_eid, mc_tc)
- OpenStat tracking (_openstat)
- Advertising IDs (fbadid, campaign_id, ad_id, ad_set_id, adset_id, placement, creative, keyword, partner, sk, sc_)

### Social Sharing & Referrals
- Social sharing IDs (share_id, igshid, igsh, ref, referrer, source, source_platform, si, __a, __d, _rdr, hl)

### Session & User Tracking
- Session tracking (session_id, user_id, timestamp, ts, ir, vt)
- Context tracking (from_ad, from_tiktok, from_twitter, from_web, contextual_post, ref, ref_src, ref_url, share, shareid, _share, ref_sdk, lr_ck)

## Important Notes

### URL Conversion Behavior
- **Facebook URLs** are converted to facebookez.com for better embedding and privacy by user decision (with prefix removal)
- **Instagram URLs** are converted to kkinstagram.com for better embedding and privacy by user decision
- **Twitter/X URLs** are converted to fixupx.com for better embedding and privacy by user decision
- All other platforms only have tracking parameters removed

### Parameter Preservation
The app preserves essential parameters needed for functionality:
- Post IDs, product IDs, and content identifiers
- Navigation and pagination parameters
- User-specific content parameters (when not tracking-related)
- Essential query parameters for proper URL functionality

### Privacy Focus
- All tracking and analytics parameters are removed
- Advertising and affiliate tracking is eliminated
- User session and behavior tracking is stripped
- Only functional parameters are preserved 