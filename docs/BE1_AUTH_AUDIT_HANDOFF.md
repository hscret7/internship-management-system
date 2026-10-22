# BE1 authentication, authorization and audit handoff

Reviewed against develop 0404d45455a2cff747830ca30f94b2d957765e66.
This change adds automated tests and handoff notes only; no frontend or business
module file is modified.

## Automated verification

AuthServiceTest is a legacy main-method harness; Maven/JUnit does not execute its
assertions automatically. Do not run that legacy harness on shared accounts: it
changes account status. AuthServiceRegressionTest uses an injected fake DAO and
fixture BCrypt hash. Its eight JUnit cases cover blank credentials without lookup,
trimmed username/exact password whitespace, bad password/missing account, nonactive
status, invalid stored hash, explicit DB error propagation, identity/role and null DAO.

AuthorizationMatrixTest adds four cases including all 32 role/area/root-versus-child
combinations. ADMIN can enter all role areas; HR/MENTOR/INTERN their own areas.
Other endpoints remain responsible for object-specific access; a passing area
filter alone does not grant meeting/inbox access.

Existing AuthenticationFilterTest adds 12 session cases: active refresh, lock/inactive/
deleted/missing-role/role-change revocation, expiration, public routes, assets,
DB failure (503 without destroying session), and malformed session. Combined local
JUnit verification passed 24/24. Run on Maven:

    mvn -Dtest=AuthServiceRegressionTest,AuthorizationMatrixTest,AuthenticationFilterTest test

These are mocked-DAO/filter regressions, not browser/Tomcat cookie acceptance tests.

## Current event coverage

LoginServlet records LOGIN SUCCESS for authenticated users and LOGIN FAILURE with
nullable actor/object for rejected credentials. LogoutServlet records LOGOUT SUCCESS
for a session user before invalidating the session. Existing Admin account creation
controllers record ACCOUNT_CREATE success/failure. Audit is best effort in a separate
transaction; an audit outage is logged by SQLState without request secrets and does
not establish atomic business+audit persistence. Other module event producers are
not covered by these hooks; do not mark system-wide US42 complete.

Successful login invalidates the old session, creates a new session and stores
currentUser/currentRole. AuthenticationFilter reloads the account for authenticated
requests and invalidates revoked/changed-role sessions. AuthorizationFilter checks
role area; endpoint services enforce meeting/inbox ownership.

## Integration boundaries

FE1 owns the bell/inbox UI, FE2 owns the audit UI. BE2/BE3/BE4 owners should wire their
own approved event producers to the trusted notification/audit APIs using actual
business recipients, not request-supplied recipient IDs. Follow the existing US36
contract and audit API contract, with no passwords/tokens in payloads or logs.

The new tests do not establish rate limiting, login/logout CSRF protection, HTTPS
cookie policy, browser cookie behavior or full integration acceptance. Existing
logout is POST and does not validate a CSRF token in LogoutServlet. Review its
frontend/token contract before a separate server-and-client change; these test-only
changes do not silently impose a new contract on the current UI.

## Draft message for teammates (send manually)

BE1 has merged meeting APIs, opt-in email/inbox notifications and audit persistence
verification. Please use the BE1 API contracts when wiring your UI/module events.
FE1: inbox/bell; FE2: audit page; BE2/BE3/BE4: event producers within your own modules.
Please confirm request/response and recipient rules before integration. The backend
checks session/role plus endpoint ownership; retain CSRF handling on write endpoints.
Do not put credentials in commits or messages. Send integration issues with endpoint,
HTTP status and sanitized reproduction steps. BE1 will coordinate contract changes
before merge; no teammate module has been edited in this verification batch.
