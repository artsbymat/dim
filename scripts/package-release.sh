#!/usr/bin/env bash
set -euo pipefail

cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.."

if [[ ! -s keystore.properties ]]; then
    printf '%s\n' 'Isi keystore.properties di root proyek sebelum membuat paket rilis.' >&2
    exit 1
fi

./gradlew --no-configuration-cache :app:assembleRelease :app:bundleRelease "$@"

printf '%s\n' \
    'app/build/outputs/apk/release/app-release.apk' \
    'app/build/outputs/bundle/release/app-release.aab'
