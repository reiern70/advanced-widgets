#!/bin/sh
# Builds the demo application here and puts it online:
#   deploy/deploy.sh ubuntu@<static-ip>
# The WAR is built on this machine, copied with the files of this folder to /opt/antilia-widgets
# on the instance, and started there with Docker Compose.
# The image running before is kept as antilia-widgets:previous, to go back to (see README.md), and
# the database is dumped first, so that the data before the migrations of the new version is kept.
set -eu

TARGET=${1:?usage: deploy/deploy.sh user@host}
DEPLOY=$(cd "$(dirname "$0")" && pwd)
ROOT=$(dirname "$DEPLOY")
REMOTE=/opt/antilia-widgets

cd "$ROOT"
mvn -q -pl demo-app -am clean package -DskipTests
WAR=$(ls -t demo-app/target/demo-app-*.war | head -n 1)

rsync -az --checksum "$WAR" "$TARGET:$REMOTE/ROOT.war"
rsync -az "$DEPLOY/Dockerfile" "$DEPLOY/compose.yaml" "$DEPLOY/Caddyfile" \
	"$DEPLOY/.env.example" "$DEPLOY/backup.sh" "$TARGET:$REMOTE/"

# the nightly backup of the database, see backup.sh
ssh "$TARGET" "sudo install -d -o \$USER -g \$USER -m 700 /var/backups/antilia-widgets \
	&& echo \"30 3 * * * \$USER $REMOTE/backup.sh >> /var/backups/antilia-widgets/backup.log 2>&1\" \
		| sudo tee /etc/cron.d/antilia-widgets > /dev/null \
	&& { chmod 600 $REMOTE/.env 2>/dev/null || true; }"

# Caddy sees its Caddyfile through a bind mount of the file, which rsync replaces by a new one:
# when the Caddyfile Caddy sees is not the one copied, the new one is validated in a container of
# its own and Caddy starts again on it; an invalid one stops the deployment, Caddy running on
# the old one
ssh "$TARGET" "cd $REMOTE \
	&& if [ ! -f .env ]; then cp .env.example .env; echo 'Fill in $REMOTE/.env, then deploy again.'; exit 1; fi \
	&& if docker compose ps --status running --services | grep -qx db; then ./backup.sh; fi \
	&& if docker image inspect antilia-widgets:latest >/dev/null 2>&1; then docker tag antilia-widgets:latest antilia-widgets:previous; fi \
	&& docker compose up -d --build \
	&& if ! docker compose exec -T caddy cat /etc/caddy/Caddyfile | cmp -s - Caddyfile; then \
		docker compose run --rm --no-deps -T caddy caddy validate --config /etc/caddy/Caddyfile --adapter caddyfile \
		&& docker compose up -d --force-recreate --no-deps caddy; fi \
	&& docker image prune -f >/dev/null \
	&& docker compose ps"
