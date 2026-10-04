#!/usr/bin/env bash
set -euo pipefail
PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$PROJECT_ROOT"
if [[ -x /usr/libexec/java_home ]]; then
  for TASK_JAVA_VERSION in 21 17 23; do
    if TASK_JAVA_PATH=$(/usr/libexec/java_home -v "$TASK_JAVA_VERSION" 2>/dev/null); then
      export JAVA_HOME="$TASK_JAVA_PATH"
      break
    fi
  done
fi
if [[ -d "$PROJECT_ROOT/.toolchain/android-sdk" ]]; then
  export ANDROID_HOME="$PROJECT_ROOT/.toolchain/android-sdk"
fi
export GRADLE_USER_HOME="${GRADLE_USER_HOME:-$PROJECT_ROOT/.toolchain/gradle}"
if [[ $# -eq 0 ]]; then
  set -- :core:test :app:lintRelease :app:assembleRelease
fi
exec ./gradlew "$@"
