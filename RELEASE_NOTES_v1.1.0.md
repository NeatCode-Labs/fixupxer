# FixupXer v1.1.0 Release Notes

## 🎉 Major Improvements

### Architecture & Code Quality
- **Migrated to MVVM Architecture**: Implemented proper Model-View-ViewModel pattern with ViewModels and StateFlow for better state management
- **Added Dependency Injection**: Integrated Hilt/Dagger for clean dependency management
- **Introduced Clean Architecture**: Separated concerns with Repository pattern and domain/data layers
- **Replaced Executors with Coroutines**: Modern asynchronous programming using Kotlin Coroutines

### Dependencies & SDK Updates
- **Updated to Android SDK 35**: Now targeting the latest Android SDK for better compatibility
- **Upgraded all dependencies**: 
  - Kotlin 1.8.21 → 1.9.23
  - Android Gradle Plugin 8.1.2 → 8.3.2
  - Compose BOM 2023.01.00 → 2024.08.00
  - Material 1.8.0 → 1.12.0
  - And many more...

### Testing & Quality
- **Added Robolectric**: Proper Android unit testing support
- **Comprehensive Test Suite**: 19 unit tests covering all URL processing scenarios
- **Added Timber Logging**: Better logging with automatic log stripping in release builds
- **Improved ProGuard Rules**: More specific rules for better code optimization

### URL Processing Enhancements
- **Configurable Tracking Parameters**: Moved tracking parameters to a maintainable configuration object
- **Improved URL Validation**: Better handling of malformed and encoded URLs
- **Fixed @ Prefix Handling**: Properly processes Instagram URLs shared with @ prefix
- **Case-Insensitive Parameter Matching**: Fixed issues with mixed-case tracking parameters

### UI/UX Improvements
- **Added Progress Indicators**: Visual feedback during URL processing
- **Better Error Handling**: More descriptive error messages
- **Improved Accessibility**: Added content descriptions for all interactive elements
- **Enhanced Share Activity**: Better handling of shared URLs with automatic processing

### Build & Development
- **Kotlin DSL Build Scripts**: Migrated from Groovy to Kotlin for build configuration
- **Build Optimizations**: Enabled R8 full mode, resource shrinking, and minification
- **Memory Leak Detection**: Added LeakCanary for debug builds
- **Proper File Organization**: Cleaner project structure with feature-based packaging

## 🐛 Bug Fixes
- Fixed tracking parameter removal for mixed-case parameters (e.g., linkCode)
- Fixed Instagram URL processing with @ prefix
- Fixed URL decoding issues with encoded URLs
- Fixed test failures with Android Uri mocking
- Removed duplicate tracking parameter handling

## 🔒 Security Improvements
- Better keystore management with environment variables
- Improved ProGuard obfuscation
- No hardcoded secrets or passwords
- Certificate pinning ready architecture

## 📦 Technical Details
- **Min SDK**: 21 (Android 5.0)
- **Target SDK**: 35 (Android 15)
- **Version Code**: 4
- **Version Name**: 1.1.0

## 🚀 Performance
- Reduced APK size through better ProGuard rules
- Faster URL processing with optimized regex patterns
- Improved memory usage with proper lifecycle management
- Background processing with coroutines instead of thread pools

## 🧪 Testing
All 19 unit tests passing, covering:
- URL tracking parameter removal
- Twitter/X to FixupX conversion
- Instagram to kkinstagram conversion
- URL encoding/decoding
- Edge cases and error scenarios

## 📝 Notes for Developers
- Project now uses Hilt for dependency injection
- All activities must be annotated with @AndroidEntryPoint
- Use ViewModels for state management
- Timber for logging instead of Android Log
- Coroutines for async operations

## Signed Package Verification
The APK and AAB are properly signed with the following certificate fingerprints:
- **SHA1**: 78:89:4C:1D:1C:14:DF:8E:D8:AA:8E:A0:73:41:CC:71:0D:4C:66:C5
- **SHA256**: 78:E3:69:50:96:3A:98:EA:39:FE:30:B9:55:C2:73:64:E1:87:FE:CA:85:A1:AF:6A:D1:09:87:D1:5F:18:EC:2F

## Final Build Updates
- Fixed bug report email address to use neatcodelabs@gmail.com

---

Made with ❤️ by NeatCode Labs 