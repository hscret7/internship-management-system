#!/usr/bin/env bash
# Exercises the scheduled-job wrapper with a real, script-owned MySQL only.
set -Eeuo pipefail
cd "$(dirname "$0")/.."
umask 077
python3 scripts/test-be1-backup-job.py
mkdir -p target
container_id=""
cleanup() {
  if [ -n "$container_id" ]; then
    docker logs "$container_id" > target/be1-backup-job-mysql.log 2>&1 || true
    docker rm -f "$container_id" >/dev/null
  fi
}
trap cleanup EXIT
trap 'printf "FAIL: backup job MySQL test line %s (exit %s)\n" "$LINENO" "$?" >&2' ERR
container_id=$(docker run -d --rm --label be1.backup.job.test=disposable \
  -e MYSQL_ALLOW_EMPTY_PASSWORD=yes mysql:8.0.46)
ready=false
for ((i=0;i<60;i++)); do
  if docker exec "$container_id" mysql --protocol=TCP -h127.0.0.1 -uroot -e 'SELECT 1' >/dev/null 2>&1; then
    ready=true; break
  fi
  sleep 2
done
test "$ready" = true
docker exec "$container_id" mysql --protocol=TCP -h127.0.0.1 -uroot -e \
  'CREATE DATABASE be1_job_test; CREATE TABLE be1_job_test.sample(id INT PRIMARY KEY); INSERT INTO be1_job_test.sample VALUES(1)'
docker exec -i "$container_id" sh -c 'umask 077; cat > /tmp/be1-job-client.cnf' <<'CNF'
[client]
user=root
protocol=TCP
host=127.0.0.1
CNF
fixture=$(mktemp -d "$PWD/target/be1-backup-job-mysql.XXXXXX")
python3 - "$fixture" "$container_id" <<'PY'
import json,pathlib,sys
root=pathlib.Path(sys.argv[1])
(root/'job.json').write_text(json.dumps(dict(enabled=True,container=sys.argv[2],database='be1_job_test',
 client_cnf='/tmp/be1-job-client.cnf',output_dir=str(root/'bundles'),timeout_seconds=1800,retention_enabled=False)))
PY
python3 scripts/run-be1-backup.py --config "$fixture/job.json"
python3 scripts/run-be1-backup.py --config "$fixture/job.json" --prune
python3 - "$fixture" <<'PY'
import json,pathlib,sys
root=pathlib.Path(sys.argv[1])/'bundles'
bundles=list(root.glob('be1-be1_job_test-*'))
assert len(bundles)==2, 'Expected two separately published backups'
for bundle in bundles:
 marker=json.loads((bundle/'.be1-job.json').read_text())
 assert marker['format']=='be1-backup-job-v1' and marker['database']=='be1_job_test'
print('PASS: real MySQL job verified two bundles; retention disabled despite --prune')
PY
for bundle in "$fixture"/bundles/be1-be1_job_test-*; do
  (cd "$bundle" && sha256sum -c SHA256SUMS)
done
printf 'Job evidence: %s\n' "$fixture"
