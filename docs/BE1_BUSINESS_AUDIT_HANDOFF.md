# BE1 registration and meeting audit

## Behavior

Public registration's account/profile transaction records REGISTER SUCCESS, object USER with the new user ID, actor NULL because registration is anonymous. Scheduling transactions record MEETING_CREATE, MEETING_UPDATE, or MEETING_CANCEL SUCCESS with the authenticated actor and meeting ID. No credentials, email addresses, tokens, titles, or participant lists are copied into audit metadata.

Audit insertion shares the business connection and occurs after business and notification writes, before commit. Failure to insert audit rolls back the entire operation; business, inbox, and email-outbox state cannot commit without its audit. Rejected input, failed authorization, stale revisions, and already-cancelled no-op requests produce no SUCCESS row. Existing best-effort LOGIN/LOGOUT/admin-account behavior is unchanged. This PR covers successful mutations, not a general failure-attempt audit matrix.

The dedicated insertInTransaction method never opens, commits, or closes a connection, requires autoCommit=false, and accepts only the four fixed business action codes. occurred_at uses the existing TIMESTAMP default CURRENT_TIMESTAMP(6), preserving the instant across session timezones; do not bind UTC wall time into a TIMESTAMP under a non-UTC session.

## Deployment prerequisites

Apply sql/schema_be1_audit.sql before deploying. The revised DDL explicitly selects InnoDB for newly created tables. CREATE TABLE IF NOT EXISTS does not convert an existing table; verify audit_logs, users, intern_profiles, meetings, meeting_participants, and enabled notification tables all use InnoDB before rollout. The current 8082 database already has audit_logs and successful auth audit writes, but its engine still needs a read-only check.

No automatic DDL or historical backfill is added. Before-deployment actions cannot be reconstructed as new SUCCESS events. Do not rerun old registration/meeting requests solely to generate audit.

## One test command

```bash
bash scripts/test-be1-business-audit.sh
```

This sequentially runs the existing registration, inbox, email-outbox, and audit disposable MySQL suites and Maven packaging. Each owns and removes its own test container. It does not read application db.properties or touch internship_mysql.

Added regressions check anonymous registration metadata, profile/account rollback on audit failure, all three meeting actions and repeated-cancel deduplication, rollback of meeting/participants/inbox on audit failures during create/update/cancel, and rollback of email outbox/snapshots on failed audit. Existing transaction tests also exercise audit-before-commit and audit failure. Real MySQL execution is pending the user's run.

Local validation: Java 21 compilation and 314 non-integration tests pass under Asia/Ho_Chi_Minh. Integration tests require the disposable MySQL scripts. Rendered production verification remains separate: after approved merge/deploy, perform one normal registration and one create/update/cancel flow, then inspect audit metadata for the returned IDs.
