# Contributing to SMS Relay

Thank you for improving SMS Relay. Keep changes small, privacy-conscious, and suitable for a dedicated Android QA device.

## Before You Start

- Use JDK 21 and Android SDK 35. The checked-in Gradle daemon criteria select JDK 21; the
  application bytecode target remains JVM 17.
- Use an Android 8.0 (API 26) or newer device or emulator.
- Never use real OTPs, phone numbers, or webhook URLs in source code, tests, screenshots, logs, commits, or issues.
- Discuss large architectural changes before implementing them.

## Local Setup

Clone the repository and enter the project directory:

```bash
git clone https://github.com/atiarmridul/TelcoOtpRelay.git
cd TelcoOtpRelay
```

Verify the build:

```bash
./gradlew assembleDebug
./gradlew check
```

`assembleDebug` writes the named APK to `app/build/outputs/distribution/SMS Relay.apk`. The
`releases/SMS Relay.apk` copy is a checked-in debug artifact for the dedicated QA phone; refresh it
only when APK publication is explicitly requested and only after verification passes.

To run connected Android verification, start an emulator or connect a device and run:

```bash
adb devices
./gradlew connectedDebugAndroidTest
```

## Development Guidelines

- Keep the app event-driven. Do not add polling, periodic work, or a continuously running service without a documented requirement.
- Keep Android framework coordination in `SmsReceiver` and reusable transformations in focused helpers.
- Preserve the complete original SMS body after a message is accepted by the saved relay mode.
- Preserve OTP-only as the safe fallback when the saved relay mode is missing or invalid.
- Keep network operations off the main thread and bounded by explicit timeouts.
- Request only permissions that are required by implemented behavior.
- Treat the webhook URL and SMS content as sensitive data.
- Avoid logging SMS bodies, OTP values, phone numbers, or webhook URLs.
- Use Android string resources for new user-facing copy.
- Keep actionable controls at least 48dp high, confirm custom button backgrounds are not tinted or
  clipped, and retain fallback scrolling for small windows and large accessibility text.
- Keep the compact configuration-state badge separate from the latest-relay result card.
- Add or update tests when changing parsing, formatting, permission handling, or delivery behavior.

## Testing Changes

Run the standard verification lifecycle:

```bash
./gradlew check
```

When an Android device or emulator is available, also run:

```bash
./gradlew connectedDebugAndroidTest
```

Follow the scenarios and coverage guidance in [docs/TESTING.md](docs/TESTING.md).

## Commit Messages

Use a concise, imperative subject. Conventional Commit prefixes are encouraged:

```text
feat: support additional OTP message formats
fix: finish pending SMS broadcasts after delivery errors
test: add multipart SMS parser coverage
docs: clarify webhook setup
```

## Pull Requests

A pull request should include:

- A clear explanation of the problem and solution
- The Android versions or devices used for verification
- Commands run and their results
- Tests for changed behavior, when practical
- Screenshots for visible UI changes
- Documentation updates for setup, permissions, architecture, or privacy changes
- Explicit privacy documentation when **All incoming messages** behavior changes
- A refreshed `releases/SMS Relay.apk` only when the change is intended to publish a new debug APK

Do not include secrets or real customer data in pull request descriptions or artifacts.

## Reporting Security Issues

Do not report vulnerabilities or exposed credentials in a public issue. Follow [SECURITY.md](SECURITY.md).
