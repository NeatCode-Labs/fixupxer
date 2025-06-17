# FixupXer

<img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.png" width="100" alt="FixupXer Logo">

A privacy-focused Android app that cleans tracking parameters from URLs and improves social media link sharing.

## Screenshots

<p align="center">
  <a href="https://github.com/NeatCode-Labs/fixupxer-android-app/raw/main/screenshots/main_screen.png">
    <img src="https://github.com/NeatCode-Labs/fixupxer-android-app/raw/main/screenshots/main_screen_thumbnail.png" width="200" alt="Main Screen">
  </a>
  <a href="https://github.com/NeatCode-Labs/fixupxer-android-app/raw/main/screenshots/share_screen.png">
    <img src="https://github.com/NeatCode-Labs/fixupxer-android-app/raw/main/screenshots/share_screen_thumbnail.png" width="200" alt="Share Screen">
  </a>
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

## Credits

This app is inspired by and builds upon the great work of several projects:

1. [FxEmbed (fixupx.com)](https://github.com/FxEmbed/FxEmbed) - The original service for improving Twitter/X embeds
2. [ClearURLs](https://github.com/ClearURLs/Addon) - A browser extension that removes tracking elements from URLs
3. [InstaFix](https://github.com/Wikidepia/InstaFix) - A service that improves Instagram embeds in platforms like Discord and Telegram

## Important Notice Regarding kkinstagram.com

FixupXer uses the kkinstagram.com service as a proxy to enhance Instagram links. Please note that:

- kkinstagram.com is a third-party proxy service not operated by us
- This service relies on bypassing Instagram's restrictions and may break without warning if Instagram changes its backend or implements new bot-detection measures
- If you notice issues with Instagram link conversion, please report it using the "Report Bug" feature in the app
- The kkinstagram proxy is maintained by an unknown entity. If they wish to be credited here, feel free to contact me.

## Building from Source

### Prerequisites
- Android Studio Iguana (or newer)
- JDK 17
- Android SDK 35

### Build Steps

1. Clone the repository:
   ```
   git clone https://github.com/NeatCode-Labs/fixupxer-android-app.git
   ```

2. Open the project in Android Studio.

3. Build the project:
   ```
   ./gradlew assembleDebug
   ```

4. Install on your device:
   ```
   ./gradlew installDebug
   ```

## Privacy

FixupXer is designed with privacy in mind:
- No data collection or analytics
- No internet permission required
- All URL processing happens locally on your device
- Open source code for transparency

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Disclaimer

This software is provided "as is", without warranty of any kind. The app names "Twitter", "X", and "Instagram" are trademarks of their respective owners. This app is not affiliated with, endorsed by, or connected to these services.

---

[Made by NeatCode Labs](https://neatcodelabs.com/)
