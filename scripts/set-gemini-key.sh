#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
ENV_FILE="$ROOT/.env"

if [[ $# -lt 1 || -z "${1:-}" ]]; then
  echo "Usage: ./scripts/set-gemini-key.sh YOUR_GEMINI_API_KEY"
  echo "Get a key from: https://aistudio.google.com/apikey"
  exit 1
fi

KEY="$1"

if [[ ! -f "$ENV_FILE" ]]; then
  cp "$ROOT/.env.example" "$ENV_FILE"
fi

python3 - "$ENV_FILE" "$KEY" <<'PY'
from pathlib import Path
import sys
path = Path(sys.argv[1])
key = sys.argv[2]
lines = path.read_text(encoding='utf-8').splitlines() if path.exists() else []
out = []
found = False
for line in lines:
    if line.startswith('GEMINI_API_KEY='):
        out.append(f'GEMINI_API_KEY={key}')
        found = True
    else:
        out.append(line)
if not found:
    out.append(f'GEMINI_API_KEY={key}')
# ensure defaults
text = '\n'.join(out) + '\n'
for required in [
    ('GEMINI_MODEL=', 'GEMINI_MODEL=gemini-2.0-flash'),
    ('AI_ENABLED=', 'AI_ENABLED=true'),
    ('SERVER_PORT=', 'SERVER_PORT=8088'),
    ('DEMO_ENABLED=', 'DEMO_ENABLED=false'),
]:
    if required[0] not in text:
        text += required[1] + '\n'
path.write_text(text, encoding='utf-8')
print(f'Updated {path}')
PY

echo "Restart the backend for the key to take effect."
echo "  source \"\$HOME/.local/arogyalens-env.sh\""
echo "  cd \"$ROOT/backend\" && mvn -Dmaven.repo.local=\"$ROOT/.m2\" spring-boot:run"
