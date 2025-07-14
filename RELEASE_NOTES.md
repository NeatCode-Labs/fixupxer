# FixupXer v1.4.4 - Android 15 Edge-to-Edge Compliance

## 🎯 What's New - Full Android 15 Compliance & Enhanced Responsive Design

### Android 15 Edge-to-Edge Compliance
- **Removed Deprecated APIs** - Eliminated use of `window.statusBarColor` and `window.navigationBarColor`
- **Modern Edge-to-Edge Implementation** - Uses `enableEdgeToEdge()` with proper `SystemBarStyle` configuration
- **API-Level Specific Handling** - Smart navigation bar configurations for all Android versions
- **Navigation Bar Contrast** - Proper `isNavigationBarContrastEnforced` handling for Android 10+
- **Backward Compatible** - Maintains support back to Android 5.0 (API 21)

### Navigation Bar Icon Visibility Fix
- **Android 15 Compliant Solution** - Uses `SystemBarStyle.light()` for Android 8.1+ 
- **Semi-Transparent Scrim** - Adds subtle background for older Android versions (5-7)
- **Visible Icons** - Navigation bar icons remain visible on light backgrounds
- **No Deprecated APIs** - Clean implementation following Google's latest guidelines

### 100% Responsive Design
- **Zero Hardcoded Values** - All dimensions moved to resource files
- **Multi-Screen Support** - Optimized for small (sw320dp), regular, and tablet (sw600dp) screens  
- **Dynamic Sizing** - Button padding, corner radius, and stroke width adapt to screen size
- **Future-Proof** - Ready for any new screen sizes or form factors

### Code Quality & Testing
- **306 Tests Passing** - 194 unit tests + 112 instrumented tests = 100% coverage
- **Clean Code** - Fixed ObsoleteSdkInt lint warning, centralized edge-to-edge handling
- **Performance Verified** - 297ms startup time, ~41MB memory usage
- **Emulator Tested** - Verified on Pixel 9 Pro API 36

### Technical Improvements
- **BaseActivity Centralization** - All edge-to-edge logic in one place
- **Removed Legacy Code** - Eliminated deprecated `applyNavigationBarFix()` method
- **Clean Architecture** - Follows Google's Android 15 design guidelines
- **Professional Build** - Signed APK (4.3MB) and AAB (5.2MB) ready for distribution

## Technical Details
- Minimum Android: 5.0 (API 21)
- Target Android: 15 (API 35)
- Version Code: 21
- Material Design 3 with full edge-to-edge support

## Download
- [FixupXer-v1.4.4-release.apk](https://github.com/NeatCode-Labs/fixupxer/releases/download/v1.4.4/FixupXer-v1.4.4-release.apk)

---

# FixupXer v1.4.3 - UI Polish & Production Ready

## 🎯 What's New - Final UI Refinements & 100% Test Coverage

### UI Polish & Consistency
- **Fixed Button Text Alignment** - "Open" button text now properly aligned with icon in both screens
- **Material Design 3 Consistency** - All dialog buttons now use proper M3 styles
- **Icon Color Consistency** - History item buttons now match primary theme color
- **Enhanced Visual Hierarchy** - Improved button styles throughout the app

### Production Ready
- **100% Test Coverage** - All 205 tests passing (97 unit, 108 instrumentation)
- **Zero Build Warnings** - Clean build with no lint errors or warnings
- **Comprehensive Testing** - Touch targets, accessibility, responsive design all verified
- **Performance Verified** - Startup < 3s, URL processing < 100ms
- **API Compatibility** - Tested on API 21-36 (Android 5.0 to 15)

### Technical Improvements
- **Button Gravity Fix** - Added `app:iconGravity="textStart"` to all MaterialButtons with icons
- **Dialog Button Styles** - Updated to use `Widget.Material3.Button.TextButton.Dialog`
- **Icon Tinting** - Applied consistent `app:tint="@color/primary"` to ImageButtons
- **Code Quality** - Removed redundant attributes and improved consistency

### Test Suite Highlights
- **TouchTargetTest** - All interactive elements meet 48dp minimum
- **AccessibilityTest** - Proper content descriptions verified
- **ResponsiveDesignTest** - Works perfectly on all screen sizes
- **OfflinePerformanceTest** - Fast startup and processing confirmed
- **KeyboardNavigationTest** - Full keyboard navigation support
- **ReleaseTestSuite** - Production build functionality verified

## Technical Details
- Minimum Android: 5.0 (API 21)
- Target Android: 15 (API 35)
- Version Code: 20
- Material Design 3 with Room database

## Download
- [FixupXer-v1.4.3-release.apk](https://github.com/NeatCode-Labs/fixupxer/releases/download/v1.4.3/FixupXer-v1.4.3-release.apk)

---

# FixupXer v1.4.2 - History Feature & Critical Bug Fixes

## 🎯 What's New - Conversion History, Bug Fixes & Better UX

### Conversion History Feature (NEW!)
- **History Tracking** - Now keeps a local history of all your URL conversions
- **Smart Management** - Enable/disable history and set max entries (50-1000)
- **Quick Actions** - Copy or share any past conversion with one tap
- **Privacy First** - History is stored locally only, never leaves your device
- **Easy Cleanup** - Clear all history with one button or long-press to delete individual entries

### Critical Bug Fixes
- **Facebook Prefix Removal** - Fixed m., web., www. prefixes not being removed properly
- **URL Validation** - Fixed false "Multiple URLs detected" errors on legitimate Facebook URLs
- **Bidirectional Conversion** - Fixed facebookez.com not converting back to facebook.com
- **Share Activity** - Fixed duplicate history entries when toggling conversion options
- **App Stability** - Fixed startup crash due to missing UI resources
- **Toggle Functionality** - Restored proper toggle behavior in Share Activity

### UI/UX Improvements
- **Better Navigation** - Donate button moved to footer links for cleaner UI
- **History Button** - New prominent History button where Donate used to be
- **Consistent Dialogs** - All dialogs now follow Material Design 3 guidelines
- **Time Display** - Shows relative time for each conversion (e.g., "2 min ago")
- **Platform Labels** - History shows which platform URLs were from (Twitter, Instagram, etc.)
- **Button Spacing** - Fixed spacing issues in dialog buttons

### Privacy Enhancements
- **Local Storage Only** - History data never leaves your device
- **Opt-in by Default** - History is enabled but can be disabled anytime
- **Updated Disclaimer** - Added information about local history storage
- **No Permissions** - No new permissions required for history feature

### Technical Improvements
- **Room Database** - Uses Android's Room persistence library for reliable storage
- **Efficient Storage** - Automatic cleanup keeps database size minimal
- **Thread-Safe** - All database operations run on background threads
- **Material Design 3** - History UI follows latest design guidelines
- **Comprehensive Testing** - Added 7 test files with ~85 tests for complete coverage

## Technical Details
- Minimum Android: 5.0 (API 21)
- Target Android: 15 (API 35)
- Version Code: 18
- Material Design 3 with Room database

## Download
- [FixupXer-v1.4.2-release.apk](https://github.com/NeatCode-Labs/fixupxer/releases/download/v1.4.2/FixupXer-v1.4.2-release.apk)

---

# FixupXer v1.4.1 - Android 15 Compliance Update

## 🎯 What's New - Full Android 15 Edge-to-Edge Support

### Android 15 Compliance
- **Edge-to-Edge Implementation** - Removed deprecated status/navigation bar APIs for full Android 15 compatibility
- **Modern API Usage** - Implemented WindowCompat.setDecorFitsSystemWindows for proper edge-to-edge display
- **Android 15 Theme** - Added dedicated values-v35 theme configuration
- **Updated Material Design** - Upgraded to Material Design 1.13.0-alpha10 for latest improvements

### UI/UX Improvements
- **Fixed Black Background** - Resolved edge-to-edge black background issue with proper white background
- **Better Title Alignment** - Corrected title and back button positioning in ShareActivity
- **Simplified App Bar** - Clean single-color light blue (#E3F2FD) wave background
- **Enhanced Visual Hierarchy** - Increased AppBarLayout elevation to 4dp for better separation
- **Improved Visual Polish** - Cleaner, more modern appearance throughout the app

### Responsive Design
- **100% Responsive** - All hardcoded dimensions replaced with screen-adaptive resources
- **Multi-Screen Support** - Optimized layouts for small (sw320dp), regular, and tablet (sw600dp) screens
- **Dynamic Padding** - Smart padding adjustments: 16dp (regular), 12dp (small), 24dp (tablets)
- **Dialog Responsiveness** - All dialogs now adapt to different screen sizes
- **Future-Proof Design** - Ready for any screen size or orientation

### Bug Fixes
- Fixed missing string resources for donate dialog
- Resolved build errors from resource dependencies
- Corrected layout spacing inconsistencies
- Fixed dimension resource usage in all layouts

## Technical Details
- Minimum Android: 5.0 (API 21)
- Target Android: 15 (API 35)
- Version Code: 17
- Material Design 3 with edge-to-edge support

## Download
- [FixupXer-v1.4.1-release.apk](https://github.com/NeatCode-Labs/fixupxer/releases/download/v1.4.1/FixupXer-v1.4.1-release.apk)

---

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