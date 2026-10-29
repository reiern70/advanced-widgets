#!/bin/sh
# Dumps the database of the site to /var/backups/antilia-widgets, every night by cron (deploy.sh
# installs /etc/cron.d/antilia-widgets), and keeps the dumps of the last 14 days. A dump is kept
# only once pg_restore reads it. With BACKUP_AGE_RECIPIENT in .env, an age public key, the dump is
# encrypted to it, widgets-<date>.dump.age, and the plain one removed: the private key, which
# alone reads it, stays off the server. Without one the dump stays plain, and every run says so
# in its output, which cron appends to backup.log. Restoring is in README.md.
set -eu

BACKUPS=${ANTILIA_WIDGETS_BACKUPS:-/var/backups/antilia-widgets}
KEEP_DAYS=14

cd "$(dirname "$0")"
umask 077
DUMP="$BACKUPS/widgets-$(date +%F-%H%M).dump"
# no plain or half written file left behind when a step fails
trap 'rm -f "$DUMP.part" "$DUMP.age.part"' EXIT

# the public key of .env, its last line, without quotes; a public key is no secret
RECIPIENT=$(grep -E '^[[:space:]]*BACKUP_AGE_RECIPIENT=' .env 2> /dev/null | tail -n 1 \
	| sed -e 's/^[^=]*=//' -e 's/^"\(.*\)"$/\1/' -e "s/^'\(.*\)'$/\1/" -e 's/[[:space:]]*$//')
if [ -n "$RECIPIENT" ] && ! command -v age > /dev/null; then
	echo "$(date -Is) ERROR: BACKUP_AGE_RECIPIENT is set, but age is not installed: no dump" \
		"made. Run setup-server.sh again, or: sudo apt-get install age" >&2
	exit 1
fi

docker compose exec -T db sh -c 'pg_dump -U "$POSTGRES_USER" -Fc "$POSTGRES_DB"' > "$DUMP.part"
docker compose exec -T db pg_restore --list < "$DUMP.part" > /dev/null

if [ -n "$RECIPIENT" ]; then
	age -r "$RECIPIENT" -o "$DUMP.age.part" "$DUMP.part"
	rm -f "$DUMP.part"
	mv "$DUMP.age.part" "$DUMP.age"
	FILE="$DUMP.age"
else
	mv "$DUMP.part" "$DUMP"
	FILE=$DUMP
	echo "$(date -Is) WARNING: $FILE is NOT ENCRYPTED: it holds emails, names, addresses and" \
		"password hashes in plain. Set BACKUP_AGE_RECIPIENT in .env, see README.md."
fi
find "$BACKUPS" \( -name 'widgets-*.dump' -o -name 'widgets-*.dump.age' \) -mtime +$KEEP_DAYS \
	-delete

echo "$(date -Is) $FILE $(du -h "$FILE" | cut -f1)"
