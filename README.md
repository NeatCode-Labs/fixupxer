# FixupXer - URL Enhancer

<img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.png" width="100" alt="FixupXer Logo">

FixupXer is a privacy-focused Android app that cleans tracking parameters from URLs and improves social media link sharing.

## Screenshots

<p align="center">
  <img src="https://github.com/NeatCode-Labs/fixupxer/blob/main/screenshots/main_screen_thumbnail.jpg" width="200" alt="Main Screen">
  <img src="https://github.com/NeatCode-Labs/fixupxer/blob/main/screenshots/mainscreen_filled_thumbnail.jpg" width="200" alt="Main Screen Filled">
  <img src="https://github.com/NeatCode-Labs/fixupxer/blob/main/screenshots/share_screen_thumbnail.jpg" width="200" alt="Share Screen">
  <img src="https://github.com/NeatCode-Labs/fixupxer/blob/main/screenshots/sharescreen_instagram_thumbnail.jpg" width="200" alt="Share Screen Instagram">
</p>



## Features

- **Clean Tracking Parameters**: Removes common tracking parameters from URLs (like UTM parameters, fbclid, etc.)
- **Improved Twitter/X Embeds**: Converts Twitter/X URLs to fixupx.com format for better embeds
- **Instagram Enhancement**: Converts Instagram URLs to kkinstagram.com for better viewing and privacy
- **URL Sharing**: Share cleaned and improved URLs directly from the app
- **Clipboard Support**: Copy processed URLs to clipboard with one tap
- **URL Processing**: Process URLs directly or via Android's share menu

## How It Works

FixupXer functions in two main ways:

1. **As a Standalone App**:
   - Paste a URL
   - Process it with one tap
   - Share, open, or copy the cleaned URL

2. **Via Android's Share Menu**:
   - Share a URL from any app
   - Select FixupXer from the share menu
   - The URL is automatically processed
   - Choose to share, open, or copy the processed URL

## Disclaimer

**Privacy**: This app processes URLs locally on your device and does not collect, store, or transmit any user data or URLs to external servers.

**Third-Party Services**: FixupXer relies on third-party proxy services for URL conversion:
- **fixupx.com** - For Twitter/X link conversion
- **kkinstagram.com** - For Instagram link conversion

These services are **not operated by NeatCode Labs** and may stop working at any time without notice. We have no control over their availability or functionality.

**Trademarks**: Names such as "Twitter", "X", "Instagram" and others are trademarks of their respective owners. This app is not affiliated with, endorsed by, or connected to these services.

**Warranty**: This software is provided "as is", without warranty of any kind.

**Note to kkinstagram.com maintainer**: If you wish to be credited in this README, please contact us via the contact form on our [website](https://neatcodelabs.com/) or use the "Report a Bug" link in the app.

## Credits

This app is inspired by and builds upon the great work of several projects:

1. [FxEmbed (fixupx.com)](https://github.com/FxEmbed/FxEmbed) - The original service for improving Twitter/X embeds
2. [ClearURLs](https://github.com/ClearURLs/Addon) - A browser extension that removes tracking elements from URLs
3. [InstaFix](https://github.com/Wikidepia/InstaFix) - A service that improves Instagram embeds in platforms like Discord and Telegram
4. [Leon - URL Cleaner](https://github.com/leon-cleaning-services/leon) - Android app for removing tracking parameters from shared URLs


## Building from Source

### Prerequisites
- Android Studio Iguana (or newer)
- JDK 17
- Android SDK 35

### Build Steps

1. Clone the repository:
   ```
   git clone https://github.com/NeatCode-Labs/fixupxer.git
   ```

2. Configure the bug report email:
   - Edit `app/src/main/res/values/strings.xml`
   - Replace `YOUR_EMAIL@EXAMPLE.COM` with your actual email address

3. Open the project in Android Studio.

4. Build the project:
   ```
   ./gradlew assembleDebug
   ```

5. Install on your device:
   ```
   ./gradlew installDebug
   ```

### Building Release APK/AAB

To build a signed release:

1. Create your keystore and configure `keystore.properties` (see `keystore.properties.template`)
2. Run:
   ```
   ./gradlew assembleRelease  # For APK
   ./gradlew bundleRelease    # For AAB
   ```

## Privacy

FixupXer is designed with privacy in mind:
- No data collection or analytics
- No internet permission required
- All URL processing happens locally on your device
- Open source code for transparency

## License

This project is licensed under the MIT License - see the LICENSE file for details.

---

<div align="center">

**Created with ❤️ by [NeatCode Labs](https://neatcodelabs.com)**  
Visit us for more useful tools and projects!

[![Website](https://img.shields.io/badge/Website-neatcodelabs.com-blue?style=for-the-badge)](https://neatcodelabs.com)
[![Ko-fi](https://img.shields.io/badge/Ko--fi-Support%20Us-ff5e5b?style=for-the-badge&logo=ko-fi)](https://ko-fi.com/neatcodelabs)

</div>
