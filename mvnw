#!/usr/bin/env sh
set -eu

MAVEN_VERSION=3.9.9
BASE_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
MAVEN_HOME="$BASE_DIR/.mvn/apache-maven-$MAVEN_VERSION"
MAVEN_CMD="$MAVEN_HOME/bin/mvn"

if [ ! -x "$MAVEN_CMD" ]; then
  echo "Maven $MAVEN_VERSION not found, downloading to .mvn..."
  mkdir -p "$BASE_DIR/.mvn"
  curl -fsSL "https://archive.apache.org/dist/maven/maven-3/$MAVEN_VERSION/binaries/apache-maven-$MAVEN_VERSION-bin.tar.gz" \
    | tar -xz -C "$BASE_DIR/.mvn"
fi

exec "$MAVEN_CMD" "$@"
