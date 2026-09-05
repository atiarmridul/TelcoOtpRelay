# Testing Guide

## Current Status

The repository contains JVM unit tests for relay-mode selection, OTP classification, extraction,
sender allow-listing, outbound message formatting, and last-relay status parsing and timestamp
formatting. Android instrumentation characterization tests protect the main screen's initial and
persisted states, mode-specific privacy warning, latest-relay privacy boundary, action-control
dimensions, webhook masking and visibility, About & Privacy navigation, requested permissions, and
the SMS receiver's manifest and action contract.

## Automated Verification

The build checks in `gradle/gradle-daemon-jvm.properties` pin the Gradle daemon to JDK 21 and include
Foojay provisioning URLs for supported operating systems and architectures. Regenerate the file
with `./gradlew updateDaemonJvm`; the app compilation target remains JVM 17.

Run all local checks:

```bash
./gradlew check
```

Build the debug APK:

```bash
./gradlew assembleDebug
```

The named distribution artifact is written to `app/build/outputs/distribution/SMS Relay.apk`.
The tracked `releases/SMS Relay.apk` is refreshed only for an explicitly requested APK publication.
Before committing it, verify that it matches the named distribution artifact:

```bash
cp "app/build/outputs/distribution/SMS Relay.apk" "releases/SMS Relay.apk"
shasum -a 256 "app/build/outputs/distribution/SMS Relay.apk" "releases/SMS Relay.apk"
```

With an emulator or physical device connected, run:

```bash
adb devices
./gradlew connectedDebugAndroidTest
```

## Unit Test Coverage

Add JVM tests under:

```text
app/src/test/java/com/mridul/telcootprelay/
```

Current `OtpParser` coverage includes:

- OTP keywords with 4-, 6-, and 8-digit values
- Case-insensitive keyword matching
- Standalone six-digit values
- Digits embedded in longer numbers
- Non-OTP transactional messages
- Empty and whitespace-only messages
- Code extraction order

Current `SmsFormatter` coverage includes:

- Complete sender and receiving number
- Empty values converted to `Unknown`
- Original multiline body preserved
- Timestamp formatting under a controlled timezone

Current `RelayMode` coverage includes:

- Missing and unknown values default to OTP-only
- Saved `ALL_MESSAGES` mode is restored
- OTP-only mode accepts and rejects messages through the parser
- `ALL_MESSAGES` mode accepts ordinary non-OTP messages
- Blank bodies are rejected in both modes

Current `SenderFilter` coverage includes:

- Empty configuration allowing every sender
- Multiple configured senders and rejection of non-matches
- Case-insensitive branded-sender matching
- Phone-number formatting normalization
- Comma, semicolon, and newline parsing

Current `LastRelayStatus` coverage includes:

- Delivered and failed stored values
- Blank sender fallback to `Unknown`
- Unknown or incomplete stored state rejection
- Timestamp formatting under a controlled timezone

Current `ConfigurationState` coverage includes:

- Missing and blank webhook setup state
- Persisted active state
- Equivalent normalized form values remaining saved
- Relay-mode changes producing an unsaved state

Current `RelayProcessor` coverage includes:

- Complete accepted-request preparation with the stable message contract
- Sender and relay-mode rejection
- Blank-webhook rejection
- Lazy SIM-number resolution only after a message qualifies

Current `ChatSender` coverage includes:

- Success without unnecessary retries
- Three-attempt retry bounds and the 1-second/2-second delay schedule
- Connection failures and non-success HTTP responses
- Connection cleanup and configured timeouts
- Lossless standard JSON encoding for control characters and Unicode

Run the JVM tests directly with:

```bash
./gradlew testDebugUnitTest
```

## Current Instrumentation Coverage

Device tests live under:

```text
app/src/androidTest/java/com/mridul/telcootprelay/
```

The current 11-test baseline covers:

- Initial setup, masked-webhook, and empty latest-relay state
- Restoring the webhook, sender allow-list, relay mode, and active state
- All-messages guidance and privacy warning
- Webhook Show/Hide behavior
- Delivered and failed latest-relay metadata without SMS bodies or OTPs
- Equal 48dp action dimensions and retained button backgrounds
- About & Privacy navigation
- Exact user-relevant permission contract
- Exported, system-permission-protected SMS receiver contract
- Ignoring non-SMS intent actions

The baseline passed on a physical Android 12 device during the September 2026 modernization.

## Remaining Recommended Instrumentation Coverage

Priority cases not yet automated:

- Webhook validation and persistence
- Visible dropdown chevron and the **All incoming messages** option label
- Destination and provider visual hierarchy
- Compact header badge for `Setup`, `Active`, `Unsaved`, `Invalid`, `Testing`, `Connected`, and
  `Failed` states
- Secondary **Test connection** followed by primary **Save configuration**
- Secondary button outline and primary button fill remain intact when disabled, enabled, pressed,
  and rendered with accessibility font scaling
- Unsaved-change status after editing any configuration field
- Save action is dimmed and disabled as **Saved** until a configuration value changes
- Version footer matches the installed package version
- Full configuration is visible without scrolling at standard phone dimensions
- Configuration remains usable via fallback scrolling on small landscape and large-text displays
- Permission-denied configuration flow
- Relay-ready status after saving configuration
- Receiver behavior when no webhook is configured
- Receiver completion on parsing and delivery errors

Avoid real SMS bodies and active webhook credentials in test fixtures.

## Manual Device Test Matrix

| Scenario                             | Expected Result                                          |
|--------------------------------------|----------------------------------------------------------|
| First launch                         | Permission explanation appears and webhook can be saved. |
| Existing configuration without mode  | OTP-only is selected and remains active.                 |
| Existing configuration without senders | Messages from every sender remain eligible.            |
| Blank sender field                    | Messages from every sender remain eligible.              |
| Multiple allowed senders              | Only messages from listed originating addresses qualify. |
| OTP keyword plus code                | Full original message is forwarded.                      |
| Standalone six-digit code            | Full original message is forwarded.                      |
| Multipart OTP SMS                    | Parts are combined in order and forwarded once.          |
| Non-OTP message                      | Nothing is sent.                                         |
| Non-OTP message in **All incoming messages** mode | Full original message is forwarded.            |
| Change dropdown without saving       | Previously saved mode remains active.                    |
| Edit any configuration field         | Header badge shows **Unsaved** and Save becomes enabled. |
| Restore the persisted form values    | Header badge returns to **Active** and Save shows **Saved**. |
| Test a valid webhook                  | Badge transitions through **Testing** to **Connected** or **Failed**. |
| No latest-relay metadata              | Latest-relay card is hidden without leaving empty space. |
| Missing phone-number permission      | Message uses `Receiving number: Unknown`.                |
| Carrier hides own number             | Message uses `Receiving number: Unknown`.                |
| Invalid webhook prefix               | Configuration is rejected.                               |
| Offline device                       | Attempts remain bounded and the receiver finishes.       |
| Successful qualifying relay          | Latest status shows delivered, sender, and timestamp only. |
| Failed qualifying relay              | Latest status shows failure and exhausted retries.       |
| Dual-SIM device                      | Receiving subscription is used when Android provides it. |

Use synthetic OTPs and a dedicated test Chat space for manual verification.

## Before Submitting Changes

Record:

- Gradle commands run
- Android version and device or emulator model
- Relevant manual scenarios
- Any remaining warnings or coverage gaps

Update this guide whenever test locations, commands, or expected behavior change.
