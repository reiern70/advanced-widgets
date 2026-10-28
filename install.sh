#!/usr/bin/env bash
# Releases the library, ui-components and its parent POM, as one version:
#
#   ./install.sh 10.11.0-RC2
#
# Run it in a terminal, from a clean checkout of the branch the version is released from
# (line-10.x for 10.x, master otherwise), as it is on origin. It asks first what to do, and the
# passwords it needs: the tokens of Maven Central and GitHub when ~/.m2/settings.xml has no server
# "central" or "github", and the passphrase of the GPG key, through gpg, whose agent keeps it for
# the signing. Then it:
#
#   1. builds everything and runs every test, with the version (skipped with --skip-tests);
#   2. signs and uploads the library to Maven Central (-Prelease), where it waits, validated, at
#      https://central.sonatype.com/publishing/deployments until published there; or is published
#      at once, if told so. A version on Central can never be removed or replaced;
#   3. uploads it to GitHub Packages, unless told not to;
#   4. tags the commit released, v<version>, and pushes the tag.
#
# The passwords are kept in the environment of this script alone: a settings file it writes for
# Maven, deleted at the end, only refers to them.
set -euo pipefail

usage() {
	echo "usage: ./install.sh [--skip-tests] <version>, 10.11.0-RC2 for example" >&2
	exit 2
}

SKIP_TESTS=false
VERSION=
for argument in "$@"; do
	case $argument in
		--skip-tests) SKIP_TESTS=true ;;
		-*) usage ;;
		*) [[ -z $VERSION ]] || usage; VERSION=$argument ;;
	esac
done
[[ -n $VERSION ]] || usage

ROOT=$(cd "$(dirname "$0")" && pwd)
cd "$ROOT"
TAG=v$VERSION
CENTRAL_URL=https://repo1.maven.org/maven2/com/antiliacode/widgets/ui-components
GITHUB_URL=https://maven.pkg.github.com/reiern70/advanced-widgets
SETTINGS=$HOME/.m2/settings.xml

fail() {
	echo "install.sh: $*" >&2
	exit 1
}

ask() {
	local answer
	read -r -p "$1 [y/N] " answer
	[[ $answer == [yY]* ]]
}

step() {
	printf '\n== %s\n' "$*"
}

has_server() {
	[[ -f $SETTINGS ]] && grep -q "<id>$1</id>" "$SETTINGS"
}

# --- what is released, from where

[[ -t 0 && -t 1 ]] || fail "run it in a terminal: it asks for passwords"
[[ $VERSION =~ ^[0-9]+\.[0-9]+\.[0-9]+(-RC[0-9]+)?$ ]] ||
	fail "$VERSION is no version such as 10.11.0 or 10.11.0-RC2"
BRANCH=$(git branch --show-current)
case $VERSION in
	10.*) [[ $BRANCH == line-10.x ]] || fail "10.x is released from line-10.x, not ${BRANCH:-a detached HEAD}" ;;
	*) [[ $BRANCH == master ]] || fail "$VERSION is released from master, not ${BRANCH:-a detached HEAD}" ;;
esac
[[ -z $(git status --porcelain --untracked-files=no) ]] || fail "commit the changes first"
if [[ -f .mvn/maven.config ]] && grep -q -- '-Drevision' .mvn/maven.config; then
	fail ".mvn/maven.config sets the revision: take it out for the release"
fi
git fetch -q origin
[[ $(git rev-parse HEAD) == $(git rev-parse "origin/$BRANCH") ]] ||
	fail "$BRANCH is not as on origin: pull or push first"
if git rev-parse -q --verify "refs/tags/$TAG" > /dev/null ||
	git ls-remote --exit-code --tags origin "refs/tags/$TAG" > /dev/null; then
	fail "$TAG exists already"
fi
if curl -sf -o /dev/null "$CENTRAL_URL/$VERSION/"; then
	fail "$VERSION is on Maven Central already"
fi
COMMIT=$(git rev-parse --short HEAD)

# --- the questions, all first

echo "Releasing ui-components $VERSION from $BRANCH at $COMMIT: $(git log -1 --format=%s | cut -c1-80)"
PUBLISH=false
if ask "Publish it on Maven Central at once, once validated? It can never be taken back."; then
	PUBLISH=true
fi
GITHUB=true
if ! ask "Upload it to GitHub Packages too?"; then
	GITHUB=false
fi
ask "Go on with $VERSION?" || fail "stopped, nothing done"

# the tokens ~/.m2/settings.xml has not, in a settings file of this run referring to them
MAVEN_SETTINGS=()
MISSING=()
has_server central || MISSING+=(central)
[[ $GITHUB == false ]] || has_server github || MISSING+=(github)
if (( ${#MISSING[@]} )); then
	TEMPORARY=$(mktemp -t antilia-settings.XXXXXX.xml)
	trap 'rm -f "$TEMPORARY"' EXIT
	SERVERS=
	for server in "${MISSING[@]}"; do
		case $server in
			central)
				echo "Maven Central: a user token of https://central.sonatype.com/account"
				read -r -p "  Its user name: " INSTALL_CENTRAL_USER
				read -r -s -p "  Its password: " INSTALL_CENTRAL_PASSWORD
				echo
				export INSTALL_CENTRAL_USER INSTALL_CENTRAL_PASSWORD
				SERVERS+="<server><id>central</id><username>\${env.INSTALL_CENTRAL_USER}</username><password>\${env.INSTALL_CENTRAL_PASSWORD}</password></server>"
				;;
			github)
				echo "GitHub Packages: a personal access token (classic) with write:packages"
				read -r -p "  Your GitHub user: " INSTALL_GITHUB_USER
				read -r -s -p "  The token: " INSTALL_GITHUB_TOKEN
				echo
				export INSTALL_GITHUB_USER INSTALL_GITHUB_TOKEN
				SERVERS+="<server><id>github</id><username>\${env.INSTALL_GITHUB_USER}</username><password>\${env.INSTALL_GITHUB_TOKEN}</password></server>"
				;;
		esac
	done
	# the user's settings with the servers missing, or settings of these servers alone
	if [[ -f $SETTINGS ]] && grep -q '</servers>' "$SETTINGS"; then
		awk -v servers="$SERVERS" '{ sub(/<\/servers>/, servers "</servers>") } 1' "$SETTINGS" > "$TEMPORARY"
	elif [[ -f $SETTINGS ]]; then
		awk -v servers="$SERVERS" '{ sub(/<\/settings>/, "<servers>" servers "</servers></settings>") } 1' \
			"$SETTINGS" > "$TEMPORARY"
	else
		printf '<settings><servers>%s</servers></settings>\n' "$SERVERS" > "$TEMPORARY"
	fi
	chmod 600 "$TEMPORARY"
	MAVEN_SETTINGS=(-s "$TEMPORARY")
fi

# the settings of this run, if any; an empty array is unbound to an older bash under set -u
MAVEN=(mvn -B ${MAVEN_SETTINGS[@]+"${MAVEN_SETTINGS[@]}"} "-Drevision=$VERSION")

# --- 1. the build, with every test

if [[ $SKIP_TESTS == true ]]; then
	step "1. The build and the tests: skipped"
else
	step "1. Building $VERSION and running every test"
	"${MAVEN[@]}" clean install
fi

# --- 2. Maven Central, signed

step "2. Signing and uploading to Maven Central"
export GPG_TTY
GPG_TTY=$(tty)
echo "The passphrase of the GPG key, which signs every file (gpg keeps it while this runs):"
echo "$VERSION" | gpg --clearsign > /dev/null || fail "gpg could not sign"
"${MAVEN[@]}" -Prelease "-Dcentral.autoPublish=$PUBLISH" -DskipTests -pl ui-components -am deploy

# --- 3. GitHub Packages

if [[ $GITHUB == true ]]; then
	step "3. Uploading to GitHub Packages"
	"${MAVEN[@]}" -DskipTests -pl ui-components -am deploy \
		"-DaltDeploymentRepository=github::$GITHUB_URL"
else
	step "3. GitHub Packages: not asked"
fi

# --- 4. the tag

step "4. Tagging $COMMIT as $TAG"
git tag -a "$TAG" -m "$VERSION: ui-components on Wicket $("${MAVEN[@]}" -q help:evaluate \
	-Dexpression=wicket.version -DforceStdout), on Maven Central" HEAD
git push origin "$TAG"

echo
if [[ $PUBLISH == true ]]; then
	echo "Done: $VERSION is publishing on Maven Central; it shows at $CENTRAL_URL/$VERSION/ within the hour."
else
	echo "Done: $VERSION waits, validated, at https://central.sonatype.com/publishing/deployments:"
	echo "publish it there, or drop it to release it anew."
fi
