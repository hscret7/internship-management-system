#!/usr/bin/env bash
# Disposable MySQL only. Does not use internship_mysql or db.properties.
set -euo pipefail
cd "$(dirname "$0")/.."
container_id=""
cleanup() {
  if [ -n "$container_id" ]; then
    docker logs "$container_id" > target/be1-inapp-mysql.log 2>&1 || true
    docker rm -f "$container_id" >/dev/null
  fi
}
mkdir -p target
trap cleanup EXIT
container_id=$(docker run -d --rm --label be1.inapp.test=disposable \
  -p 127.0.0.1:13319:3306 \
  -e MYSQL_ALLOW_EMPTY_PASSWORD=yes -e MYSQL_ROOT_HOST=% mysql:8.0.46)
ready=false
for ((i=0;i<60;i++)); do
  if docker exec "$container_id" mysql --protocol=TCP -h127.0.0.1 -uroot -e 'SELECT 1' >/dev/null 2>&1; then
    ready=true;break
  fi
  sleep 2
done
if [ "$ready" != true ]; then
  echo "Disposable MySQL did not become ready" >&2
  exit 1
fi
docker exec "$container_id" mysql --protocol=TCP -h127.0.0.1 -uroot -e \
  'CREATE DATABASE be1_inapp_test CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci'
mvn -Duser.timezone=Asia/Ho_Chi_Minh -Dbe1.inapp.jdbc=true \
  -Dtest=InAppNotificationJdbcTest,InAppNotificationServiceTest,NotificationEventTest,NotificationServletTest,MeetingInboxTransactionTest,MeetingNotificationTransactionTest,MeetingServiceTest,MeetingServletTest,MeetingNotificationTest test
