# FixupXer Browser Mode Guide

Browser mode turns FixupXer into an optional filter browser. Android sends
HTTP and HTTPS links to FixupXer first, FixupXer cleans them locally, and then
performs the action you configured.

FixupXer never loads websites itself and does not request network permission.

## Enable Browser mode

1. Open FixupXer.
2. Open **Settings**.
3. Under **Browser integration**, enable **Enable FixupXer as browser**.
4. Open Android **System Settings > Apps > Default apps > Browser app**.
5. Select **FixupXer**.

Android does not change the default browser when you enable the switch in
FixupXer. You must complete the system-setting step manually.

To stop using Browser mode, select another default browser or disable the
FixupXer Browser mode switch.

## Make links pass through FixupXer

Apps such as YouTube, Instagram, and X can register themselves to open their
own links. Android may send those links directly to the native app and bypass
the default browser.

For each native app whose links you want FixupXer to clean:

1. Open **System Settings > Apps > [app name] > Set as default**.
2. Disable **Open supported links**.

On many devices you can also long-press the app icon, open **App info**, select
**Set as default**, and disable **Open supported links**.

If you still want the cleaned link to open in its native app, choose
**Follow action order** in FixupXer and put **Open in native app** first.

## Choose what happens after cleaning

Open **FixupXer > Settings > After-clean behavior**.

- **Ask every time** shows FixupXer's action dialog after each link is cleaned.
- **Follow action order** tries the configured actions from top to bottom until
  one succeeds.

The available actions are:

1. Open in native app
2. Open in browser
3. Share menu
4. Copy to clipboard

When **Follow action order** is selected, reorder these actions under
**Settings > Action priority**.

## Configure Browser mode conversions

Browser mode has conversion settings separate from the toggles on FixupXer's
main screen.

1. Open **Settings > After-clean behavior**.
2. Select **Conversion defaults**.
3. Enable or disable conversion for Twitter/X, Instagram, Facebook, and TikTok.
4. Save the settings.

Custom URL rules can also be limited to the **Browser mode** context. See the
[Custom URL Rules Guide](CUSTOM_URL_RULES_GUIDE.md).

## Troubleshooting

### Native apps bypass FixupXer

Disable **Open supported links** for each app whose URLs should pass through
FixupXer.

### “Ask every time” is skipped

The native app is probably handling the URL before Android reaches the default
browser. Disable **Open supported links** for that app.

### Enabling Browser mode changes nothing

The in-app switch only enables FixupXer as a browser candidate. Select FixupXer
manually under Android **Default apps > Browser app**.

### Social links are not converted

Open **Conversion defaults** and check the Browser mode conversion settings.
The main-screen conversion toggles do not control Browser mode.

### The wrong YouTube app opens

Use **Follow action order** with **Open in native app** first. FixupXer tries
compatible YouTube/ReVanced handlers before the official YouTube app when they
are available.

### A cleaned link opens in the browser instead of a native app

No installed native app accepted the cleaned URL, so FixupXer continued to the
next configured action.

## Privacy note

URL cleaning and rule processing stay offline inside FixupXer. The **How to
Use** button opens this GitHub page through an external browser; that browser's
own privacy policy applies once it opens the page.
