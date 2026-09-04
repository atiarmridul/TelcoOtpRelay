# Project Context

## Product Summary

SMS Relay is an internal Android utility for a dedicated QA phone. Its purpose is to make
selected SMS messages received by that phone visible in a controlled Google Chat space without
requiring a person to monitor the handset continuously.

## Primary User

The primary user is a QA or engineering team operating an authorized Android device and Google Chat space. This is not designed as a consumer SMS application or a default SMS client.

## Core Job

Given a newly received SMS, apply the optional sender allow-list and user-selected relay mode and, when accepted, forward a
readable record containing the receiving number, sender, local timestamp, and complete original body
to the configured Google Chat webhook.

## Success Criteria

- The app can be configured once and does not need to remain open.
- Configuration and connection-test state is visible in a compact header badge, independently from
  the latest incoming-message delivery result.
- The standard phone layout keeps the full configuration, actions, privacy link, and version visible
  without scrolling while preserving fallback scrolling for constrained windows.
- New multipart SMS messages are reconstructed correctly.
- **All incoming messages** mode forwards every non-empty incoming SMS body.
- A blank sender list accepts every sender; a populated list accepts only matching originating
  addresses.
- OTP-only mode forwards likely OTP messages and rejects non-qualifying messages.
- Existing installations without a saved mode remain OTP-only.
- Missing SIM-number data is represented honestly as `Unknown`.
- Processing remains event-driven and avoids continuous battery usage.
- No SMS history or analytics is stored locally. Only the most recent relay result, sender, and
  timestamp are retained; SMS bodies and OTPs are not retained.
- Failures finish the pending broadcast without crashing the app process.

## Out of Scope

- Acting as the default SMS application
- Reading historical messages from the SMS database
- Sending or replying to SMS messages
- MMS, RCS, email, or notification forwarding
- User accounts, server-side APIs, or a management dashboard
- Guaranteed delivery or durable offline queuing
- Duplicate suppression
- OTP autofill into another application
- Analytics, advertising, or location tracking

## Domain Terms

| Term               | Meaning in this project                                                            |
|--------------------|------------------------------------------------------------------------------------|
| Qualifying message | An SMS accepted by the selected relay mode.                                        |
| Original body      | The concatenated body reconstructed from all SMS parts without rewriting its text. |
| Receiving number   | The active SIM's own phone number when Android and the carrier expose it.          |
| Sender             | The SMS originating address supplied by Android.                                   |
| Webhook            | A Google Chat incoming-webhook URL saved through the app UI.                       |
| Relay mode         | The saved choice between **OTP messages only** and **All incoming messages**.       |
| Sender allow-list  | Optional comma- or newline-separated originating addresses; blank means all senders. |
| Relay              | Formatting and posting a qualifying SMS to Google Chat.                            |
| Last relay         | The latest delivery result, sender, and SMS timestamp; not a message history.       |
| Status badge       | Compact configuration/test state; not an incoming-message delivery record.         |

## Current OTP Rules

A message qualifies when either condition is met:

1. An OTP-related keyword is followed within 80 non-digit characters by a standalone 4–8 digit number.
2. The message contains any standalone six-digit number.

Recognized keywords include OTP, one-time password, verification, verify, authentication, login, code, passcode, and PIN. This favors simple coverage over semantic accuracy and can produce false positives.

These rules apply only in OTP-only mode. **All incoming messages** mode bypasses OTP classification.

## Message Contract

```text
Receiving number: <number or Unknown>
Sender: <originating address or Unknown>
Timestamp: <device-local timestamp>
Body: <complete original SMS body>
```

## Operational Assumptions

- The app is installed on a phone with SMS-capable telephony hardware.
- The user grants SMS reception permission.
- Phone-number permission is optional for delivery; denial produces `Unknown`.
- The device has internet connectivity when delivery is attempted.
- The configured webhook belongs to a restricted Google Chat space.
- The user understands that **All incoming messages** mode can forward personal and
  non-authentication content.
- Sender filtering is applied in addition to the selected relay mode.
- Android may terminate the process before long asynchronous delivery completes.

## Source of Truth

- Runtime behavior: Kotlin source and `AndroidManifest.xml`
- Dependency and platform versions: Gradle build files
- Architectural rationale: `docs/DECISIONS.md`
- Data handling: `docs/PRIVACY.md`
- Security reporting: `SECURITY.md`
- Verification expectations: `docs/TESTING.md`

When documentation conflicts with executable behavior, verify the implementation and update both in the same change.
