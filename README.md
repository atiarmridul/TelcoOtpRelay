# Telco OTP Relay

[![Android verification](https://github.com/atiarmridul/TelcoOtpRelay/actions/workflows/android.yml/badge.svg?branch=master)](https://github.com/atiarmridul/TelcoOtpRelay/actions/workflows/android.yml?query=branch%3Amaster)
[![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Gradle](https://img.shields.io/badge/Gradle-Wrapper-02303A?logo=gradle&logoColor=white)](https://gradle.org/)
[![Java](https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/)

> **An Android QA utility for capturing OTP/SMS messages from a dedicated test device and relaying them to Google Chat.**

Telco OTP Relay was built to remove a repetitive step from mobile and telecom testing: manually checking a physical QA device for verification SMS messages during test execution.

**Test flow:** Trigger test → SMS arrives → OTP is detected → message is relayed → continue testing.

This project is designed for **authorized, dedicated QA devices and controlled test environments**.

---

## Why This Project Exists

OTP-based flows are common in telecom and mobile applications, but automated tests can still depend on a physical device receiving the verification SMS.

The usual workflow is:

```text
Trigger test
    ↓
Wait for SMS
    ↓
Pick up phone
    ↓
Read OTP
    ↓
Continue test
```

Telco OTP Relay turns that into:

```text
Trigger test
    ↓
SMS arrives
    ↓
OTP/Sender filter
    ↓
Google Chat
    ↓
Continue test
```

The goal is a focused QA utility — not a consumer messaging application.

---

## What It Does

When an SMS arrives, the application:

1. Receives the Android `SMS_RECEIVED` broadcast.
2. Reconstructs multipart SMS messages.
3. Applies an optional sender allow-list.
4. Applies the configured relay mode:
   - **OTP messages only**
   - **All incoming messages**
5. Resolves the receiving SIM number when Android/carrier information is available.
6. Formats the sender, receiving number, timestamp, and original message.
7. Sends qualifying messages to a configured Google Chat webhook.
8. Records privacy-safe delivery metadata for troubleshooting.

### Example

```text
Receiving number: 01XXXXXXXXX
Sender: 21209
Timestamp: 27 Aug 2026, 07:39:56 PM
Body: <#> Your verification code is 123456
```

If the device or carrier does not expose its own phone number, the receiving number is shown as `Unknown`.

---

## Engineering Highlights

### Event-driven Android architecture

Uses `BroadcastReceiver` to react to incoming SMS events instead of continuously polling the device.

### OTP filtering

OTP-only mode uses a regex-based parser to filter messages before delivery.

### Sender allow-list

Forwarding can be restricted to specific senders. A blank list accepts every sender.

### Reliable delivery

The delivery pipeline includes:

- bounded retries
- timeout controls
- network availability handling
- exponential retry for transient failures
- a maximum pending-data lifetime
- connection cleanup after every attempt

### Privacy-aware diagnostics

Relay history is designed to retain delivery metadata without exposing sensitive message content.

The history surface does not display or store:

- SMS bodies
- OTP values
- receiving numbers
- webhook URLs
- host addresses
- raw exception text

### Automated verification

The repository includes JVM unit tests and Android instrumentation tests covering configuration, relay processing, OTP parsing, formatting, webhook behavior, privacy boundaries, UI states, permissions, and SMS receiver behavior.

### CI verification

GitHub Actions is used to verify the Android project automatically.

---

## Architecture

```text
                    Android SMS
                         |
                         v
                  +-------------+
                  | SmsReceiver |
                  +-------------+
                         |
                         v
               SmsBroadcastParser
                         |
                         v
                  RelayProcessor
                  /      |      \
                 /       |       \
                v        v        v
       SenderFilter  RelayMode  Validation
                         |
                         v
                     OtpParser
                         |
                         v
                  SmsFormatter
                         |
                         v
                    ChatSender
                         |
                         v
                  Google Chat
```

### Main components

| Component | Responsibility |
| --- | --- |
| `SmsReceiver` | Owns the Android SMS broadcast lifecycle and delivery/result persistence |
| `SmsBroadcastParser` | Reconstructs Android SMS broadcasts into an incoming-message model |
| `RelayProcessor` | Applies sender and relay-mode rules before delivery |
| `SenderFilter` | Parses and matches the optional sender allow-list |
| `RelayMode` | Determines whether OTP filtering is required |
| `OtpParser` | Determines whether an SMS looks like an OTP message |
| `SimNumberResolver` | Resolves the receiving SIM number when exposed by the platform |
| `SmsFormatter` | Builds the outbound Google Chat message |
| `ChatSender` | Sends the webhook request with bounded retries and timeouts |
| `RelayHistoryStore` | Stores privacy-safe delivery-result metadata locally |
| `ConfigurationState` | Handles persisted and edited configuration state |
| `LastRelayStatus` | Represents the latest delivery result and safe diagnostics |

---

## Quality Engineering

Quality is treated as part of implementation rather than something added after the feature is complete.

### Automated tests

JVM tests cover:

- configuration state
- relay processing
- sender filtering
- relay-mode selection
- OTP parsing
- message formatting
- timestamp formatting
- JSON encoding
- bounded HTTP retries

Instrumentation tests cover:

- main configuration states
- privacy boundaries
- local relay history
- failure reasons
- privacy navigation
- webhook visibility
- action controls
- requested permissions
- SMS receiver behavior

Run:

```bash
./gradlew check
```

For connected Android tests:

```bash
./gradlew connectedDebugAndroidTest
```

---

## Battery-Efficient Design

The application is designed around event-driven execution rather than a continuously running background process.

- No foreground or always-running service
- No polling or periodic timer
- No alarm
- No wake lock
- No network heartbeat
- No GPS/location access
- No SMS database scan
- One-time, network-constrained WorkManager jobs
- Eight-second overall delivery budget
- Two-second per-stage timeout caps
- At most three webhook attempts per qualifying SMS
- Exponential retry for transient failures
- Pending delivery data removed after at most 24 hours
- Network connections closed after every attempt

---

## Privacy & Security

This application handles SMS content, which may contain sensitive information.

Use it only with an **authorized, dedicated QA device** and an appropriate test environment.

- Treat the Google Chat webhook URL as a credential.
- Never commit webhook URLs or other credentials to Git.
- Configuration is stored in app-private storage.
- Relay-result history stores privacy-safe metadata rather than SMS bodies or OTP values.
- Application backup is disabled in the Android manifest.
- **All incoming messages** mode can forward non-OTP SMS messages, so enable it only when appropriate for the test environment.

See [SECURITY.md](SECURITY.md) for security reporting guidance.

---

## Relay Modes

| Mode | Behavior |
| --- | --- |
| **OTP messages only** | Forwards messages accepted by the OTP regex filter. |
| **All incoming messages** | Forwards every non-empty incoming SMS without applying the OTP filter. |

OTP-only mode is the default for new and upgraded installations.

---

## Quick Start

### 1. Clone

```bash
git clone https://github.com/atiarmridul/TelcoOtpRelay.git
cd TelcoOtpRelay
```

### 2. Build

```bash
./gradlew assembleDebug
```

The debug APK is produced at:

```text
app/build/outputs/distribution/SMS Relay.apk
```

### 3. Install

```bash
adb install -r "app/build/outputs/distribution/SMS Relay.apk"
```

### 4. Configure

Open the app and:

1. Grant the requested permissions.
2. Enter a Google Chat incoming webhook URL.
3. Optionally configure allowed senders.
4. Select **OTP messages only** or **All incoming messages**.
5. Tap **Save configuration**.
6. Use **Test connection** to verify the webhook.

Leave the sender field blank to accept messages from every sender.

---

## Prerequisites

- Android SDK 35
- JDK 21 for the checked-in Gradle daemon criteria
- Android 8.0 (API 26) or newer device with SMS capability
- USB debugging and `adb`
- Google Chat incoming webhook

Check the environment:

```bash
java -version
adb devices
./gradlew --version
```

The application's Java/Kotlin JVM target is 17.

---

## Permissions

| Permission | Purpose |
| --- | --- |
| `RECEIVE_SMS` | Detect incoming SMS messages |
| `READ_PHONE_NUMBERS` | Resolve the receiving SIM number when available |
| `INTERNET` | Send qualifying messages to Google Chat |
| `ACCESS_NETWORK_STATE` | Detect whether a validated internet connection is available |

The app does not request contacts, camera, microphone, location, storage, notification, or default-SMS-app access.

---

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
│       │   ├── ConfigurationState.kt
│       │   ├── LastRelayStatus.kt
│       │   ├── MainActivity.kt
│       │   ├── OtpParser.kt
│       │   ├── RelayMode.kt
│       │   ├── RelayProcessor.kt
│       │   ├── SenderFilter.kt
│       │   ├── SmsBroadcastParser.kt
│       │   ├── SmsFormatter.kt
│       │   └── SmsReceiver.kt
│       └── res/
├── gradle/wrapper/
├── releases/
│   └── SMS Relay.apk
├── docs/
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

---

## Documentation

| Document | Purpose |
| --- | --- |
| [Architecture](docs/ARCHITECTURE.md) | Runtime flow, component boundaries, threading, and design decisions |
| [Project context](docs/PROJECT_CONTEXT.md) | Product goals, scope, terminology, constraints, and success criteria |
| [Decision log](docs/DECISIONS.md) | Important architectural choices and tradeoffs |
| [Testing](docs/TESTING.md) | Automated verification, manual scenarios, and coverage gaps |
| [Privacy](docs/PRIVACY.md) | Data handling, permissions, storage, transmission, and user controls |
| [Contributing](CONTRIBUTING.md) | Development workflow, quality checks, and coding standards |
| [Security](SECURITY.md) | Vulnerability reporting and security guidance |
| [Agent guidance](AGENTS.md) | Repository instructions for coding agents |

---

## Build & Verification

Build:

```bash
./gradlew assembleDebug
```

Run verification:

```bash
./gradlew check
```

Run connected Android tests:

```bash
./gradlew connectedDebugAndroidTest
```

When intentionally updating the checked-in debug APK:

```bash
cp "app/build/outputs/distribution/SMS Relay.apk" "releases/SMS Relay.apk"
shasum -a 256 "app/build/outputs/distribution/SMS Relay.apk" "releases/SMS Relay.apk"
```

---

## Important Limitations

- Android and mobile carriers do not always expose the SIM's own phone number.
- OTP detection is regex-based and can produce false positives or miss unusual OTP formats.
- Qualifying messages may be queued and retried for up to 24 hours when transient delivery failures occur.
- Android/OEM battery restrictions can delay background work, so delivery is not guaranteed.
- A lost network response can result in a duplicate Google Chat message after a retry.
- There is no duplicate suppression.
- The application handles SMS only and does not process MMS messages.

---

## Tech Stack

**Android**
- Kotlin
- Android SDK
- AndroidX Core / AppCompat
- View Binding
- XML layouts

**Build & CI**
- Gradle Kotlin DSL
- Gradle Wrapper
- GitHub Actions
- Java 17 application target

**Messaging**
- Android `BroadcastReceiver`
- Google Chat incoming webhook
- `HttpURLConnection`
- `JSONObject`

**Storage**
- `SharedPreferences`
- SQLite

**Date & Time**
- `java.time`

---

## Why This Is a QA Engineering Project

This repository is intentionally more than a small Android utility.

It combines:

**QA problem identification → solution design → implementation → automated verification → CI → reliability → privacy → documentation**

The project is maintained as an engineering system so that its behavior, architecture, quality checks, and operational constraints are understandable and repeatable.

---

## References

- [Android SMS intents](https://developer.android.com/reference/android/provider/Telephony.Sms.Intents)
- [Android runtime permissions](https://developer.android.com/training/permissions/requesting)
- [Google Chat incoming webhooks](https://developers.google.com/workspace/chat/quickstart/webhooks)

---

## Contributing

Before submitting a change:

1. Read [CONTRIBUTING.md](CONTRIBUTING.md).
2. Run `./gradlew check`.
3. Run connected Android tests when a device/emulator is available.
4. Never commit credentials, webhook URLs, or sensitive SMS data.

For security issues, use [SECURITY.md](SECURITY.md).
