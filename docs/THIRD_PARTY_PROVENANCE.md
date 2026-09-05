# Third-Party Provenance Audit

## Policy

FixupXer may adopt publicly documented **behaviour** from open-source URL
cleaners. It does not copy their source code, regular expressions, rule
catalogues, tests, parsers, data files, or architecture. Every FixupXer
implementation is written independently in the FixupXer Kotlin/cleaner
architecture and is covered by FixupXer's own fixtures.

Each source is credited below. **Inspiration only** means a source may inform
problem discovery or fixture design but supplies no adopted rule behaviour.
**Adapted (behaviour)** means FixupXer may independently implement the stated
observable URL transformation, subject to its safety contract: offline-only,
host-boundary checks, positive and negative fixtures, and removal of known
keys while preserving unknown keys unless a redirect wrapper is being safely
unwrapped.

This audit was performed on 2026-07-16. The Léon source was inspected at
commit `3d26d22aa828ce4ed2dc5b227fdb194f7c7309db` (2026-07-07), verified with
`git log`. Léon is licensed GPL-3.0-or-later. No code has been copied from
Léon.

## Source registry

| Source | Project URL | License | Upstream revision / date | Status and what FixupXer takes |
|---|---|---|---|---|
| Léon | https://github.com/leon-cleaning-services/leon | GPL-3.0-or-later | `3d26d22aa828ce4ed2dc5b227fdb194f7c7309db`, 2026-07-07 | **Adapted (behaviour).** The audited parameter-removal, canonicalisation, and offline redirect-unwrapping behaviour in the table below. |
| Untracker by zhanghai | https://github.com/zhanghai/Untracker | Apache-2.0 | No revision or date was pinned for this audit. | **Inspiration only.** Publicly documented URL-cleaning scope and independently devised fixture ideas; no source, rules, or catalogue imported. |
| FxEmbed / fxbsky.app | https://github.com/FxEmbed/FxEmbed | MIT | No revision or date was pinned for this audit. | **Inspiration only.** Publicly documented embedding/proxy-link behaviour; the `fxbsky.app` hostname swap is independently implemented and no source, rules, or catalogue is imported. |
| ClearURLs Rules | https://github.com/ClearURLs/Rules | LGPL-3.0 | No revision or date was pinned for this audit. | **Inspiration only / test oracle.** May be consulted to compare expected outcomes; no ClearURLs rule catalogue, regex, or data file is imported. |

Only Léon is a pinned upstream source for this document. A future behaviour
adoption from any other registry entry must record the exact upstream revision
and date before implementation.

## Léon sanitizer audit

Scope: all **79** sanitizer instances registered in Léon's
`app/src/main/kotlin/com/svenjacobs/app/leon/startup/ContainerInitializer.kt`
at the pinned commit. “Deletes from `?` onward” is deliberate wording: Léon's
`RegexFactory.AllParameters` is `\?.*`, so it removes the query and any later
fragment when a query is present.

The verdict is a planning decision, not a statement that the FixupXer feature
already exists. In rows marked **adopted**, FixupXer implements the selected
behaviour independently and, where Léon deletes a whole query, deliberately
uses the safer known-key/keep-unknown mechanism noted in the row. Rows marked
**intentional difference** describe behaviour FixupXer already covers for the
same target but deliberately does differently.

| Léon sanitizer | Target domain(s) | Verified upstream behaviour | Verdict and reason |
|---|---|---|---|
| `AdobeMarketoEngageSanitizer` | Any host | Removes parameters whose names start with `mkt_`. | **intentional difference** — FixupXer removes only the proven `mkt_tok` key; the blanket `mkt_` prefix is deliberately not adopted. |
| `AliexpressSanitizer` | AliExpress subdomains on `/item/` paths | Deletes from `?` onward. | **adopted** — third-batch AliExpress support; FixupXer removes known AliExpress keys and keeps unknown keys instead of deleting the whole query. |
| `BlueskyRedirectSanitizer` | `go.bsky.app/redirect` | Decodes and returns the `u` parameter. | **adopted** — planned offline Bluesky redirect unwrapping with HTTP(S) destination validation and fixtures. |
| `AmazonProductSanitizer` | `amazon.<TLD>` product paths | Extracts the product token from `d`/`dp` or `gp/product`-style paths and emits `<origin>/dp/<token>/`. | **intentional difference** — FixupXer already canonicalises Amazon products, but validates its supported ASIN forms and emits its own canonical form. |
| `AmazonSanitizer` | `amazon.<TLD>` paths | Removes exact `ref` and `ref_` query parameters. | **adopted** — existing Amazon coverage follows the known-key approach; this behaviour remains part of that coverage. |
| `AolSearchSanitizer` | `search.aol.com` | Decodes and returns the path value following `RU=`. | **rejected (backlog)** — niche search redirect with no fixture or demand. |
| `AtAnalyticsSanitizer` | Any host | Removes parameter names starting with `at_`. | **behavior adopted** — FixupXer's `GeneralTrackingCleaner` already removes the `at_` prefix on every host (pre-existing, independent implementation). |
| `AutoTraderSanitizer` | `autotrader.co.uk` | Deletes from `?` onward. | **rejected (backlog)** — regional marketplace without fixtures or demand. |
| `BilibiliSanitizer` | `bilibili.com` | Removes `vd_source`, `seid`, `from`, `share_source`, and `copy_link`. | **adopted** — curated Bilibili support removes four known share keys on `bilibili.com` and subdomains; FixupXer deliberately preserves generic `from` and unknown keys. |
| `CarGurusSanitizer` | `cargurus.co.uk` | Deletes every query parameter except `listingId` and `entitySelectingHelper.selectedEntity`. | **rejected (backlog)** — regional marketplace without fixtures or demand. |
| `ChangeSanitizer` | `change.org` | Deletes from `?` onward. | **rejected (backlog)** — whole-query approach is unsafe without a demonstrated fixture. |
| `CxAnalyticsSanitizer` | Any host | Removes parameters beginning `cx_`, `cxrecs_s`, or `mibextid`. | **rejected (backlog)** — generic cross-site keys need a fixture and false-positive review. |
| `DouyinSanitizer` | `douyin.com`, `v.douyin.com`, `iesdouyin.com` | Extracts the first HTTP(S) URL before a query/whitespace, then removes matching trailing share-password suffixes. | **rejected (backlog)** — regional platform and nonstandard text transformation lack fixtures. |
| `DingtalkSanitizer` | `dingtalk.com` and `dingtalk.cn` subdomains | Removes `from`, `scene`, `channel`, `source`, and `refer`. | **rejected (backlog)** — no fixtures or user demand. |
| `DianpingSanitizer` | `dianping.com` | Removes `from`, `source`, `channel`, `refer`, `wm`, `c`, and `wx*`. | **rejected (backlog)** — regional service without fixtures or demand. |
| `EbaySanitizer` | `ebay.<TLD>/itm/` | Deletes from `?` onward. | **adopted** — third-batch eBay support; FixupXer removes known eBay keys and preserves unknown keys. |
| `EchoboxSanitizer` | Any host | Removes a trailing `#Echobox=...` fragment. | **adopted** — the general cleaner removes only a fragment that exactly starts with `Echobox=`; all other fragments are preserved. |
| `ElFinancieroSanitizer` | `elfinanciero.com.mx` | Removes `outputType`. | **rejected (backlog)** — regional news site without fixtures or demand. |
| `EmptyParametersSanitizer` | Any host | Removes empty `name=` query parameters. | **rejected (backlog)** — global mutation needs explicit compatibility fixtures. |
| `FacebookAnalyticsSanitizer` | Any host | Removes names beginning `fb_`, `fbclid`, `sfnsn`, or `cHash`. | **adopted** — accepted Facebook/analytics tracking behaviour, independently represented by known-key cleaning. |
| `FacebookSanitizer` | `facebook.com` and `m.facebook.com` | Deletes every query parameter except `id` and `story_fbid`. | **intentional difference** — FixupXer preserves unknown and functional parameters rather than using Léon's allow-list/whole-query pattern. |
| `FastCompanySanitizer` | `fastcompany.com` | Deletes from `?` onward. | **rejected (backlog)** — whole-query approach without fixtures or demand. |
| `FlipkartSanitizer` | Flipkart subdomains | Deletes from `?` onward. | **rejected (backlog)** — regional marketplace without fixtures or demand. |
| `FeishuSanitizer` | `feishu.cn` and `feishu.net` subdomains | Removes `from`, `scene`, `channel`, `source`, and `refer`. | **rejected (backlog)** — no fixtures or user demand. |
| `GeoRiotSanitizer` | `target.georiot.<TLD>/Proxy.ashx` | Decodes and returns the `GR_URL` parameter. | **adopted** — offline GeoRiot/Geniuslink unwrapping on exact host `target.georiot.com` and path `/Proxy.ashx` only, with strict single percent-decode and HTTP(S) destination validation. |
| `GoogleAdsSanitizer` | `googleadservices.com`, including `pagead/aclk` links | Decodes and returns `adurl`. | **adopted** — offline Google Ads redirect unwrapping, with destination validation and fixtures. |
| `GoogleAnalyticsSanitizer` | Any host | Removes names beginning `ga_`, `utm_`, `gclid`, or `gad_`. | **adopted** — accepted known analytics-key behaviour, independently implemented. |
| `GoogleMapsSanitizer` | `google.com/maps` and `maps.google.com` | Extracts an `@latitude,longitude,zoom` path segment and emits `<scheme>://www.google.com/maps/<coordinates>`, preserving the input scheme. | **adopted** — Google platform expansion; FixupXer will use independently tested coordinate canonicalisation. |
| `GoogleSearchSanitizer` | `google.<TLD>/url` | Decodes and returns `url` or `q`. | **intentional difference** — FixupXer already unwraps Google redirects but applies its own destination validation and query-cleaning rules. |
| `GoogleStoreSanitizer` | `store.google.com` | Removes `hl` and `selections`. | **adopted** — accepted Google behaviour, subject to FixupXer fixtures. |
| `HeiseSanitizer` | `heise.de` | Deletes from `?` onward. | **rejected (backlog)** — regional news site without fixtures or demand. |
| `IkeaSanitizer` | `ikea.com` | Deletes from `?` onward. | **rejected (backlog)** — whole-query approach without fixtures or demand. |
| `IlMessaggeroSanitizer` | `archivio.ilmessaggero.it` | Deletes every query parameter except `topic`. | **rejected (backlog)** — regional news archive without fixtures or demand. |
| `InstagramSanitizer` | `instagram.com` | Removes `igsh`. | **adopted** — existing Instagram coverage independently removes this known tracking key. |
| `JdoqocySanitizer` | `jdoqocy.com/click` | Decodes and returns `url`. | **rejected (backlog)** — affiliate wrapper without fixtures or demand. |
| `JodelSanitizer` | `shared.jodel.com/a/key_live_...` | URL-decodes `data`, Base64-decodes it as JSON, and returns its `$android_url` value. | **rejected (backlog)** — niche encoded wrapper without fixtures or demand. |
| `JdSanitizer` | `jd.com` and `3.cn` | Normalises `&amp;`, removes `share` and `jkl`, then removes a trailing `?` or `&`. | **rejected (backlog)** — regional marketplace without fixtures or demand. |
| `KoganSanitizer` | `kogan.com` | Deletes from `?` onward. | **rejected (backlog)** — regional marketplace without fixtures or demand. |
| `KuaishouSanitizer` | `kuaishou.com` and `v.kuaishou.com` | Removes `share`, `userId`, and `photoId`. | **rejected (backlog)** — regional platform without fixtures or demand. |
| `LatinaTodaySanitizer` | `latinatoday.it` | Deletes from `?` onward. | **rejected (backlog)** — regional news site without fixtures or demand. |
| `LazadaSanitizer` | `lazada.com.my` | Deletes from `?` onward. | **rejected (backlog)** — regional marketplace without fixtures or demand. |
| `LinkedInSanitizer` | `linkedin.com` | Removes `rcm`. | **adopted** — accepted LinkedIn known-key behaviour, independently implemented with functional keys retained. |
| `LinkSynergySanitizer` | `linksynergy.<TLD>/link` | Decodes and returns `murl`. | **adopted** — offline LinkSynergy/Rakuten unwrapping on exact host `click.linksynergy.com` and path `/link` only, with strict single percent-decode and HTTP(S) destination validation. |
| `MetaAdSanitizer` | Any host | Removes `ad_id`, `adset_id`, `campaign_id`, `gc_id`, `h_ad_id`, and `placement`. | **rejected (backlog)** — global ad-key removal needs targeted fixtures and false-positive review. |
| `MyDealzParametersSanitizer` | `mydealz.de`, `chollometro.com`, `dealabs.com`, `desidime.com`, `hotukdeals.com`, `nl.pepper.com`, `pepper.it`, `pepper.pl`, `pepper.ru`, `promodescuentos.com`, `pelando.com.br`, `preisjaeger.at` | Deletes from `?` onward. | **rejected (backlog)** — regional deal sites without fixtures or demand. |
| `MyDealzRedirectsSanitizer` | `mydealz.de`, `chollometro.com`, `dealabs.com`, `desidime.com`, `hotukdeals.com`, `nl.pepper.com`, `pepper.it`, `pepper.pl`, `pepper.ru`, `promodescuentos.com`, `pelando.com.br`, `preisjaeger.at` | Rewrites `https://<host>/share-deal-from-app/<id>` to `https://<host>/deals/a-<id>`. | **rejected (backlog)** — regional redirect/canonicalisation without fixtures or demand. |
| `MeituanSanitizer` | `meituan.com`, `meituan.cn`, and `meituan.net` | Removes `from`, `source`, `channel`, `refer`, `wm`, `c`, and `wx*`. | **rejected (backlog)** — regional service without fixtures or demand. |
| `NetflixSanitizer` | `netflix.com` and `help.netflix.com` | Deletes from `?` onward. | **adopted** — third-batch Netflix support; FixupXer removes known Netflix keys and preserves unknown keys. |
| `NewEggSanitizer` | `newegg.<TLD>` product paths | Removes the category path before `/p/<product-id>`. | **rejected (backlog)** — no fixture or user demand for this canonicalisation. |
| `PearlSanitizer` | `pearl.de` | Deletes from `?` onward. | **rejected (backlog)** — regional marketplace without fixtures or demand. |
| `PddSanitizer` | `pinduoduo.com`, `pdd.com`, and `yangkeduo.com` | Removes `pid`, `share_uin`, `track_id`, and `goods_sign`. | **rejected (backlog)** — regional marketplace without fixtures or demand. |
| `RedditMailSanitizer` | `click.redditmail.com` | Extracts and URL-decodes the encoded destination after the first path segment, then deletes its query. | **adopted** — planned offline Reddit Mail wrapper unwrapping; FixupXer validates the decoded HTTP(S) destination and cleans it through its own pipeline. |
| `RedditOutSanitizer` | `out.reddit.com` | Decodes and returns `url`. | **intentional difference** — FixupXer already unwraps this wrapper but validates HTTP(S) destinations and leaves an unextractable wrapper intact. |
| `RedditSanitizer` | `reddit.com` | Deletes from `?` onward. | **intentional difference** — FixupXer retains unknown and functional query keys rather than deleting every query. |
| `SalesforceParametersSanitizer` | Any host | Removes names beginning `utm_` or `sfmc_`. | **intentional difference** — FixupXer covers `utm_` and removes only the proven `sfmc_activityid` key; the blanket `sfmc_` prefix is deliberately not adopted. |
| `SessionIdsSanitizer` | Any host | Removes names beginning `sessionid` or `jsessionid`. | **rejected (backlog)** — global session-key removal needs compatibility fixtures. |
| `ShopeeSanitizer` | `shopee.com.my` | Deletes from `?` onward. | **rejected (backlog)** — regional marketplace without fixtures or demand. |
| `SnapchatSanitizer` | `snapchat.com` | Deletes from `?` onward. | **adopted** — curated Snapchat support removes `share_id`; FixupXer retains unrelated keys instead of deleting the entire query. |
| `SpiegelSanitizer` | `spiegel.de` | Deletes from `?` onward. | **rejected (backlog)** — regional news site without fixtures or demand. |
| `SpotifySanitizer` | `spotify.com` and `open.spotify.com` | Deletes from `?` onward. | **adopted** — curated Spotify support removes `si`, `dl_branch`, and `dl_mobileapp` while preserving context and `uri`, unlike Léon's whole-query deletion. |
| `SubstackSanitizer` | `substack.com` and `open.substack.com` | Deletes from `?` onward. | **intentional difference** — FixupXer already removes known Substack tracking keys while preserving unknown and functional keys. |
| `TheGuardianSanitizer` | `theguardian.com` | Deletes from `?` onward. | **rejected (backlog)** — whole-query approach without fixtures or demand. |
| `ThreadsSanitizer` | `threads.net` and `threads.com` | Deletes from `?` onward. | **adopted** — curated Threads support removes `igshid` and `xmt` only, preserving unknown keys. |
| `TiktokSanitizer` | `tiktok.com` | Deletes from `?` onward. | **intentional difference** — FixupXer already removes known TikTok tracking keys and preserves functional or unknown keys. |
| `TaobaoSanitizer` | `taobao.com`, `tmall.com`, `tb.cn`, `e.tb.cn`, and `m.tb.cn` | Normalises `&amp;`, removes `smid`, `ut_ma`, `track_id`, `spm`, `share_crt_v`, `tbkt`, `isg`, and `tk`, then trims a trailing separator. | **rejected (backlog)** — regional marketplace without fixtures or demand. |
| `WebtrekkSanitizer` | Any host | Removes names beginning `wt_`. | **adopted** — the general cleaner removes the `wt_` analytics prefix (alongside the existing `wt.` Webtrends form), backed by FixupXer fixtures. |
| `WikipediaSanitizer` | `wikipedia.org` and subdomains | Removes `wprov`. | **adopted** — curated Wikipedia support. |
| `WechatSanitizer` | `weixin.qq.com` and `url.cn` | Removes `__biz`, `mid`, `idx`, `sn`, `scene`, and `wx_header`. | **rejected (backlog)** — regional platform and identifiers may be functional; no fixtures. |
| `WeiboSanitizer` | `weibo.com` and `m.weibo.cn` | Removes `from`, `refer`, and `share_token`. | **rejected (backlog)** — regional platform without fixtures or demand. |
| `XSanitizer` | `twitter.com` and `x.com` | Deletes from `?` onward. | **intentional difference** — FixupXer already removes known Twitter/X tracking keys while preserving functional or unknown keys. |
| `XiaohongshuSanitizer` | `xiaohongshu.com` and `xhslink.com` | Deletes from `?` onward. | **rejected (backlog)** — regional platform without fixtures or demand. |
| `YahooReferrerSanitizer` | Any host | Removes `guccounter`, `guce_referrer`, and `guce_referrer_sig`. | **adopted** — the general cleaner removes only these three exact Yahoo/Guce referrer keys case-insensitively on any host; near-match keys are preserved. |
| `YahooSearchSanitizer` | `search.yahoo.com` | On `/search`, deletes all parameters except `p`; otherwise decodes and returns the `RU=` destination. | **rejected (backlog)** — no fixtures or user demand. |
| `YandexSanitizer` | `yandex.com` and `ya.ru` | Deletes every query parameter except `text`. | **rejected (backlog)** — regional search engine and whole-query approach lack fixtures. |
| `YoutubeMusicSanitizer` | `music.youtube.com` | Removes the `music.` hostname label, producing the corresponding `youtube.com` URL. | **intentional difference** — FixupXer keeps the YouTube Music host and cleans only its known tracking keys. |
| `YoutubeRedirectSanitizer` | `youtube.com/redirect` | Decodes and returns `q`. | **adopted** — planned offline YouTube redirect unwrapping with HTTP(S) destination validation. |
| `YoutubeSanitizer` | `youtube.com`, `music.youtube.com`, and `youtu.be` | Deletes every query parameter except `v`, `search_query`, `list`, `t`, and `channel_id`. | **intentional difference** — FixupXer already retains its documented functional keys and removes only known tracking keys. |
| `YoutubeShortUrlSanitizer` | `youtu.be` | Rewrites a short URL to `https://www.youtube.com/watch?v=<video-id>`. | **intentional difference** — FixupXer deliberately retains the short host and only cleans its known query keys. |
| `ZhihuSanitizer` | `zhihu.com` | Removes `share_redirect` and `share_code`. | **rejected (backlog)** — regional platform without fixtures or demand. |

### Planned behaviour not represented by a Léon sanitizer

The following accepted plan items have no separately registered Léon sanitizer
at the pinned commit, so they cannot appear as rows in the complete Léon table
above:

- Facebook `l.php` (`u`) and LinkedIn `/safety/go` (`url`) offline wrappers;
- Twitch `tt_medium` and `tt_content`;
- Pinterest `e_t`, `e_t_s`, `e_t_cs`, `ouuid`, and `pin_unauth`;
- WhatsApp `link_source` and `link_medium`;
- Medium `source` and `sk`; and
- Bing `cvid` plus DuckDuckGo `t` and `atb`.

These accepted behaviours are independently specified for FixupXer; they are
not attributed to Léon. Each remains subject to the same FixupXer fixture,
host-boundary, offline-only, and HTTP(S)-destination checks.

## Maintenance

At every major feature release, compare Léon's registered sanitizers with this
pinned commit and run the same acceptance gate for each new candidate:

1. record the exact upstream commit and source behaviour;
2. add a positive fixture and a host-boundary negative fixture;
3. require an offline-only transformation; and
4. remove only known keys while keeping unknown keys, unless safely extracting
   an explicitly supported redirect destination.

Do not import an upstream rule catalogue as a shortcut. Revisit a
**rejected (backlog)** row only when it has user demand and those acceptance
fixtures.

## User-controlled local exports

FixupXer also ships an optional **Settings > Backup & restore** JSON export
(format `fixupxer-local-backup`, schema v1) for whitelisted preferences, custom
rules, and remembered after-clean destinations. That file format is defined and
implemented entirely in FixupXer; it is not derived from Léon, Untracker, or
other third-party cleaner projects. URL history and internal rollback snapshots
are excluded by design.
