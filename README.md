# Mathapelo Keyboard

A separate Android keyboard for Mathapelo, with a rose and plum theme. Its
application ID is `com.mathapelo.keyboard`, so it can be installed alongside
Serame Keyboard.

## Quick replacements

Typing `.id`, `.nu`, `.em`, or `.pas` inserts the saved value immediately. Tap
the ✦ key to open these replacements from fields that only allow numbers. The
values can be changed in the app settings. The `.pas` setting is masked there.

The **Copied** button beside Mathapelo opens a dashboard of copied text and
picture thumbnails. Tap an item to paste it; clear the history from the
dashboard or app settings. Picture clips up to 10 MB are kept on the device.

The repository does not contain Mathapelo's private replacement values. The
private GitHub Actions build receives them through the
`MATHAPELO_SHORTCUTS_B64` repository secret. A local build without that secret
still works; enter the values in the app settings after installing it.

## Build

Open the repository in Android Studio, or run `gradle assembleDebug` with
Android SDK 34 and JDK 17. GitHub Actions uploads a debug APK after each push.


## Keyboard 2.0 (feature branch)

- Four palette choices: Rose, Midnight, Lavender and Pearl
- On-device word prediction, conservative autocorrect, personal/learned dictionary
- Searchable clipboard and pinned clips in Settings; tap and long-press clipboard items on the keyboard
- Screenshots: **copy the image** in an app that supports copying images, or open Photos/Gallery, select the screenshot, tap **Share > Save to Mathapelo**. Alternatively go to Mathapelo Settings > **Import a screenshot**. This is necessary because Android does not automatically place every saved screenshot onto the clipboard.
- Images are stored privately on the device up to 10 MB each, with a maximum history of 30 entries. Direct image paste requires support from the target app; otherwise the image is put on the system clipboard for manual Paste.
- JVM prediction tests are run by GitHub Actions before the debug APK is built.

The personal dictionary has no network service and does not process passwords. You can delete its learned words from Settings.
