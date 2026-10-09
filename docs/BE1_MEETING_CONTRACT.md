# BE1 US35 Task 35.1 — meeting data source

Status: backend integrated into develop through PR #53 and #54 (2026-10-08). FE/BE3 acceptance remains pending; full US35 is not complete.

The authoritative meeting source is the new `meetings` and `meeting_participants` tables. Apply `sql/schema_be1_meetings.sql` manually after core/BE2/BE3 schema, initially on an isolated test DB. Existing program/enrollment/mentor tables are read and locked for authorization, never modified by this module. No runtime DDL, seed data, email sends or UI changes.

`MeetingRequest`: programId, title (trimmed 1..200), description (optional <=10000), location (trimmed 1..500), startsAt/endsAt as UTC Instant, distinct enrollmentIds (1..200). Participants must be ACTIVE enrollments in the selected PLANNED/ACTIVE program, resolving to ACTIVE INTERN users. No arbitrary client-supplied recipient emails/user IDs. Meeting start must be in the future; end after start. TIMESTAMP range 1970-01-01T00:00:01Z through 2038-01-19T03:14:07Z; microseconds preserved. FE should display Asia/Ho_Chi_Minh but submit instants with timezone.

`MeetingService.create(actorUserId, request)` returns meeting ID. `update(actorUserId, meetingId, expectedRevision, request)` replaces current details/participants atomically, preserving organizer/program. `cancel(actorUserId, meetingId, expectedRevision)` keeps meeting/participant rows, marks CANCELLED and increments revision. Updating cancelled meetings is rejected. A fresh cancellation of an already cancelled meeting with its current revision is a no-op. A stale revision is rejected; callers must reload.

Actor IDs must come from authenticated server sessions, never a request field. Service re-reads ACTIVE role from DB. ADMIN/HR may manage meetings. MENTOR may create only for currently assigned ACTIVE mentees; only the organizing Mentor may update/cancel, and must still have current assignments for every existing and proposed participant. After reassignment or inactive enrollment, HR/Admin manage existing meeting cleanup. INTERN cannot write. Authority is checked in the same transaction as writes; DAO does not commit.

Both meeting data and participants commit/rollback together. Dedicated connection sets SESSION timezone UTC; JDBC date reads/binds use UTC Calendar. Foreign keys use existing signed INT program/enrollment and signed BIGINT user IDs. No cascading deletion. Participant changes replace current links; Task 35.2 now preserves per-revision delivery recipient snapshots; these are not a general enrollment/assignment audit history.

## HTTP contract (BE1, for FE integration)
GET `{contextPath}/meetings`: `programId` optional positive INT, `status` optional SCHEDULED/CANCELLED, `page` default1 (1..1000000), `size` default20 (1..100). Returns `{items:[...],page,size,csrfToken}`; no total-count field. Ascending startsAt then ID. Filters applied before pagination; concurrent scope changes can shorten a page. GET with `id` returns `{item:{...},csrfToken}`. Details contain id,programId,organizerUserId,title,description,location,startsAt,endsAt,status,revision,enrollmentIds. Intern gets an empty enrollmentIds array to avoid exposing other participants' enrollment identifiers.

POST same route with `application/x-www-form-urlencoded` body and `csrfToken` from authenticated GET. `action=create`: programId,title,optional description,location,startsAt,endsAt,enrollmentIds (comma-separated enrollment IDs). `action=update`: same fields plus id,revision. `action=cancel`: id,revision. Create returns201 `{id}`; update/cancel200 `{id}`. Actor comes from currentUser in the session, not client actorUserId. Re-fetch details for current revision after changes. Do not send a JSON body.

Read scope: ACTIVE ADMIN/HR all meetings; ACTIVE INTERN only meetings linked to their own profile/enrollment (including cancelled history); ACTIVE MENTOR only if every participant is their currently assigned active mentee. Creator alone does not confer continued Mentor access after reassignment. Writes use the policy above. No shared filter changes; AuthenticationFilter still validates sessions before this endpoint. Anonymous requests normally redirect to login; direct servlet calls401. No-store/nosniff. Errors400 invalid data,403 role/scope/CSRF denied,404 missing or unreadable detail,409 stale revision or cancelled update,415 wrong POST encoding,503 DB unavailable. Returned errors do not contain SQL or credentials.

WSL build, opt-in local MySQL DAO test and core Tomcat role/CSRF smoke evidence are recorded in PR #53 for head 8554f29. Review fixes require fresh validation; these results do not prove concurrent lifecycle behavior. UI remains owned by FE.

Existing-meeting writes check Mentor ownership/current scope before revision and cancelled-state errors. Authorized stale writes return409; unauthorized Mentor writes return403. Basic request parsing/validation can still return400 before service authorization. Mentor SQL list visibility uses the same active Intern user/role, enrollment/program and current assignment conditions as detail reads; concurrent changes can still shorten pages.

Task 35.2 has added transactional outbox/versioned delivery snapshots for: CREATE/CHANGE/CANCEL/reminder events, recipients including removed participants, retry schedule/claim locking and dedup by meeting/revision/event/recipient. Never perform SMTP while holding the scheduling transaction. Existing EmailService.sendEmail is reusable; no external messages sent in Task 35.1. Email exactly-once delivery must not be promised when SMTP acknowledgment is uncertain.

Open integration decisions: HR scope is currently all programs (existing project role policy); Reminder lead is configured by meeting.notifications.reminder.minutes (1..10080). Delivery uses users.email; reminders default to 30 minutes and are configurable in the notification runbook. Existing enrollment/mentor lifecycle owners retain their modules. Task 35.1 remains In Progress until migration and JDBC behavior are tested against MySQL, new API passes runtime role/CSRF checks and FE/BE3 integration contract is reviewed.


## Bằng chứng cập nhật — 2026-10-09
- PR #53 merge commit 016f6f4; PR #54 merge commit 376adfe. Người dùng đã phê duyệt từng merge.
- WSL develop 376adfe: mvn clean package PASS, 266 tests / 240 pass / 26 skipped / 0 failures/errors. Skipped gồm 1 BE1 DAO JDBC, 2 BE3 MySQL, 11 BE4 MySQL, 12 notification JDBC do thiếu opt-in properties.
- Chạy riêng script disposable MySQL 8.0.46 tại 376adfe: 54/54 PASS, 0 skipped, gồm 12 notification JDBC. Không coi 13 integration tests BE3/BE4 là đã pass.
- Tomcat test 18082 dùng WAR ce3f003 đã build trước merge: default-disabled login/read; enqueue-only create/update/cancel/repeated cancel đều đạt. Snapshot giữ email người bị loại, CHANGE gửi union người cũ/mới; reminder cũ SUPERSEDED.
- SMTP loopback Mailpit: CREATE/CHANGE/CANCEL được capture; cuộc họp 2 CREATE/REMINDER SENT attempts=1. Pause Mailpit khi tạo cuộc họp 3: CREATE PENDING attempts=2 EMAIL_FAILURE; unpause -> SENT attempts=3, lỗi xóa. Reminder cuộc họp 3 đến hạn -> SENT attempts=1.
- DB cuối: 8 SENT, 2 SUPERSEDED. SMTP runtime này là ce3f003, không phải triển khai lại WAR post-merge; không có thử internet/TLS/auth/load.
- Người dùng lưu SQL, outbox, Mailpit message listing, Surefire (54-test run), commit và log trong thư mục bằng chứng local rồi dừng đúng Tomcat test và hai container có label. Cổng 18082/18025/11025/13318 không còn listener. Không redeploy Tomcat 8082 hoặc migrate DB chung.
- Chi tiết bằng chứng: https://github.com/hscret7/internship-management-system/pull/53 và https://github.com/hscret7/internship-management-system/pull/54.
