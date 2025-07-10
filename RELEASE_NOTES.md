# FixupXer v1.4.0 - Major Engine Overhaul

## 🚀 What's New - Complete Engine Redesign

### Revolutionary Modular Architecture
- **All-New Cleaning Engine** - Replaced monolithic processor with lightning-fast modular architecture
- **11 Specialized Cleaners** - Each platform gets dedicated, optimized cleaning logic
- **964 Unique Tracking Parameters** - Industry-leading coverage across all platforms
- **Deep-Clean Technology** - Multi-pass cleaning ensures nothing gets missed
- **Lightning Fast** - O(1) domain lookup with intelligent caching

### New Platform Support
- **Substack Support Added** - Comprehensive tracking removal while preserving article links
- **Enhanced Coverage** - Every major platform now has dedicated, optimized cleaning

### Performance Breakthroughs
- **5x Faster Processing** - Optimized domain dispatch and parallel cleaning
- **Smart Caching** - LRU cache with 1-hour TTL reduces redundant processing
- **Thread-Safe Design** - Built for modern multi-core devices
- **Memory Efficient** - Stateless cleaners with minimal footprint

### Enhanced URL Detection
- **International Domain Support** - Full IDN (Internationalized Domain Names) support
- **Zero-Width Character Protection** - Removes invisible tracking characters
- **Improved Glued URL Detection** - Better accuracy with fewer false positives
- **Unicode Normalization** - Handles all character encodings properly

### Technical Excellence
- **100% Kotlin** - Modern, null-safe implementation
- **Comprehensive Testing** - Every cleaner thoroughly tested
- **Future-Proof Design** - Easy to add new platforms and parameters
- **Clean Architecture** - Maintainable, extensible, professional-grade code

## Cleaner Capabilities

### Social Media
- **Facebook** - 119 parameters removed (fbclid, fb_action_ids, mibextid, etc.)
- **Instagram** - 67 parameters removed (igshid, ig_mid, ig_cache_key, etc.)
- **Twitter/X** - 99 parameters removed (s, t, ref_src, twclid, etc.)
- **TikTok** - 124 parameters removed (_t, sec_uid, share_app_id, etc.)
- **LinkedIn** - 117 parameters removed (trackingId, lipi, midToken, etc.)
- **Reddit** - 91 parameters removed (context, rdt_cid, feature, etc.)

### E-commerce
- **Amazon** - 147 parameters removed (tag, ref_, ascsubtag, etc.)
- **AliExpress** - 100+ parameters removed (spm, algo_pvid, aff_trace_key, etc.)

### Content Platforms
- **YouTube** - 139 parameters removed (si, pp, feature, embeds_euri, etc.)
- **Substack** - 87 parameters removed (token, r, utm_*, etc.) - **NEW!**

### Search & Utilities
- **Google Search** - URL extraction plus 140 parameters removed
- **General Tracking** - 106 universal parameters for any website

## Why This Matters
- **Superior Privacy** - Most comprehensive tracking removal available
- **Faster Than Ever** - Optimized for instant results
- **Future Ready** - Built to grow with new tracking methods
- **Professional Grade** - Enterprise-quality architecture and testing

## Technical Details
- Minimum Android: 5.0 (API 21)
- Target Android: 15 (API 35)  
- Version Code: 16
- Light theme with Material Design 3

## Download
- [FixupXer-v1.4.0-release.apk](https://github.com/NeatCode-Labs/fixupxer/releases/download/v1.4.0/FixupXer-v1.4.0-release.apk)
- [FixupXer-v1.4.0-release.aab](https://github.com/NeatCode-Labs/fixupxer/releases/download/v1.4.0/FixupXer-v1.4.0-release.aab)

---

# FixupXer v1.3.5

## What's New
- **Improved URL Detection** - Refined glued URL detection to reduce false positives
- **Enhanced User Experience** - Error messages now appear in "Processed URL" field instead of temporary toasts
- **Better Domain Boundary Checking** - More accurate detection of URL boundaries

## Technical Changes
- Enhanced InputValidator with improved glued URL detection logic
- Updated UI to display error messages in result field for better visibility
- Improved domain boundary checking for more accurate URL detection
- Updated automated tests to expect error messages in result field

## Bug Fixes
- Fixed false positive "Multiple URLs detected" errors on legitimate URLs
- Improved error message visibility by moving from toast to result field
- Enhanced glued URL detection accuracy

## Compatibility
- Minimum Android: 5.0 (API 21)
- Target Android: 15 (API 35)
- Light theme with Material Design 3

## Download
- [FixupXer-v1.3.5-release.apk](https://github.com/NeatCode-Labs/fixupxer/releases/download/v1.3.5/FixupXer-v1.3.5-release.apk)
- [FixupXer-v1.3.5-release.aab](https://github.com/NeatCode-Labs/fixupxer/releases/download/v1.3.5/FixupXer-v1.3.5-release.aab)

---

# FixupXer v1.3.4

## What's New
- **Facebook URL Conversion** - Convert Facebook URLs to facebookez.com for better embedding and privacy
- **Facebook Prefix Removal** - Automatically removes prefixes like m., www., mobile., touch., web. when converting
- **Enhanced Toggle Functionality** - "Create embeddable link?" toggle now supports Facebook, Instagram, and Twitter/X
- **Platform-Agnostic Toggle** - Single toggle controls all supported platforms (Facebook, Instagram, Twitter/X)
- **Improved URL Processing** - Enhanced regex patterns for Facebook URL transformation
- **Consistent Behavior** - Facebook conversion works in both main screen and share screen

## Platform Support
- **Facebook**: facebook.com ↔ facebookez.com (with prefix removal)
- **Instagram**: instagram.com ↔ kkinstagram.com
- **Twitter/X**: x.com/twitter.com ↔ fixupx.com

## Technical Changes
- Added Facebook domain detection and processing scenarios
- Implemented comprehensive Facebook prefix removal regex
- Updated both MainActivity and ShareActivity with Facebook support
- Enhanced URL processing logic for Facebook URLs
- Improved toggle text to be platform-agnostic

## Bug Fixes
- Fixed toggle text specificity (now generic "Create embeddable link?" for all platforms)
- Enhanced Facebook URL processing with proper prefix handling
- Improved consistency between main screen and share screen functionality

## Compatibility
- Minimum Android: 5.0 (API 21)
- Target Android: 15 (API 35)
- Light theme with Material Design 3

## Download
- [FixupXer-v1.3.4-release.apk](https://github.com/NeatCode-Labs/fixupxer/releases/download/v1.3.4/FixupXer-v1.3.4-release.apk)

---

# FixupXer v1.3.3

## What's New
- **Fixed Instagram URL Detection** - Instagram URLs no longer incorrectly flagged as "Multiple URLs detected"
- **Fixed Case Sensitivity** - Instagram post IDs now preserve case (e.g., DLRNJjEx45S won't become lowercase)
- **Enhanced Glued URL Detection** - Improved detection to avoid false positives on legitimate URLs
- **Added Open Button to Share Screen** - Can now open processed URLs directly from the share screen
- **Improved Accessibility** - Fixed hardcoded text and added proper content descriptions
- **Better RTL Support** - Fixed padding issues for right-to-left languages
- **Minor UI Improvements** - Added autofill hints and fixed various layout issues

## Bug Fixes
- Fixed Instagram URLs being rejected with "Multiple URLs detected" error
- Fixed case-sensitive URLs (like Instagram posts) being converted to lowercase
- Fixed glued URL detection triggering on legitimate URLs containing common TLDs
- Fixed missing Open button in ShareActivity
- Fixed hardcoded content descriptions for better accessibility
- Fixed RTL padding symmetry issues
- Fixed missing autofill hints on URL input field

## Technical Changes
- Enhanced InputValidator with TLD-based glued URL detection
- Added comprehensive TLD list for accurate domain boundary detection
- Improved URL validation logic to handle edge cases
- Reduced lint warnings from 97 to 94
- Added proper string resources for all UI text
- Synced all changes to GITHUB folder for F-Droid builds

## Compatibility
- Minimum Android: 5.0 (API 21)
- Target Android: 15 (API 35)
- Light theme with Material Design 3

## Download
- [FixupXer-v1.3.3-release.apk](https://github.com/NeatCode-Labs/fixupxer/releases/download/v1.3.3/FixupXer-v1.3.3-release.apk)

---

# FixupXer v1.3.2

## What's New
- **F-Droid Release Preparation** - Updated build configurations for F-Droid compliance
- **Build System Updates** - Enhanced build configuration for reproducible builds
- **Minor Improvements** - Various small fixes and optimizations

## Technical Changes
- Added dependenciesInfo block to GITHUB build configuration
- Updated version metadata for F-Droid submission
- Improved build reproducibility

## Compatibility
- Minimum Android: 5.0 (API 21)
- Target Android: 15 (API 35)
- Light theme with Material Design 3

## Download
- [FixupXer-v1.3.2-release.apk](https://github.com/NeatCode-Labs/fixupxer/releases/download/v1.3.2/FixupXer-v1.3.2-release.apk)

---

# FixupXer v1.3.1

## What's New
- **Enhanced Security Protection** - Comprehensive input validation to protect against malicious URL attacks
- **Improved Input Handling** - Better detection and rejection of glued URLs, malformed inputs, and attack vectors
- **Crash Prevention** - App now gracefully handles invalid input without freezing or crashing
- **Real-time Validation** - Instant feedback when typing problematic URLs or content
- **Better Error Messages** - Clear, user-friendly notifications for different types of invalid input
- **Share Screen Security** - Enhanced protection for content shared from other apps
- **Performance Optimization** - Faster validation with timeout protection against DoS attacks

## Security Improvements
- **Glued URL Protection** - Prevents attacks like "www.instagram.comwww.x.com"
- **Invisible Character Filtering** - Removes zero-width spaces and control characters
- **URL Encoding Safety** - Handles encoded attacks like "www%2Einstagram.com"
- **Multiple URL Detection** - Rejects input containing multiple URLs
- **Length Limits** - Prevents DoS attacks with overly long inputs
- **Unicode Normalization** - Protects against homograph attacks using lookalike characters

## Technical Changes
- Added comprehensive InputValidator utility class
- Enhanced MainActivity with real-time input validation
- Improved ShareActivity with security hardening
- Added timeout protection for regex operations
- Implemented proper error handling and user feedback
- Updated input sanitization pipeline

## Bug Fixes
- Fixed app crashes when pasting malformed URLs
- Fixed input field issues with special characters
- Fixed share screen handling of invalid content
- Improved error message clarity and consistency

## Compatibility
- Minimum Android: 5.0 (API 21)
- Target Android: 15 (API 35)
- Light theme with Material Design 3

## Download
- [FixupXer-v1.3.1-release.apk](https://github.com/NeatCode-Labs/fixupxer/releases/download/v1.3.1/FixupXer-v1.3.1-release.apk)

---

# FixupXer v1.3.0

## What's New
- **Updated to Android API 35** - Latest Android 15 support
- **Enhanced tracking parameter coverage** - Added comprehensive tracking parameter removal for all supported platforms
- **Improved documentation** - Completely updated SUPPORTED_PLATFORMS.md with accurate platform coverage details
- **Better URL conversion clarity** - Clear distinction between URL conversion (Instagram/Twitter) and tracking removal (all platforms)
- **F-Droid compatibility** - Added dependenciesInfo configuration for F-Droid builds
- **Build system modernization** - Migrated from KAPT to KSP for better performance
- **Enhanced privacy focus** - Comprehensive removal of tracking, analytics, and advertising parameters

## Platform Support
- **URL Conversion**: Instagram (→ kkinstagram.com) and Twitter/X (→ fixupx.com) by user decision
- **Tracking Removal**: 25+ platforms including Facebook, LinkedIn, YouTube, TikTok, Amazon, eBay, and more
- **General Tracking**: All UTM parameters, click IDs, analytics, advertising, and session tracking

## Technical Changes
- Updated compileSdk and targetSdk to 35 (Android 15)
- Migrated from KAPT to KSP annotation processing
- Added F-Droid-specific build configuration
- Enhanced tracking parameter definitions with platform-specific categories
- Improved build performance and reliability

## Bug Fixes
- Fixed build system compatibility issues
- Resolved dependency metadata conflicts for F-Droid
- Improved platform detection accuracy

## Compatibility
- Minimum Android: 5.0 (API 21)
- Target Android: 15 (API 35)
- Light theme with Material Design 3

## Download
- [FixupXer-v1.3.0-release.apk](https://github.com/NeatCode-Labs/fixupxer/releases/download/v1.3.0/FixupXer-v1.3.0-release.apk)

---

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
- Light theme with Material Design 3

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
- Light theme with Material Design 3

## Download
- [FixupXer-v1.2.1-release.apk](https://github.com/NeatCode-Labs/fixupxer/releases/download/v1.2.1/FixupXer-v1.2.1-release.apk)

---

Made with ❤️ by NeatCode Labs 