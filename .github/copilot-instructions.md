# GitHub Copilot Instructions

Follow the canonical repository guidance in `AGENTS.md` and the factual context in `docs/PROJECT_CONTEXT.md`.

Core rules:

- Preserve the event-driven `SMS_RECEIVED` architecture.
- Preserve both saved relay modes and use OTP-only for missing or unknown stored values.
- Treat SMS content, OTPs, phone numbers, and webhook URLs as sensitive.
- Never add real credentials or personal message data to code, tests, documentation, or logs.
- Keep network work off the main thread with bounded timeouts.
- Preserve the full original body of qualifying multipart SMS messages.
- Request only necessary Android permissions.
- Add focused tests for changed relay-mode, parsing, or formatting logic.
- Run `./gradlew check` after source or build changes.
- Update architecture, privacy, security, and testing documentation when their behavior changes.

Do not introduce polling, an always-running service, analytics, persistent SMS storage, or new network destinations without an explicit requirement.
