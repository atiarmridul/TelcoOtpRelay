# Security Policy

SMS Relay processes authentication messages and a Google Chat webhook credential. Security and privacy issues must be handled carefully.

## Supported Version

Security fixes are applied to the latest code on the repository's default branch. Older commits and locally modified builds are not maintained as separate supported releases.

## Reporting a Vulnerability

Do not open a public GitHub issue for a vulnerability, exposed webhook, real OTP, phone number, or SMS body.

Use GitHub's private vulnerability reporting or Security Advisory feature for this repository when available. If private reporting is unavailable, contact the repository owner through the contact method listed on the owner's GitHub profile and include only enough information to establish a private reporting channel.

Include:

- A concise description of the issue
- Affected Android version and device type
- Steps to reproduce with synthetic data
- Expected and actual behavior
- Potential impact
- A suggested mitigation, if known

Do not include an active webhook URL, a real OTP, or personal SMS content. Revoke exposed credentials before continuing the report.

## Security-Sensitive Areas

Changes require additional review when they affect:

- SMS broadcast receiver exposure or permissions
- Relay-mode selection, persistence, fallback behavior, and forwarding rules
- OTP detection and the broader data exposure of **All incoming messages** mode
- Webhook URL validation or storage
- Storage and display of latest-relay sender, timestamp, and result metadata
- HTTP request construction and redirect behavior
- Logging, analytics, crash reporting, or backups
- SIM and phone-number access
- Background execution or persistent retry storage

## Credential Response

If a Google Chat webhook is exposed:

1. Revoke or remove the webhook in Google Chat.
2. Create a replacement webhook.
3. Update the app configuration on the authorized device.
4. Remove the credential from Git history, logs, screenshots, and shared artifacts where possible.
5. Review the Chat space for unexpected messages.

Deleting the current file alone does not remove a secret from Git history.

## Deployment Guidance

- Install the app only on authorized QA devices.
- Keep OTP-only mode selected unless forwarding all SMS is explicitly required and approved.
- Restrict access to the device and its configured Google Chat space.
- Keep Android security updates current.
- Do not enable backups or diagnostic logging that captures SMS content.
- Keep the last-relay UI limited to result, sender, and timestamp; never add SMS bodies or OTPs.
- Remove the app and revoke its webhook when the relay is retired.

## APK Integrity

The repository may contain `releases/SMS Relay.apk` as a convenience debug build for the dedicated
QA phone. It is debug-signed, is not a separately supported production release, and must not contain
embedded webhook credentials or SMS fixtures. For higher assurance, build the APK from the reviewed
source revision with `./gradlew assembleDebug`. When refreshing the tracked APK, compare its SHA-256
checksum with `app/build/outputs/distribution/SMS Relay.apk` before committing it.
