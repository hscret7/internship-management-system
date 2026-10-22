# US36 — inbox backend and integration contract

This draft implements the BE1 inbox and meeting events. BE3 task/deadline and BE4
feedback/evaluation producers are integration seams, not implemented adapters.
The FE1 bell/UI and QA2 acceptance remain separate work. Do not mark the whole US36 done.

## Rollout

1. Apply `sql/schema_be1_inapp_notifications.sql` to the explicitly selected database,
   after the meeting schema. The application does not run migrations.
2. Restart the intended Tomcat with `-Dinapp.notifications.enabled=true`.
   Default is false: no meeting inbox writes, and authenticated inbox requests return 503.
3. Email settings remain independent. Inbox does not require SMTP or a valid email address.
4. To disable, restart without the flag. Existing inbox data is retained.

Deploy only after disposable MySQL verification. Do not copy credentials, run migrations
against another member's database, or redeploy the shared 8082 instance as part of this draft.

## FE1 HTTP contract

All paths are relative to `/internship-management-system`; use the current origin.
The existing auth filter can redirect an expired session to login. Check the final URL
and JSON content type as well as HTTP status; a login HTML response can be HTTP 200.

`GET /notifications?page=1&size=20&unreadOnly=false`

```json
{"items":[{"id":12,"kind":"CREATE","sourceType":"MEETING","sourceId":1,
"sourceRevision":1,"title":"Cuộc họp mới","body":"Meeting details",
"availableAt":"2026-10-09T08:00:00Z","readAt":null}],
"unreadCount":1,"page":1,"size":20,"csrfToken":"session-bound-token"}
```

- Defaults: page 1, size 20, unreadOnly false. Bounds: page 1–1,000,000; size 1–100.
- unreadOnly accepts only `true` or `false`. Stable order: availableAt descending, then id descending.
- unreadCount covers all visible unread notifications of the current user, not just the page.
- Pending future, expired reminder and superseded records are excluded from both list and count.
- No userId parameter selects another inbox. The session user is rechecked as ACTIVE in the DB.
- `readAt=null` means unread. Render title/body as text (`textContent`), never as HTML.
- Source identifiers are metadata, not authorization. Existing source APIs must authorize navigation.
  No client-supplied link is accepted. FE owns routing for known source types.

`POST /notifications`, content type `application/x-www-form-urlencoded`:

```text
action=read&id=12&csrfToken=<token from GET>
```

Success: HTTP 200 `{"id":12,"read":true}`. Repeating read preserves the first read timestamp.
Missing, foreign, future, superseded or expired notification: 404 with the same error.
CSRF failure: 403; invalid input: 400; unsupported encoding: 415; disabled/DB failure: 503.
Direct unauthenticated servlet requests return 401; the filter may redirect first.
Responses use JSON, UTF-8, no-store and nosniff. No HTTP publish/delete/read-all endpoint is provided.

## Meeting behavior

- CREATE goes to current participant users. It also schedules a reminder 30 minutes before start,
  or immediately if the meeting starts within 30 minutes.
- CHANGE goes to the union of old and new participants; removed users receive an explicit removal note.
- CANCEL goes to the current participant set. Cancelling an already cancelled meeting at its current
  revision does not create another notification or increment revision.
- Update/cancel supersedes reminders of earlier revisions. A reminder stops being visible at start.
- Inbox publishing and reminder supersession run on the same connection before the meeting commit.
  Failure rolls back meeting, participants and any enabled email outbox writes.
- No background SMTP worker is needed: reminders become visible by their available_at time.
  FE polling/reloading is needed to refresh the bell; this is not a push delivery service.
- Reminder invalidation currently follows meeting update/cancel. BE3 enrollment/mentor lifecycle
  changes require agreed integration; they do not retroactively purge historical inbox messages.

## BE3 / BE4 trusted producer integration

`NotificationPublisher` is the opt-in seam; default false is a no-op. Construct one server-side,
authorize the recipient through the business service, then publish before that service commits:

```java
// c is the existing business transaction: autoCommit=false, session/JDBC timezone UTC.
// Recipient, source ID and revision come from authorized server data, not request userId fields.
publisher.publish(c, recipientUserId, new NotificationEvent(
    "task:" + taskId + ":" + revision + ":assigned", "TASK", "TASK", taskId, revision,
    "Bạn được giao nhiệm vụ", taskTitle, Instant.now()));
// Caller performs its normal commit/rollback; publisher never commits, closes or sends email.
```

Allowed kinds: CREATE, CHANGE, CANCEL, REMINDER, TASK, DEADLINE, FEEDBACK.
Allowed sources: MEETING, TASK, REPORT, EVALUATION. Key: ASCII letters/digits `:._-`, length 1–160.
Source ID/revision positive; title nonblank ≤200 characters; body ≤10,000 characters.
The producer owns the event key: use one immutable key per logical event revision.
Unique `(event_key,recipient_user_id)` makes retries/concurrent publishes idempotent. A duplicate
does not rewrite content or reset readAt, even if the caller supplies different content.
Inactive/missing recipients are skipped. A failure for an active recipient must propagate and
roll back the caller's transaction. Do not catch it and commit business changes anyway.

Publishing does not independently authorize recipients: BE3/BE4 must keep their existing scope
checks. This draft does not change their services, controllers, schema or FE flows.
Deadline events may use a future availableAt. Cancellation/rescheduling semantics for non-meeting
deadlines must be agreed before their adapters are added; this draft has no generic expiry API.

## Verification and remaining gates

Run `bash scripts/test-be1-inapp-notifications.sh` from the checkout. It creates only a disposable
MySQL 8.0.46 database `be1_inapp_test` on loopback port 13319, uses no app db.properties, and removes
only its own container. The nine JDBC scenarios cover atomic meeting writes, recipient changes,
cancel dedupe, ownership/read idempotency, reminder due/expiry, rollback, concurrent dedupe,
inactive users and rejected writes. The fixture refuses any other JDBC URL.

Local Java 21 compilation and non-DB regression results are recorded in the PR. Real MySQL
execution is a separate pending gate in this environment (Docker unavailable; Unix socket blocked).
FE1 acceptance and BE3/BE4 adapters remain pending. No merge or shared deployment is authorized here.
