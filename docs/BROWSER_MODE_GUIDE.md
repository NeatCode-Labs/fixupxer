# FixupXer Browser Mode Guide

## What Browser mode is

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

## Set up Browser mode

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

## Which links reach FixupXer

FixupXer accepts browser intents only for `http://` and `https://` URLs. A link
is processed only when Android actually dispatches that intent to FixupXer.
Examples that may not arrive include:

- links handled inside the source app;
- links opened by an explicit app choice;
- links claimed directly by a native app through verified App Links;
- non-HTTP(S) links such as `mailto:` or app-specific schemes.

### Control verified App Link bypass

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

## Choose what happens after processing

Open **FixupXer > Settings > Configure Browser mode** and find the **After
processing an opened link** card.

### Ask what to do

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

### Try actions automatically

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

## Preferred browser

Open **Settings > Configure Browser mode > Preferred browser > Choose browser**,
choose an installed external browser or **Always ask**, then tap **Save**. This is FixupXer's destination
browser; it does not change Android's default browser. FixupXer can remain the
default handler that cleans links before passing them to your chosen browser.

When an after-clean action opens a browser, the preferred browser receives the
final URL directly. The browser picker also offers **Use once** and **Always
use this browser**. Use once does not replace your saved preference. If the
saved browser is unavailable, choose another compatible browser; FixupXer is
never offered as its own destination.

This preference does not skip **Ask what to do**, change action priority or
override a valid per-host saved app choice. To open browser-directed links
without either picker, select **Try actions automatically**, put **Open in
browser** first in **Action order**, and choose a preferred browser.

## Browser frontends

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

### Unavailable choices and restoring a category

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

## Read Configuration status

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

## Custom rules in Browser mode

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

See the [Custom URL Rules Guide](CUSTOM_URL_RULES_GUIDE.md) for scopes, actions,
phases, traces, and safe testing.

## Common use cases

### Clean browser navigation before it opens

Set FixupXer as default, leave frontend conversions off, and choose **Open in
browser**. Eligible links Android sends to FixupXer are cleaned locally, then
opened by an external browser.

### Clean first, then return to a native app

Disable that app's **Open supported links**, then put **Open in native app**
first in **Action order** with **Try actions automatically** selected. If no
compatible native app accepts the
cleaned URL, the next configured action is tried.

### Open supported social links through privacy readers

Enable a Browser reader conversion for X, Bluesky, Reddit, or Pinterest and
select an active Reader. FixupXer rewrites the URL locally; the external app
that receives the Reader URL performs the network request.

### Apply stricter cleanup only to routed links

Create a narrowly scoped custom rule with only the **Browser mode** context.
Verify it with the Browser Test Lab profile, then test one real routed link.

## Privacy boundary

Validation, built-in cleaning, custom-rule execution, privacy-domain rewriting,
and Configuration status checks happen locally. FixupXer declares no network
permission and never contacts the original site or a Reader service.

The privacy boundary ends at handoff. When you choose a browser, native app,
share target, or privacy Reader URL, Android passes the resulting URL outside
FixupXer. That recipient can access the network and applies its own privacy
policy. The **How to Use** button likewise opens this guide in an external
browser only after you request it.

## Troubleshooting

### Enabling the switch changes nothing

Complete setup step 2: select FixupXer under Android **Default apps > Browser
app**. Check **Configuration status** afterward.

### A native app or “Ask what to do” bypasses FixupXer

Android probably gave the verified App Link directly to the native app.
Disable **Open supported links** for that app. FixupXer cannot ask what to do
with an intent it never receives.

### Some browser links still bypass FixupXer

Confirm that they are HTTP(S), were not opened inside an app's embedded
browser, and are not claimed by a verified App Link. Android and OEM routing
rules determine eligibility.

### A supported social link is cleaned but not converted

Open **Configure Browser frontends** and verify the platform, enabled switch and
selected target. Main/Share conversion toggles do not control Browser mode.
Some targets accept only particular link paths. Unsupported paths, credentials
in the URL authority, or nonstandard ports remain local instead of being
converted lossily and dispatched automatically.

### A conversion says the selected frontend is unavailable

Use **Change frontend**, restore the appropriate built-in category or choose
another target, then save. Cancelling discards the draft restore.

### The browser picker appears for every link

Choose **Preferred browser** in Browser settings, or choose a browser in the
picker and use **Always use this browser**. **Always ask** deliberately keeps
the picker. **Ask what to do** is a separate action choice; use automatic
actions with Open in browser first if you also want to skip that action dialog.

### Processing stopped or settings changed while a dialog was open

FixupXer keeps the result locally when processing is incomplete, a target is
unsupported, configuration changed, or an external action failed. Review the
settings and choose **Retry** to run the Browser flow again. It does not send
the original URL automatically as a substitute for a failed result.

### A cleaned link opens in a browser instead of a native app

No known installed native app accepted it, so **Ask what to do** used its
native-app fallback or **Try actions automatically** continued to the browser
action.

### The wrong YouTube app opens

Browser frontend conversion does not apply to YouTube. Put **Open in native
app** first if you want FixupXer to try compatible YouTube/ReVanced handlers
before the official YouTube app.

### Configuration status says “Unable to verify”

FixupXer could not determine the Android default-browser role on that device.
This is not proof of failure. Check **Default apps > Browser app** manually and
test an eligible link.


### Cancelling an action

Browser mode keeps the processed link in FixupXer when you cancel the action or
app destination picker. Tap **Retry** to choose again. Share and multi-browser
choices use a FixupXer destination dialog; the action completes only after the
selected app is launched. Cancelling never triggers the next priority action.
If an app disappears before selection, the action fails locally or the next
configured priority action receives the same processed URL.
