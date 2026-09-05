# Android Build Checklist - FixupXer

This checklist ensures the app meets all quality standards before release.
**TEMPLATE — copy the checkboxes fresh for every release; do not reuse old [x] marks.**
Record the per-version results (test counts, artifact names, dates) in `BUILD_REPORT.md` and `TESTING_REPORT.md`.

## Pre-Build Code Analysis
- [ ] **Lint Analysis**: No critical warnings or errors
- [ ] **Code Review**: All recent changes reviewed
- [ ] **TODO/FIXME Check**: No pending TODO or FIXME comments
- [ ] **Deprecated API Check**: No usage of deprecated Android APIs

## Build Verification
- [ ] **Release Build**: `.\gradlew.bat assembleRelease` completes successfully
- [ ] **Unit Tests**: all unit tests pass (`.\gradlew.bat test`)
- [ ] **Android Tests**: full instrumentation suite passes on `Pixel_API_35_Play` (`.\gradlew.bat connectedAndroidTest`); record pass counts in `TESTING_REPORT.md` and `BUILD_REPORT.md`
- [ ] **Lint**: `.\gradlew.bat lintRelease` completes successfully
- [ ] **ProGuard/R8**: Release build with obfuscation completes successfully
- [ ] **APK Size**: Generated APK size is reasonable (under 10MB)
- [ ] **AAB Build**: App Bundle builds successfully (`.\gradlew.bat bundleRelease`)

## Security & Privacy
- [ ] **Permissions Check**: ZERO permissions declared in manifest (privacy-first requirement)
- [ ] **Network Security**: No network access of any kind
- [ ] **Secret Scanning**: No hardcoded passwords, API keys, or secrets
- [ ] **Debug Logs**: No debug logging in release builds (Timber ReleaseTree filters below WARN)

## Functionality Testing
- [ ] **App Installation**: Release APK installs correctly on device/emulator
- [ ] **App Launch**: App starts without crashes
- [ ] **Core Functionality**: Primary features work as expected
- [ ] **Share Functionality**: URL sharing and processing works correctly
- [ ] **Edge Cases**: Common edge cases handled gracefully

## Performance & Compatibility
- [ ] **Memory Usage**: No memory leaks detected during testing
- [ ] **ANR Check**: No Application Not Responding issues
- [ ] **API Compatibility**: Review minimum SDK 21 and target SDK 36 compatibility; record the API levels actually executed and distinguish emulator/device coverage from Robolectric coverage
- [ ] **Device Compatibility**: Tested on target devices/emulators

## Release Artifacts
- [ ] **Signing Configuration**: Release builds properly signed with production keystore
- [ ] **Version Code**: Version code incremented appropriately (root AND `GITHUB/fixupxer` mirror)
- [ ] **Version Name**: Version name follows semantic versioning
- [ ] **Release Notes**: Changelog updated with new features and fixes
- [ ] **Google Play AAB**: `FixupXer-vX.Y.Z-release.aab` generated and signed (from root)
- [ ] **GITHUB/F-Droid APK**: APK built from `GITHUB/fixupxer` tree and verified WITHOUT Play dependency metadata (`dependencies.pb`) and WITHOUT `adi-registration.properties`

## Final Verification
- [ ] **Smoke Test**: Complete end-to-end functionality test
- [ ] **Regression Test**: Previously working features still function
- [ ] **Documentation**: README and documentation updated as needed
- [ ] **Backup**: Release artifacts backed up securely

## Sign-off
- [ ] **Developer Review**: All checklist items completed
- [ ] **Quality Assurance**: App meets quality standards for release
- [ ] **Ready for Distribution**: App approved for release to users

---
**Build Status**: (fill in)  
**Completion Date**: (fill in)  
**Build Version**: (fill in)  
**Total Tests**: (fill in)  
**Test Success Rate**: (fill in)  
