# US41 — manual MySQL backup and isolated restore verification

This foundation adds scripts only. It does not install cron, restart Tomcat, change
application code, prune existing backups or restore to the application database.

## Manual backup

Run `bash scripts/backup-be1-mysql.sh CONTAINER DATABASE ABS_CLIENT_CNF OUTPUT_DIRECTORY`.
All four arguments are required. The client option file must already exist inside
the selected container and contain `[client]` connection credentials. Keep its
permissions restricted; do not put passwords in command arguments or commit it.
The source operation uses mysqldump only. Choose a protected destination outside
webapps, use encrypted storage as required, and provision sufficient space.

The bundle contains database.sql, SHA256SUMS and metadata.txt. Files are private
under umask 077. Failed dumps are removed and are not published as backup bundles.
Verify with `(cd BUNDLE && sha256sum -c SHA256SUMS)` before any restore. Checksums
detect accidental corruption; they do not authenticate an untrusted backup.

InnoDB data uses a single transaction. Pause migrations/DDL during backup. Other
storage engines and external attachments require a separate consistency plan.
Routines, triggers and events are included; restore privileges and DEFINER accounts
must be reviewed on the destination. No automatic production restore is provided.

## Isolated smoke test

Run `bash scripts/test-be1-backup-restore.sh` with Docker. It creates one MySQL
8.0.46 container with no published host port and removes only that container on exit.
It tests failed-dump cleanup, checksum integrity, Unicode/binary data, and routine,
trigger and disabled-event restoration. DROP DATABASE is used only for the owned
be1_backup_test fixture after its disposable label is verified. Evidence stays
under target/be1-backup-smoke.*; this SQL includes test data only.

## Rollout decisions still required

Agree on host/account, database scope, encrypted destination/off-host copy,
backup frequency, retention period, alerting and a maintenance window. Then add
an opt-in scheduler and retention policy with a recovery drill on an isolated
server. This PR does not complete scheduled backups or production restore acceptance.
Real Docker verification must pass and the user must approve before merge.
