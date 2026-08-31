#!/bin/sh
set -eu
APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
DIST_VERSION=8.14.3
DIST_DIR="${HOME}/.gradle/wrapper/dists/gradle-${DIST_VERSION}-bin"
GRADLE_HOME="${DIST_DIR}/gradle-${DIST_VERSION}"
ZIP="${DIST_DIR}/gradle-${DIST_VERSION}-bin.zip"
if [ ! -x "${GRADLE_HOME}/bin/gradle" ]; then
  mkdir -p "${DIST_DIR}"
  if [ ! -f "${ZIP}" ]; then
    if command -v curl >/dev/null 2>&1; then curl -L --fail --retry 3 -o "${ZIP}" "https://services.gradle.org/distributions/gradle-${DIST_VERSION}-bin.zip"
    elif command -v wget >/dev/null 2>&1; then wget -O "${ZIP}" "https://services.gradle.org/distributions/gradle-${DIST_VERSION}-bin.zip"
    else echo "curl or wget is required to bootstrap Gradle ${DIST_VERSION}." >&2; exit 1; fi
  fi
  rm -rf "${GRADLE_HOME}"
  unzip -q "${ZIP}" -d "${DIST_DIR}"
fi
exec "${GRADLE_HOME}/bin/gradle" "$@"
