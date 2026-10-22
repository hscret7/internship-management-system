#!/usr/bin/env bash
# Own disposable MySQL only. Never loads db.properties or targets internship_mysql.
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p target
container_id=""
cleanup() {
  if [ -n "$container_id" ]; then
    docker logs "$container_id" > target/be1-registration-mysql.log 2>&1 || true
    docker rm -f "$container_id" >/dev/null
  fi
}
trap cleanup EXIT
container_id=$(docker run -d --rm --label be1.registration.test=disposable \
  -p 127.0.0.1:13321:3306 \
  -e MYSQL_ALLOW_EMPTY_PASSWORD=yes -e MYSQL_ROOT_HOST=% mysql:8.0.46)
ready=false
for ((i=0;i<60;i++)); do
  if docker exec "$container_id" mysql --protocol=TCP -h127.0.0.1 -uroot -e 'SELECT 1' >/dev/null 2>&1; then
    ready=true;break
  fi
  sleep 2
done
if [ "$ready" != true ]; then
  echo "Disposable registration MySQL did not become ready" >&2
  exit 1
fi
docker exec "$container_id" mysql --protocol=TCP -h127.0.0.1 -uroot -e \
  'CREATE DATABASE be1_registration_test CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci'
mvn -Dbe1.registration.jdbc=true \
  -Dtest=RegistrationJdbcTest,RegistrationTransactionTest,RegistrationProfileTest,RegistrationServiceTest,RegisterServletTest,InternMeetingPageServletTest,AuthenticationFilterTest test
