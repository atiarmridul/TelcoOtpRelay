# Repository Agent Instructions

These instructions apply to the entire repository. This is the canonical guidance for coding agents; tool-specific instruction files should remain short and point here.

## Read First

Before changing behavior, read:

1. `README.md`
2. `docs/PROJECT_CONTEXT.md`
3. `docs/ARCHITECTURE.md`
4. `docs/DECISIONS.md`
5. `docs/PRIVACY.md` for data or permission changes
6. `docs/TESTING.md` for verification expectations

## Project Purpose

SMS Relay is a small Android utility for an authorized, dedicated QA phone. It receives new
SMS broadcasts and forwards complete original messages to a configured Google Chat webhook according
to a saved relay mode.

Preserve these product invariants unless the user explicitly changes the requirements:

- Processing starts from `SMS_RECEIVED`; the app does not poll SMS history.
- The user can choose **OTP messages only** or **All incoming messages** mode; missing settings
  default to OTP-only.
- In OTP-only mode, the parser is a gate. In **All incoming messages** mode, every non-empty SMS
  qualifies.
- A qualifying message's full original body is forwarded.
- Multipart SMS bodies are reconstructed before filtering.
- Missing sender or receiving-number data uses `Unknown`.
- Delivery work stays off the main thread and has bounded timeouts and retries.
- There is no duplicate suppression, persistent queue, MMS support, analytics, or background heartbeat.
- The activity is configuration UI only; it does not need to remain open for reception.

## Technology and Compatibility

- Android Gradle Plugin 9.3.2 with built-in Kotlin support
- Gradle wrapper 9.5.0
- Android application module `:app`
- Namespace and application ID: `com.mridul.telcootprelay`
- Minimum SDK 26
- Target SDK 33
- Compile SDK 35
- Java/Kotlin JVM target 17
- AndroidX Core and AppCompat
- Gradle Kotlin DSL

Do not upgrade SDKs, Gradle, Kotlin, or dependencies as an incidental change. Treat upgrades as separate work with compatibility verification.

## Source Map

| Path                                                         | Responsibility                                                  |
|--------------------------------------------------------------|-----------------------------------------------------------------|
| `app/src/main/java/com/mridul/telcootprelay/MainActivity.kt` | Permission request plus webhook and relay-mode configuration UI |
| `app/src/main/java/com/mridul/telcootprelay/AboutPrivacyActivity.kt` | Read-only privacy and data-flow explanation UI          |
| `app/src/main/java/com/mridul/telcootprelay/LastRelayStatus.kt` | Latest relay-result model, persistence, and formatting        |
| `app/src/main/java/com/mridul/telcootprelay/SmsReceiver.kt`  | SMS orchestration plus opportunistic SIM-number resolution      |
| `app/src/main/java/com/mridul/telcootprelay/OtpParser.kt`    | OTP classification and extraction regexes                       |
| `app/src/main/java/com/mridul/telcootprelay/RelayMode.kt`    | Persisted relay-mode values and filtering decision              |
| `app/src/main/java/com/mridul/telcootprelay/SmsFormatter.kt` | Outbound message formatting                                     |
| `app/src/main/java/com/mridul/telcootprelay/ChatSender.kt`   | Google Chat HTTP delivery and retries                           |
| `app/src/main/AndroidManifest.xml`                           | Permissions, hardware requirements, and Android components      |
| `app/src/main/res/layout/activity_main.xml`                  | Compact configuration, status, action, and privacy layout       |

## Implementation Rules

- Prefer small, focused changes over introducing new architectural layers.
- Keep pure parsing and formatting logic independent from Android framework objects.
- Always call `BroadcastReceiver.PendingResult.finish()` on every asynchronous exit path.
- Never perform network I/O on the main thread.
- Preserve multipart message ordering and the original body content.
- Preserve OTP-only as the fallback for missing or unknown stored mode values.
- Use platform constants instead of copied intent-action or extra strings.
- Use app context for work that can outlive an activity or receiver callback.
- Use Android string resources for new user-facing text.
- Keep configuration controls at least 48dp high and preserve complete button outlines at supported
  phone widths and accessibility text sizes.
- Keep configuration state in the header badge and latest-delivery metadata in its separate card;
  never add SMS bodies or OTP values to either surface.
- Use a standard JSON encoder if changing request serialization.
- Document changes to permissions, background execution, storage, or network destinations.

## Security and Privacy Rules

- Never add real OTPs, phone numbers, SMS bodies, webhook URLs, tokens, keystores, or signing credentials.
- Use synthetic values in tests and documentation.
- Do not log SMS content, parsed OTPs, sender addresses, receiving numbers, or webhook URLs.
- Do not add analytics, crash reporting, remote logging, backups, or additional destinations without explicit approval.
- Keep the broadcast receiver protected by the system `BROADCAST_SMS` permission.
- Request the narrowest Android permissions that support implemented behavior.
- Treat webhook validation and redirect behavior as security-sensitive.
- Update `SECURITY.md` and `docs/PRIVACY.md` when the data flow changes.

## Verification

For source or build changes, run:

```bash
./gradlew check
```

For changes affecting device behavior, permissions, activities, or receivers, also run when a device or emulator is available:

```bash
./gradlew connectedDebugAndroidTest
```

For packaging changes, run:

```bash
./gradlew assembleDebug
```

The repository has JVM tests for relay modes, parsing, and formatting but no instrumentation test
classes. Add focused tests for changed logic, and state clearly when an Android test task reports
`NO-SOURCE`.

Do not hide lint failures with a baseline or suppression unless the underlying behavior is verified and the reason is documented.

## Documentation and Git Hygiene

- Update README and detailed docs when commands, permissions, architecture, or limitations change.
- Keep machine-specific files, build output, local SDK paths, and credentials ignored.
- Keep generated files under `app/build/` ignored. Refresh the tracked
  `releases/SMS Relay.apk` only when the user explicitly requests APK publication, and generate it
  with `./gradlew assembleDebug` from the same source revision.
- Do not edit generated files under `build/` or `.gradle/`.
- Do not commit `local.properties`.
- Do not rewrite unrelated user changes.
- Do not commit, push, create releases, or modify remote repository settings unless explicitly requested.

## Known Risks

- Broadcast-based network work is best-effort and can be interrupted by process termination.
- Three HTTP attempts can outlive the safe receiver execution window.
- Regex detection can create false positives and false negatives.
- **All incoming messages** mode can forward personal and unrelated SMS content by design.
- Retries can deliver duplicates if Google accepts a request but its response is lost.
- The webhook is stored in ordinary app-private `SharedPreferences`, not encrypted storage.
- SIM phone-number APIs are carrier-dependent and may return no value.

Do not silently describe these as solved. If a task affects them, explain the resulting reliability, battery, and privacy tradeoffs.
