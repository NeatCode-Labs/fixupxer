# FixupXer Privacy Policy

## Overview
FixupXer is a URL processing app that helps users clean tracking parameters from URLs and convert social media links for better embedding. This privacy policy explains how we handle user data.

## Data Collection and Usage
FixupXer does not collect, store, or share any personal user data. The app operates entirely on the user's device and processes URLs locally.

### What We Don't Collect
- No personal information
- No usage statistics
- No analytics data
- No device information
- No location data
- No user identifiers

### Local Storage
The app only stores the following data locally on your device:
- Preferences: whether to clean tracking parameters, social-link conversion choices, selected built-in or custom embed proxies, Browser-mode settings (enabled state, post-clean action mode and priority order, X/Bluesky/Reddit/Pinterest Reader choices), remembered after-clean destinations (host → app choice only), app layout, and history settings (enabled state, maximum entries)
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
Since FixupXer does not collect or transmit any user data, there are no security concerns regarding data storage or transmission.

## Changes to This Policy
We may update this privacy policy from time to time. Any changes will be reflected in the app's listing on the Google Play Store.

## Contact
If you have any questions about this privacy policy, please contact us through the bug report feature in the app or through the contact form: https://neatcodelabs.com/#contact
