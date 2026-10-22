# BE3 — Application timestamps and FE handoff

## Backend contract

- `applications.submitted_at` and `applications.reviewed_at` are MySQL `TIMESTAMP` columns in `sql/schema_be2.sql`, representing instants, not local wall-clock `DATETIME` values.
- All six application read paths project `UNIX_TIMESTAMP(column)` and map the decimal epoch seconds to `Instant`, then `Timestamp`. This preserves fractional seconds and avoids depending on the JDBC connection timezone matching the MySQL session timezone. No global/session timezone or database data is changed by these reads.
- Existing JavaBean properties `submittedAt` / `reviewedAt` remain `java.sql.Timestamp` for compatibility with JSTL `fmt:formatDate` and existing callers.
- New JavaBean properties `submittedAtIso` / `reviewedAtIso` are ISO-8601 UTC strings with `Z`, e.g. `2026-10-12T02:00:00Z`.
- `reviewedAt` and `reviewedAtIso` remain `null` until reviewed. An absent timestamp is not converted into the current time, epoch zero or text `"null"`.
- The ISO properties are additional JavaBean getters available to JSP EL; this patch does not introduce an application JSON endpoint or automatically add JSON fields.
- Existing writes still use the database `CURRENT_TIMESTAMP`. No migration, MySQL/JDBC configuration change, frontend source change or status/approval/email behavior change is part of this patch.
- Do not render `Timestamp.toString()` directly as a transport value: its local text has no timezone. Do not add/subtract seven hours in the DAO or FE.

MySQL reference: [UNIX_TIMESTAMP()](https://dev.mysql.com/doc/refman/8.4/en/date-and-time-functions.html#function_unix-timestamp) reads a `TIMESTAMP` column's internal timestamp directly. This contract depends on the canonical columns remaining `TIMESTAMP`; a schema changed to `DATETIME` requires a separate explicit storage-zone decision.

## FE display contract (not changed by BE3)

Update: develop `c86571c` includes FE1 PR #78 (TTS date formatting) and FE2 PR #79 (HR date formatting). These frontend changes were merged from develop without editing their source in this BE3 patch. The following contract and browser acceptance checks remain applicable.

### JSP server-rendered views

Apply this to both:

1. `src/main/webapp/WEB-INF/views/hr/applications.jsp`: list submission date; detail submission and review dates. Replace raw `<c:out value="${app.submittedAt}"/>` / `${application.submittedAt}` / `${application.reviewedAt}` rendering with explicit formatting.
2. `src/main/webapp/WEB-INF/views/intern/applications.jsp`: keep the existing formatting/null fallback, but add the explicit display timezone to both date formatters.

Use the existing `Timestamp` getters, the `jakarta.tags.fmt` taglib and a known locale. Every formatter must explicitly set `timeZone="Asia/Ho_Chi_Minh"`; do not convert the input to a String first.

```jsp
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN" />
<fmt:formatDate value="${app.submittedAt}" pattern="dd/MM/yyyy HH:mm"
                timeZone="Asia/Ho_Chi_Minh" var="submittedFmt" />
```

For the HR detail view use `application` instead of `app`. For the reviewed date use `reviewedAt`; when null, render `—` or `Chưa duyệt`, not `Invalid Date`, epoch zero or the submission date. Reset an output variable to empty before a formatter if reusing it in a loop, so a null date cannot reuse the previous row's text. Keep `c:out` escaping and all current CSRF/form/role behavior.

### If using JavaScript or HTML `<time>`

- Read `${app.submittedAtIso}` / `${app.reviewedAtIso}` (or the `application` detail equivalent), with escaped attribute output.
- An HTML `datetime` attribute must use the ISO value with `Z`, not a preformatted local date.
- If JavaScript formats it, use `new Date(iso)` and `Intl.DateTimeFormat('vi-VN', {timeZone: 'Asia/Ho_Chi_Minh', ...})`. Browser timezone is not the display-zone policy.
- Guard null/empty input and reject invalid timestamps; never remove `Z`, concatenate a `Z` onto a local time, or add seven hours manually.
- Prefer fixing the current JSP rendering; do not add a new JSON/API layer merely for these dates.

## Acceptance checks

1. Same event `2026-10-12T02:00:00Z` renders `12/10/2026 09:00` in both HR and TTS views.
2. Event `2026-10-12T23:59:59Z` renders `13/10/2026 06:59`, proving date rollover is handled.
3. UTC and `Asia/Ho_Chi_Minh` JVMs and different browser timezones show the same Vietnam-local date/time.
4. PENDING applications have null review time; APPROVED/REJECTED show the stored actual review time.
5. Mixed rows with reviewed/unreviewed dates do not carry a preceding row's formatted date into a null row.
6. Approval/rejection, filtering, escaped text and CSRF forms still work unchanged.

Backend regression tests: `ApplicationDAOTimeZoneTest` exercises all six reads across three JVM zones with reviewed/null dates, precise epoch conversion and connection ownership; `ApplicationTimeContractTest` checks ISO/null/legacy-getter compatibility. These are not a substitute for FE/QA browser acceptance checks.

### Repeatable real MySQL regression

Run `mvn -Dbe3.mysql=true -Dtest=ApplicationDAOTimeZoneMySqlTest test` with your ignored local `db.properties` configured. MySQL needs permission to create/drop the test's disposable database.

- The test creates a new `be3_app_time_test_<UUID>` database and loads the canonical BE1/BE2 schemas there. It never redirects or modifies the actual application's database/configuration.
- Fixtures are inserted with fixed UTC SQL literals while asserting session `+00:00`. No existing application timestamp is updated or migrated.
- Eight cases cover JVM `UTC`/`GMT+07:00`, JDBC `UTC`/`Asia/Ho_Chi_Minh`, and session `+00:00`/`+07:00`. Explicit JDBC/session mismatches catch the previous timezone-dependent `getTimestamp()` read.
- All six DAO reads must match independent expected UTC `Instant`s, preserve null reviews and BIGINT reviewers, handle date rollover, retain the caller's transaction/session, and leave stored timestamp/status/reviewer values unchanged.
- `FOR UPDATE` locks only fixture rows and is rolled back. Cleanup drops only the newly created database after validating its fixed prefix and 32-character UUID; it does not change global timezone.
- The test is opt-in (not silently claimed as run in an ordinary package build). JVM timezone is restored in `finally`; timestamp unit/model/MySQL tests share a resource lock.

## Verification (2026-10-12, base develop `9869f7c`)

- `mvn clean package`: success; 517 detected, 440 passed, 77 opt-in tests skipped, zero failures/errors. Includes all 45 new timestamp tests (40 DAO + 5 model).
- `mvn -Dbe3.mysql=true -Dtest=Be3MySqlIntegrationTest test`: both existing MySQL integration tests passed. Their UUID-named disposable database was cleaned up; no actual SMTP delivery was attempted.
- Read-only MySQL probe against the local application data: 8 combinations of JVM UTC/Vietnam, JDBC UTC/Vietnam and connection session `+00:00`/`+07:00`, with 96 application read checks. Model instants matched MySQL epoch seconds and ISO getters in every case, including intentionally mismatched JDBC/session zones.
- No actual application data, global database configuration, frontend source or deployed web instance was changed. FE/QA still needs to verify the display-zone acceptance checks above after implementing the JSP formatting change.

### Persisted regression update (base develop `c86571c`)

- `mvn -Dbe3.mysql=true -Dtest=ApplicationDAOTimeZoneMySqlTest,ApplicationDAOTimeZoneTest,ApplicationTimeContractTest,InternApplicationDateTest,Be3MySqlIntegrationTest test`: all 58 targeted tests passed with zero skipped/failures/errors, including all 8 actual MySQL timezone cases.
- `mvn -Dbe3.mysql=true clean package`: BUILD SUCCESS; 528 detected, 453 passed, 75 other opt-in tests skipped, zero failures/errors. The actual MySQL regressions ran in this build; they were not skipped.
- Test databases were disposed after each run. All actual application's timestamps/data/configuration stayed untouched. FE1/FE2 formatter changes from develop are retained unchanged; this update does not claim full browser/UAT acceptance.
