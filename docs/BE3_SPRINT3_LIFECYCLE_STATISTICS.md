# BE3 – US32/US33: statistics and explicit closure

Migration: run `sql/schema_be3_lifecycle.sql` after the existing BE3 schema. This adds event tables only. Do not reset existing schema or delete any task, attendance, allowance, report, feedback or evaluation.

## FE1/FE2 contract

Session uses BE1 `currentUser`; only ACTIVE HR/ADMIN may call these routes. Unauthenticated requests may be redirected by the shared authentication filter; clients must detect a login response. JSON uses `{success,data,csrfToken}` or `{success:false,error}`. All responses are no-store.

- GET `/api/hr/program-lifecycle`: obtain shared `csrfToken`.
- POST `/api/hr/program-lifecycle`: form-urlencoded `csrfToken`, `programId`, `status=COMPLETED|CANCELLED`, `reason` (required, 1–1000 characters). HR must confirm that ALL currently ACTIVE enrollments in the program will receive this status. Returns `closedEnrollments`.
- POST `/api/hr/enrollment-lifecycle`: same fields except `enrollmentId`; closes only that enrollment.
- GET `/api/hr/program-statistics`: optional `programId`, enrollment `status=ACTIVE|COMPLETED|CANCELLED`, paired `cohortFrom/cohortTo` (inclusive program start-date range, ISO dates).

An ACTIVE program can be completed or cancelled. A PLANNED program can only be cancelled. Terminal states cannot be reopened or closed again. No date/job/score automatically marks an enrollment complete. Completion sets `completed_at`; cancellation does not invent a completion date. Both close the current mentor assignment with `ended_at`. Atomic event rows preserve actor, from/to status, reason and timestamp; event-write failure rolls back the complete mutation.

Lock order: program -> enrollment in ascending ID order; existing BE4 writes lock enrollment before checking permissions. A concurrent write either commits before closure or observes terminal enrollment afterward and is denied. Enrollment IDs do not change. BE4 must use the same enrollment lock for every intern/mentor write (including draft/progress).

Statistics: distinct `intern_profile_id` counts people, total enrollment rows counts internship periods. University/major group counts use distinct people; missing/blank values are `Chưa khai báo`. Avoid joining one-to-many detail tables. Completion rate = COMPLETED enrollments / ALL enrollments **within the same supplied filter** ×100, rounded to 2 decimals. Includes ACTIVE and CANCELLED in an unfiltered denominator; empty denominator =0.00. Selecting COMPLETED deliberately yields 100% of that filtered subset; UI must show the filter, not imply a global rate.

No FE1 markup changed by this PR. FE1/FE2 own the program/closure/statistics screens; FE3 can consume these data for end-term reporting. Migration is not automatically applied to users' databases.

## Verification

Unit filter tests: `mvn test`. Opt-in actual MySQL fixture: `mvn -Dtest=ProgramLifecycleJdbcTest -Dbe3.lifecycle.jdbc=true test`. Reads ignored local `db.properties`; only localhost/127.0.0.1 at3306/3307; creates and removes its own random `be3_lifecycle_test_<uuid>` database, never the application database. Tests cascade closure, preserved histories, role/invalid-state rejection, audit rollback, distinct counts and concurrent closure.

## External acceptance still outstanding

US38 QR/card: waiting on the real scan source/protocol and BE2 attendance interface; not implemented or counted as done. SMTP: unit coverage is available, but actual delivery/failure tests require BE1 SMTP configuration and agreed test-recipient mailbox; no real emails have been sent. QA integration and FE1/FE2 closure UI verification remain required.
