# FixupXer – "Browser" Mode & Settings Feature Specification

Created: 2025-07-19
Maintainer: **TODO: add name**
Status: Draft → Authoritative once merged

---

## 0. Purpose

Add an **optional** "Browser" mode that lets FixupXer receive generic `http/https` VIEW-intents, clean the URL, then carry out a user-selected *post-clean* action.  Goals:

* Users can set FixupXer as their system default browser and have every web-link cleaned first.
* After cleaning, FixupXer can (in priority order)
  1. Launch the site’s **native app** if one exists and is allowed;
  2. Open the link in the user’s **regular browser**;
  3. Open Android’s share-sheet;
  4. Copy to clipboard.
* Behaviour is fully user-configurable in a new **Settings** screen that matches the existing Material 3 look-and-feel.
* All implementation must be **original work** → **do not copy code or comments** from any external project (e.g. Léon).

---

## 1. High-level Architecture

```
tap link → Android resolver
          ├─ specialised filter present & allowed → native app (URL not cleaned)
          └─ else → BrowserAlias (FixupXer)

FixupXer MainActivity
    ├─ cleans URL
    └─ PostCleanRunner              (new class)
         ├─ tries Action #1 (if configured)
         │    └─ success? return
         ├─ tries Action #2 …
         └─ … falls back to share sheet
```

### 1.1 Components to add or change

| File / Class | Change | Notes |
|--------------|--------|-------|
| **AndroidManifest.xml** | • add `<activity-alias>` named `BrowserAlias` (disabled by default).<br>• no additional Activity class required. | see § 4.1 |
| **MainActivity** | • detect VIEW intents from the alias.<br>• route to **PostCleanRunner** after cleaning. | |
| **PostCleanRunner (new)** | Implements the priority list algorithm. | see § 4.3 |
| **SettingsActivity / SettingsScreen (new)** | Material 3 Preference screen. | see § 3 |
| **Burger-menu** | Rearrange items & add “Settings” and “Instructions”. | see § 3.4 |
| **DisclaimerDialog** | Update wording if browser mode may leak information. | |
| **Docs** | This file (implementation spec) + user-facing instructions text. | |
| **connectedAndroidTest/** | Extend to test alias enabled/disabled & priority flow. | see § 6 |

---

## 2. Licensing & Clean-room Rule

* All code **MUST be original**.  No copy/paste from GPL projects or code you do not own.
* Use Android & Kotlin standard APIs only.
* Keep commit messages factual; do not refer to third-party source code.

---

## 3. New **Settings** UI

### 3.1 Navigation Entry

* A **Settings** list item is added to the burger menu (first position).
* Clicking it opens a standard `SettingsActivity` that hosts a single `SettingsScreen` composable.

### 3.2 Data persistence

* Use **Jetpack DataStore (Preferences)** – already used elsewhere in the app.
* New keys (defaults in parentheses):
  * `browser_enabled` (Boolean, `false`)
  * `action_mode` (Enum `Ask`, `Priority` – default `Ask`)
  * `action_priority` (List<Action>) – default `[OpenInBrowser, ShareMenu]`

### 3.3 Screen Layout (Material 3, no hard-coded px)

```
Settings
┌───────────────────────────────┐
│ Browser integration           │  (Switch)
│  ▶ Enable FixupXer as browser │
├───────────────────────────────┤
│ After-clean behaviour         │  (Radio-group)
│  • Ask every time             │
│  • Follow action order        │
│                               │
│ Action priority               │  (Re-orderable list, enabled only
│  ════════════════════════     │   if mode = Priority)
│  ☰ Open in native app         │
│  ☰ Open in browser            │
│  ☰ Share menu                 │
│  ☰ Copy to clipboard          │
└───────────────────────────────┘
```

*Use `Modifier.dragHandle()` (Compose 1.6+) for drag-and-drop.
*All paddings/spacings via `MaterialTheme.spacing` extension to avoid magic numbers.

### 3.4 Burger menu order (update NavDrawer implementation)

1. Settings
2. Instructions (new dialog)
3. Disclaimer
4. Report a Bug
5. Donate
6. About

### 3.5 Instructions dialog

* Re-use the `AlertDialog` pattern from **Disclaimer** dialog.
* Text goes into `strings.xml` so it can easily be localised.  Use one string resource per paragraph to respect the 4k-character limit in Android resources.
* **Full, ready-to-paste dialog text** (English):

```
<h3>1 What is Browser mode?</h3>

Browser mode turns FixupXer into an optional 
"filter browser". When it is enabled *and* you set
FixupXer as the system-wide default browser, every
web link (http or https) is routed to FixupXer first.
The app removes tracking parameters, unnecessary
redirects and other clutter, then performs the next
action you have configured.

<h3>2 How do I enable it?</h3>

• Open the three-line menu ▸ <b>Settings</b> ▸
  <b>Browser integration</b> and turn the switch on.<br/>
• Android will not switch browsers automatically.
  Now go to
  <b>System Settings ▸ Apps ▸ Default apps ▸ Browser</b>
  and pick <b>FixupXer</b>.

You can undo this any time by choosing a different
browser or by turning the switch back off.

<h3>3 Why do some links still open in the native app?</h3>

Many apps (YouTube, Instagram, maps, etc.) register
"specialised" link handlers. When those handlers are
allowed, Android sends the link directly to the app
and FixupXer never sees it. If you want FixupXer to
clean those links first:

1. Open <b>System Settings ▸ Apps ▸ &lt;App Name&gt;</b>.<br/>
2. Tap <b>Open by default</b> or <b>Set as default</b>.<br/>
3. Set <b>Open supported links</b> to <b>Don’t allow</b>
   (or <b>Ask every time</b> if you still want the choice).

Repeat for each app whose links you want to filter.

<h3>4 Ask vs Priority list</h3>

After a link is cleaned FixupXer can either:

• <b>Ask every time</b> — Android’s chooser appears and
  you decide where to send the clean link.<br/>
• <b>Follow priority list</b> — FixupXer looks at your
  ordered list of actions and executes the first one
  that works:

    1. Open in native app (only if such an app exists
       and is allowed)
    2. Open in browser
    3. Share menu
    4. Copy to clipboard

You can reorder the list under <b>Settings ▸ Action
priority</b>. Drag and drop items to match your
workflow.

Tip: If the top action cannot be performed (e.g., no
native app installed), FixupXer automatically moves
to the next item—no error dialogs, no extra taps.

<h3>5 Troubleshooting</h3>

• Link opens unchanged → Make sure FixupXer is still
  the default browser and the originating app is not
  forcing its own link handler.
• Loop opens FixupXer again → Remove FixupXer from
  the native-app position in the priority list.
• Want the original link?  Use the Share button in
  FixupXer and choose <i>Copy original URL</i>.

Enjoy faster, cleaner and more private links!
```

*Strings resource hint*: store each heading and paragraph as
`<string name="instructions_section1">…</string>` etc.  Newlines can be encoded with `\n` and headings wrapped with basic HTML tags which `TextView` will render when using
`android:textAppearance="…@style/TextAppearance.Material3.BodyMedium"` and `android:autolink="web"`.

*Accessibility*: ensure dialog is scrollable (`Modifier.verticalScroll`).

---

## 4. Core Implementation Details

### 4.1 Manifest Alias (no code duplication)

```xml
<activity-alias
    android:name="${applicationId}.BrowserAlias"
    android:targetActivity=".MainActivity"
    android:exported="true"
    android:enabled="false"> <!-- toggled by Settings -->
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data android:scheme="http" />
        <data android:scheme="https" />
    </intent-filter>
</activity-alias>
```

Toggling:
```kotlin
fun setBrowserAliasEnabled(enable: Boolean) {
    val pm = context.packageManager
    val cn = ComponentName(context, "${context.packageName}.BrowserAlias")
    val newState = if (enable) {
        PackageManager.COMPONENT_ENABLED_STATE_ENABLED
    } else {
        PackageManager.COMPONENT_ENABLED_STATE_DISABLED
    }
    pm.setComponentEnabledSetting(cn, newState, PackageManager.DONT_KILL_APP)
}
```

### 4.2 MainActivity – VIEW-intent handling

Add at top of `onCreate` and `onNewIntent`:
```kotlin
if (intent.action == Intent.ACTION_VIEW && intent.data != null) {
    handleViewIntent(intent)
    return
}
```

`handleViewIntent` flow:
1. Verify scheme == `http` or `https`.
2. Pass to existing `UrlCleaner` → obtain `cleanUri`.
3. Call `PostCleanRunner.run(cleanUri)`.
4. Finish `Activity` to keep task stack shallow.

### 4.3 `PostCleanRunner`

```kotlin
object PostCleanRunner {
    suspend fun run(uri: Uri) {
        when (Settings.actionMode()) {
            Ask -> showSystemChooser(uri)
            Priority -> runPriority(uri)
        }
    }

    private fun runPriority(uri: Uri) {
        for (action in Settings.priorityList()) {
            val handled = when (action) {
                NativeApp -> launchNativeApp(uri)
                Browser   -> launchBrowser(uri)
                ShareMenu -> { share(uri); true }
                Clipboard -> { copy(uri); true }
            }
            if (handled) return
        }
        share(uri) // final fallback
    }
}
```

#### 4.3.1 `launchNativeApp` heuristic
* Query `PackageManager.queryIntentActivities(viewIntent, MATCH_DEFAULT_ONLY)`.
* Filter out (a) FixupXer itself, (b) known browsers via intent-filter category `APP_BROWSER` (API 30+) or hard-coded package list fallback.
* If at least one candidate remains, set `intent.package` to first candidate and `startActivity`.
* Return `true` if started, else `false`.

Security note → see § 5.2.


---

## 5. Quality & Security Considerations

### 5.1 Robustness
* Loop-guard: If FixupXer is selected as *native* handler (edge case), skip it.
* Null checks for `intent.data`, catch `ActivityNotFoundException`.
* Drag-and-drop list persisted atomically; if corrupted, reset to defaults.

### 5.2 Security / Privacy
* The new alias increases the attack surface—URLs now reach FixupXer directly.  Mitigations:  
  * Keep **exported=true** but validate schemes; reject everything except `http/https` early.
  * Do not attempt to render content; FixupXer only processes strings.
* `launchNativeApp` must *not* allow arbitrary package spoofing; we only set `intent.package` to a value that we retrieved from `PackageManager` for that particular intent.
* Clipboard copy passes `ClipDescription.MIMETYPE_TEXT_URILIST` to prevent code execution in malicious apps expecting other mime types.


---

## 6. Testing

### 6.1 Unit tests (Robolectric)
* `PostCleanRunnerTest` – feed mocked priority lists and verify which intent is started.
* `SettingsRepositoryTest` – default values & migration path.

### 6.2 Connected tests (instrumented)
* **debug** + **release** variants (Gradle matrix).  New cases:
  1. Enable alias → assert `pm.getComponentEnabledSetting()` returns `ENABLED`.
  2. Launch VIEW intent with YouTube installed & allowed.
     * Expect FixupXer cleans, relaunches intent with `package == com.google.android.youtube`.
  3. Launch VIEW when YouTube disabled.
     * Expect final intent has no package (browser).
  4. Mode = Ask → expect system chooser (use `UiAutomator` to detect).


---

## 7. Documentation & Strings

* `strings.xml`: add keys for Settings titles, dialog texts, enum labels.
* `docs/user_guide_browser_mode.md` (new): expanded version of burger-menu *Instructions* text with screenshots.

---

## 8. Backwards Compatibility

* Alias disabled by default → existing users see no change until they opt in.
* DataStore migration: when new keys missing, use defaults.

---

## 9. Timeline / Milestones

1. **Week 1** – Scaffold SettingsActivity + DataStore keys.
2. **Week 2** – Implement Manifest alias & toggle logic.
3. **Week 3** – PostCleanRunner & native-app heuristic.
4. **Week 4** – UI polish, Instructions dialog, burger-menu reorder.
5. **Week 5** – Instrumented tests, docs, code-review.

### 9.1 Versioning

* Bump <code>versionCode</code> **by +1** (single increment) in <code>app/build.gradle.kts</code>.
* Set <code>versionName</code> to <strong>1.4.6</strong>.
* Update CHANGELOG entry to reflect the new version number and highlight the new Browser mode, Settings screen and instructions dialog.

---

## 10. Done criteria

* All new unit & connected tests pass on CI for `debug` and `release` builds.
* Manual QA: links from Gmail, WhatsApp, & QR scanner cleaned and opened per priority list.
* No lint / detekt violations; min-SDK unchanged.
* README & in-app Instructions updated.
* Play Store privacy section unchanged (no new data collection).

---

*End of spec.* 