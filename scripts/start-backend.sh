#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
# shellcheck disable=SC1090
[ -f "$HOME/.local/arogyalens-env.sh" ] && source "$HOME/.local/arogyalens-env.sh"
cd "$ROOT/backend"
# Load root .env if present
if [ -f "$ROOT/.env" ]; then
  set -a
  # shellcheck disable=SC1091
  source "$ROOT/.env"
  set +a
fi
exec mvn -Dmaven.repo.local="$ROOT/.m2" spring-boot:run
