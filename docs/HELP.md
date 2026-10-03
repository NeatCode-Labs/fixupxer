# FixupXer Help

FixupXer processes links locally. It removes known tracking parameters and can optionally rewrite supported social links for another frontend. It does not load web pages or make network requests. This guide covers everyday use, Browser mode, custom frontends, custom URL rules, history, and backups.

Domains such as `shop.example.com`, `news.example.com`, `reader.example`, and `social.example` below are fictional placeholders, not real websites or recommendations.

From **v2.9.0**, open **Help** beside **What's new?** in the Main or Share menu, or use a topic-specific Help link beside a feature. The external-link icon and **Opens in browser** label mean that a separate browser opens this online guide. Older app versions can use this guide too; their menu labels and Help entry points may differ.

## Contents

- [Getting started](#getting-started)
- [Link cleaning and Private Link Guard](#link-cleaning)
- [Alternative frontends](#alternative-frontends)
- [Add custom proxies](#custom-proxies)
- [Browser mode](#browser-mode)
  - [Set up Browser mode](#set-up-browser-mode)
  - [After-clean choices](#after-clean)
  - [Preferred browser](#preferred-browser)
  - [Saved app choices](#saved-app-choices)
  - [Browser frontends](#browser-frontends)
  - [Configuration status](#configuration-status)
  - [Browser troubleshooting](#browser-mode-troubleshooting)
- [Custom URL rules](#custom-rules)
  - [Build your first rule](#your-first-rule-step-by-step)
  - [Actions and examples](#actions)
  - [Test Lab](#test-lab)
  - [Teach from example](#teach-from-example)
  - [Import, export and rollback](#import-export-and-rollback)
- [History and backup](#history-and-backup)
- [App settings](#app-settings)
- [Troubleshooting](#troubleshooting)

<a id="getting-started"></a>

## Getting started

To clean a link, use **Share** in the app that contains it and choose **FixupXer**, or open FixupXer, paste the link, and tap **Process URL**. Review the before-and-after result, then choose an available action such as **Copy**, **Share**, or **Open**.

You can also select a link in another app and choose **Clean link** from Android's text-selection menu. FixupXer processes it on the device. If you hand the result to a browser, native app, or sharing app, that recipient receives the URL and handles any network access.

Cleaning and conversion are separate. Cleaning removes recognized tracking data while preserving other URL information. Conversion is optional and changes a supported social-media host to an embed or reader frontend.

[Back to contents](#contents)

<a id="link-cleaning"></a>

## Link cleaning and Private Link Guard

FixupXer's built-in cleaners remove known tracking parameters for supported sites, followed by a general cleaner for common tracking keys on other sites. The app keeps unknown parameters because they may control a search, product, article, or other useful page behavior. Cleaning is selective; it cannot guarantee that every site-specific identifier has been removed.

Some supported redirect links can be unwrapped locally when their destination is structurally valid. FixupXer does not visit the original site to resolve a redirect.

For example:

```text
https://shop.example.com/product/42?color=blue&utm_source=newsletter
→ https://shop.example.com/product/42?color=blue
```

The known campaign tag is removed and the functional `color` parameter stays.
**No changes made** means that the current settings and applicable rules did
not modify the URL. It does not guarantee that every identifier is harmless
or that the link contains no tracking.


**Private Link Guard** checks for a short list of high-confidence clues in the URL, such as an email address, sign-in token, or precise latitude and longitude. For example, `https://files.example/share?email=alex@example.org&token=abcdefgh1234` can trigger a warning if those values remain after processing. The check runs offline. It cannot tell whether the data is intentional, encrypt the link, or detect every private value. You can go back, remove a query parameter, or continue anyway.

When History is on, a sensitive original URL is never saved. If processing removes all detected sensitive data and an entry is saved, History stores only the processed result with an **Input redacted for privacy** marker. If detected sensitive data remains in the result, that URL is not saved to History. History is optional.

[Back to contents](#contents)

<a id="alternative-frontends"></a>

## Alternative frontends

An alternative frontend is another website that can display supported social content. FixupXer rewrites and copies the URL locally. A browser or receiving app may contact the frontend when it loads the page or generates a preview. Copying the link alone does not contact the service.

- **Embed frontends** are designed to improve previews in chat apps. The URL payload passes through that third-party service.
- **Privacy readers** focus on reading supported public content without an account. For chat previews, choose an embed frontend; the actual preview depends on the service and receiving app.
- **Custom frontends** use a domain you provide. FixupXer does not verify that a custom domain is available, trustworthy, or private, and it does not present custom targets as verified privacy readers.

Using an illustrative post URL, the built-in X catalog offers these kinds of destinations:

```text
Original:       https://x.com/alex/status/123
Embed example:  https://fixupx.com/alex/status/123
Reader example: https://xcancel.com/alex/status/123
```

The host changes while the post path stays the same. These are examples of the current catalog, not service endorsements or availability promises. FixupXer creates the rewritten URL locally. A browser or chat app may contact the selected site when it loads the page or makes a preview.

For example, if you send an X post in a chat and want a preview, an embed frontend is designed for that use; the chat and service may or may not show one. If you open a public post in a browser to read it, a reader frontend is designed to display the content. What you can see still depends on the reader and the post.

Availability depends on the platform and processing context. Main and Share choices are configured separately from Browser mode choices. For the current platform-specific options, see [Supported Platforms](https://github.com/NeatCode-Labs/fixupxer/blob/main/docs/SUPPORTED_PLATFORMS.md).

For links on the Main screen or when another app sends a link to FixupXer with Android's Share action, use the platform's conversion toggle and **Change** link. You can also manage those selections under **Settings > Link processing > Alternative frontends**. Each platform remembers its choice. Turn off the conversion toggle to keep the original host. In Browser mode, **Clean only** skips frontend conversion while cleaning and enabled custom rules still apply.

**Main/Share** settings apply to links processed on the Main screen or received from another app through Android's Share action. **Browser** settings apply when Android routes an eligible web link to FixupXer as a browser handler. The choices are saved separately. For example, you can use the X embed for Main/Share and an X reader for Browser mode; changing one choice does not change the other.

[Back to contents](#contents)

<a id="custom-proxies"></a>

## Add custom proxies

A custom proxy is a frontend domain that you add yourself. Here, "proxy" means an alternate website host for supported links; it is not a VPN and does not route the phone's general internet traffic.

For a fictional example, imagine a compatible X frontend at `social.example`. If you add and select it, this supported link:

`https://x.com/alex/status/123`

becomes:

`https://social.example/alex/status/123`

FixupXer changes the host in that URL; it does not check whether the service works or route the phone's general internet traffic. A VPN changes the network route instead of rewriting this supported link.

To add one for Main and Share:

1. Open **Settings > Link processing > Alternative frontends**.
2. Choose a platform.
3. Tap **Add custom proxy**.
4. Enter the domain of your chosen frontend service and tap **Add**. Use a domain rather than a full post URL.
5. Select the new entry in the platform picker to use it.

You can also open the platform picker with **Change** beside a Main or Share conversion toggle. Adding a domain makes it available; selecting it chooses the active frontend for that platform.

To add a custom frontend for Browser mode, open **Settings > Configure Browser mode > Configure Browser frontends**, choose a platform, then use **Change frontend** and **Add custom proxy**. Select the new frontend and tap **Save** in the parent Browser dialog. Browser selections are separate from Main and Share selections.

The frontend roster is shared. Editing or removing a domain can affect selections in more than one context, so review the displayed impact before saving. The app rejects invalid, reserved, and duplicate domains. Add only a service you trust: when you open a rewritten URL, the service can receive your IP address and applies its own privacy practices.

[Back to contents](#contents)

<a id="browser-mode"></a>

## Browser mode

### What Browser mode is

Browser mode makes FixupXer an optional **filter-browser candidate**. It does
not contain a web renderer, display pages, or make network requests. For an
eligible HTTP(S) link that Android routes to FixupXer, the flow is:

```text
Android → FixupXer local processing → selected after-clean action → external app
```

FixupXer cleans the URL, optionally applies Browser-specific rules and frontend
conversion, then hands the resulting URL to a browser, native app, sharing
app, or clipboard. The receiving app loads any page.

Browser mode does not intercept every link. Android decides which app receives
each intent, and some links never reach the system default browser.

### Set up Browser mode

Both steps are required:

1. Open **FixupXer > Settings > Configure Browser mode** and enable **Enable
   Browser mode**. This enables the otherwise-disabled browser alias so
   Android can offer FixupXer as a browser candidate.
2. Open Android **Settings > Apps > Default apps > Browser app** and select
   **FixupXer**. Menu names vary slightly by device.

The in-app switch cannot assign the Android default-browser role. If only step
1 is complete, FixupXer is available as a candidate but normal links are not
automatically routed through it.

To stop routing links through FixupXer, select another default browser. You can
also disable the in-app switch; if FixupXer still holds the default-browser
role, **Configuration status** reports that conflict.

### Which links reach FixupXer

FixupXer accepts browser intents only for `http://` and `https://` URLs. A link
is processed only when Android actually dispatches that intent to FixupXer.
Examples that may not arrive include:

- links handled inside the source app;
- links opened by an explicit app choice;
- links claimed directly by a native app through verified App Links;
- non-HTTP(S) links such as `mailto:` or app-specific schemes.

#### Control verified App Link bypass

Apps such as YouTube, Instagram, Reddit, and X may claim their verified links
before Android considers the default browser. For an app whose links you want
to pass through FixupXer:

1. Open Android **Settings > Apps > [app] > Open by default** (sometimes
   **Set as default**).
2. Disable **Open supported links**, or clear that app's supported-link
   associations.

You can usually reach the same screen through **App info** after long-pressing
the app icon. This is an Android per-app setting, not a FixupXer setting.
Disabling it stops that native app from automatically claiming its verified
links; you can still choose **Open in native app** after FixupXer cleans a link.
Re-enable the setting at any time to restore direct native-app handling.

<a id="after-clean"></a>

### Choose what happens after processing

Open **FixupXer > Settings > Configure Browser mode** and find the **After
processing an opened link** card.

This setting chooses what happens after local processing; it does not choose
the frontend domain. **Ask what to do** pauses for your choice. **Try actions
automatically** tries the configured actions in order.

For example, set **Try actions automatically** with **Open in native app** first
and **Open in browser** second. FixupXer tries a compatible app for the cleaned
link; if no app can open it, FixupXer continues to the browser action and then
any remaining actions in the configured order. It stops after the first action
that succeeds.

#### Ask what to do

FixupXer shows its own action dialog with:

1. **Open in native app**
2. **Open in browser**
3. **Share menu**
4. **Copy to clipboard**
5. **Always use app for this host** — pick a compatible native app or external
   browser once; FixupXer saves that choice per normalized host and uses it
   before the action picker on future Browser-mode links for the same host.
   Manage or delete them with **Saved app choices** on the Browser mode
   screen.

The key is the exact cleaned host immediately before an optional frontend
conversion, so changing frontend instances does not create a different route.
`example.com` and `www.example.com` are separate keys. A route can be created
and used only with Browser mode and **Ask what to do**. It remains saved but
inactive after switching to **Try actions automatically** or disabling Browser
mode.

Choosing **Open in native app** tries known compatible installed apps. Supported
X, Instagram and TikTok embed links use the original platform domain for this
action (for example, `fxtwitter.com/user/status/123` becomes
`x.com/user/status/123`). TikTok `vm.` and `vt.` short-link subdomains are kept;
FixupXer does not expand them over the network. The same rule applies to
automatic native actions and saved native app choices. If no app
accepts the URL, FixupXer falls back to an external browser. **Open in browser**
uses only external browser packages and excludes FixupXer itself.

#### Try actions automatically

FixupXer tries the configured actions from top to bottom and stops after the
first success. Reorder them in **Action order** inside the **After processing
an opened link** card. If a native app or external browser
cannot handle the URL, processing continues to the next action; share and
clipboard provide later fallbacks when ordered there.

FixupXer excludes its own package from browser candidates, so handing off a
cleaned URL cannot select FixupXer again and create a browser loop. In Browser
mode with **Ask what to do**, saved app choices are checked before the action
picker; temporarily unavailable or incompatible saved choices remain saved and
the normal flow is offered. Reader and custom frontends skip native shortcuts.
Browser, Share and Copy receive the final processed URI, including any selected
frontend. Native canonicalization does not change that result or history.
Unknown/custom frontends, privacy readers and unsupported proxy forms do not
gain native shortcuts. Share also excludes FixupXer.
If all attempts fail, the result remains in the app for an explicit **Retry**.
Rotation or recreation does not automatically repeat a failed action. Redirect
extraction in the URL pipeline also has cycle detection and a five-hop limit.

<a id="preferred-browser"></a>

### Preferred browser

Open **Settings > Configure Browser mode > Preferred browser > Choose browser**,
choose an installed external browser or **Always ask**, then tap **Save**. This is FixupXer's destination
browser; it does not change Android's default browser. FixupXer can remain the
default handler that cleans links before passing them to your chosen browser.

When an after-clean action opens a browser, the preferred browser receives the
final URL directly. The browser picker also offers **Use once** and **Always
use this browser**. Use once does not replace your saved preference. If the
saved browser is unavailable, choose another compatible browser; FixupXer is
never offered as its own destination.

For example, if Firefox is installed and selected under **Preferred browser**,
the **Open in browser** action sends the final URL to Firefox. Choose **Always
ask** if you want to pick a browser for each handoff. This is the general
browser destination, not a frontend choice or Android's default-browser setting.

This preference does not skip **Ask what to do**, change action priority or
override a valid per-host saved app choice. To open browser-directed links
without either picker, select **Try actions automatically**, put **Open in
browser** first in **Action order**, and choose a preferred browser.

<a id="saved-app-choices"></a>

### Saved app choices

A saved choice sends links for one exact website host to the compatible native app or external browser you picked. Create one from **Always use app for this host** in the **Ask what to do** dialog. Manage or delete choices from **Saved app choices** on the Browser mode settings screen.

Saved choices apply only when Browser mode is enabled and **Ask what to do** is selected. They do not affect Paste or Share. The host is matched before optional frontend conversion, so `example.com` and `www.example.com` are separate choices. If a saved app is unavailable or cannot handle a link, FixupXer offers the normal action flow and keeps the saved choice for later.

Example: in **Ask what to do**, choose **Always use app for this host** for `news.example` and select a compatible app or browser. Future Browser-mode links for exactly `news.example` use that saved destination first. `blog.news.example` is a different host. This host-specific choice is separate from **Preferred browser**, and it does not change Paste or Share.

<a id="browser-frontends"></a>

### Browser frontends

Open **Settings > Configure Browser mode > Configure Browser frontends**.
Use **Change frontend** for a platform and then **Save** in the parent dialog.
Browser choices are separate from Main/Share selections, including when Android
offers **Open with > FixupXer** instead of using FixupXer as its default browser.

| Platform | Browser choices |
|---|---|
| TikTok, Instagram | Clean only, built-in embed, custom frontend |
| X / Twitter, Bluesky | Clean only, built-in reader, built-in embed, custom frontend |
| Reddit, Pinterest | Clean only, built-in reader, custom frontend |
| Facebook | Clean only, custom frontend |
| YouTube, Threads | Clean only |

The picker groups **Privacy readers**, **Embed frontends**, and **Custom
frontends** separately. Experimental and retired targets are not offered.
Use **Add custom proxy** directly in this picker. **Edit** reveals the edit and
delete controls for each frontend. Editing
a custom entry changes its domain. Editing a built-in entry creates a custom
replacement; deleting a built-in entry disables it and allows later restoration.
Custom targets are not presented as verified privacy readers.

The frontend list is shared with **Settings > Link processing > Alternative
frontends**, but the active Browser and Main/Share choices are separate. Review
the displayed impact when changing an entry used by either context. Removing
an active entry must leave an explicit replacement or Clean only choice.

All Browser frontend additions, edits, removals, restores and selections remain
drafts until **Save** in the parent dialog. Returning from a platform picker
keeps those drafts available for review. **Cancel**, Back or dismissing the
parent dialog discards them.

New conversions start off. Upgrades preserve the four older Browser reader
choices, including their remembered target when switched off. **Clean only**
skips frontend conversion and leaves an existing frontend host intact; cleaning
and explicitly enabled custom rules still apply.

**Copy**, **Share**, and **Open in browser** use the final processed URL. For example,
select TikTok's `tnktok.com` embed, save, and open
`https://vm.tiktok.com/Z123/?utm_source=example` with FixupXer: the local result
is `https://vm.tnktok.com/Z123/`. **Open in native app** instead tries the
compatible TikTok app with `https://vm.tiktok.com/Z123/`, retaining the processed
proxy URL for fallback. This example demonstrates string processing;
the placeholder is not a live video or an availability check.

#### Unavailable choices and restoring a category

A reader uses its saved active target, otherwise the first active reader, or
Clean only when none is active. A temporary fallback does not overwrite the
remembered target. An unavailable embed is never silently replaced. Deleting
a custom domain clears that platform's active or remembered Browser selection.

Use **Restore built-in readers** or **Restore built-in embed frontends** in
the picker to restore only that category. Restores and selections remain drafts
until **Save**. **Cancel**, Back, dismissing or recreating the dialog does not
save them. The roster is shared, so an explicitly restored target can become
available in Main/Share as well. If a changed platform was edited elsewhere,
Save keeps the newer settings and asks you to review the refreshed choices.

<a id="configuration-status"></a>

### Read Configuration status

The **Configuration status** card at the top of Settings opens a read-only
dialog with:

- **Browser integration** reports whether the browser alias is on. Off is
  normally optional; it needs attention when FixupXer still holds the default
  browser role.
- **Default browser** reports FixupXer, another/unset browser, or **Unable to
  verify**. Unable to verify is informational—check Android settings manually.
- **Browser frontends** lists configured platform routes and unavailable choices.
  **None enabled** means Clean only and is not an error. A route describes local
  configuration, not a successful network or privacy check.
- **Custom rules** shows whether the master switch is on and how many rules are
  enabled.
- **After-clean behavior** shows **Ask what to do** or **Try actions
  automatically**.

The status cannot predict Android's intent routing. Verified App Links can
still bypass FixupXer even when the operational settings are correct.

### Custom rules in Browser mode

Each custom rule can include any combination of Main, Share, and Browser
contexts. A Browser-only rule runs only after Android has routed an eligible
link to FixupXer.

The Browser pipeline uses the same three ordered phases:

1. **Before built-in cleaning**
2. built-in cleaning, then **After built-in cleaning**
3. optional Browser frontend conversion, then **After domain conversion**

Rules run in their saved order within each phase. Redirect targets re-enter the
bounded pipeline so normal cleaning can still apply.

In the rule editor, **Test Lab > Browser** simulates this processing profile
with current Browser frontend settings and the unsaved rule draft. Draft preview
intentionally runs even when the custom-rules master switch is off. Test Lab
does not make FixupXer the default browser, invoke the after-clean action, or
prove that Android will route a real link through FixupXer.

See the [Custom URL rules](#custom-rules) for scopes, actions,
phases, traces, and safe testing.

### Common use cases

#### Clean browser navigation before it opens

Set FixupXer as default, leave frontend conversions off, and choose **Open in
browser**. Eligible links Android sends to FixupXer are cleaned locally, then
opened by an external browser.

#### Clean first, then return to a native app

Disable that app's **Open supported links**, then put **Open in native app**
first in **Action order** with **Try actions automatically** selected. If no
compatible native app accepts the
cleaned URL, the next configured action is tried.

#### Open supported social links through privacy readers

Enable a Browser reader conversion for X, Bluesky, Reddit, or Pinterest and
select an active Reader. FixupXer rewrites the URL locally; the external app
that receives the Reader URL performs the network request.

#### Apply stricter cleanup only to routed links

Create a narrowly scoped custom rule with only the **Browser mode** context.
Verify it with the Browser Test Lab profile, then test one real routed link.

### Privacy boundary

Validation, built-in cleaning, custom-rule execution, privacy-domain rewriting,
and Configuration status checks happen locally. FixupXer declares no network
permission and never contacts the original site or a Reader service.

The privacy boundary ends at handoff. When you choose a browser, native app,
share target, or privacy Reader URL, Android passes the resulting URL outside
FixupXer. That recipient can access the network and applies its own privacy
policy. Opening documentation pages uses an external browser only when you request it.

<a id="browser-troubleshooting"></a>

### Browser mode troubleshooting

#### Enabling the switch changes nothing

Complete setup step 2: select FixupXer under Android **Default apps > Browser
app**. Check **Configuration status** afterward.

#### A native app or “Ask what to do” bypasses FixupXer

Android probably gave the verified App Link directly to the native app.
Disable **Open supported links** for that app. FixupXer cannot ask what to do
with an intent it never receives.

#### Some browser links still bypass FixupXer

Confirm that they are HTTP(S), were not opened inside an app's embedded
browser, and are not claimed by a verified App Link. Android and OEM routing
rules determine eligibility.

#### A supported social link is cleaned but not converted

Open **Configure Browser frontends** and verify the platform, enabled switch and
selected target. Main/Share conversion toggles do not control Browser mode.
Some targets accept only particular link paths. Unsupported paths, credentials
in the URL authority, or nonstandard ports remain local instead of being
converted lossily and dispatched automatically.

#### A conversion says the selected frontend is unavailable

Use **Change frontend**, restore the appropriate built-in category or choose
another target, then save. Cancelling discards the draft restore.

#### The browser picker appears for every link

Choose **Preferred browser** in Browser settings, or choose a browser in the
picker and use **Always use this browser**. **Always ask** deliberately keeps
the picker. **Ask what to do** is a separate action choice; use automatic
actions with Open in browser first if you also want to skip that action dialog.

#### Processing stopped or settings changed while a dialog was open

FixupXer keeps the result locally when processing is incomplete, a target is
unsupported, configuration changed, or an external action failed. Review the
settings and choose **Retry** to run the Browser flow again. It does not send
the original URL automatically as a substitute for a failed result.

#### A cleaned link opens in a browser instead of a native app

No known installed native app accepted it, so **Ask what to do** used its
native-app fallback or **Try actions automatically** continued to the browser
action.

#### The wrong YouTube app opens

Browser frontend conversion does not apply to YouTube. Put **Open in native
app** first if you want FixupXer to try compatible YouTube/ReVanced handlers
before the official YouTube app.

#### Configuration status says “Unable to verify”

FixupXer could not determine the Android default-browser role on that device.
This is not proof of failure. Check **Default apps > Browser app** manually and
test an eligible link.


#### Cancelling an action

Browser mode keeps the processed link in FixupXer when you cancel the action or
app destination picker. Tap **Retry** to choose again. Share and multi-browser
choices use a FixupXer destination dialog; the action completes only after the
selected app is launched. Cancelling never triggers the next priority action.
If an app disappears before selection, the action fails locally or the next
configured priority action receives the same processed URL.
[Back to contents](#contents)

<a id="custom-rules"></a>

## Custom URL rules

Custom URL rules extend FixupXer's built-in cleaners with your own offline
matching and transformation logic. Rules can remove query parameters, rewrite
URLs, or extract destinations from redirect wrappers.

Rules are powerful. Start with a narrow scope, verify the result in **Test
Lab**, and only then enable the rule for normal use.

### Quick navigation

- [Build your first rule](#your-first-rule-step-by-step)
- [Choose a processing phase](#which-phase-should-i-choose)
- [Choose an include scope](#include-scopes)
- [Configure actions and see examples](#actions)
- [Test safely in Test Lab](#test-lab)
- [Teach FixupXer from an example](#teach-from-example)
- [Fix common beginner mistakes](#common-beginner-mistakes)

### The 30-second mental model

Every rule answers three questions:

1. **Where should it run?** The context and include/exclude scopes decide which
   URLs can reach the action.
2. **When should it run?** The phase decides whether the rule sees the URL
   before or after FixupXer's built-in processing.
3. **What should it do?** The action removes parameters, extracts a destination,
   or rewrites the URL.

For this URL:

```text
https://shop.example.com/product/42?color=blue&utm_source=email#reviews
```

- `https` is the scheme.
- `shop.example.com` is the host.
- `/product/42` is the path.
- `color=blue&utm_source=email` is the query.
- `reviews` is the fragment.

Most beginner rules need only a host scope and a parameter-removal action. You
do not need regex for ordinary tracking parameters.

### Open and enable custom rules

1. Open **FixupXer > Settings > Custom URL rules**.
2. Enable **Enable custom rules** in Settings.
3. Select **Manage rules**.
4. Select **Add rule**, or install one of the bundled **Templates**.

Custom rules are off by default after a new installation or an update from
v2.0. The master switch in Settings disables or enables the entire rule engine.
Each rule also has its own enabled switch in the rule library and editor.

### Your first rule, step by step

This example removes two fictional newsletter parameters from `example.com`
and its subdomains while keeping the functional `article` parameter.

> `example.com` is a placeholder. Replace it with the real website you want to
> target.

Select **Add rule**, then fill the editor like this:

- **Rule name:** `Remove example.com newsletter tags`
- **Rule enabled:** On
- **Processing phase:** `After built-in cleaning`
- **Apply in:** Main screen, Share menu, and Browser mode
- **Include scope:** `Domain and subdomains`
- **Host, host list, or regex:** `example.com`
- **Exclude scopes:** Leave empty
- **Action:** `Remove named parameters`
- **Parameter names:** enter these on separate lines:

  ```text
  newsletter_id
  campaign_code
  ```

- **Ignore case:** On
- **Stop remaining rules in this phase:** Off

In **Test Lab**, use:

```text
https://news.example.com/read?article=42&newsletter_id=weekly&campaign_code=july#comments
```

Expected result:

```text
https://news.example.com/read?article=42#comments
```

Also test a URL that must not change:

```text
https://another-site.example/read?newsletter_id=weekly
```

If both results are correct, select **Save**. Make sure the master **Enable
custom rules** switch is still on.

### Rule processing order

Every rule belongs to one fixed phase:

1. **Before built-in cleaning** — runs before FixupXer's built-in cleaners.
   Use this for redirect wrappers or transformations that must see the original
   query.
2. **After built-in cleaning** — runs after built-in tracking removal and
   before social-domain conversion. This is the safest default for additional
   parameter cleanup.
3. **After domain conversion** — runs last, after conversions such as
   `x.com` to `fixupx.com`.

Rules execute from top to bottom within their phase. Reorder them with the drag
handle or the up/down buttons. A rule cannot be dragged into another phase;
edit the rule and select a different phase instead.

Enable **Stop remaining rules in this phase after a change** when later rules
in the same phase should not process the changed URL.

#### Which phase should I choose?

Use this beginner rule:

- Choose **After built-in cleaning** for removing extra tracking parameters.
- Choose **Before built-in cleaning** for a redirect wrapper whose destination
  is stored in a query parameter. If built-in cleaning removed that parameter
  first, there would be nothing left to extract.
- Choose **After domain conversion** only when the rule must see the converted
  host. For example, a rule scoped to `fixupx.com` cannot match before an
  `x.com` link is converted.

Example pipeline:

```text
Input
https://go.example.com/?target=https%3A%2F%2Fshop.example%2Fp%3Futm_source%3Dmail

Before built-in cleaning
Extract target → https://shop.example/p?utm_source=mail

Built-in cleaning
Remove utm_source → https://shop.example/p
```

An extracted redirect destination starts the pipeline again. This is why the
destination can still receive normal built-in cleaning.

### Processing contexts

A rule can run in any combination of:

- **Main screen**
- **Share menu**
- **Browser mode**

At least one context must be selected. For example, a rule that should affect
automatically opened links but not manually pasted URLs can be limited to
**Browser mode**.

The Browser context does not intercept links by itself. It applies only after
Android routes an eligible HTTP(S) link to FixupXer through Browser mode.
Verified App Links and in-app browsers can bypass that route. See the
[Browser mode](#browser-mode) for setup and Android routing
controls.

Common choices:

- Select all three contexts when the same cleanup should always happen.
- Select only **Browser mode** for an aggressive rule you want on automatically
  opened links but not on manually pasted or shared links.
- Select only **Share menu** when preparing a special URL format for messaging
  apps.
- Select **Main screen** while experimenting, then add the other contexts after
  Test Lab and normal-flow testing succeed.

### Include scopes

The include scope decides which URLs can match:

- **All URLs** — no host restriction.
- **Exact host** — matches only the entered host. `example.com` does not match
  `www.example.com`.
- **Domain and subdomains** — `example.com` matches both `example.com` and
  hosts such as `www.example.com`.
- **Host list** — enter hosts on separate lines or separated by commas. A host
  matches its subdomains by default; prefix it with `=` for exact matching:

  ```text
  example.com
  =accounts.example.net
  ```

- **URL regex** — an RE2/J expression searched against the complete current URL
  at that phase.

Enter hosts without a scheme, path, query, or fragment.

#### Scope examples

If the scope is **Exact host** with:

```text
example.com
```

it matches `https://example.com/page`, but not
`https://www.example.com/page` or `https://news.example.com/page`.

If the scope is **Domain and subdomains** with the same value, all three URLs
match.

For **Host list**, this value:

```text
example.com
=static.example.net
```

matches `example.com` plus every `*.example.com` subdomain, and matches only
the exact `static.example.net` host. It does not match
`cdn.static.example.net`.

Use a **URL regex** when the path matters. This scope matches `/download` on
`example.com` or `www.example.com`, but not other paths:

```regex
^https://(?:www\.)?example\.com/download(?:[/?#]|$)
```

#### Regex behavior

FixupXer uses RE2/J to prevent catastrophic regex backtracking. RE2 syntax does
not support lookaround or pattern backreferences. Regex matching searches for
a match within the URL, so use `^` and `$` when the entire URL must match.

Regex characters such as `.`, `?`, `+`, `(`, and `)` have special meanings.
For example, `example\.com` uses `\.` to mean a literal dot. If you do not know
regex yet, prefer a host scope and one of the parameter actions.

Enable **Ignore case** when the selected scope or action should use
case-insensitive matching.

### Exclude scopes

Excludes are checked after the include scope. Enter one exclude per line:

- `example.com` — domain and all subdomains
- `=example.com` — exact host only
- `regex:pattern` — URL regex

Example:

```text
=accounts.example.com
regex:^https://example\.com/signed/
```

This is useful for protecting login, payment, signed-download, and other
sensitive URLs from a broad rule.

For example, suppose a rule removes all parameters from `example.com`, but
account and checkout links need their parameters. Keep the include scope as
**Domain and subdomains**, then enter:

```text
=accounts.example.com
regex:^https://shop\.example\.com/checkout(?:[/?#]|$)
```

The first line protects only `accounts.example.com`. Remove the `=` if all of
its subdomains should also be protected. The second line protects only the
checkout path on `shop.example.com`.

### Actions

#### Remove all parameters

Removes the complete query while preserving the path and fragment.

Example editor values:

- **Processing phase:** `After built-in cleaning`
- **Include scope:** `Exact host`
- **Scope value:** `links.example.com`
- **Action:** `Remove all parameters`

```text
https://links.example.com/page?id=1&utm_source=x#part
→ https://links.example.com/page#part
```

Use this only when the target website never needs query parameters. Otherwise,
use **Remove named parameters**.

#### Remove named parameters

Enter parameter names on separate lines or separated by commas. Matching is
performed on decoded parameter names while untouched query tokens retain their
original encoding and order.

Example action value:

```text
campaign_code
newsletter_id
```

Example:

```text
https://example.com/article?id=42&campaign_code=july&newsletter_id=weekly
→ https://example.com/article?id=42
```

Names are literal. `utm_*` is not a wildcard; list every name you want removed.
Enable **Ignore case** if `Campaign_Code` and `campaign_code` should be treated
as the same name.

#### Keep only named parameters

Keeps only the listed query parameters and removes all others. An empty list
removes the whole query.

Use narrow host scopes for this action: removing an unknown functional
parameter can break a URL.

Example editor values:

- **Include scope:** `Exact host`
- **Scope value:** `video.example.com`
- **Action:** `Keep only named parameters`
- **Parameter names:** `v` and `list`, on separate lines

```text
https://video.example.com/watch?v=abc123&list=favorites&tracking=mail&theme=dark
→ https://video.example.com/watch?v=abc123&list=favorites
```

Here `v` and `list` are assumed to be functional. Confirm the real website's
requirements before using a keep-only rule.

#### Regex search and replace

Runs an RE2/J replacement on the complete current URL. Choose whether to
replace the first match or all matches. Replacement capture references such as
`$1` and `${name}` are supported when the corresponding group exists.

The result must remain a valid absolute HTTP or HTTPS URL.

Example: rename `/old-product/42` to `/product/42` while keeping everything
after the ID.

- **Include scope:** `Exact host`
- **Scope value:** `shop.example.com`
- **Action:** `Regex search and replace`
- **Pattern:**

  ```regex
  ^(https://shop\.example\.com)/old-product/([0-9]+)(.*)$
  ```

- **Regex replacement:** `$1/product/$2$3`
- **Replace all matches:** Off

```text
https://shop.example.com/old-product/42?color=blue#reviews
→ https://shop.example.com/product/42?color=blue#reviews
```

`$1`, `$2`, and `$3` insert the text captured by the three parenthesized
groups. Test regex rewrites carefully: replacing too much can create an
invalid URL.

#### Extract redirect parameter

Finds the named query parameter and uses its value as the new URL. Choose one
decode mode:

- **NONE** — use the raw value.
- **PERCENT ONCE** — decode `%xx` sequences once without treating `+` as a
  space.
- **FORM ONCE** — decode form encoding once, including `+` as a space.
- **BASE64URL** — decode an unpadded or padded Base64 URL value.

The extracted value must be a valid HTTP or HTTPS URL. It re-enters the
pipeline so it can be cleaned normally. FixupXer detects redirect cycles and
limits re-entry to five hops.

Example: unwrap a fictional `go.example.com` redirect.

- **Processing phase:** `Before built-in cleaning`
- **Include scope:** `Exact host`
- **Scope value:** `go.example.com`
- **Action:** `Extract redirect parameter`
- **Parameter name:** `target`
- **Decode mode:** `PERCENT ONCE`
- **Stop remaining rules in this phase:** On

```text
https://go.example.com/out?target=https%3A%2F%2Fdestination.example%2Fpage%3Fid%3D42
→ https://destination.example/page?id=42
```

Choose the decode mode that matches the wrapper. Start with **PERCENT ONCE**
for values containing `%3A`, `%2F`, and similar sequences. Use **FORM ONCE**
only when the wrapper uses form encoding, where `+` means a space.

#### Template rewrite

Builds a new URL from literal text and these placeholders:

- `{scheme}`
- `{host}`
- `{port}` — empty or prefixed with `:`
- `{path}`
- `{query}` — without `?`
- `{fragment}` — without `#`

Example:

```text
https://proxy.example/{host}{path}
```

The final template output must be an absolute HTTP or HTTPS URL. Add your own
`?` or `#` delimiters when using query or fragment placeholders.

Example: send documentation paths to another host and intentionally drop the
old query and fragment.

- **Include scope:** `Exact host`
- **Scope value:** `docs.example.com`
- **Action:** `Template rewrite`
- **Template:** `https://archive.example.net{path}`

```text
https://docs.example.com/guides/setup?source=menu#android
→ https://archive.example.net/guides/setup
```

To preserve the query and fragment, use:

```text
https://archive.example.net{path}?{query}#{fragment}
```

This can leave a trailing `?` or `#` when the source URL has no query or
fragment. If that matters, use a regex rewrite with separate rules for the
different URL shapes.

<a id="test-lab"></a>

### Test Lab

Test Lab is a local preview in the rule editor. It runs the processing pipeline
with your unsaved rule draft and shows the result plus a trace; it does not open
the URL in another app. For example, enter
`https://shop.example.com/item/7?article=42&campaign_code=spring` in the Main
profile and test a draft **Remove named parameters** action for
`campaign_code`. This fictional parameter is not removed by a built-in cleaner:
the expected result is `https://shop.example.com/item/7?article=42`, with the
draft rule marked **APPLIED** in the trace.

Before saving:

1. Enter a representative URL under **Test Lab**.
2. Select the Main, Share, or Browser profile.
3. Select **Run test**.
4. Review the result and trace.

The trace reports whether rules were applied, skipped by context or scope,
excluded, produced no change, or generated invalid output. Test both expected
matches and URLs that must remain unchanged.

Test Lab runs the complete FixupXer pipeline with the unsaved draft inserted
among your saved rules. The final result can therefore include built-in
cleaning and changes from other enabled custom rules.

The **Browser** Test Lab profile simulates Browser pipeline processing with the
current Browser frontend settings (Clean only, supported readers, embeds or custom targets); it does not assign the Android default
browser role, test verified App Link dispatch, or run the configured
after-clean handoff. A successful preview proves rule/pipeline behavior, not
that Android will route a real link through FixupXer.

<a id="teach-from-example"></a>

#### Teach from example

When creating a new rule, expand **Teach from example** and enter one original
URL plus the exact URL you want. FixupXer can safely infer only two narrow
cases: removing named query parameters while preserving every surviving raw
query token and its order, or extracting one unambiguous redirect parameter.

For example, give Teach this original URL and the exact result you want:

```text
https://shop.example.com/read?article=42&newsletter_id=weekly
https://shop.example.com/read?article=42
```

Because the only difference is removal of a named query parameter, Teach can
draft a rule to remove `newsletter_id`. It cannot infer every kind of rewrite.

The generated draft is deliberately disabled, scoped to the original exact
host, limited to the Main screen, and placed before built-in cleaning. It also
adds your example as a saved test vector. Review the fields, run the vector,
and enable the rule only after it passes. If FixupXer already produces the
desired result through its existing pipeline, no draft is created.

#### Saved test vectors

Use **Test vectors** to save up to 20 input → expected-output pairs with a
rule. **Run all** checks the rule by itself through its configured phase and
scope; it does not read preferences or touch history, the cleaner cache, or
other rules. Evaluation uses the first context the rule is active in (Main,
then Share, then Browser), so a vector's outcome is the same in every context.
An enabled rule must pass every saved vector. Failed imports are
kept as disabled drafts and identified in the import preview. Existing enabled
rules are never automatically disabled, but their saved-vector failures appear
when you open the editor.

Useful trace statuses include:

- **APPLIED** — the rule matched and changed the URL.
- **NO_OP** — the rule matched, but there was nothing to change. For example,
  the requested parameter was not present or was already removed.
- **SCOPE_MISS** — the URL did not match the include scope.
- **CONTEXT_MISS** — the selected Test profile is not enabled for the rule.
- **EXCLUDED** — an exclude protected this URL.
- **INVALID_OUTPUT** — the action tried to create something that was not a
  valid HTTP or HTTPS URL, so FixupXer kept the previous safe URL.

For every rule, try at least these cases:

1. A matching URL containing the data you want changed.
2. A matching URL that needs no change.
3. A URL from another host.
4. An excluded URL, if the rule has excludes.
5. A URL with a fragment such as `#comments`, to confirm it is preserved when
   expected.

#### Example Test Lab session

For the first-rule example earlier in this guide:

```text
Test profile: MAIN
Input:  https://news.example.com/read?article=42&newsletter_id=weekly
Result: https://news.example.com/read?article=42
Trace:  POST_CLEAN: Remove example.com newsletter tags — APPLIED
```

Then test:

```text
Input:  https://other.example/read?newsletter_id=weekly
Result: https://other.example/read?newsletter_id=weekly
Trace:  POST_CLEAN: Remove example.com newsletter tags — SCOPE_MISS
```

### Start quickly with Templates

The large **Templates** button adds ready-made example rules to **Your rule
library**. It is useful when you want working rules immediately or want to
learn by opening and editing complete examples instead of starting with an
empty editor.

Tap **Templates**, then choose one of these bundled sets:

- **Privacy basics** adds two rules that remove common `utm_*` campaign
  parameters and click identifiers such as `fbclid`, `gclid`, and `msclkid`.
- **Offline redirect wrappers** adds two rules that extract the real
  destination from Facebook and LinkedIn outbound redirect links.

The selected rules appear in the library as ordinary editable rules. You can
tap them to inspect their scope and action, test them in Test Lab, disable
them, reorder them, duplicate them, or delete them. Choosing the same set
again does not create duplicate copies; the import result reports how many
rules were added or skipped.

Templates are stored and processed entirely offline. If the master **Enable
custom rules** switch in Settings is off, the template rules remain saved but
do not run until you enable it.

> **Templates** is a library of ready-made rules. It is different from the
> **Template rewrite** action, which builds a new URL from placeholders inside
> one rule.

### Manage rules

- Tap a rule to edit, duplicate, or delete it.
- Use the row switch to disable a rule without deleting it.
- Use **Templates** to add the bundled examples described above.
- Use **Delete all rules** only when you no longer need any saved rule.

The current limits are 200 rules, 100 host/parameter entries per relevant
field, 50 excludes per rule, and a 1 MiB import bundle.

When several rules share a phase, imagine passing a piece of paper down the
list. The first rule receives the phase's starting URL; every following rule
receives the result left by the rule above it.

Example:

```text
Starting URL:
https://example.com/page?campaign_code=july&session=abc

Rule 1 removes campaign_code:
https://example.com/page?session=abc

Rule 2 keeps only session:
https://example.com/page?session=abc
```

Reversing those two rules happens to produce the same result, but many regex,
redirect, and template combinations do not. Use drag ordering deliberately.

### Import, export, and rollback

**Export** writes all rules to a versioned JSON bundle at a location you select
through Android's file picker. **Import** first validates the entire bundle and
shows a preview for these modes:

- **Add new** — add rules with new IDs and skip matching IDs.
- **Update matching** — update matching IDs and add new IDs.
- **Replace all** — replace the complete current rule set.

Example: your app currently has rules `A` and `B`, while the imported file has
rules `B` and `C`.

- **Add new** keeps local `A` and `B`, skips imported `B`, and adds `C`.
- **Update matching** keeps `A`, updates `B` from the file, and adds `C`.
- **Replace all** removes the local set and leaves only imported `B` and `C`.

Matching uses each rule's internal ID, not its displayed name. Two rules with
the same name can still be different rules.

Import is atomic: an invalid bundle changes nothing. **Undo last import**
restores the most recent pre-import snapshot.

Rule bundles can contain private domains, regexes, templates, and test URLs.
Inspect a bundle before sharing it. FixupXer never uploads or synchronizes
rules.

For a complete settings transfer, **Settings > Backup & restore** creates a
separate versioned JSON file containing whitelisted preferences, all custom
rules, and remembered Browser-mode destinations. Restoring that file replaces
those items rather than offering the three rule-only merge modes above. URL
history and rule rollback snapshots are never included.

### Common beginner mistakes

#### The rule does nothing

Check all of these:

- The master **Enable custom rules** switch is on.
- The rule's own switch is on.
- The current Main, Share, or Browser context is selected.
- The host value does not contain `https://`, a path, or a trailing query.
- An exclude is not protecting the URL.
- The chosen phase still contains the parameter or host you expect to match.
- Another earlier rule has not already removed or rewritten it.

Use Test Lab and read the trace status; it usually identifies the missed
condition directly.

#### `www.example.com` does not match

An **Exact host** scope for `example.com` intentionally excludes `www`. Change
the scope to **Domain and subdomains**, or enter the exact `www.example.com`
host.

#### I entered `https://example.com` as a host

Host fields accept only:

```text
example.com
```

Use a URL regex only when you need to match the scheme, path, query, or
fragment.

#### I entered `utm_*`, but parameters remain

Parameter actions do not support wildcards. Enter each complete name on its
own line, or use a carefully tested regex replacement.

#### My redirect extraction reports invalid output

The decoded parameter must become a complete `http://` or `https://` URL.
Check that you selected the correct parameter name and decode mode. If the
value still begins with `%` after **PERCENT ONCE**, it may be encoded twice;
FixupXer intentionally decodes only one layer per redirect action.

#### My template removed information

A template preserves only the placeholders you include. If the template omits
`{query}` or `{fragment}`, that part is intentionally dropped.

#### A broad rule broke a website

Disable the individual rule immediately. Narrow it to one exact host, add an
exclude, or replace **Remove all parameters** with **Remove named parameters**.
The original URL remains visible in the Main/Share before-and-after display and
can also remain in history when history is enabled.

### Recommended workflow

1. Give the rule a descriptive name.
2. Start with **Exact host** or **Domain and subdomains**.
3. Select only the required contexts.
4. Use **After built-in cleaning** unless the action requires another phase.
5. Add excludes for sensitive paths or hosts.
6. Test a matching URL and a non-matching URL.
7. Save and verify the result in the normal app flow.
8. Export a backup after building a stable rule set.
[Back to contents](#contents)

<a id="history-and-backup"></a>

## History and backup

History is optional and stays on the device. Open it with the History button on the Main screen. From the History screen you can reopen a processed URL, copy, share, or delete an entry, clear all entries, and change the maximum number retained. Sensitive originals intercepted by Private Link Guard are not saved; a fully cleaned result may be kept with a redacted-input marker.

When you delete a History entry, **Undo** restores the same entry and its original timestamp. Use the History switch to turn recording on or off, and the settings icon to change the retention limit.

Open **Settings > Data & backup > Backup & restore** to export or restore a manual backup. A backup is a versioned, **unencrypted JSON file**. Store it somewhere you trust. It can include settings, the History on/off setting and limit, custom rules, frontend and reader rosters, and saved app choices. It does not contain URL history entries, Android's default-browser role, installed apps, or rule-import rollback snapshots.

Restoring replaces the settings and rules included in the backup. It does not restore URL history; entries already on the device remain and may be trimmed if the restored History limit is lower. Older backup versions are supported with migration behavior described below.

For example, if you delete a History row, tap **Undo** while that action is offered to restore the row. Exporting or restoring a backup cannot bring the row back: a backup includes the History setting and limit, not the URL entries. A restore may trim entries already on the device if the restored limit is lower.

### Browser choices and preferred browser

New exports use schema v2 and include a Browser preference for each of the nine platforms. TikTok and Instagram support embed and custom targets; X and Bluesky also support readers; Reddit and Pinterest support readers and custom targets; Facebook supports custom targets. YouTube and Threads remain Clean only in Browser mode.

Browser choices are independent of Main/Share choices. Turning a choice off can retain its target for later use. A temporarily unavailable reader may fall back to another active reader without overwriting the saved target. An unavailable embed or custom target is not silently substituted.

Version 2.8.0 also includes the preferred browser package in schema-v2 exports.
Older files without this field restore **Always ask**. A browser that is not
installed on the receiving device remains a saved preference, but cannot launch;
choose an available replacement in Browser settings or when opening a link.
Malformed package names and FixupXer itself are rejected during validation.

Only saved frontend changes are exported. Uncommitted Browser picker additions,
edits and removals are discarded when cancelled and never enter a backup.

### Importing older backups

Schema v1 remains supported. Import migrates the four old reader settings, including disabled selections, and resets the other Browser platforms to Clean only. It replaces the entire Browser map, so a newer TikTok or Instagram Browser selection does not remain enabled accidentally after restoring an older file.

An app upgrade migrates existing reader settings once. Upgrading the app does not enable new Browser conversions automatically. Known retired frontend selections are handled before validating an import; unsafe frontend domains cannot be restored as custom targets.

### Validation and interrupted restore

Restore validates the complete file before applying it, including platform/target combinations, custom-domain ownership and collisions, and rule data. Custom targets are checked against the domains in the imported backup. Invalid files are rejected without partially applying settings.

Restore replaces settings and rules. Browser dispatch is paused while restore or recovery is incomplete. If applying the backup fails, FixupXer attempts to recover the previous state and retains its recovery record until that succeeds. A failed save is not reported as a successful restore. After an interruption, reopen the app and let recovery finish before retrying a Browser action.

See [Browser mode](#browser-mode) for frontend selection, fallback and Retry behavior.
[Back to contents](#contents)

<a id="app-settings"></a>

## App settings

In **Settings > Appearance**, choose **Theme > System**, **Light**, or **Dark**.
System follows Android's theme. Under **Action button layout**, choose **Left hand**
or **Right hand** to arrange the main action controls for your dominant hand. These
choices change the presentation, not link processing.

Use these settings to reach the main controls:

- **Settings > Link processing > Alternative frontends** manages the Main and Share frontend roster and choices.
- **Settings > Configure Browser mode** controls Browser routing, after-clean actions, the preferred browser, saved app choices, and Browser frontend selections.
- **Settings > Link processing > Custom URL rules > Manage rules** opens the rule library; **Enable custom rules** enables or pauses all custom rules.
- **Settings > Backup & restore** exports or restores settings and rules.
- **History** controls local URL-history collection, entries, and its retention limit.
- **Configuration status** at the top of Settings is a read-only summary of Browser mode, frontends, custom rules, and after-clean behavior.

[Back to contents](#contents)

<a id="troubleshooting"></a>

## Troubleshooting

- **A link still contains a parameter:** built-in cleaners intentionally keep unknown or functional parameters. If you add a custom rule, check its context, scope, and phase in [Test Lab](#test-lab).
- **A social link is cleaned but not converted:** confirm that conversion is enabled for that platform and that the selected frontend supports that URL. Main/Share selections do not control Browser mode.
- **A Browser link bypasses FixupXer:** Android may have sent it to a verified App Link, an embedded browser, or an explicit app choice. Review [Browser mode setup and routing](#browser-mode) and the Android app's supported-link settings.
- **A frontend is unavailable:** choose another active target, restore its built-in category when available, or use **Clean only**. The app does not silently replace an unavailable embed or custom target.
- **A rule does nothing or changes too much:** disable it, narrow its scope, review the [custom-rule troubleshooting](#common-beginner-mistakes), and test a matching and non-matching URL before enabling it again.
- **A saved browser or app choice cannot launch:** choose an installed compatible destination. A missing app does not erase the saved choice; see [Preferred browser](#preferred-browser) and [Saved app choices](#saved-app-choices).
- **A backup is rejected:** the file may be invalid or from an unsupported format. Restore validates before applying settings. If recovery is still in progress, reopen the app and let recovery finish before retrying.

[Back to contents](#contents)
