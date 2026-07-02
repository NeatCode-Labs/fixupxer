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
- Preferences: whether to clean tracking parameters, whether to convert Twitter/X, Instagram, and Facebook URLs, the selected Instagram embed proxy, any custom Instagram proxy domains you add yourself, browser-mode settings (enabled state, post-clean action mode and priority order, per-platform conversion defaults), and history settings (enabled state, maximum entries)
- Conversion history (optional, on by default, can be disabled or cleared at any time): the original URL, the cleaned URL, a timestamp, and the kind of change made — stored in a local database on your device only

Preferences are stored using Android's SharedPreferences system and history in a local Room (SQLite) database. Both are only accessible to the app itself and never leave your device.

### URL Processing
- All URL processing happens locally on your device
- URLs are not sent to any external servers
- No data is transmitted to third parties
- No tracking or analytics are performed
- Link conversions (e.g. to fixupx.com, facebookez.com, or an Instagram embed proxy — built-in or custom) are pure text replacements; the app never contacts those domains

### Browser Mode (optional)
If you enable Browser mode and set FixupXer as your default browser, links you open are cleaned locally and then forwarded to the app or browser you choose. FixupXer itself has no network permission and never loads the links.

## Third-Party Services
FixupXer does not integrate with any third-party services that collect user data. The app only uses standard Android system services for:
- Sharing URLs
- Opening URLs in a browser
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
