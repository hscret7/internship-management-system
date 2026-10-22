# BE1 US42 AuditLog contract (FE2 / BE2 / BE3 / BE4)

## Deployment
Apply `sql/schema_be1_audit.sql` manually to the selected database before deploying this WAR. It only adds audit_logs; it does not drop/update existing tables or data. Review existing table compatibility if the name already exists. No runtime self-healing DDL. App DB account needs SELECT and INSERT; migration needs CREATE. Do not apply to shared DB without team approval.

## Read API for FE2
GET `{contextPath}/admin/audit-logs`, session authentication, ACTIVE ADMIN only; no public write/delete endpoint. Existing AuthenticationFilter redirects anonymous requests to login; direct servlet calls return 401. Other roles return 403. Cache-Control no-store.

Optional parameters: actorUserId (positive integer), action and objectType (uppercase code, max64), result (SUCCESS/FAILURE), from (inclusive), to (exclusive). Dates are ISO8601 instants with timezone, e.g. `2026-10-07T00:00:00Z`. page default1, range1..1000000; size default20, range1..100. Blank filters ignored. Invalid input400; DB unavailable503. Unknown valid action gives empty list. Always newest occurredAt then id descending.

Response example:
```json
{"items":[{"id":1,"actorUserId":5,"action":"LOGIN","objectType":"USER","objectId":5,"occurredAt":"2026-10-07T09:00:00Z","result":"SUCCESS"}],"total":1,"page":1,"size":20}
```
`actorUserId`/`objectId` nullable. FE2 should render text safely and format UTC instants in Asia/Ho_Chi_Minh. Count and page are separate read queries; concurrent inserts can make total/page differ briefly. Offset pagination is intended for the small project dataset, not snapshot navigation.

## Shared recording entry point
`AuditLogService.record(actorId, action, objectType, objectId, result)` validates IDs/codes and throws SQLException on persistence failure. Only pass server-selected action/type/result and authorized IDs. Never supply passwords, tokens, request bodies or user text as audit codes. DB assigns occurredAt. No FK: deletion does not erase historical IDs. Current hooks use recordBestEffort: failed audit persistence warns with SQLState and does not roll back the primary operation. These writes are separate transactions; this is not atomic or guaranteed delivery. Modules needing atomic audit must add connection-aware integration rather than assume this helper is transactional.

Included hooks: LOGIN success/failure (unknown/rejected actor IDs null; attempted username not stored), LOGOUT success, Admin ACCOUNT_CREATE success from both /admin/users JSON and /admin/accounts form (email delivery result not represented by account-creation result). Public registration and BE2/3/4 business actions are not yet wired; owners can integrate the shared entry point in a follow-up. No bulk HTTP request logging: a 200 form validation response must not be mistaken for a successful business action.

US42 scope: common persistence and Admin querying supplied; cross-module event coverage and FE2 page still require integration. No UI files modified. This is not complete system-wide audit coverage.

## UTC correction
Audit DAO sets `SET SESSION time_zone = '+00:00'` on its own short-lived connection before each insert/query and explicitly supplies a UTC Calendar for JDBC timestamp reads and date-filter bindings. This aligns DB values and JDBC interpretation even when the shared URL declares serverTimezone=Asia/Ho_Chi_Minh. It changes neither global MySQL timezone nor db.properties nor another module's connection. Existing TIMESTAMP rows do not need rewriting. SQL inspection in a UTC MySQL session and API Z timestamps must agree (e.g. ID4 2026-10-07 11:24:53.246524 -> 2026-10-07T11:24:53.246524Z). This adds one session initialization statement per audit connection.
