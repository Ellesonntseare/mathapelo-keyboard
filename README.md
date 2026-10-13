# Mathapelo Keyboard

A separate Android keyboard for Mathapelo, with a rose and plum theme. Its
application ID is `com.mathapelo.keyboard`, so it can be installed alongside
Serame Keyboard.

## Quick replacements

Typing `.id`, `.nu`, `.em`, or `.pas` inserts the saved value immediately. Tap
the ✦ key to open these replacements from fields that only allow numbers. The
values can be changed in the app settings. The `.pas` setting is masked there.

The repository does not contain Mathapelo's private replacement values. The
private GitHub Actions build receives them through the
`MATHAPELO_SHORTCUTS_B64` repository secret. A local build without that secret
still works; enter the values in the app settings after installing it.

## Build

Open the repository in Android Studio, or run `gradle assembleDebug` with
Android SDK 34 and JDK 17. GitHub Actions uploads a debug APK after each push.
