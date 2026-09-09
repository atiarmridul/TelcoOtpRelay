# Architecture Decision Log

This file records the important decisions visible in the current implementation. A change that reverses one of these decisions should update its status and document the replacement.

## ADR-001: Use the SMS Broadcast as the Event Trigger

- **Status:** Accepted
- **Decision:** Register `SmsReceiver` for Android's `SMS_RECEIVED` broadcast and perform no continuous polling.
- **Reason:** New-message broadcasts provide the required trigger without an always-running component.
- **Consequences:** Battery use remains low, but asynchronous process lifetime is limited and delivery is not durable.

## ADR-002: Keep the Application Single-Module and Lightweight

- **Status:** Accepted
- **Decision:** Use one Android application module with small Kotlin components and minimal AndroidX dependencies.
- **Reason:** The application has one narrow workflow and does not currently justify multiple modules or a dependency-injection framework.
- **Consequences:** The code is easy to navigate. Pure state and relay-processing boundaries plus
  injectable delivery seams cover most behavior with JVM tests, while Android broadcast extraction
  and lifecycle behavior still require device tests.

## ADR-003: Use OTP Parsing Only as a Gate

- **Status:** Superseded by ADR-010
- **Decision:** Use regex rules to determine whether a message qualifies, then forward the complete original body rather than only the extracted code.
- **Reason:** QA users need the message context, sender, and instructions alongside the OTP.
- **Consequences:** Useful context is preserved, but false-positive matches can disclose more SMS content to the Chat space.

## ADR-004: Preserve Multipart SMS Content

- **Status:** Accepted
- **Decision:** Join all message parts in Android-provided order before parsing or forwarding.
- **Reason:** Filtering individual parts could miss a code or send incomplete message content.
- **Consequences:** The receiver must operate on the complete broadcast payload before making a delivery decision.

## ADR-005: Store Configuration in App-Private Preferences

- **Status:** Accepted
- **Decision:** Store the webhook URL, relay mode, optional receiving-number override, and sender allow-list in the `relay` `SharedPreferences` file.
- **Reason:** The app needs only a few small persistent configuration values and does not require a
  database.
- **Consequences:** Setup is simple, but the credential is not protected with application-level encryption. Backup remains disabled.

## ADR-006: Use Direct HTTPS Delivery With Bounded Retry

- **Status:** Superseded by ADR-015
- **Decision:** Post directly to Google Chat with `HttpURLConnection`, explicit per-stage timeouts,
  no more than three attempts while time remains, and a six-second outer receiver deadline that
  also bounds DNS waits.
- **Reason:** This avoids a networking library, backend service, and continuous worker for one small request type.
- **Consequences:** Delivery is best-effort. Process termination can lose messages, and ambiguous
  failures can produce duplicates. A deadline releases the receiver queue but an interrupted HTTP
  operation can still have an uncertain remote outcome.

## ADR-007: Do Not Persist Messages or Delivery State

- **Status:** Superseded by ADR-011 and ADR-015
- **Decision:** Keep SMS and delivery data in memory only.
- **Reason:** This minimizes local retention of authentication data and avoids database and cleanup complexity.
- **Consequences:** There is no offline queue, audit history, delivery status, or recovery after process death.

## ADR-008: Resolve the Receiving Number Opportunistically

- **Status:** Accepted
- **Decision:** Prefer an optional user-configured receiving number; otherwise read the broadcast subscription ID and use permitted Android telephony APIs, falling back to `Unknown`.
- **Reason:** Dual-SIM QA workflows benefit from identifying the receiving line, but carrier and platform support is inconsistent.
- **Consequences:** Phone-number permission improves metadata but is not required to forward a message. The configured override is stored locally and transmitted with qualifying messages. `Unknown` remains valid when neither source provides a number.

## ADR-009: Serialize Delivery Work

- **Status:** Superseded by ADR-015
- **Decision:** Use one shared single-thread executor for receiver processing.
- **Reason:** This bounds thread creation and keeps simultaneous SMS delivery ordered within the process.
- **Consequences:** A slow request delays later messages, and queued broadcasts can outlive Android's receiver execution window.

## ADR-010: Make Message Filtering User-Selectable

- **Status:** Accepted
- **Decision:** Let the user choose between **OTP messages only** and **All incoming messages** relay
  modes, with OTP-only as the fallback for missing or invalid stored values. The latter maps to the
  internal `ALL_MESSAGES` value.
- **Reason:** Some QA workflows need every incoming SMS, while others should minimize disclosure by
  forwarding only likely OTP messages.
- **Consequences:** The app supports both workflows without a second build. **All incoming
  messages** mode has a substantially broader privacy impact because personal, transactional, and
  marketing messages can be forwarded.

## ADR-011: Retain Only the Latest Relay Result Metadata

- **Status:** Superseded by ADR-014
- **Decision:** Persist only the most recent delivered/failed result, sender, and SMS timestamp in
  app-private preferences. Never persist the SMS body, OTP, receiving number, or webhook as part of
  the status record.
- **Reason:** QA users need immediate confirmation that the latest relay worked without turning the
  configuration activity into an SMS or OTP history screen.
- **Consequences:** The UI can show useful operational state after reopening the app, but the sender
  and timestamp remain locally until overwritten or app data is cleared. This is not a retry queue;
  a displayed failure means the bounded retries have already been exhausted.

## ADR-012: Separate Configuration State From Latest Delivery State

- **Status:** Accepted
- **Decision:** Show configuration and connection-test state in a compact header badge, and show the
  latest incoming-SMS delivery result in a separate card only when metadata exists. Keep the primary
  and secondary actions as equal-width 48dp controls inside the compact configuration card.
- **Reason:** Configuration state and delivery history answer different questions. Separating them
  removes duplicated active-state content, preserves room on a standard phone screen, and prevents
  action outlines from colliding with the card edge.
- **Consequences:** Transient state uses short labels such as **Unsaved**, **Testing**, and **Failed**;
  detailed latest-delivery information remains available without displaying the SMS body or OTP.
  Small windows and large accessibility text still rely on fallback scrolling.

## ADR-013: Make Sender Filtering an Optional Allow-list

- **Status:** Accepted
- **Decision:** Store an optional list of originating sender addresses in app-private preferences
  and require a match before applying the relay-mode filter. A blank list allows every sender.
  Matching ignores case and common phone-number formatting characters.
- **Reason:** Dedicated QA workflows may need messages from only selected numbers or branded sender
  IDs, while existing installations must continue forwarding from all senders.
- **Consequences:** The sender list becomes locally retained configuration. Sender addresses exposed
  by carriers can vary in format, so users must configure the address Android reports; missing
  originating addresses do not match a populated list.

## ADR-014: Retain Privacy-Safe Relay-Result History Locally

- **Status:** Accepted
- **Decision:** Continue storing the latest relay metadata in preferences for the compact home-card
  summary and append every qualifying delivery result to an app-private SQLite history. Store only
  result, sender, SMS timestamp, safe failure category, and optional HTTP status.
- **Reason:** QA users need to inspect prior successes and failures, including why delivery failed,
  by tapping the latest-relay card without consulting SMS content or remote logs.
- **Consequences:** The unlocked device exposes sender and delivery metadata for all recorded relay
  attempts until app data is cleared or the app is removed. SMS bodies, OTPs, receiving numbers,
  webhook URLs, host addresses, and raw exception text remain excluded. The history is not a queue
  and never retries or resends a message.

## ADR-015: Persist Pending Relays and Retry With WorkManager

- **Status:** Accepted
- **Decision:** Persist each qualifying formatted message and its webhook in an app-private SQLite
  queue before finishing the SMS broadcast. Use network-constrained one-time WorkManager jobs to
  retry transient failures with exponential backoff. Delete a record after success, a permanent
  HTTP failure, or a separate unconstrained expiry job reaches 24 hours.
- **Reason:** Direct network work inside Xiaomi's SMS broadcast window repeatedly timed out and
  blocked or lost later relays. WorkManager provides durable, system-scheduled work across process
  death, temporary disconnection, and reboot.
- **Consequences:** Delivery reliability improves but is not guaranteed, OEM background controls
  can delay jobs, and ambiguous network outcomes can create duplicates. Qualifying SMS content,
  including an OTP when present, and the selected webhook are stored without app-level encryption
  in app-private storage for up to 24 hours. Result history remains a separate metadata-only store.

## Changing a Decision

When changing an accepted decision:

1. Mark the old entry **Superseded** and reference the replacement.
2. Add the new decision, rationale, alternatives, and consequences.
3. Update `README.md`, `docs/ARCHITECTURE.md`, `docs/PROJECT_CONTEXT.md`, and `docs/PRIVACY.md` where applicable.
4. Add tests for the new behavior and record the verification commands.
