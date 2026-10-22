# BE4 Sprint 3 – task, weekly report, criteria assessment, end-term export

Run `sql/schema_be4_sprint3.sql` after the original BE4 migration. Back up and pause writers first: MySQL DDL auto-commits. Guarded steps support reruns after interruption. Three additive tables store structured report sections, actual progress actors and export-generation evidence. The existing `evaluations` table/Evaluation model is extended with nullable skill_score/attitude_score. Existing score-0..100 rows remain unchanged, with a generated-key UNIQUE constraint preserving one legacy evaluation per enrollment; new criteria rows have NULL score, validated integer criteria1..5 and nonblank comment and are append-only. No separate InternEvaluation or parallel criteria table. No history rows are deleted or auto-converted. A server missing the migration must be fixed; do not treat database errors as an empty list.

## Contract consumed by FE3

Existing `/mentor/be4`, `/intern/be4`, `/hr/be4` routes use BE1 session currentUser and the existing BE4 CSRF mechanism: GET `/csrf`, then form-urlencoded POST with X-CSRF-Token or `_csrf`. Request fields never select the actor. Errors400 validation,403 authorization/CSRF,409 state/duplicates,503 export configuration failure; SQL failures500. Shared authentication may redirect expired sessions to login; FE must reject HTML/redirect responses.

### Tasks / progress

- Mentor POST `/tasks/create-assign`: enrollmentId,title(1..255),description(1..10000),dueDate(ISO day, required within program start/end). Creates task AND assignment atomically; no orphan draft on assignment failure. Returns `{task,assignment}`.
- Mentor POST `/assignments`: taskId,enrollmentId; same description/deadline validation applies to a legacy task owned by the mentor.
- Intern POST `/progress`: assignmentId,completionPercentage(integer0..100),optional note<=10000. Locks enrollment then assignment, verifies active owner/program/task, preserves monotonic progress and rejects updates to DONE. Writes append-only progress, assignment status and actual actor evidence in one transaction.
- GET `/progress`: history includes updatedBy; old entries without recorded authors return null, never infer an author retroactively.
- Mentor GET `/meeting-participants`: current active mentees/program IDs, for BE1 meeting forms only. BE1 `/meetings` remains authoritative for scheduling/auth/revisions/notifications.

### Reports / feedback

- Intern POST `/reports`: enrollmentId,weekNumber,results,difficulties,nextPlan. Draft sections can be empty; each max10000. Week1 begins at program start; maximum =floor((end-start days)/7)+1. Unique enrollment/week remains enforced.
- Intern POST `/reports/update`: reportId and the three sections; DRAFT only.
- Intern POST `/reports/submit`: reportId; all three structured sections must have content. Legacy single-content drafts retain their old representation; FE3 offers explicit editing into structured sections.
- GET `/reports`: includes results,difficulties,nextPlan,legacyContent. Content remains populated for older consumers. **Server** omits DRAFT rows from mentor responses, regardless of FE filtering.
- Mentor POST `/feedback`: reportId,required nonblank comment<=10000,optional rating1..5. SUBMITTED or REVIEWED reports allow multiple append-only responses. First feedback changes status to REVIEWED; later responses do not overwrite the first. Actual reviewerUserId/time remain attached.
- Every draft update/submit/feedback uses enrollment-before-child lock order. Interns retain readonly history after completion. Only the last assigned mentor can read a terminal enrollment; a superseded mentor in an active enrollment cannot read/write current mentee data. HR has full end-term read scope.

### Criteria assessment (US19)

- Mentor POST `/assessments`: enrollmentId,skillsScore,attitudeScore (each integer1..5),required comment<=10000. Current mentor and ACTIVE program/enrollment only; every save creates a history entry with genuine mentor/time.
- Intern/Mentor/HR GET `/assessments?enrollmentId=...`: newest-first permitted history. Last terminal mentor and intern may read; old mentor after handover cannot write. No automatic average/rating/lifecycle outcome.
- Legacy `/evaluation` and HR `/evaluations` return only nonnull score0..100 rows and must be labelled as such. They are NOT converted into the two1..5 criteria or mistaken for newer criteria rows. End-term joins only the single legacy row, preventing multiplication when criteria history grows.

### End-term reporting (US20 / partial US34)

- HR/Admin GET `/hr/be4/end-term`: optional programId,enrollmentId,status(ACTIVE/COMPLETED/CANCELLED),page(default1),size(default20,max100). `{items,page,size,hasMore,generatedAt}`. One row per enrollment; includes profile ID/name, school/major, program/date/status, mentor, tasks total/DONE, submitted reports including reviewed, reviewed count, criteria history and legacyScore.
- HR/Admin GET `/hr/be4/end-term/export?format=xlsx|pdf` with the same three filters. A fresh consistent server snapshot is generated; not merely the visible page. All filtered rows, max1000; exceeding that cap returns409 asking for narrower filters, never silent truncation. XLSX has a summary and full criteria history. Numeric counts/dates are typed; user strings cannot become formulas. Long comments use continuation rows with repeated stable IDs. PDF embeds a Vietnamese font, wraps and paginates with page labels. Empty data has an explanation.
- Font: server JVM `-Dbe4.pdf.font=<path-to-Vietnamese-capable-TTF>`; Windows default Arial. No licensed fonts committed. Missing/unsupported font fails visibly, not broken text. Export evidence records actor,filters,format,row count and generation time; it proves file generation, not successful delivery by the browser.
- Build dependencies: Apache POI5.5.1 and PDFBox3.0.8; `pom.xml` is shared, so BE1 should review this change before merge. Official docs: https://poi.apache.org/download.cgi and https://pdfbox.apache.org/3.0/getting-started.html.

### Statistics export (US34 consuming US32/33)

HR/Admin GET `/hr/be4/program-statistics/export?format=xlsx|pdf`, filters exactly `programId,status,cohortFrom,cohortTo` from BE3 `/api/hr/program-statistics`. The adapter calls ProgramStatisticsDAO directly in a consistent server snapshot, with the same DISTINCT Intern and enrollment denominator; zero enrollment is labelled “Chưa có dữ liệu”, not a misleading 0% completion. XLSX contains overview/school/major tabs and typed counts/percentages; PDF contains full groups. Over2000 combined groups fails409 with a narrower-filter message. Export evidence includes PROGRAM_STATISTICS kind and cohort bounds. FE2 owns the statistics screen integration; this does not change their markup.

### Attendance export (US34 consuming BE2)

- HR/Admin GET `/hr/be4/attendance/export?format=xlsx|pdf&from=2026-10-01&to=2026-10-10`, optional positive `programId,enrollmentId`. Parameter names match BE2 `/hr/attendance`. Both inclusive date bounds are required; maximum366 days. Unsupported/repeated parameters are rejected400, not silently ignored. Over10000 ACTIVE work shifts returns409 before loading the report, never a partial export.
- `AttendanceReportService` calls the existing BE2 `AttendanceService.getReport` with the exact filters, not a parallel attendance formula. Authorization is checked against the active HR/Admin database account, then preflight and both BE2 queries share a repeatable-read snapshot. The mutable BE2 result is copied into immutable export rows. SQL failure is visible500, never an empty-success report.
- Each row represents an ACTIVE shift, not a DISTINCT day. PRESENT/ABSENT/LEAVE/MISSING_CHECKOUT, approved leave and workedHours are kept exactly as BE2 calculates them. Multiple shifts on a day can share its check-in/out, so no summed hours or attendance percentage is invented. XLSX uses typed dates/timestamps/hours/booleans and literal names; PDF embeds Vietnamese text and paginates all rows. Null checkout is a blank Excel cell/em dash in PDF, not a fabricated checkout.
- Run additive `sql/schema_be4_attendance_export.sql` after `schema_be4_sprint3.sql`. It adds nullable report_from/report_to to export evidence only; cohort_from/cohort_to retain their program-statistics meaning. Rerunnable; no attendance/history rows deleted. Records ATTENDANCE kind, actor, applied filters, format and generated row count, only after file generation succeeds.
- **Integration blocker verified on2026-10-10:** develop2038801 AttendanceDAO uses `pe.intern_id` although canonical schema has `pe.intern_profile_id` (MySQL1054). BE2 bugfix6fda458 on `bugfix/be2-sprint3-qa` corrects the join but is not merged. BE4 does not copy/edit that fix or merge the whole bugfix branch; BE2 must review its remaining changes and PR it. End-to-end export acceptance on develop remains pending that fix plus QA comparison with the BE2 screen. FE2 owns attendance-screen export buttons; this PR does not rewrite FE2 markup.

## Verification

`mvn -B -ntp clean package`; opt-in local fixture `mvn -B -ntp -Dbe4.mysql=true -Dtest=Be4Sprint2MySqlTest,EndTermExportTest,Sprint3WorkflowValidationTest test`. Local fixture creates/drops only its newly generated be4_sprint2_test_<uuid> database using ignored db.properties; never the application database. Tests cover private draft, sections/week validation, two criteria history/handover, terminal readonly access, counts without row multiplication, actual progress actor/rollback and HR-only export. Optional `-Dbe4.export.samples=true` writes QA samples under ignored target/export-qa, not repository documents. PDF test is explicitly skipped when no Vietnamese font exists.

UI/browser acceptance, deployment migration, actual SMTP and end-to-end meeting delivery need integration QA; build/unit/JDBC alone do not establish all Sprint 3 acceptance.
