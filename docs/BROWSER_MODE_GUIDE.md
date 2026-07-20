# FixupXer Browser Mode Guide

## What Browser mode is

Browser mode makes FixupXer an optional **filter-browser candidate**. It does
not contain a web renderer, display pages, or make network requests. For an
eligible HTTP(S) link that Android routes to FixupXer, the flow is:

```text
Android → FixupXer local processing → selected after-clean action → external app
```

FixupXer cleans the URL, optionally applies Browser-specific rules and privacy
conversion, then hands the resulting URL to a browser, native app, Android
share sheet, or clipboard. The receiving app—not FixupXer—loads any page.

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

The key is the exact cleaned host immediately before an optional Reader
conversion, so changing Reader instances does not create a different route.
`example.com` and `www.example.com` are separate keys. A route can be created
and used only with Browser mode and **Ask what to do**. It remains saved but
inactive after switching to **Try actions automatically** or disabling Browser
mode.

Choosing **Open in native app** tries known compatible installed apps. If none
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
picker; invalid, disabled, or incompatible saved choices are removed and the
normal flow runs once. Reader-only privacy frontends skip a saved native route
without deleting it. Redirect
extraction in the URL pipeline also has cycle detection and a five-hop limit.

## Browser privacy readers

Open **Settings > Configure Browser mode** and select **Configure privacy
readers**. The **Browser privacy readers** dialog supports optional reader
conversions for exactly four platforms:

- X / Twitter
- Bluesky
- Reddit
- Pinterest

Each enabled platform uses a built-in target with the **Reader** role. Browser
mode does not use custom or embed-oriented targets. Its saved reader choices
are separate from the embed-friendly frontend choices used by the Main and
Share screens.

A conversion runs only when its platform toggle is enabled and an active
Reader target exists. If conversions are all off, eligible links are still
cleaned normally.

### Restore a platform with no active Reader

If every built-in Reader for a platform was disabled:

1. Open **Browser privacy readers**.
2. On the affected platform, select **Change** even though its conversion
   switch is unavailable.
3. In the empty picker, select **Restore built-in readers**. This re-enables
   only the platform's built-in Readers; embed frontends you removed from the
   Main/Share pickers stay removed.
4. Choose a Reader, return to the conversion dialog, enable the platform if
   desired, and select **Save**.

The restore is part of the dialog draft: **Save** keeps it, while **Cancel**,
Back, or dismissing the dialog rolls the Reader roster back to its previous
state. The conversion switch remains unavailable until an active Reader can be
selected. Configuration status marks an enabled conversion with no active
Reader as needing attention.

## Read Configuration status

The **Configuration status** card at the top of Settings opens a read-only
dialog with:

- **Browser integration** reports whether the browser alias is on. Off is
  normally optional; it needs attention when FixupXer still holds the default
  browser role.
- **Default browser** reports FixupXer, another/unset browser, or **Unable to
  verify**. Unable to verify is informational—check Android settings manually.
- **Privacy readers** lists active platform → Reader routes. **None
  enabled** means cleaning-only and is not an error. **Broken** means enabled
  routes have no active Reader; **mixed** means some routes work and some need
  attention.
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
3. optional Browser Reader conversion, then **After domain conversion**

Rules run in their saved order within each phase. Redirect targets re-enter the
bounded pipeline so normal cleaning can still apply.

In the rule editor, **Test Lab > Browser** simulates this processing profile
with current Browser privacy settings and the unsaved draft. Draft preview
intentionally runs even when the custom-rules master switch is off. Test Lab
does not make FixupXer the default browser, invoke the after-clean action, or
prove that Android will route a real link through FixupXer.

See the [Custom URL Rules Guide](CUSTOM_URL_RULES_GUIDE.md) for scopes, actions,
phases, traces, and safe testing.

## Common use cases

### Clean browser navigation before it opens

Set FixupXer as default, leave privacy conversions off, and choose **Open in
browser**. Eligible links Android sends to FixupXer are cleaned locally, then
opened by an external browser.

### Clean first, then return to a native app

Disable that app's **Open supported links**, then put **Open in native app**
first in **Action order** with **Try actions automatically** selected. If no
compatible native app accepts the
cleaned URL, the next configured action is tried.

### Open supported social links through privacy readers

Enable a Browser privacy conversion for X, Bluesky, Reddit, or Pinterest and
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

Open **Browser privacy readers** through **Configure privacy readers** and
verify that the platform is one of X, Bluesky, Reddit, or Pinterest, its switch
is enabled, and an active Reader is selected. Main/Share conversion toggles do
not control Browser mode.

### A conversion says no active privacy frontend

Use **Change > Restore built-in readers**, select a Reader, then enable the
platform and save. Cancelling the dialog instead discards the restore.

### A cleaned link opens in a browser instead of a native app

No known installed native app accepted it, so **Ask what to do** used its
native-app fallback or **Try actions automatically** continued to the browser
action.

### The wrong YouTube app opens

Browser privacy conversion does not apply to YouTube. Put **Open in native
app** first if you want FixupXer to try compatible YouTube/ReVanced handlers
before the official YouTube app.

### Configuration status says “Unable to verify”

FixupXer could not determine the Android default-browser role on that device.
This is not proof of failure. Check **Default apps > Browser app** manually and
test an eligible link.
