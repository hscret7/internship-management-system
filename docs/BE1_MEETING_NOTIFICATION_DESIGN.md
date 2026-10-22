# BE1 US35 — Task 35.2 notification design

Status: draft implementation added; validation incomplete. See BE1_MEETING_NOTIFICATION_RUNBOOK.md. Branch stacks on Task35.1 commit1e432b4; do not merge directly to develop until prerequisite is integrated.

## Confirmed choices (user, 2026-10-08)
- Intern recipient address: users.email, never an address supplied in the meeting HTTP request.
- One reminder, default30 minutes before start; configurable positive lead time.
- No real email sends during development/verification. Delivery worker disabled by default.

## Existing integration
EmailService is com.internship.service.email.EmailService, implemented by SmtpEmailService. sendEmail(recipient,subject,content) uses text UTF-8 and may throw EmailException/runtime validation failures. Constructor loads mail.properties and fails if absent. Create SMTP adapter only when sending is explicitly enabled; do not initialize it in disabled mode. Existing registration/application email behavior is outside this change.

MeetingService owns scheduling transactions. Add event/recipient capture using the same JDBC connection before commit, without SMTP/network calls. Outbox insertion failure rolls back scheduling write and participant changes. Later SMTP failure must not roll back an already committed meeting.

## Events and recipient snapshots
Each actual CREATE/CHANGE/CANCEL transition captures meeting ID, resulting revision, title/location/time/status, event type and per-recipient user ID/email/content snapshot. One outbox row per event/revision/user, unique key(meeting_id,revision,event_type,recipient_user_id). A repeated cancel with current revision is a no-op and creates no duplicate event.

Initial recipient scope: enrolled Intern participants; organizer/Mentor copies are outside this increment unless requested. CHANGE captures old participants before replacement and new participants after validation; notify union by user ID. Removed recipients receive an explicit removed-from-meeting message, newly added recipients receive current invitation details, retained recipients receive change details. Removed recipient emails come from the previous saved participant snapshot, not an arbitrary current client field. Persist versioned snapshots so participant replacement does not lose delivery history. CANCEL recipients are current participants for that revision. Each row contains only its own recipient; do not expose other attendees' emails.

Resolve and validate a single mailbox per user. Invalid/missing address policy must be implemented explicitly: reject new scheduling operation with sanitized validation error instead of silently dropping required delivery. Never print SMTP secrets or raw provider exceptions to API clients.

## Reminders and obsolete work
Schedule reminder due at startsAt minus configured lead. Meetings created inside the30-minute window get an immediate reminder while still future. No reminders at/after start or for CANCELLED meetings. Reminder scheduling is idempotent per revision/user.
On CHANGE/CANCEL invalidate unsent obsolete reminders in the scheduling transaction and generate new ones only for current participants. Historical CREATE/CHANGE/CANCEL events retain revision ordering per meeting/user; avoid delivering an old invitation after its cancellation.
Recheck current revision/status for reminder before sending. A meeting can change during SMTP; cannot promise elimination of every stale in-flight message.

## Delivery worker
Dedicated connection claims bounded due rows with FOR UPDATE SKIP LOCKED, sets claim token/lease and increments attempt count, then commits and closes before SMTP. Sender uses injected EmailService; tests use fake sender. Completion/failure updates require matching claim token; expired leases are recoverable. Prevent parallel claims of later events for same meeting/user while prior delivery is pending/in-flight.

States: PENDING, IN_FLIGHT, SENT, FAILED, SUPERSEDED. Retry bounded (proposed5 attempts; configurable) with exponential backoff and safe maximum delay. Treat config/input errors as terminal/configuration failures; transient SMTP/DB errors use documented retry policy. Store sanitized error code, not raw exception details. Claim lease must exceed configured SMTP timeouts; heartbeat or conservative single-row claim required to avoid batch lease expiry.

Delivery is at least once under uncertain SMTP acknowledgment or worker crash after send. DB unique keys prevent duplicate queue rows, not duplicate physical emails. Do not claim exactly-once SMTP delivery.

Worker/listener shutdown must stop scheduled executor on undeploy. No public trigger endpoint, no runtime DDL. Explicit deployment configuration enables background processing/sending only after local tests and user authorization for actual mail.

## Migration and verification
Manual migration adds snapshot and outbox tables/indexes after schema_be1_meetings.sql. Existing users/enrollments/assignments remain unchanged. Default-disabled settings ensure missing mail config cannot break scheduling startup; outbox-enabled scheduling requires migrated tables.

Required tests before completion:
- scheduling+outbox atomic commit and rollback on real MySQL, recipient union/removal snapshot, no-op cancel dedup;
- disabled worker does not instantiate SMTP or send;
- claim exclusion, lease recovery/token fencing, bounded retry, event ordering;
- reminder boundary/current revision/cancel suppression;
- text rendering and one-mailbox validation;
- injected sender failure preserves meeting, no credentials in errors;
- full Maven, opt-in MySQL, Tomcat smoke with fake/local capture only.

Migration, outbox integration, worker/listener, unit tests and12 disposable-MySQL tests are now implemented on this branch.42 non-DB tests pass. JDBC execution is blocked by unavailable MySQL socket in the review environment; no JDBC/full Maven/Tomcat35.2 pass is asserted. Default enqueue/send remain disabled; no real email has been sent.
