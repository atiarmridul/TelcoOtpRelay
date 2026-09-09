# Privacy and Data Handling

This document describes the current implementation's technical data flow. It is not a substitute for an organization's legal privacy notice or compliance review.

## Intended Use

SMS Relay is intended for an authorized, dedicated internal QA device. It should not be installed on a personal phone or used to forward messages without the device owner's and message recipient's authorization.

## Data Processed

For an incoming SMS, the app may process:

- Full SMS body
- OTP or verification code contained in the body
- Originating sender address
- SMS timestamp
- Receiving SIM phone number, when Android and the carrier expose it
- Receiving subscription identifier during in-memory processing

The app also processes the Google Chat webhook URL entered by the user.
It stores the user's relay-mode choice, optional receiving-number override, and optional sender allow-list.
For a qualifying message, the formatted body and selected webhook are stored in an app-private
pending-relay database until delivery succeeds, a permanent failure occurs, or 24 hours elapse.
After a qualifying delivery attempt finishes, it stores the delivered/failed result, sender, SMS
timestamp, and a privacy-safe failure category or HTTP status. The latest result appears in the
home-screen card; tapping that card opens the complete local result history. It does not store
exception messages, resolved host addresses, receiving numbers, or webhook data as diagnostics.
The compact header badge displays configuration and test state only. No result surface receives the
SMS body, OTP, receiving number, or webhook value.

## Data Collection Rules

- SMS content is inspected only after Android delivers an incoming SMS broadcast.
- When a sender allow-list is configured, messages from other originating addresses are not
  forwarded. A blank list accepts every sender.
- In OTP-only mode, a regex filter determines whether the message appears to contain an OTP.
- In **All incoming messages** mode, every non-empty incoming SMS is accepted without OTP
  classification.
- The full original body of every accepted message is included in the outbound Chat message.
- The app does not scan the SMS inbox or historical SMS database.
- The app does not collect contacts, location, media, advertising identifiers, or analytics.

## Local Storage

| Data                  | Stored Locally? | Notes                                                               |
|-----------------------|-----------------|---------------------------------------------------------------------|
| Webhook URL           | Yes             | Stored in app-private `SharedPreferences`.                          |
| Relay mode            | Yes             | Stored in app-private `SharedPreferences`; defaults to OTP-only.    |
| Receiving-number override | Yes          | Optional configuration used when Android cannot expose the SIM number. |
| Sender allow-list     | Yes             | Optional originating addresses; blank means all senders.           |
| Relay result history | Yes              | Delivered/failed state for every qualifying delivery attempt.      |
| Relay sender history | Yes              | Sender associated with each qualifying delivery attempt.           |
| Relay timestamps     | Yes              | SMS timestamp associated with every history entry.                  |
| Failure diagnostics  | Yes              | Safe category and optional HTTP status; no URL or exception text.   |
| Pending formatted SMS and webhook | Yes | App-private queue; deleted after success, permanent failure, or 24 hours. |
| SMS body or OTP history | No            | The result-history database contains metadata only.                |
| SIM number            | No              | Resolved for the current message only.                              |

Application backup is disabled. Clearing app data or uninstalling the app removes its configuration,
pending relays, and relay-result history.

## Data Transmission

Accepted message data is sent over HTTPS to the configured `chat.googleapis.com` webhook. In **All
incoming messages** mode, this includes non-OTP messages. Google Chat and the organization
controlling the destination space receive and retain the resulting message according to their own
configuration and policies.

The app does not send data to analytics, advertising, crash-reporting, or developer-operated servers.

## Android Permissions

| Permission | Data Access |
| --- | --- |
| `RECEIVE_SMS` | Allows the app to receive incoming SMS broadcasts. |
| `READ_PHONE_NUMBERS` | Allows an attempt to identify the receiving SIM number. |
| `INTERNET` | Allows HTTPS delivery to Google Chat. |
| `ACCESS_NETWORK_STATE` | Allows the app to detect whether Android has a validated internet connection. |
| `RECEIVE_BOOT_COMPLETED` | Allows WorkManager to reschedule pending relays after reboot. |
| `WAKE_LOCK` | Allows WorkManager to keep the processor awake while a scheduled relay runs. |
| `FOREGROUND_SERVICE` | Declared by WorkManager for compatible long-running work; this app does not start an always-running service. |

Phone-number access may return no value even when permission is granted.

## Retention and Deletion

The app retains configuration and relay-result history until cleared. A queued SMS body, OTP when
present, and webhook are retained only while that relay is pending, for at most 24 hours. Result
history does not retain SMS bodies, OTPs, receiving numbers, webhook data, resolved host addresses,
or exception text.
To remove locally stored data:

1. Use **Clear history** in the relay-history screen to remove result metadata only (it does not cancel pending relays),
2. Clear the app's storage in Android settings to remove all local data, or
3. Uninstall the app.

Messages already delivered to Google Chat must be managed in the destination Chat space. Removing the app does not delete messages from Google Chat.

## User and Administrator Controls

- Android permissions can be revoked in system settings.
- The webhook, relay mode, and receiving-number override can be replaced from the app.
- The sender allow-list can be changed or cleared; clearing it allows all senders.
- The latest-relay card is hidden when no record exists and displays only result, sender,
  timestamp, and a safe failure diagnostic when populated.
- Tapping the latest-relay card opens all locally retained result metadata, newest first.
- The confirmed **Clear history** action removes all result history and latest-card metadata while
  preserving the relay configuration.
- The header badge exposes configuration/test state but no message content or credential value.
- The in-app **About & Privacy** screen summarizes storage, transmission, permissions, and controls.
- A Google Chat administrator can revoke the webhook.
- Access to the destination Chat space can be managed through Google Workspace.
- Uninstalling the app stops future relay processing.

## Privacy Risks

- Regex filtering may forward a non-OTP message containing a standalone six-digit number.
- Carrier-provided sender formatting can differ from the configured value and cause an expected
  message not to be forwarded.
- Sender filtering reduces which messages are relayed but does not authenticate the sender; SMS
  originating addresses may be spoofed depending on the network.
- **All incoming messages** mode intentionally forwards personal, transactional, and marketing SMS
  messages as well as OTPs.
- A qualifying SMS may contain personal or account information beyond the OTP.
- Anyone with access to the destination Chat space may be able to view forwarded content.
- Anyone who obtains the webhook URL may be able to post into the Chat space.
- Delivery retries can create duplicate messages.
- Someone with access to the unlocked QA device can see relay-result metadata in the UI. Pending
  SMS bodies, OTPs, and webhooks are not shown by the app but remain sensitive app-private data.

Use a restricted Chat space, limit device access, rotate exposed webhooks, and periodically review whether the relay is still required.
