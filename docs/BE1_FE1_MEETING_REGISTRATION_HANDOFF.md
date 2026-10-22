# BE1 / FE1 — meeting page and public registration

Base: develop 8cd2d35175e91ce6a27fed0b893d312a996ed599.

## Meeting page

GET `{contextPath}/intern/meetings` forwards to FE1's existing
`/WEB-INF/views/intern/meetings.jsp`. Only ACTIVE INTERN sessions can open it.
Anonymous/malformed sessions redirect to login; other roles/inactive users get 403.
The existing AuthenticationFilter rechecks the current account in the DB.
The page route does not change the JSON `/meetings` endpoint.

FE1 should activate the existing meeting sidebar link after merge and deployment.
The JSP and JS remain FE1's files. UTC timestamps still use Instant.toString() with Z.
organizerName is not added: the existing organizerUserId display remains supported.
Browser acceptance must check loading, empty list, pagination, filters, timezone,
expired session and denied access on the actual deployed build.

## Public registration

POST `/auth/register` now passes phone, university, major and gpa to a new
UserService overload. UserDAO inserts users and intern_profiles on one connection,
with autoCommit=false and one commit after both inserts. Profile insert failure
rolls back the account insert. No schema changes or BE2 DAO/service edits.

This requires the existing core and BE2 schemas, with InnoDB tables.
Missing profile schema is an error; the account is not intentionally committed alone.
Existing account-only UserService callers keep their five-argument API.
This change does not backfill older accounts or alter profiles created earlier.

Fields follow the current optional registration form:

- phone: trim; optional; max 20 characters; leading +, digits, spaces, parentheses
  and hyphens; 7–15 digits when supplied.
- university: trim; optional; max 150 characters.
- major: trim; optional; max 100 characters.
- gpa: optional/null; decimal 0–4, maximum two decimal places; no silent rounding.
- blank optional text is saved as empty string to existing NOT NULL columns.
- fullName/email: max 100 trimmed characters, matching the stricter profile columns.
- INTERN role is assigned by the server, never accepted from request data.

FE1 should remove outdated comments claiming these fields are not saved, align
fullName/email maxlength and GPA precision, and retain escaped form values on errors.
No passwords are stored in profile fields or request attributes.
Account uniqueness is enforced by existing DB constraints; a concurrent duplicate
can still return the existing generic database error path rather than a field error.

## Verification

Local Java21 compilation and full JUnit suite: 302 passed, 46 opt-in DB tests skipped,
zero failures. Five new MySQL scenarios remain pending the script below.

```bash
bash scripts/test-be1-registration.sh
```

The script owns one disposable MySQL 8.0.46 at loopback 13321,
database be1_registration_test. It never uses app db.properties.
It verifies linked Unicode/profile persistence, empty optional values,
rollback after profile failure, duplicate constraints and validation before writes.
Run from an isolated checkout; do not point this fixture at a shared database.

US36 backend is already merged; FE1 can use docs/BE1_US36_INAPP_CONTRACT.md.
It remains disabled until its schema and runtime flag are deployed.
No notification page route is added until a corresponding FE view exists.

## Message draft for manual sending after merge

BE1 đã bổ sung route /intern/meetings cho JSP hiện có và sửa đăng ký để lưu
phone/university/major/gpa vào intern_profiles cùng transaction với users.
FE1 giúp bật link Lịch họp trong sidebar, cập nhật chú thích form đăng ký,
giới hạn họ tên/email 100 ký tự và GPA tối đa hai số thập phân.
Giờ họp trả UTC có Z; organizerName chưa bổ sung.
US36 backend đã có trong develop, dùng docs/BE1_US36_INAPP_CONTRACT.md
để nối giao diện thông báo. Mình sẽ báo commit và bằng chứng khi PR được merge.
