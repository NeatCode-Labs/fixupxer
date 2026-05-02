# Android Test Coverage for FixupXer v1.4.9

## Overview
This document describes the Android instrumentation tests covering all major features through v1.4.9. v1.4.9 fixes browser-mode routing and stabilizes the full emulator suite: 151/151 instrumentation tests pass on `Pixel_API_35_Play`, alongside 117/117 unit tests.

v1.4.8 refreshed the Instagram proxy roster (`toinstagram.com` Primary/default, `adamlikes.men` Primary, `instagram7.com` Backup) and introduced bare-hostname conversion. Legacy proxies (`kkinstagram.com`, `eeinstagram.com`) remain detected for auto-migration.

## v1.4.9 Test Stabilization
- [x] `SettingsTest` conversion-defaults tests now use deterministic `NestedScrollView` scrolling instead of flaky Espresso `scrollTo()`.
- [x] `BrowserModeTest.testActionModeSelection` checks effective visibility state for the priority section instead of requiring the section to be inside the current viewport.
- [x] `ApiCompatibilityTest` closes `ActivityScenario` instances deterministically to avoid emulator/process lifecycle crashes.
- [x] Full `connectedAndroidTest` completed with 0 failures on `Pixel_API_35_Play`.

## Instagram Proxy Selection Tests (v1.4.8)

### InstagramProxyPreferenceTest.kt (instrumentation)
- [x] Default proxy is `toinstagram.com` (= `Constants.INSTAGRAM_DEFAULT_PROXY`)
- [x] Setting toinstagram.com persists
- [x] Setting adamlikes.men persists
- [x] Setting instagram7.com persists
- [x] Invalid stored value falls back to default
- [x] Legacy `kkinstagram.com` stored value migrates to default
- [x] Legacy `eeinstagram.com` stored value migrates to default

### SettingsActivityProxyTest.kt (instrumentation)
- [x] Default radio is `radioProxyTo` (toinstagram.com)
- [x] Clicking adamlikes persists the choice
- [x] Clicking instagram7 persists the choice
- [x] Previously selected proxy restored on relaunch
- [x] Info icon visible

### MainActivityProxyLabelTest.kt (instrumentation)
- [x] Instagram URL shows "Active: toinstagram.com." by default
- [x] Changing pref to adamlikes.men updates label on relaunch
- [x] Changing pref to instagram7 updates label on relaunch
- [x] Facebook URL keeps toggle container visible but hides proxy row

### ShareActivityProxyLabelTest.kt (instrumentation)
- [x] Instagram share shows "Active: toinstagram.com." by default
- [x] Share uses adamlikes.men when preference is set
- [x] Clicking "Change." opens inline dialog (not Settings) and updates the label in place

### InstagramProxySelectionTest.kt (unit)
Forward / reverse / cross-proxy / no-op / dirty / `www.` stripping / sub-prefix stripping / legacy auto-migration / `isInstagramUrl` coverage. All assertions explicit via `Constants.*_DOMAIN` references — no string literals duplicated across tests.

## Prior Coverage (unchanged)

## Running the Tests

To run all Android instrumentation tests, use:
```bash
# With emulator running (Pixel_API_35_Play)
./gradlew connectedAndroidTest

# Or specifically for debug variant
./gradlew connectedDebugAndroidTest
```

## Test Files and Coverage

### 1. **HistoryDatabaseTest.kt** [x] ACTIVE
Tests the Room database functionality for URL history:
- [x] Insert and retrieve history entries
- [x] Multiple entries with proper ordering
- [x] Delete individual entries by ID
- [x] Delete all history
- [x] Trim history to keep only newest entries
- [x] Facebook prefix removal verification

### 2. **ShareActivityTest.kt** [x] ACTIVE
Tests the Share Activity functionality:
- [x] Instagram URL conversion (toggle ON/OFF)
- [x] Twitter/X URL conversion (toggle ON/OFF)
- [x] Facebook URL conversion with prefix removal
- [x] "Nothing to do" message for already clean URLs
- [x] Dirty URL cleaning while preserving domain preference
- [x] Share and Copy button functionality
- [x] Toggle visibility based on URL type
- [x] Multiple URLs rejection
- [x] Invalid URL handling
- [x] History recording on share

### 3. **BidirectionalConversionTest.kt** [x] ACTIVE
Comprehensive tests for bidirectional URL conversions:
- [x] **Instagram**: clean/dirty × instagram.com / active proxy / legacy proxy × toggle ON/OFF (v1.4.8)
- [x] **Twitter/X**: clean/dirty × x.com/fixupx.com × toggle ON/OFF
- [x] **Facebook**: clean/dirty × facebook.com/facebookez.com × toggle ON/OFF
- [x] **"Nothing to do"** scenarios for all platforms
- [x] **Edge cases**: mixed case, fragments, fxtwitter.com
- [x] **All prefix removals**: m., web., www. for Facebook

### 4. **MainActivityHistoryTest.kt** [x] ACTIVE
Tests the History dialog and related UI:
- [x] History button visibility
- [x] History dialog opening
- [x] RecyclerView display with entries
- [x] Long press to delete entries
- [x] Clear all history functionality
- [x] Copy/Share buttons in history items
- [x] Donate button in footer
- [x] History dialog dismissal
- [x] URL validation with history feature

### 5. **UrlValidationImprovementsTest.kt** [x] ACTIVE
Tests the improved URL validation logic:
- [x] Facebook story.php URLs not falsely rejected
- [x] URLs with multiple query parameters accepted
- [x] URLs with dots in query parameters accepted
- [x] Actual multiple URLs still rejected
- [x] Glued URLs without space rejected
- [x] URLs with file extensions accepted
- [x] URLs with port numbers accepted
- [x] Alternative domains (facebookez, fixupx, toinstagram, adamlikes, instagram7, legacy kkinstagram for migration) recognized

### 6. **SettingsTest.kt** [x] ACTIVE
All tests now active:
- [x] About dialog display
- [x] History toggle in settings dialog
- [x] Max history entries dialog and validation  
- [x] Back navigation (close dialog)
- [x] Max entries validation (min/max values)
- [x] History disabled stops recording

### 7. **UrlInputValidationTest.kt** [x] ACTIVE
Tests URL input validation:
- [x] Glued URLs rejection
- [x] Zero-width space attack prevention
- [x] URL-encoded dot attack prevention
- [x] Control character attack prevention
- [x] Valid URL acceptance
- [x] Multiple protocols rejection
- [x] Unicode normalization handling
- [x] Process button functionality
- [x] Empty input handling

### 8. **BrowserModeTest.kt** [x] ACTIVE
Tests browser integration functionality:
- [x] Browser mode toggle functionality
- [x] Action mode selection (Ask vs Priority)
- [x] Action priority configuration
- [x] Browser mode independent from main toggles
- [x] Conversion defaults handling
- [x] VIEW intent handling
- [x] Menu items order
- [x] Settings activity browser mode toggle

### 9. **AccessibilityTest.kt** [x] ACTIVE
Tests accessibility compliance:
- [x] Content descriptions for all buttons
- [x] Main activity accessibility
- [x] Color contrast verification

### 10. **ResponsiveDesignTest.kt** [x] ACTIVE
Tests responsive design implementation:
- [x] Portrait orientation layout
- [x] Landscape orientation layout
- [x] Orientation changes
- [x] Small screen size adaptation

### 11. **TouchTargetTest.kt** [x] ACTIVE
Tests touch target compliance:
- [x] Button touch targets meet 48dp minimum
- [x] Clickable text views accessibility

### 12. **KeyboardNavigationTest.kt** [x] ACTIVE
Tests keyboard navigation:
- [x] Input field keyboard handling
- [x] Paste button functionality
- [x] Navigation flow between elements
- [x] Empty state validation

### 13. **OfflinePerformanceTest.kt** [x] ACTIVE
Tests performance and offline functionality:
- [x] Offline functionality verification
- [x] App startup time testing
- [x] URL processing performance
- [x] Memory usage testing
- [x] History performance testing

### 14. **ApiCompatibilityTest.kt** [x] ACTIVE
Tests API compatibility:
- [x] Current API level compatibility
- [x] Minimum SDK compatibility
- [x] Configuration changes handling
- [x] Material Design components
- [x] Theme compatibility

### 15. **ReleaseTestSuite.kt** [x] ACTIVE
Tests release build functionality:
- [x] App launches successfully
- [x] Core URL processing
- [x] Release configuration
- [x] All platform conversions
- [x] History feature
- [x] Copy/Share buttons

### 16. **SmartFooterTest.kt** [x] ACTIVE
Tests smart footer functionality:
- [x] Footer visibility
- [x] Footer content verification
- [x] Footer clickability
- [x] Scroll view positioning

### 17. **ShareActivityNoDuplicatesTest.kt** [x] ACTIVE
Tests duplicate prevention in share activity:
- [x] Share activity launches and processes URL
- [x] Instagram URL processed without duplicates
- [x] Facebook URL processed without duplicates
- [x] Toggle changes don't cause errors

## Test Dependencies Added
```kotlin
androidTestImplementation("androidx.test.espresso:espresso-contrib:3.5.1") // RecyclerView
androidTestImplementation("androidx.test.ext:junit:1.1.5")
androidTestImplementation("androidx.test:runner:1.5.2")
androidTestImplementation("androidx.room:room-testing:2.6.1") // Room testing
```

## Features Covered

### History Feature [x]
- Room database operations
- History dialog UI
- Long press to delete
- Clear all functionality
- Max entries enforcement (via trimHistory)

### ShareActivity [x]
- All toggle combinations
- URL reprocessing on toggle change
- "Nothing to do" logic
- History integration
- Facebook prefix removal

### URL Validation Improvements [x]
- Query parameter handling
- Domain-only dot counting
- Alternative domain support

### Bidirectional URL Conversions [x] (COMPLETE)
All possible combinations tested:
- Clean → Converted (toggle ON)
- Converted → Original (toggle OFF)
- Dirty → Clean + Converted
- Already correct → "Nothing to do"

### Settings [x]
- History toggle functionality
- History disabled stops recording
- Max entries dialog and validation
- About dialog display
- Back navigation from dialogs

### Facebook Conversion [x]
- Bidirectional conversion
- Prefix removal (m., web., www.)
- Clean URL preservation

## Running Individual Test Classes

```bash
# Run specific test class
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.fixupxer.HistoryDatabaseTest

# Run with specific test method
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.fixupxer.ShareActivityTest#testFacebookUrlConversionWithToggleOn

# Run the new bidirectional tests
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.fixupxer.BidirectionalConversionTest
```

## Current Test Status
- **Unit Tests**: 117/117 passing
- **Instrumentation Tests**: 151/151 passing on `Pixel_API_35_Play`
- **Total Tests**: 268/268 passing
- **Test Success Rate**: 100%

## URL Conversion Test Matrix
All scenarios from the URL_CONVERSION_TEST_COVERAGE.md are now fully tested with the addition of BidirectionalConversionTest.kt

## Notes
- Tests require Android emulator or device; final v1.4.9 verification used `Pixel_API_35_Play`.
- Emulator animations should be disabled for stable Espresso runs (`window_animation_scale`, `transition_animation_scale`, `animator_duration_scale` = `0`).
- Wait times in tests account for async UI processing where idling resources are not available.
- SettingsActivity tests are active and passing, including browser conversion defaults and Instagram proxy selection.
- All core v1.4.9 browser-mode fixes and v1.4.8 Instagram proxy behavior are covered by active tests.
- Bidirectional URL conversions are comprehensively tested.
