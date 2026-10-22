#!/usr/bin/env bash
# Read-only source operation. Credentials stay in a client option file inside Docker.
set -euo pipefail
umask 077
if [ "$#" -ne 4 ]; then
  echo 'Usage: backup-be1-mysql.sh CONTAINER DATABASE ABS_CLIENT_CNF OUTPUT_DIRECTORY' >&2
  exit 2
fi
container=$1 database=$2 client_cnf=$3 output_dir=$4
[[ "$database" =~ ^[A-Za-z][A-Za-z0-9_]*$ ]] || { echo 'Invalid database name' >&2; exit 2; }
[[ "$client_cnf" == /* ]] || { echo 'Client option file must be absolute inside container' >&2; exit 2; }
# Resolve a unique container ID once; never select a default application container.
container_id=$(docker inspect -f '{{.Id}}' "$container")
test "$(docker inspect -f '{{.State.Running}}' "$container_id")" = true
mkdir -p "$output_dir"
output_dir=$(cd "$output_dir" && pwd)
staging=$(mktemp -d "$output_dir/.be1-backup.XXXXXX")
trap 'rm -rf -- "$staging"' EXIT
# InnoDB snapshot; no table locks, source modifications, password arguments, or GTID replay.
# DDL changes must be suspended for the duration of this backup.
docker exec "$container_id" mysqldump "--defaults-extra-file=$client_cnf" \
  --single-transaction --quick --no-tablespaces --set-gtid-purged=OFF \
  --routines --events --triggers --hex-blob --databases "$database" \
  > "$staging/database.sql"
test -s "$staging/database.sql"
(cd "$staging" && sha256sum database.sql > SHA256SUMS)
printf 'database=%s\ncreated_utc=%s\nsource_container_id=%s\n' \
  "$database" "$(date -u +%FT%TZ)" "$container_id" > "$staging/metadata.txt"
# Reserve a unique destination; publish the bundle only after a successful dump.
final=$(mktemp -d "$output_dir/be1-${database}-$(date -u +%Y%m%dT%H%M%SZ).XXXXXX")
if ! mv "$staging/database.sql" "$staging/SHA256SUMS" "$staging/metadata.txt" "$final/"; then
  echo "Bundle publication failed: $final" >&2; exit 1
fi
printf 'Backup bundle: %s\n' "$final"
