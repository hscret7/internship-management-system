# US41 — opt-in scheduled job and retention

## Scope and validation

The job uses the existing backup-be1-mysql.sh. It adds no application changes and
installs no cron entry. The example config is disabled; retention is disabled.
Run `python3 scripts/test-be1-backup-job.py` for ten tests using temporary folders
and fake dump helpers, with no Docker or application database access. The separate
`bash scripts/test-be1-backup-restore.sh` verifies actual dump/restore.
`bash scripts/test-be1-backup-job-mysql.sh` runs the job twice against its own
real MySQL container, verifies managed bundles and disabled retention; Docker run
is pending user validation. It publishes no host port and cleans up only its container.

## Configuration and first run

Copy scripts/be1-backup-config.example.json to a private absolute path outside git.
Set the explicit source container, database, absolute client option file path inside
that container, and private absolute output directory. The CNF file carries the
credentials; the JSON contains no password. Confirm storage capacity, encryption
and a separate off-host copy plan before enabling. Keep the destination and all
parent directories writable only by the backup owner; do not place them in webapps.

Set enabled=true only for the intended environment. Run:

    python3 /ABS_REPO/scripts/run-be1-backup.py --config /ABS_PRIVATE/backup.json

A successful job verifies the SQL SHA256 and database metadata before reporting
`succeeded` and marking the new bundle as managed. Each job takes an exclusive
nonblocking lock for its output directory. Do not use different output directories
for simultaneous jobs targeting the same database. Exit 75 means another job owns
the lock. Exit 1 and a `failed` JSON event indicate a problem. Configure monitoring
to alert the operator on failed events and stale/missing succeeded events; no email
or chat message is sent by this tool.

The configurable timeout defaults to 1800 seconds. A timeout kills the local helper
process group and creates .be1-backup.blocked. A Docker server-side dump may briefly
outlive its client. Later jobs fail closed until the operator checks that no old dump
is running, reviews any partial staging files and removes that exact marker.
Do not remove the lock file: deleting a locked file can allow overlapping jobs.
The filesystem lock releases automatically when the process ends.

## Retention is a separate opt-in

Agree on keep_days and min_copies first. Both must be positive integers. Set
retention_enabled=true AND invoke the job with --prune to apply the policy.
Pruning runs only after a NEW successful, verified backup. It keeps at least the
newest min_copies verified copies, even if all are older than keep_days. It deletes
only older bundles carrying this job's marker for the configured database. It
leaves manual/unmarked, foreign, symlinked, incomplete and corrupted bundles alone.
Investigate those leftovers manually; they can consume disk space. SHA256 provides
corruption detection, not authentication against a malicious writer.

## Cron template — review before installation

Use absolute paths and a protected log. For an example daily run at 02:00 in the
HOST'S cron timezone, a reviewed crontab entry could be:

    MAILTO=""
    0 2 * * * /usr/bin/python3 /ABS_REPO/scripts/run-be1-backup.py --config /ABS_PRIVATE/backup.json >> /ABS_PRIVATE/backup-job.log 2>&1

This is a template, not an installation command. Confirm the host timezone,
Docker permissions, PATH for bash/docker, log rotation, monitoring and DDL maintenance
window. Add --prune only after the explicit retention policy is approved. No cron
or systemd service is installed by the scripts or tests.

Acceptance still requires an agreed deployment policy, real-host scheduled execution,
monitoring and an isolated recovery drill. The fixture checks do not establish
production RPO/RTO, encryption or off-host availability.
