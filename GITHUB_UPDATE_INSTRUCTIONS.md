# GitHub Update Instructions for FixupXer (Future-Proof)

This guide provides a step-by-step, version-agnostic process for updating the FixupXer app on GitHub for any future release. It incorporates the latest best practices and explicit maintainer requirements for reproducible builds.

## Prerequisites
- Git installed and configured
- Access to the FixupXer GitHub repository
- Proper permissions to push and create releases
- Android build environment set up (Android Studio, JDK, etc.)

## Step 1: Update Version Information

1. **Update version numbers in your app's build files:**
   - `app/build.gradle.kts` (update `versionCode` and `versionName`)
   - `app/src/main/res/values/strings.xml` (update `app_version` if present)
2. **Update changelog and documentation:**
   - `RELEASE_NOTES.md` (add a new section for the release)
   - Any other relevant documentation files

## Step 2: Commit and Push Changes

1. **Stage all changes:**
   ```bash
   git add .
   ```
2. **Commit with a descriptive message:**
   ```bash
   git commit -m "Update to version X.Y.Z"
   ```
   Replace `X.Y.Z` with your new version.
3. **Push to the main branch:**
   ```bash
   git push origin main
   ```

## Step 3: Create and Push a Tag for the Release

1. **Create a new annotated tag:**
   ```bash
   git tag -a vX.Y.Z -m "Release vX.Y.Z"
   ```
2. **Push the tag to GitHub:**
   ```bash
   git push origin vX.Y.Z
   ```
   Replace `X.Y.Z` with your new version.

## Step 4: Build the Release APK/AAB from a Clean Clone (Maintainer Requirement)

1. **Clone a fresh copy of the repository:**
   ```bash
   git clone https://github.com/NeatCode-Labs/fixupxer.git fixupxer-clean
   cd fixupxer-clean
   ```
2. **Checkout the tagged commit:**
   ```bash
   git checkout vX.Y.Z
   ```
3. **Build the release APK/AAB:**
   ```bash
   ./gradlew assembleRelease   # For APK
   ./gradlew bundleRelease     # For AAB (if needed)
   ```
   - The APK will be in `app/build/outputs/apk/release/`
   - The AAB will be in `app/build/outputs/bundle/release/`

## Step 5: Prepare a GitHub Release and Upload the Artifact

1. **Go to your repository on GitHub.**
2. **Click on 'Releases' > 'Draft a new release'.**
3. **Select the new tag (e.g., `vX.Y.Z`).**
4. **Fill in the release title and description:**
   - Title: `FixupXer vX.Y.Z`
   - Description: Copy the relevant section from `RELEASE_NOTES.md` or your changelog
5. **Attach the APK/AAB file built from the clean clone:**
   - Upload your built `FixupXer-vX.Y.Z-release.apk` or `.aab` file from the clean build
6. **Publish the release.**

## Step 6: Verify Everything is Up to Date

- Check that the main branch, tags, and releases reflect the new version
- Ensure all documentation and changelogs are current
- Confirm that the release artifacts are downloadable from GitHub and match the tagged commit

## Troubleshooting
- **Tag not found:** Make sure you pushed the tag to GitHub and it matches your release.
- **Build errors:** Double-check version numbers and dependencies in your build files.
- **Permission issues:** Ensure you have push and release rights on the repository.
- **APK/AAB mismatch:** Always build from a clean clone at the tagged commit to ensure reproducibility.

## Notes
- **Always use annotated tags for releases (with `-a` and `-m`).**
- **Keep your changelog and documentation up to date for every release.**
- **Do not rewrite history or force-push unless absolutely necessary.**
- **Coordinate with collaborators to avoid merge conflicts.**
- **Always build and upload release artifacts from a clean clone at the tagged commit.**

---

**This guide is version-agnostic and should be followed for all future FixupXer GitHub updates.** 