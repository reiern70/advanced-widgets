#!/bin/sh
# Builds the demo application here and puts it online:
#   deploy/deploy.sh ubuntu@<static-ip>
# The WAR is built on this machine (Wicket 11 is a snapshot built here), copied with the files
# of this folder to /opt/antilia-widgets on the instance, and started there by start.sh, which
# checks .env first and refuses settings that would open the site. The image running before is
# kept as antilia-widgets:previous, to go back to (see README.md), and the database is dumped
# first, so that the data before the migrations of the new version is kept. The images of
# PostgreSQL and Caddy, and the one of Jetty the image of the demo starts from, are pulled again
# every time: they are pinned by digest, which Dependabot moves on.
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
	"$DEPLOY/.env.example" "$DEPLOY/backup.sh" "$DEPLOY/start.sh" "$DEPLOY/database-role.sql" \
	"$TARGET:$REMOTE/"

# the nightly backup of the database, see backup.sh
ssh "$TARGET" "sudo install -d -o \$USER -g \$USER -m 700 /var/backups/antilia-widgets \
	&& echo \"30 3 * * * \$USER $REMOTE/backup.sh >> /var/backups/antilia-widgets/backup.log 2>&1\" \
		| sudo tee /etc/cron.d/antilia-widgets > /dev/null"

ssh "$TARGET" "$REMOTE/start.sh"
