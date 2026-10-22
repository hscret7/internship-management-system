#!/usr/bin/env bash
# Creates and destroys only its own disposable Docker container; no host DB port.
set -Eeuo pipefail
trap 'printf "FAIL: restore smoke stopped at line %s (exit %s)\n" "$LINENO" "$?" >&2' ERR
cd "$(dirname "$0")/.."
umask 077
mkdir -p target
container_id=""
cleanup() {
  if [ -n "$container_id" ]; then
    docker logs "$container_id" > target/be1-backup-mysql.log 2>&1 || true
    docker rm -f "$container_id" >/dev/null
  fi
}
trap cleanup EXIT
container_id=$(docker run -d --rm --label be1.backup.test=disposable \
  -e MYSQL_ALLOW_EMPTY_PASSWORD=yes mysql:8.0.46)
ready=false
for ((i=0;i<60;i++)); do
  if docker exec "$container_id" mysql --protocol=TCP -h127.0.0.1 --default-character-set=utf8mb4 -uroot -e 'SELECT 1' >/dev/null 2>&1; then
    ready=true; break
  fi
  sleep 2
done
test "$ready" = true
# This fixture contains a binary value, Unicode, trigger, routine and disabled event.
docker exec -i "$container_id" mysql --protocol=TCP -h127.0.0.1 --default-character-set=utf8mb4 -uroot <<'SQL'
CREATE DATABASE be1_backup_test CHARACTER SET utf8mb4;
USE be1_backup_test;
CREATE TABLE sample(id INT PRIMARY KEY, title VARCHAR(100), payload VARBINARY(10)) ENGINE=InnoDB;
INSERT INTO sample VALUES(1,'Minh Huệ',UNHEX('00FF1020'));
CREATE TRIGGER sample_trim BEFORE INSERT ON sample FOR EACH ROW SET NEW.title=TRIM(NEW.title);
CREATE PROCEDURE count_samples() SELECT COUNT(*) AS total FROM sample;
CREATE EVENT fixture_event ON SCHEDULE EVERY 1 DAY DISABLE DO SELECT 1;
SQL
docker exec -i "$container_id" sh -c 'umask 077; cat > /tmp/be1-backup-client.cnf' <<'CNF'
[client]
user=root
protocol=TCP
host=127.0.0.1
CNF
# A failed dump must not publish a bundle.
failure_dir=$(mktemp -d "$PWD/target/be1-backup-failure.XXXXXX")
if bash scripts/backup-be1-mysql.sh "$container_id" absent_database /tmp/be1-backup-client.cnf "$failure_dir"; then
  echo 'Missing database unexpectedly succeeded' >&2; exit 1
fi
test -z "$(find "$failure_dir" -mindepth 1 -print -quit)"
bundles=$(mktemp -d "$PWD/target/be1-backup-smoke.XXXXXX")
bash scripts/backup-be1-mysql.sh "$container_id" be1_backup_test /tmp/be1-backup-client.cnf "$bundles"
bundle=$(find "$bundles" -mindepth 1 -maxdepth 1 -type d -name 'be1-*')
test -n "$bundle"
(cd "$bundle" && sha256sum -c SHA256SUMS)
# Verify checksum detection on a COPY, preserving the original evidence.
cp "$bundle/database.sql" "$bundle/tampered.sql"
printf '\ncorruption\n' >> "$bundle/tampered.sql"
test "$(sha256sum "$bundle/tampered.sql" | cut -d' ' -f1)" != "$(cut -d' ' -f1 "$bundle/SHA256SUMS")"
rm "$bundle/tampered.sql"
# Destructive operation is restricted to this script's own disposable fixture.
test "$(docker inspect -f '{{ index .Config.Labels "be1.backup.test" }}' "$container_id")" = disposable
docker exec "$container_id" mysql --protocol=TCP -h127.0.0.1 --default-character-set=utf8mb4 -uroot -e 'DROP DATABASE be1_backup_test'
docker exec -i "$container_id" mysql --protocol=TCP -h127.0.0.1 --default-character-set=utf8mb4 -uroot < "$bundle/database.sql"
result=$(docker exec "$container_id" mysql --protocol=TCP -h127.0.0.1 --default-character-set=utf8mb4 -uroot --batch --skip-column-names be1_backup_test -e \
  "SELECT CONCAT(id,':',title,':',HEX(payload)) FROM sample")
printf 'Restored row: %s\n' "$result"
test "$result" = '1:Minh Huệ:00FF1020'
test "$(docker exec "$container_id" mysql --protocol=TCP -h127.0.0.1 --default-character-set=utf8mb4 -uroot -N be1_backup_test -e 'CALL count_samples()')" = 1
docker exec "$container_id" mysql --protocol=TCP -h127.0.0.1 --default-character-set=utf8mb4 -uroot be1_backup_test -e "INSERT INTO sample VALUES(2,'  Trimmed  ',NULL)"
test "$(docker exec "$container_id" mysql --protocol=TCP -h127.0.0.1 --default-character-set=utf8mb4 -uroot -N be1_backup_test -e 'SELECT title FROM sample WHERE id=2')" = Trimmed
test "$(docker exec "$container_id" mysql --protocol=TCP -h127.0.0.1 --default-character-set=utf8mb4 -uroot -N -e "SELECT STATUS FROM information_schema.EVENTS WHERE EVENT_SCHEMA='be1_backup_test' AND EVENT_NAME='fixture_event'")" = DISABLED
printf 'PASS: dump failure, checksum, data/Unicode/binary, routine, trigger, event restore\nEvidence bundle: %s\n' "$bundle"
