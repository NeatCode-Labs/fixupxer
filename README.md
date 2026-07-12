# FixupXer - Your Privacy-First URL Cleaner

<div align="center">
<img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.png" width="120" alt="FixupXer Logo">

**Clean URLs • Get embeddable links • Use Browser Mode**

[![Version](https://img.shields.io/badge/version-2.1.0-blue?style=for-the-badge)](https://github.com/NeatCode-Labs/fixupxer/releases)
[![Android](https://img.shields.io/badge/Android-5.0+-green?style=for-the-badge&logo=android)](https://developer.android.com/about/versions/lollipop)
[![License](https://img.shields.io/badge/license-GPL--3.0--or--later-green?style=for-the-badge)](LICENSE)

[![Google Play](https://img.shields.io/badge/Google%20Play-Download-green?style=for-the-badge&logo=google-play)](https://play.google.com/store/apps/details?id=com.fixupxer)
[![F-Droid](https://img.shields.io/badge/F--Droid-Download-blue?style=for-the-badge&logo=f-droid)](https://f-droid.org/packages/com.fixupxer/)

<a href="https://apt.izzysoft.de/packages/com.fixupxer">
  <img src="https://gitlab.com/IzzyOnDroid/repo/-/raw/master/assets/IzzyOnDroidButton_nofont.svg" alt="Get it on IzzyOnDroid" width="150"/>
</a>

</div>

## 🎯 What is FixupXer?

FixupXer is a free Android app that makes your shared links cleaner, safer, and more private. When you share links from social media or shopping sites, they often contain hidden tracking codes that follow you around the internet. FixupXer removes these trackers and makes your links work better.

### 🌟 Key Benefits:
- **🔒 Protects Your Privacy** - Removes 1000+ different tracking codes from your links
- **⚡ Lightning Fast** - Cleans any URL in less than 0.1 seconds
- **📱 Works Everywhere** - Supports 25+ popular websites including Facebook, Amazon, YouTube, and more
- **🎯 100% Offline** - No internet needed, your links never leave your phone
- **📊 History Tracking** - Keep track of all the links you've cleaned (optional)
- **🌗 Modern Design** - Material 3 before/after flow interface with full dark mode and a System/Light/Dark theme picker (new in v2.0.0)

## 📸 Screenshots

<p align="center">
  <img src="./screenshots/main_empty.png" width="200" alt="Main Screen">
  <img src="./screenshots/main_filled.png" width="200" alt="Processing URL">
  <img src="./screenshots/share_filled.png" width="200" alt="Share Screen">
  <img src="./screenshots/main_dark.png" width="200" alt="Dark Mode">
  <img src="./screenshots/history.png" width="200" alt="History">
  <img src="./screenshots/about.png" width="200" alt="About Dialog">
  <img src="./screenshots/infographics.png" width="200" alt="Features Infographic">
</p>

## 🚀 How to Use FixupXer

### Method 1: Share from Any App
1. **Long-press any link** in any app
2. **Tap "Share"** from the menu
3. **Choose FixupXer** from the share list
4. Your link is cleaned automatically!
5. Share, copy, or open the clean version

### Method 2: Direct Link Cleaning
1. **Copy any link** from your browser or any app
2. **Open FixupXer** 
3. **Tap "Paste"** - your link appears automatically
4. **Tap "Process"** - tracking is removed instantly
5. **Choose what to do:**
   - 📋 **Copy** - Save the clean link to clipboard
   - 🔗 **Share** - Send via WhatsApp, Email, etc.
   - 🌐 **Open** - View in your browser

### Method 3: Browser Mode (Advanced) 🌐

Browser mode turns FixupXer into an optional "filter browser". When enabled and set as the system-wide default browser, every web link (http or https) is routed to FixupXer first for cleaning.

See the **[Browser Mode Guide](docs/BROWSER_MODE_GUIDE.md)** for setup,
native-app handling, action priority, conversion defaults, and troubleshooting.

### 📜 History Feature
- **View Past Links** - Tap the History button to see all your cleaned links
- **Quick Actions** - Copy or share any previous link with one tap
- **Delete Entries** - Long-press any entry to delete it
- **Privacy Control** - Turn history on/off in settings
- **Auto-Cleanup** - Set maximum entries to keep (default: 100)

## 🛡️ What FixupXer Cleans

### Social Media
- **Facebook** (119 trackers) - Plus converts to facebookez.com for better sharing
- **Instagram** (67 trackers) - Plus converts to a user-selectable proxy (toinstagram.com, adamlikes.men, instagram7.com, kkinstagram.com, or your own custom proxy) for embeddable links
- **Twitter/X** (99 trackers) - Plus converts to fixupx.com for better embeds
- **TikTok** (124 trackers) - Plus converts to a user-selectable proxy (tnktok.com, tfxktok.com, tiktokez.com, kktiktok.com, or your own custom proxy) for embeddable links
- **LinkedIn** (117 trackers) - Cleans job posts and profile links
- **Reddit** (91 trackers) - Removes tracking from posts and comments

### Shopping Sites
- **Amazon** (147 trackers) - Removes affiliate codes and tracking
- **AliExpress** (100+ trackers) - Cleans product links
- **eBay** - Removes session tracking

### Video & Content
- **YouTube** (139 trackers) - Removes tracking while keeping timestamps
- **Substack** (87 trackers) - Cleans newsletter links

### Search Engines
- **Google Search** (140 trackers) - Extracts actual destination URLs

### ✨ Special Features
- **Universal Tracking Removal** - Works on ANY website, not just the ones listed
- **Smart Detection** - Automatically identifies and removes new tracking methods
- **Link Enhancement** - Some platforms get special treatment for better sharing

## 🎨 Toggle Options Explained

When you see the "Embed?" toggle:

- **ON** ✅ - Converts social media links for better preview/embedding:
  - Facebook → facebookez.com (better previews)
  - Instagram → configurable proxy (toinstagram.com / adamlikes.men / instagram7.com / kkinstagram.com / custom)
  - Twitter/X → fixupx.com (better embeds)
  - TikTok → configurable proxy (tnktok.com / tfxktok.com / tiktokez.com / kktiktok.com / custom)
  
- **OFF** ❌ - Only removes tracking, keeps original domain

### Instagram embed proxy selection

Instagram proxies occasionally go offline. To keep embeds working, FixupXer lets you pick the active proxy:

**Primary** (rich embed — media + post / reel title and description)
- **toinstagram.com** *(default)*
- **adamlikes.men**

**Backup** (media only, no title/description)
- **instagram7.com**
- **kkinstagram.com**

**Custom** (yours)
- Tap **Add custom proxy…** at the bottom of the chooser to add any Instagram embed proxy domain you like. Custom entries get a delete icon in the list and can be removed at any time; deleting the one you had selected falls back to the default. The domain is used as-is for the link swap — the app never contacts it.

When you share or paste an Instagram link and the **Embed?** toggle is visible, a small label on the right shows *Active: &lt;proxy&gt;. Change.* — tap **Change.** to pick another proxy. Your choice persists across both screens (Main and Share).

Tapping **Change.** opens the proxy chooser as an inline dialog on both the Main and Share screens, so the flow is never interrupted. The dialog includes a small **(i)** info icon that explains the Primary / Backup / Custom distinction. If you already processed an Instagram link, picking a different proxy refreshes the result automatically.

Converted links are sent without a `www.` prefix — these proxies render best at the bare hostname. Pasting an old-style URL on the retired proxy `eeinstagram.com` still works: FixupXer recognises it and converts it to your currently selected proxy.

### TikTok embed proxy selection

TikTok gets the same treatment (new in v1.7.0). When a TikTok link is detected, its own **Embed?** toggle appears with an *Active: &lt;proxy&gt;. Change.* label:

**Primary** (embed videos *and* multi-image slideshows, with like/comment/share counts)
- **tnktok.com** *(default — fxTikTok, open source)*
- **tfxktok.com** *(FxTikTok)*

**Backup**
- **tiktokez.com** *(EmbedEZ — embeds media like the primaries)*
- **kktiktok.com** *(kkScript — video only, no slideshows or stats)*

**Custom** (yours)
- The chooser has the same **Add custom proxy…** row as the Instagram one, with identical validation. The domain is used as-is for the link swap — the app never contacts it.

Unlike Instagram, TikTok conversions **keep the host prefix**: `vm.tiktok.com/…` becomes `vm.tnktok.com/…` and `www.tiktok.com/…` becomes `www.tnktok.com/…`, because TikTok short links live on subdomains and the proxies mirror them. Links on the dead services `vxtiktok.com` (shut down 11/2025) and `tiktxk.com` are still recognised and auto-converted to your selected proxy.

## ⚙️ Settings & Options

### Browser Integration
- **Enable/Disable Browser Mode** - Set FixupXer as your default browser
- **Action Priority Configuration** - Reorder post-clean actions by dragging
- **[How to Use](docs/BROWSER_MODE_GUIDE.md)** - Detailed Browser mode setup and troubleshooting

### Custom URL rules
- Opt-in by design: custom rules stay off until you enable them in Settings
- Create ordered offline rules for all URLs, one host, a domain plus subdomains, host groups, or an RE2 URL pattern
- Remove all parameters, remove selected parameters, keep only selected parameters, replace URL text, extract redirect targets, or apply component templates
- Choose before-cleaning, after-cleaning, or after-conversion phases and Main, Share, or Browser contexts
- Add excludes, stop-after-match, live Test Lab previews, bundled templates, drag reorder, duplicate, versioned JSON import/export, and import rollback
- Rules stay in the local Room database and are exported only when you explicitly choose a destination through Android's file picker
- **[How to Use](docs/CUSTOM_URL_RULES_GUIDE.md)** - Complete scope, action, ordering, Test Lab, and portability guide

### History Settings
- **Enable/Disable** - Toggle history tracking on or off
- **Max Entries** - Choose how many links to keep (any number, default: 100)
- **Clear All** - Delete entire history with one button

### Menu Options
- **Settings** - Configure browser mode and action priorities
- **About** - App version and information
- **Report Bug** - Send feedback to developers
- **Disclaimer** - Privacy and usage information
- **Donate** - Support development (optional)

## 🔒 Privacy & Security

### Your Privacy is Protected
- ✅ **100% Offline** - No internet permission, links stay on your device
- ✅ **No Data Collection** - We don't track or store anything
- ✅ **No Analytics** - No Google Analytics or any tracking
- ✅ **Open Source** - Anyone can verify our code
- ✅ **Local History** - History is stored only on your phone

### Security Features
- 🛡️ Protects against malicious URLs
- 🛡️ Validates all input for safety
- 🛡️ Handles international characters properly
- 🛡️ Removes hidden tracking characters

## 📲 Installation

### Option 1: Google Play Store
1. Visit the [Google Play Store](https://play.google.com/store/apps/details?id=com.fixupxer)
2. Tap "Install" 
3. The app will download and install automatically
4. Open and enjoy!

### Option 2: F-Droid
1. Visit [F-Droid](https://f-droid.org/packages/com.fixupxer/)
2. Tap "Download APK" or add the F-Droid repository
3. Install through F-Droid app or directly
4. Open and enjoy!

### Option 3: Download APK
1. Go to [GitHub Releases](https://github.com/NeatCode-Labs/fixupxer/releases)
2. Download the latest `.apk` file
3. Open the file on your Android device
4. Allow installation from unknown sources if prompted
5. Install and enjoy!

## ❓ Frequently Asked Questions

**Q: Does FixupXer need internet?**  
A: No! It works 100% offline. Your links never leave your device.

**Q: What's the difference between cleaning and converting?**  
A: Cleaning removes tracking codes. Converting changes the domain for better functionality (like Twitter → fixupx).

**Q: Can I turn off history?**  
A: Yes! Tap History → Toggle off "History Enabled" at the top.

**Q: Why do some sites get converted to different domains?**  
A: Some third-party services provide better link previews and privacy. You can turn this off with the toggle.

**Q: Is my data safe?**  
A: Absolutely! FixupXer has no internet permission and can't send data anywhere.

## 🛠️ Technical Details

<details>
<summary>Click to expand technical information</summary>

### Requirements
- Android 5.0 (API 21) or higher
- Works on phones and tablets
- Supports Android 15 (API 35)
- Material Design 3 interface

### Architecture
- Modular cleaner system with 11 specialized modules
- O(1) domain lookup performance
- LRU cache with 1-hour TTL
- 467 automated tests (273 unit + 194 instrumentation)
- Thread-safe, stateless design

### Building from Source
1. Prerequisites:
   - Android Studio Ladybug or newer
   - JDK 17
   - Android SDK 35

2. Clone and build:
   ```bash
   git clone https://github.com/NeatCode-Labs/fixupxer.git
   cd fixupxer
   ./gradlew assembleDebug
   ```

3. For release builds:
   - Create keystore using `keystore.properties.template`
   - Run `./gradlew assembleRelease`

</details>

## ⚠️ Important Disclaimers

### Third-Party Services
The following link conversion services are **not operated by NeatCode Labs**:
- **facebookez.com** - Facebook link enhancement
- **fixupx.com** - Twitter/X link enhancement  
- **toinstagram.com** / **adamlikes.men** / **instagram7.com** / **kkinstagram.com** - Instagram link enhancement (user-selectable; `eeinstagram.com` from earlier versions is still recognised in pasted URLs and auto-converted to the active proxy). Any **custom proxy** you add yourself is likewise a third-party service.
- **tnktok.com** / **tfxktok.com** / **tiktokez.com** / **kktiktok.com** - TikTok link enhancement (user-selectable; the dead services `vxtiktok.com` and `tiktxk.com` are still recognised in pasted URLs and auto-converted to the active proxy). Any **custom proxy** you add yourself is likewise a third-party service.

These services may stop working at any time. We have no control over them.

### Trademarks
All company and product names (Facebook, Twitter, Instagram, etc.) are trademarks™ or registered® trademarks of their respective holders. Use of them does not imply any affiliation or endorsement.

### Warranty

## 📄 License

FixupXer is licensed under the **GNU General Public License v3.0 or later (GPL-3.0-or-later)**.

### Retroactive Licensing

All versions of FixupXer, including historical commits prior to this GPL migration, are hereby relicensed under the terms of the GNU General Public License version 3 or any later version at your option. This supersedes any previous license notices.

### License Summary

- ✅ **Commercial use** - You can use this software for commercial purposes
- ✅ **Modification** - You can modify the source code  
- ✅ **Distribution** - You can distribute the software
- ✅ **Patent use** - You can use any patents related to this software
- ✅ **Private use** - You can use this software privately
- ❗ **Disclose source** - If you distribute this software, you must include the source code
- ❗ **License and copyright notice** - Include the license and copyright notice
- ❗ **Same license** - Derivatives must use the same license
- ❌ **Liability** - No warranty or liability is provided
- ❌ **Warranty** - No warranty is provided

For the complete license terms, see the [LICENSE](LICENSE) file or visit <https://www.gnu.org/licenses/gpl-3.0.html>.

---
This software is provided "as is" without warranty of any kind.

## 🙏 Credits & Inspiration

FixupXer builds upon ideas from these excellent projects:

- [ClearURLs](https://github.com/ClearURLs/Addon) - Tracking parameter database
- [Leon URL Cleaner](https://github.com/svenjacobs/leon) - Android URL cleaning concept
- [FxEmbed](https://github.com/FxEmbed/FxEmbed) - Twitter embed improvements
- [InstaFix](https://github.com/Wikidepia/InstaFix) - Instagram embed enhancements
- [fxTikTok](https://github.com/okdargy/fxTikTok) - TikTok embed enhancements
- [RE2/J](https://github.com/google/re2j) by Google and RE2/Go contributors - the linear-time regular-expression engine used for user-authored URL rules; version 1.8 is used unmodified under its upstream Go License (`LICENSES/LicenseRef-RE2J.txt`)

Special thanks to the maintainers of facebookez.com, toinstagram.com, adamlikes.men, instagram7.com, kkinstagram.com, tnktok.com, tfxktok.com, tiktokez.com, and kktiktok.com. Thanks to [@gautamnabin5](https://github.com/gautamnabin5) for proposing TikTok conversion support in [PR #5](https://github.com/NeatCode-Labs/fixupxer/pull/5). Contact us if you'd like attribution!

The custom-rule engine and bundled templates were independently authored for FixupXer. No third-party cleaner ruleset, parser, regex corpus, or proprietary code was copied.

## 💖 Support Development

FixupXer is free and open source. If you find it useful:

- ⭐ **Star this repository**
- 🐛 **Report bugs** via the app menu
- 💬 **Share with friends** who value privacy
- ☕ **[Buy us a coffee](https://ko-fi.com/neatcodelabs)** (optional)

---

<div align="center">

**Created with ❤️ by [NeatCode Labs](https://neatcodelabs.com)**  
*Making the internet a cleaner place, one URL at a time*

[![Website](https://img.shields.io/badge/Website-neatcodelabs.com-blue?style=for-the-badge)](https://neatcodelabs.com)
[![Ko-fi](https://img.shields.io/badge/Ko--fi-Support%20Us-ff5e5b?style=for-the-badge&logo=ko-fi)](https://ko-fi.com/neatcodelabs)

</div>

