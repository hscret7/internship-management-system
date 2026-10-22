# BE1 logout CSRF handoff

Base: develop bdb8ce8e0cd1086642e5ebcbca710772a7dcfd0d.

## Result

Existing-session POST /auth/logout now requires the session-bound csrfToken. Missing, incorrect, or another session's token returns 403 before invalidation and before LOGOUT SUCCESS audit. Valid POST preserves redirect to /auth/login and records the actor before invalidating the session. A request without a session redirects without creating one. GET remains 405 in the servlet; the authentication filter may redirect an unauthenticated GET first.

The three existing logout forms (common header, login.jsp, index.jsp) receive a hidden token through CsrfToken.expose(request). This is the required form integration; layout, sidebar, and unrelated FE behavior are unchanged. Future logout forms must include the same token.

## Validation

Local Java 21 compilation and JUnit scan: 310 passed, 47 optional integration tests skipped, zero failures. Eight new servlet regressions cover absent/incorrect/cross-session tokens, token bootstrap rejection, valid audit and invalidation, best-effort audit outage, no-session behavior, and GET rejection.

Before merge, run on the isolated branch:

```bash
mvn -Dtest=LogoutServletTest,AuthenticationFilterTest,AuthServiceRegressionTest,AuthorizationMatrixTest,AuditLogServiceTest test
mvn clean package
```

After deploying the approved merge to 8082, verify the three rendered JSP logout forms contain nonempty tokens. Missing/invalid-token POST must return 403 and leave the login session usable. A valid logout form must redirect to login; the previous authenticated session must no longer access protected pages. Verify LOGOUT SUCCESS in the database for the correct actor. Never include session cookies, credentials, or CSRF token values in test reports.

## Audit review still open

Code hooks found: LOGIN success/failure in LoginServlet, LOGOUT success in LogoutServlet, ACCOUNT_CREATE success in both admin account controllers. Public registration and meeting create/update/cancel did not contain audit hooks in the reviewed BE1 source. This is a code-review finding, not a complete runtime audit matrix. Their required coverage and transaction behavior need a separate BE1 change; this PR only protects logout and verifies its existing audit behavior. Other members' business-event audit coverage is not assessed here.
