#!/usr/bin/env bash
# Génère docs/RAPPORT.pdf si pandoc est disponible
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
if command -v pandoc >/dev/null 2>&1; then
  pandoc docs/RAPPORT.md docs/architecture.md docs/devops-answers.md \
    -o docs/RAPPORT.pdf \
    --metadata title="Rapport Test ADRIA - ALOUANE Ayoub" \
    -V geometry:margin=1in
  echo "PDF généré: docs/RAPPORT.pdf"
else
  python - <<'PY'
from pathlib import Path
# Fallback: concat markdown ready for print-to-PDF
parts = [
    Path("docs/RAPPORT.md").read_text(encoding="utf-8"),
    "\n\n---\n\n",
    Path("docs/architecture.md").read_text(encoding="utf-8"),
    "\n\n---\n\n",
    Path("docs/devops-answers.md").read_text(encoding="utf-8"),
]
out = Path("docs/RAPPORT_COMPLET.md")
out.write_text("".join(parts), encoding="utf-8")
print(f"pandoc absent — rapport Markdown complet: {out}")
print("Ouvrez-le et exportez en PDF (VS Code / browser print).")
PY
fi
