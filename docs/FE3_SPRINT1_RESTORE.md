# FE3 - Restore missing Sprint 1 UI

## Scope and source

Restore only the FE3 changes lost from `develop` after its exact restore to
`8af20bc64b383575f8e7c75daedb68bdabe3f152`:

- PR #17, source commit `636c82deb16d9252108938d20e3959e10f70ba92`:
  HR dashboard page/route, sidebar navigation, dashboard CSS and JavaScript.
- PR #18, source commit `d22c3ef9426699f56a5a4e72bea643ec70e882fd`:
  shared layout for the HR applications, catalog and enrollments pages.

The eight restored implementation files match their contents in
`recovery/develop-before-exact-restore-20260929` at `f49d9a9`.
The three HR JSP page-directive conflicts were resolved by retaining
`contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"`, consistent with
the shared header. No business rules or database schema were changed.

PR #15's UTF-8 changes already exist as file contents on `develop`, so they
were not applied twice. FE1 PR #23 and its Intern page changes are outside
this restoration; `intern/applications.jsp` and `intern/enrollment.jsp`
are untouched.

## Verification (2026-10-01)

- `mvn clean package`: 56 tests, 0 failures, 0 errors, 0 skips.
- 10 new regression tests protect `/hr/dashboard`, role/session checks,
  existing FE3 page dispatching, UTF-8 and shared layout.
- Tomcat 10.1 Jasper compilation of all four restored HR JSPs: 0 errors.
- `node --check` of `fe3.js`: passed.
- Browser check on an isolated local preview with synthetic database data:
  `/hr/dashboard` loads and shows 4 interns, 3 active enrollments,
  1 completed enrollment and 25% completion, plus university/major/mentor
  breakdowns. No data-error notice appears.
- The HR applications, catalog, enrollment list and enrollment form load
  with the common layout, correct Vietnamese text and functioning links.
  The enrollment form retains its CSRF token. No business-data mutation
  was needed for these UI checks.
- Responsive dashboard check at 390 px: no horizontal overflow.
- HTTP checks: Admin dashboard 200, Mentor/Intern 403,
  unauthenticated dashboard redirects to login (302). HR was checked in
  the browser with live dashboard data.

This is a targeted smoke/regression check, not a replacement for full QA.

## Integration

Review this branch's PR into `develop`. Do not reset or force-push
`develop`, and do not merge the entire recovery branch to restore this UI.
Sprint 2 FE3 PR #26 must include this restoration when updating its base;
keep its mentor UI and the restored HR layout together.

Remaining non-FE3 items (including FE1 PR #23) need their own owner review.
