# US42 audit — disposable MySQL verification

This change adds regression verification for the existing BE1 audit implementation.
It does not change production Java, schema, frontend, or another module's hooks.

Run `bash scripts/test-be1-audit.sh` from this checkout with Java 21, Maven and Docker.
The script starts its own MySQL 8.0.46 on loopback port 13320, creates `be1_audit_test`,
and removes only the container it created when finished, including on test failure.
It does not read app db.properties or connect to internship_mysql. If the port is
occupied, Docker fails rather than the script selecting an existing database.

Eight JDBC cases cover nullable/historical IDs without foreign keys, UTC timestamp
round-trip with a deliberately Asia/Ho_Chi_Minh JDBC timezone, inclusive/exclusive
microsecond date bounds, combined filters, stable pagination, DB-assigned timestamps,
Admin service authorization and explicit persistence failure/best-effort behavior.
The fixture recreates only audit_logs inside the dedicated database and refuses a
different supplied fixture URL. The script also runs existing servlet/service/timezone
and account-audit regression tests. Default Maven builds skip the JDBC class.

Real MySQL execution is pending until this script succeeds on the user's Docker host.
Local compilation/unit-test results are recorded in the draft PR; they are not a
substitute for a JDBC pass. Do not merge without the user's explicit approval.

Existing limitations remain: recording hooks use separate best-effort transactions;
count/page are separate reads; FE2 page and BE2/BE3/BE4 event coverage are separate
integration work. This is not complete system-wide audit acceptance or atomic audit.
