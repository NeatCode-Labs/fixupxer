# FixupXer v1.2.2

## What's New
- **Fixed paste button freeze issue** - App no longer freezes when pasting large text
- **Enhanced URL detection** - Shows "URL is already clean" or "No action necessary" messages appropriately
- **Added Twitter/X toggle** - Convert between x.com and fixupx.com (appears only for Twitter URLs)
- **Added FxTwitter support** - Now supports fxtwitter.com domain for Twitter embeds
- **Improved Instagram toggle logic** - Better handling of clean/dirty URLs with kkinstagram conversion
- **Fixed share screen layout** - Shared text field now has proper fixed height
- **Added button icons** - Share, Copy, and Open buttons now have visual icons
- **Better user feedback** - Shows "No URL found in clipboard" when appropriate
- **UI improvements** - Removed URL input underline, changed Process button to blue outline style

## Technical Changes
- Migrated to View Binding (removed all findViewById calls)
- Moved all hardcoded strings to resources
- Added dimension resources for responsive design
- Optimized URL finding with 500ms timeout to prevent freezes
- Fixed extra closing brace in UrlProcessor.kt
- Improved coroutine usage for better performance

## Bug Fixes
- Fixed app freeze when pasting large clipboard content
- Fixed share screen text field expanding with content
- Fixed Instagram toggle behavior for all 8 scenarios
- Fixed button color inconsistencies
- Fixed layout issues on different screen sizes

## Compatibility
- Minimum Android: 5.0 (API 21)
- Target Android: 14 (API 35)
- Supports both light and dark themes

## Download
- [FixupXer-v1.2.2-release.apk](https://github.com/NeatCode-Labs/fixupxer/releases/download/v1.2.2/FixupXer-v1.2.2-release.apk)

---

# FixupXer v1.2.1

## What's New
- Added clickable footer link in main screen
- Added disclaimer dialog for transparency
- Improved UI consistency and fixed minor bugs
- Cleaned up codebase structure

## Technical Changes
- Removed legacy Java code directories
- Updated to latest Android build tools
- Enhanced link processing reliability
- Improved error handling

## Compatibility
- Minimum Android: 5.0 (API 21)
- Target Android: 14 (API 35)
- Supports both light and dark themes

## Download
- [FixupXer-v1.2.1-release.apk](https://github.com/NeatCode-Labs/fixupxer/releases/download/v1.2.1/FixupXer-v1.2.1-release.apk)

---

Made with ❤️ by NeatCode Labs 