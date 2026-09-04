# SMS Relay

[![Android verification](https://github.com/atiarmridul/TelcoOtpRelay/actions/workflows/android.yml/badge.svg?branch=master)](https://github.com/atiarmridul/TelcoOtpRelay/actions/workflows/android.yml?query=branch%3Amaster)
[![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-AGP%20built--in-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Gradle](https://img.shields.io/badge/Gradle-Wrapper-02303A?logo=gradle&logoColor=white)](https://gradle.org/)
[![Java](https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/)

A lightweight Android utility that forwards SMS messages from a dedicated phone to a Google Chat
space. Users can relay every incoming SMS or restrict forwarding to likely OTP messages.

## What This App Does

When an SMS arrives, the app:

1. Receives the Android `SMS_RECEIVED` broadcast.
2. Reconstructs the complete body of multipart messages.
3. Applies the optional sender allow-list; a blank list accepts every sender.
4. Applies the saved relay mode: all incoming messages, or OTP messages only.
5. Resolves the receiving SIM number when Android and the carrier expose it.
6. Formats the receiving number, sender, timestamp, and original body.
7. Sends the message to a configured Google Chat webhook.

In OTP-only mode, the parser acts only as a filter. In both modes, the full original SMS body is
forwarded unchanged.

## Quick Start

Clone the repository:

```bash
git clone https://github.com/atiarmridul/TelcoOtpRelay.git
cd TelcoOtpRelay
```

Build the debug APK:

```bash
./gradlew assembleDebug
```

Install it on a connected Android device:

```bash
adb install -r "app/build/outputs/distribution/SMS Relay.apk"
```

The debug build also produces the shareable artifact
`app/build/outputs/distribution/SMS Relay.apk`.

A prebuilt debug APK from the checked-in source is available at
[`releases/SMS Relay.apk`](releases/SMS%20Relay.apk) for installation on the dedicated QA phone. It
uses the Android debug signing key and is not a production release artifact; build from source when
independent verification is required.

Open the app, grant the requested permissions, enter a Google Chat webhook URL, optionally enter
one or more allowed senders separated by commas, choose **OTP messages only** or **All incoming
messages** from the dropdown, and tap **Save configuration**. Leave the sender field blank to relay
messages from every sender.

## Prerequisites

- Android Studio with Android SDK 35
- JDK 17 or newer to start Gradle; the checked-in daemon criteria uses JDK 21
- Android 8.0 (API 26) or newer device with SMS capability
- USB debugging and `adb` for command-line installation
- A Google Chat incoming webhook URL

Check the local toolchain:

```bash
java -version
adb devices
./gradlew --version
```

The Foojay resolver in `settings.gradle.kts` supplies cross-platform JDK download locations for
Gradle's daemon criteria. If the criteria need to be regenerated, run `./gradlew updateDaemonJvm`.
This daemon setting does not change the app's Java/Kotlin JVM target of 17.

## Project Structure

```text
.
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/mridul/telcootprelay/
│       │   ├── AboutPrivacyActivity.kt
│       │   ├── ChatSender.kt
│       │   ├── LastRelayStatus.kt
│       │   ├── MainActivity.kt
│       │   ├── OtpParser.kt
│       │   ├── RelayMode.kt
│       │   ├── SmsFormatter.kt
│       │   └── SmsReceiver.kt
│       └── res/
├── gradle/wrapper/
├── releases/
│   └── SMS Relay.apk
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

## Architecture

The app uses a small event-driven pipeline:

```text
Android SMS broadcast
        |
        v
   SmsReceiver
        |
        +--> SenderFilter
        +--> RelayMode --> OtpParser when required
        +--> SimNumberResolver
        +--> SmsFormatter
        +--> ChatSender --> Google Chat webhook
```

| Component           | Responsibility                                                            |
|---------------------|---------------------------------------------------------------------------|
| `MainActivity`      | Requests permissions and stores webhook, sender, and relay-mode configuration. |
| `AboutPrivacyActivity` | Explains storage, forwarding, permissions, and user controls.          |
| `LastRelayStatus`   | Stores and restores the most recent result, sender, and timestamp.         |
| `SmsReceiver`       | Coordinates SMS reconstruction, filtering, formatting, and delivery.      |
| `RelayMode`         | Restores the saved mode and decides whether an SMS should be forwarded.   |
| `SenderFilter`      | Parses the optional sender allow-list and matches originating addresses.  |
| `OtpParser`         | Determines whether an SMS looks like an OTP message.                      |
| `SimNumberResolver` | Reads the receiving SIM number when the platform exposes it.              |
| `SmsFormatter`      | Produces the Google Chat message text.                                    |
| `ChatSender`        | Posts JSON to the webhook with bounded retries and timeouts.              |

## Documentation

| Document | Purpose |
| --- | --- |
| [Architecture](docs/ARCHITECTURE.md) | Runtime flow, component boundaries, threading, and design decisions. |
| [Project context](docs/PROJECT_CONTEXT.md) | Product goals, scope, terminology, constraints, and success criteria. |
| [Decision log](docs/DECISIONS.md) | Important architectural choices and their tradeoffs. |
| [Testing](docs/TESTING.md) | Automated verification commands, manual scenarios, and coverage gaps. |
| [Privacy](docs/PRIVACY.md) | Data handling, permissions, storage, transmission, and user controls. |
| [Contributing](CONTRIBUTING.md) | Development workflow, quality checks, and coding standards. |
| [Security](SECURITY.md) | Vulnerability reporting and guidance for handling sensitive data. |
| [Agent guidance](AGENTS.md) | Canonical repository instructions for coding agents. |

## Google Chat Message Format

```text
Receiving number: 01XXXXXXXXX
Sender: 21209
Timestamp: 27 Aug 2026, 07:39:56 PM
Body: <#> Your verification code is 123456
```

If the device or carrier does not expose its own phone number, the receiving number is shown as `Unknown`.

## Relay Modes

| Mode                | Behavior                                                                                                    |
|---------------------|-------------------------------------------------------------------------------------------------------------|
| `OTP messages only`     | Forwards messages accepted by the OTP regex filter. This is the default for new and upgraded installations. |
| `All incoming messages` | Forwards every non-empty incoming SMS body without applying the OTP filter.                                 |

The compact configuration card identifies Google Chat as the destination provider. The webhook is
masked by default and has an explicit Show/Hide control. The relay-mode dropdown shows contextual
guidance and a dedicated privacy warning for **All incoming messages**. The header badge reports
`Setup`, `Active`, `Unsaved`, `Invalid`, `Testing`, `Connected`, or `Failed` as configuration and test
state changes. Editing the webhook, sender list, or mode does not affect active reception until **Save
configuration** is tapped; when the form matches persisted settings, that action becomes a dimmed,
disabled **Saved** button. **Test connection** remains a separate secondary action. Both actions use
equal-width 48dp controls with complete rounded backgrounds and clearance from the card edge.

When relay metadata exists, a separate card shows only the latest delivered/failed result, sender,
and SMS timestamp. It never displays or stores the SMS body or OTP. The full standard-phone layout
is intentionally compact, with scrolling retained for smaller windows and accessibility text. The
in-app **About & Privacy** card opens detailed data-handling information.

## Permissions

| Permission | Purpose |
| --- | --- |
| `RECEIVE_SMS` | Detect incoming SMS messages. |
| `READ_PHONE_NUMBERS` | Resolve the receiving SIM number when available. |
| `INTERNET` | Send qualifying messages to Google Chat. |

The app does not request contacts, camera, microphone, location, storage, notification, or default-SMS-app access.

## Build and Verification

Build the application:

```bash
./gradlew assembleDebug
```

The named output is `app/build/outputs/distribution/SMS Relay.apk`. When intentionally publishing
the checked-in debug APK, copy that exact artifact and verify that both files have the same checksum:

```bash
cp "app/build/outputs/distribution/SMS Relay.apk" "releases/SMS Relay.apk"
shasum -a 256 "app/build/outputs/distribution/SMS Relay.apk" "releases/SMS Relay.apk"
```

Run the Gradle verification lifecycle:

```bash
./gradlew check
```

Run connected Android tests when a device or emulator is available:

```bash
./gradlew connectedDebugAndroidTest
```

The project includes JVM unit tests for relay-mode selection, OTP parsing, message formatting, and
last-relay status parsing and display formatting.
Instrumentation coverage has not yet been added, so the connected test task currently validates the
Android test package without executing device test cases.

## Battery-Efficient Design

- No foreground or always-running service
- No polling, periodic timer, or alarm
- No wake lock or network heartbeat
- No GPS or location access
- No SMS database or history scan
- One shared background executor
- Short HTTP connection and read timeouts
- At most three webhook attempts per qualifying SMS
- Network connections closed after every attempt

## Important Limitations

- Android and mobile carriers do not always expose the SIM's own phone number.
- OTP detection is regex-based and can produce false positives or miss unusual OTP formats.
- Messages are delivered on a best-effort basis; there is no persistent retry queue.
- Delivery retries can produce duplicate Google Chat messages if a response is lost.
- Every qualifying message is forwarded; there is no duplicate suppression.
- The app handles SMS only. It does not process MMS messages.

## Security and Privacy

- Use this app only on an authorized, dedicated QA device.
- Treat the Google Chat webhook URL as a credential and do not commit it to Git.
- The webhook, relay mode, optional sender list, and latest relay result metadata are stored locally in the app's private
  preferences. SMS bodies and OTPs are not retained locally.
- SMS content is sent according to the saved relay mode. **All incoming messages** mode forwards
  non-OTP messages too.
- Application backup is disabled in the Android manifest.

## Troubleshooting

### SMS messages are not detected

- Open the app and grant the SMS permission.
- Confirm that the device supports telephony and can receive SMS messages.
- Check the app's permissions in Android system settings.

### Receiving number shows `Unknown`

- Grant the phone-number permission.
- Confirm that the active SIM is available to Android.
- Some carriers do not store or expose the phone number; `Unknown` is expected in that case.

### Google Chat receives nothing

- Confirm that the saved URL starts with `https://chat.googleapis.com/`.
- Verify that the phone has internet access.
- In OTP-only mode, confirm that the incoming message matches the OTP filter.
- Create a new webhook if the existing webhook has been revoked.

### Gradle build fails

- Confirm that JDK 17 and Android SDK 35 are installed.
- Open the project in Android Studio and allow Gradle synchronization to complete.
- Verify the SDK path in the local, untracked `local.properties` file.

## Tech Stack

- Kotlin
- Android SDK
- AndroidX Core and AppCompat
- Gradle Kotlin DSL
- `BroadcastReceiver` for SMS events
- `HttpURLConnection` for Google Chat delivery
- `SharedPreferences` for local configuration and the latest relay result metadata

Dependency and SDK versions are defined in `app/build.gradle.kts` and the root Gradle configuration.

## References

- [Android SMS intents](https://developer.android.com/reference/android/provider/Telephony.Sms.Intents)
- [Android runtime permissions](https://developer.android.com/training/permissions/requesting)
- [Google Chat incoming webhooks](https://developers.google.com/workspace/chat/quickstart/webhooks)

## Contributing

Contributions are welcome. Read [CONTRIBUTING.md](CONTRIBUTING.md) before submitting a change.

For security vulnerabilities or accidental credential exposure, follow [SECURITY.md](SECURITY.md) instead of opening a public issue.
