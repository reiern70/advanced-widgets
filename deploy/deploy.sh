#!/bin/sh
# Builds the demo application here and puts it online:
#   deploy/deploy.sh ubuntu@<static-ip>
# The WAR is built on this machine (Wicket 11 is a snapshot built here), copied with the files
# of this folder to /opt/antilia-widgets on the instance, and started there with Docker Compose.
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
	"$DEPLOY/.env.example" "$TARGET:$REMOTE/"

ssh "$TARGET" "cd $REMOTE \
	&& if [ ! -f .env ]; then cp .env.example .env; echo 'Fill in $REMOTE/.env, then deploy again.'; exit 1; fi \
	&& docker compose up -d --build \
	&& docker image prune -f >/dev/null \
	&& docker compose ps"
