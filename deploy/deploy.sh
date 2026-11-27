#!/bin/sh
# Builds the demo application here and puts it online:
#   deploy/deploy.sh ubuntu@<static-ip>
# The WAR is built on this machine, copied with the files of this folder to /opt/antilia-widgets
# on the instance, and started there by start.sh, which checks .env first and refuses settings
# that would open the site. The image running before is kept as antilia-widgets:previous, to go
# back to (see README.md). The image of Caddy, and the one of Jetty the image of the demo starts
# from, are pulled again every time: they are pinned by digest, which Dependabot moves on. There is
# no database: the site counts its visits in a text file a day on its volume. The token of the
# visits as JSON, which start.sh makes on the server, is copied the first time to
# ~/.config/antilia-widgets/stats-token, readable by its owner alone, for reading them with
#   curl -H "Authorization: Bearer $(cat ~/.config/antilia-widgets/stats-token)" https://<domain>/api/stats
set -eu

TARGET=${1:?usage: deploy/deploy.sh user@host}
DEPLOY=$(cd "$(dirname "$0")" && pwd)
ROOT=$(dirname "$DEPLOY")
REMOTE=/opt/antilia-widgets

cd "$ROOT"
mvn -q -pl demo-app -am clean package -DskipTests
WAR=$(ls -t demo-app/target/demo-app-*.war | head -n 1)

rsync -az --checksum "$WAR" "$TARGET:$REMOTE/ROOT.war"
rsync -az "$DEPLOY/Dockerfile" "$DEPLOY/.dockerignore" "$DEPLOY/compose.yaml" \
	"$DEPLOY/Caddyfile" "$DEPLOY/.env.example" "$DEPLOY/start.sh" "$TARGET:$REMOTE/"

ssh "$TARGET" "$REMOTE/start.sh"

# the token of /api/stats, once, never printed
TOKEN_FILE="$HOME/.config/antilia-widgets/stats-token"
if [ ! -s "$TOKEN_FILE" ]; then
	mkdir -p "$(dirname "$TOKEN_FILE")"
	(umask 077 && ssh "$TARGET" \
		"sed -n 's/^ANTILIA_WIDGETS_STATS_TOKEN=//p' $REMOTE/.env | tail -n 1" > "$TOKEN_FILE")
	if [ -s "$TOKEN_FILE" ]; then
		echo "deploy.sh: the token of /api/stats is in $TOKEN_FILE"
	else
		rm -f "$TOKEN_FILE"
	fi
fi
