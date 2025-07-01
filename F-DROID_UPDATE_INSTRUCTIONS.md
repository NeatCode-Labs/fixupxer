# F-Droid Update Instructions for FixupXer (Future-Proof)

This guide walks you through updating FixupXer on F-Droid for any future release, following the latest best practices from F-Droid maintainers.

## Prerequisites
- Git installed and configured
- Access to your GitHub repository
- Access to your F-Droid data repository fork on GitLab
- Basic familiarity with terminal commands

## Step 1: Prepare Your Local Repository

```bash
cd /path/to/your/FixupXer_app
```

## Step 2: Update Version Information

1. **Update version numbers in your app's build files** (e.g., `build.gradle.kts`, `strings.xml`).
2. **Commit and push** your changes to your GitHub repository.
3. **Create and push a new tag** for the release:
   ```bash
   git tag -a vX.Y.Z -m "Release vX.Y.Z"
   git push origin vX.Y.Z
   ```
   Replace `X.Y.Z` with your new version.

## Step 3: Update F-Droid Metadata (com.fixupxer.yml)

1. **Navigate to your F-Droid data repository**:
   ```bash
   cd /path/to/your/fdroiddata
   git fetch origin
   git checkout -b fixupxer-update
   ```
2. **Edit the metadata file**:
   ```bash
   nano metadata/com.fixupxer.yml
   ```
3. **Update ONLY the following fields to match your new release:**
   - `versionName:` (e.g., `1.3.4`)
   - `versionCode:` (e.g., `13`)
   - `commit:` (e.g., `v1.3.4`)
   - `CurrentVersion:` (e.g., `1.3.4`)
   - `CurrentVersionCode:` (e.g., `13`)

   **Example block:**
   ```yaml
   Builds:
     - versionName: 1.3.4
       versionCode: 13
       commit: v1.3.4
       subdir: app
       gradle:
         - yes
       prebuild:
         - sed -i -e '/org.gradle.java.home/d' ../gradle.properties
         - sed -i -e '/jetbrains/d' ../settings.gradle.kts
   AllowedAPKSigningKeys: <your-key>
   AutoUpdateMode: Version
   UpdateCheckMode: Tags
   CurrentVersion: 1.3.4
   CurrentVersionCode: 13
   ```
   - **Do NOT keep old build blocks** for previous versions. Only the latest version should be present.
   - **Do NOT change other fields unless necessary.**

## Step 4: Update Changelog (Optional but Recommended)

1. Add a new changelog file for the version:
   ```bash
   nano metadata/en-US/changelogs/<versionCode>.txt
   ```
2. Paste your changelog content for this release.

## Step 5: Commit and Push F-Droid Changes

```bash
git add metadata/com.fixupxer.yml
# Optionally add changelog:
git add metadata/en-US/changelogs/<versionCode>.txt
git commit -m "Update FixupXer to vX.Y.Z"
git push origin fixupxer-update
```

## Step 6: Update or Create Merge Request on GitLab
- Go to your fork of `fdroiddata` on GitLab
- Create or update a merge request targeting the main F-Droid repo
- Ensure the MR description is clear and references the new version

## Step 7: Monitor CI Pipeline
- Check that all jobs pass (linting, metadata validation, etc.)
- If there are issues, review the error messages, fix the YAML, and re-push

## Step 8: Wait for Review and Merge
- Respond to any maintainer comments
- Once merged, your new version will be published on F-Droid

## Troubleshooting
- **Pipeline fails due to metadata:** Double-check YAML formatting, ensure only the latest build block is present, and that all required fields are updated.
- **Tag not found:** Make sure you pushed the tag to GitHub and it matches the `commit:` field in the YAML.
- **Other issues:** Review the pipeline logs for details and fix accordingly.

## Notes
- **Always keep only the latest version block in `com.fixupxer.yml`.**
- **Update only the version fields for each new release.**
- **Do not rewrite history or force-push unless instructed by a maintainer.**
- **Keep your changelogs up to date for user clarity.**

---

**This guide is version-agnostic and should be followed for all future FixupXer F-Droid updates.**  