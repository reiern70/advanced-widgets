#!/bin/sh
# Dumps the database of the site to /var/backups/antilia-widgets, every night by cron (deploy.sh
# installs /etc/cron.d/antilia-widgets), and keeps the dumps of the last 14 days. A dump is kept
# only once pg_restore reads it. Restore one with:
#   docker compose exec -T db sh -c 'pg_restore -U "$POSTGRES_USER" -d "$POSTGRES_DB" --clean --if-exists' < widgets-<date>.dump
set -eu

BACKUPS=/var/backups/antilia-widgets
KEEP_DAYS=14

cd /opt/antilia-widgets
FILE="$BACKUPS/widgets-$(date +%F-%H%M).dump"

docker compose exec -T db sh -c 'pg_dump -U "$POSTGRES_USER" -Fc "$POSTGRES_DB"' > "$FILE.part"
docker compose exec -T db pg_restore --list < "$FILE.part" > /dev/null
mv "$FILE.part" "$FILE"
find "$BACKUPS" -name 'widgets-*.dump' -mtime +$KEEP_DAYS -delete

echo "$(date -Is) $FILE $(du -h "$FILE" | cut -f1)"
