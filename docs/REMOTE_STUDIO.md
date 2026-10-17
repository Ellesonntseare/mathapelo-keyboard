# Mathapelo Studio — Windows PC server + Android keyboard

This branch is based on \`feature/keyboard-refresh-v2\`. No modifications to \`main\` are required until a reviewed merge.

## Architecture

\`\`\`
Windows PC                             Android Phone
Node.js server (127.0.0.1:8787)       Mathapelo IME (Kotlin)
       |                                     |
 Local encrypted Tailscale network (Serve HTTPS)
       |--- GET /api/device/config <--------| Bearer device token
       |
Admin dashboard                            |
 localhost:8787                       Cache last good revision in app-private storage
 Save draft / Preview / Publish        Apply theme, toolbar, key height, motion,
 Rollback / Issue pair token           suggestions and optional dictionary words
\`\`\`

The phone **only sends a GET request with a device token**. It never sends keystrokes, typed words, learned vocabulary, clipboard contents, photos, screenshots, .id/.nu/.em/.pas values or other private user data to the PC. The server has no upload endpoint. It is impossible to publish executable Kotlin code through this configuration channel.

## On the Windows PC

1. Install **Node.js 20+** (LTS preferred) and Tailscale.
2. Download or clone this private branch from GitHub, keeping the repository private.
3. Double-click **\`control-server/start-windows.bat\`**. A generated admin password is printed the **first time** the server starts. Store it securely; use \`reset-password.bat\` if you lose it.
4. Open **http://127.0.0.1:8787** on the same PC, sign in with that password.
5. Sign into the **same Tailscale tailnet** on your Windows PC and Android phone. Enable HTTPS certificates in your Tailscale admin settings if prompted.
6. In Windows Terminal on the PC run: **\`tailscale serve --bg 8787\`**. **Do not use \`tailscale funnel\`**, which would expose the panel publicly.
7. Run \`tailscale serve status\` to obtain the **https://...ts.net** URL. Use **that HTTPS address**, not the PC's local IP, in the Android app.

The Node server is bound to **127.0.0.1 only**. The Tailscale connection provides private HTTPS access to the phone; no router port-forwarding is required. Keep Windows online with Node and Tailscale running. If Windows is off, Mathapelo continues using its last saved design.

For a new device: from the Control Panel, enter a phone name and click **Create token**. Copy the token to the phone. You can also double-click \`control-server/pair-phone.bat\`. Tokens are shown once and stored on the PC only as SHA-256 digests; keep the original token secret.

## On Android

1. Install a new APK built from this branch. Existing Android keyboard configuration stays local unless the installation signing certificate changes.
2. Open **Mathapelo Keyboard > Remote Studio**.
3. Paste the private Tailscale **HTTPS URL** into Server Address.
4. Paste the device's pairing token and tap **Connect and save**.
5. Use **Check for updates** to verify the connection.
6. Open a text field with Mathapelo enabled. The keyboard checks for a new revision when opened and about every **30 seconds while visible**. Changes apply without reinstalling another APK.

### Things you can publish without reinstalling

- Rose, Midnight, Lavender, Pearl and optional custom accent
- Key height and corner rounding
- Key press animations
- Three word suggestions and conservative autocorrect
- Number row
- Toolbar controls: clipboard, shortcuts, one-handed, settings
- Shared custom dictionary words (explicitly authored in the admin panel only)

The phone's locally learned words and personal text shortcuts remain private to that phone and are not editable from the server.

## Automatic screenshots (requested feature)

Go to **Mathapelo Keyboard > Automatic screenshots > Enable**, then approve **full Photos access**.

When Android saves a new screenshot to MediaStore, the opt-in observer attempts to copy its image into Mathapelo's private clipboard history immediately, while Android keeps the keyboard IME process running. The screenshot remains **on-device only**. To paste it, open Mathapelo's Clipboard panel, tap the screenshot image and use rich image insertion in supported apps. When the target app does not support keyboard-inserted images, Mathapelo copies the image to the Android clipboard for **long-press > Paste**.

**Limitation:** Android does not offer a global guaranteed third-party screenshot event with the image contents. The Android 14 screenshot callback works only while the detecting app's activity is visible, and it does not return screenshot pixels. Android may stop background processes or limit Photos access. Automatic detection cannot be guaranteed when the keyboard process is not running, or when the phone only granted selected-photo access. Screenshot folders and vendor behavior vary. **Share > Save to Mathapelo** and **Import screenshot** remain reliable alternatives.

Automatic screenshots is **off by default**. The phone never sends these images to the PC.

## Safety / recovery

- Admin dashboard requires a random, first-run password with a salted scrypt hash.
- Device API requires an independent random 256-bit Bearer token, hashed at rest.
- Cross-site script access is denied; config schema enforces a strict allowlist and bounds.
- Node server has no external dependencies and listens on localhost only.
- Changes are drafts until **Publish**; last 20 published versions can be restored by publishing them again.
- Android only accepts HTTPS, never redirects, and caches the last working config offline.
- Never commit the \`control-server/data/\` directory, screenshots, keystrokes or pairing tokens.
- To stop remote access, run \`tailscale serve off\`, stop the Node server, or use **Disconnect remote configuration** in the Android app.
- To replace an existing Android installation **without losing its data**, the updated APK must be signed with the same Android signing certificate and have a higher versionCode. GitHub's default debug APK uses an ephemeral runner debug signing key, so it might not install over an older debug APK. Use a stable private signing key for persistent installs; keep the private keystore out of Git.

## Validation

- Windows server: \`cd control-server && node --test tests/*.test.js\` (no npm install required)
- Android: \`gradle testDebugUnitTest assembleDebug\` (JDK 17 and Android SDK 34)
- GitHub Actions runs both server tests and Android tests/build on every push.
- Still test screenshot imports and UI animations on an actual Android phone; CI cannot validate OEM MediaStore events or WhatsApp's clipboard behavior.
