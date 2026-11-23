#!/bin/sh
# Starts the version deploy.sh copied to /opt/antilia-widgets, on the server; deploy.sh runs it
# over SSH. It checks .env first and stops before touching anything when a setting would open the
# site: the demo must run in DEPLOYMENT mode, without the switches meant for a developer's machine,
# and with no value change-me left from .env.example. No value of .env is ever printed.
set -eu

cd "$(dirname "$0")"

fail() {
	echo "start.sh: $*" >&2
	exit 1
}

# .env, readable by its owner alone from the start: made from the example the first time
if [ ! -f .env ]; then
	install -m 600 .env.example .env
	echo "Fill in $(pwd)/.env, then deploy again."
	exit 1
fi
chmod 600 .env

# the lines of .env that count, its comments left out
settings() {
	grep -v '^[[:space:]]*#' .env || true
}

if settings | grep -q 'change-me'; then
	fail ".env still holds a value change-me: make each one up, e.g. with openssl rand -hex 24"
fi

# DEPLOYMENT mode, in the JAVA_OPTIONS Docker Compose takes, the last ones of .env
JAVA_OPTIONS_LINE=$(settings | grep -E '^[[:space:]]*JAVA_OPTIONS=' | tail -n 1)
if ! printf '%s\n' "$JAVA_OPTIONS_LINE" | grep -q -- '-Dwicket.configuration=deployment'; then
	fail "JAVA_OPTIONS in .env must hold -Dwicket.configuration=deployment"
fi
if printf '%s\n' "$JAVA_OPTIONS_LINE" | grep -Eo -- '-Dwicket\.configuration=[^[:space:]"'\'']*' \
	| grep -qvx -- '-Dwicket.configuration=deployment'; then
	fail "JAVA_OPTIONS in .env must set no other -Dwicket.configuration than deployment"
fi

# the switches of a developer's machine: everybody signed in as an account, or the demos open to
# everybody
if settings | grep -Eq -- '-Dantilia-widgets\.(sign-in-as|open-demos)([=[:space:]"'\'']|$)'; then
	fail ".env must not set -Dantilia-widgets.sign-in-as or -Dantilia-widgets.open-demos"
fi

# the image running before, to go back to (see README.md)
if docker image inspect antilia-widgets:latest > /dev/null 2>&1; then
	docker tag antilia-widgets:latest antilia-widgets:previous
fi

# the images pinned in compose.yaml and the Dockerfile, pulled again
docker compose pull --quiet caddy
docker compose build --pull app

# the application and Caddy; the database container of earlier versions, which compose.yaml no
# longer has, goes
docker compose up -d --remove-orphans

# Earlier versions kept the users and the accesses in PostgreSQL, dumped every night: since issue
# #174 the site keeps no record of anybody, so what they left is deleted, once, for good: the
# database's volume, the dumps with their nightly job, and the files they ran on.
if docker volume inspect antilia-widgets_database > /dev/null 2>&1; then
	docker volume rm antilia-widgets_database
	echo "start.sh: deleted the database of earlier versions"
fi
if [ -e /etc/cron.d/antilia-widgets ] || [ -e /var/backups/antilia-widgets ]; then
	sudo rm -f /etc/cron.d/antilia-widgets
	sudo rm -rf /var/backups/antilia-widgets
	echo "start.sh: deleted the dumps of the database and their nightly job"
fi
rm -f backup.sh database-role.sql

# Caddy sees its Caddyfile through a bind mount of the file, which rsync replaces by a new one:
# when the Caddyfile Caddy sees is not the one copied, the new one is validated in a container of
# its own and Caddy starts again on it; an invalid one stops the deployment, Caddy running on
# the old one
if ! docker compose exec -T caddy cat /etc/caddy/Caddyfile | cmp -s - Caddyfile; then
	docker compose run --rm --no-deps -T caddy caddy validate --config /etc/caddy/Caddyfile \
		--adapter caddyfile
	docker compose up -d --force-recreate --no-deps caddy
fi

docker image prune -f > /dev/null
docker compose ps
