# FixupXer Privacy Policy

## Overview
FixupXer is a URL processing app that helps users clean tracking parameters from URLs and convert social media links for better embedding. This policy explains local storage and the choices that can pass a URL to an Android system service or external recipient.

## Data Collection and Usage
FixupXer does not collect, upload, or synchronize personal user data. URLs are processed locally. Optional local features store selected data on the device, and a URL is passed to an Android system service or external recipient only through a user-selected action or the opted-in **Open share menu** setting described below.

### What We Don't Collect
- No personal information
- No usage statistics
- No analytics data
- No device information
- No location data
- No user identifiers

### Local Storage
The app only stores the following data locally on your device:
- Preferences: whether to clean tracking parameters, social-link conversion choices, selected built-in or custom embed proxies, the optional Auto-copy shared links setting (off by default), the Share after-clean action (Show result by default, or opt-in Open share menu), Browser-mode settings (enabled state, post-clean action mode and priority order, X/Bluesky/Reddit/Pinterest Reader choices), remembered after-clean destinations (host → app choice only), app layout, and history settings (enabled state, maximum entries)
- Conversion history (optional, on by default, can be disabled or cleared at any time): the original URL, the cleaned URL, a timestamp, and the kind of change made — stored in a local database on your device only. If the Private Link Guard detects sensitive data (such as tokens, e-mail addresses, or precise coordinates) in the link you enter, the original URL is never saved: when cleaning removes all sensitive data, history keeps only the safe final URL marked as "Input redacted for privacy"; when sensitive data remains in the result, nothing is saved at all
- Custom URL rules (optional): names, domains, patterns, replacements, templates, test URLs, ordering, and local rollback snapshots. Rules are stored in the local Room database and are not included in automatic cloud backup.

Preferences are stored using Android's SharedPreferences system and history/custom rules in a local Room (SQLite) database. FixupXer never uploads or synchronizes them. Depending on your device and Google account settings, Android may include the preferences file in its OS-managed cloud backup or device-to-device transfer. The Room database, including URL history, custom rules, and rule rollback snapshots, is excluded from Android's automatic backup.

### Rule Import and Export
Custom rules can be exported or imported only when you explicitly use Android's Storage Access Framework file picker. You choose the file and destination. Exported bundles may reveal private domains, patterns, or test URLs that you entered, so store and share them carefully. FixupXer does not upload or synchronize rule files.

### Manual settings backup (optional)
From **Settings > Backup & restore**, you can export or import a local JSON backup of whitelisted preferences, custom rules, and remembered after-clean destinations (normalized host, app type, and package name). URL history, rule rollback snapshots, and other internal data are never included. Restore replaces the backed-up items in full; invalid or unsupported files leave your current settings unchanged. The file may reveal private custom domains, rule data, or remembered hosts, so store and share it carefully. Android's automatic preference backup/device transfer continues independently and is not replaced by this feature.

### URL Processing
- All URL processing happens locally on your device
- During processing, FixupXer does not send URLs to external servers
- No tracking or analytics are performed
- Link conversions (e.g. to embed frontends such as fixupx.com or toinstagram.com, or to account-free reader frontends such as xcancel.com or a Redlib instance — built-in or custom) are pure text replacements; the app never contacts those domains

When processing finishes, Android may pass the resulting URL to the browser,
native app, share target, or privacy Reader selected by you or by your
after-clean configuration. Any network request happens in that external
recipient, whose privacy policy then applies. Copying a URL places it in the
Android clipboard.

For links received through Android's ordinary Share action, **After receiving a
shared link** defaults to **Show result**. If you opt in to **Open share menu**,
FixupXer opens Android's native sharesheet with the final URL after safe,
complete processing, and you choose the receiving app. Link Guard findings or
incomplete processing remain available for review instead of being shared
automatically. This setting is separate from Browser mode and Process Text
preview. The system sharesheet and any app you select handle the URL according
to their own behavior and privacy policies.

**Auto-copy shared links** is optional and off by default. When enabled, it
copies the final successfully processed URL only for links shared from another
app to FixupXer; the Share screen remains open unless **Open share menu** is enabled. It does not affect Paste, Main,
Process Text, or Browser mode. Automatic copying is skipped while Private Link
Guard still reports sensitive data or processing is incomplete. As with a
manual Copy action, enabling Auto-copy writes the URL to Android's clipboard.

### Browser Mode (optional)
Browser mode requires enabling FixupXer's browser alias and separately selecting
FixupXer as Android's default browser. FixupXer processes only eligible HTTP(S)
links that Android routes to it; verified App Links and in-app browsers may
bypass the default browser. Processing remains local, and FixupXer never renders
or loads a page. It then hands the result to the configured external action.
Optional Browser privacy conversions use built-in Reader targets for X,
Bluesky, Reddit, and Pinterest and are separate from Main/Share embed targets.

### External Help Links
The **How to Use** buttons open FixupXer documentation hosted on GitHub through an external browser. This happens only when you tap a help link. FixupXer does not load the page itself; the selected browser and GitHub apply their own privacy policies.

## Third-Party Services
FixupXer does not connect to third-party services itself. It uses standard Android system services for:
- Sharing URLs
- Opening URLs in a browser
- Opening user-requested documentation links
- Copying URLs to clipboard
- Sending bug reports via email (only when explicitly requested by the user)

## Age Restrictions
FixupXer is not intended for children under 13 years of age. The app is designed for users 13 and older.

## Data Security
FixupXer has no app-managed remote collection or synchronization. Optional local history and manual backups can contain the data described above. Android's system sharesheet or another recipient receives a URL only through an action you select or the opted-in Share after-clean setting; that recipient's own handling and privacy policy then apply. Keep your device and any exported backup secure.

## Changes to This Policy
We may update this privacy policy from time to time. Any changes will be reflected in the app's listing on the Google Play Store.

## Contact
If you have any questions about this privacy policy, please contact us through the bug report feature in the app or through the contact form: https://neatcodelabs.com/#contact
