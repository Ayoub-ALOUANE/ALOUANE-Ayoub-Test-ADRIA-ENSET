#!/usr/bin/env bash
set -euo pipefail
BASE_ACC=${ACCOUNT_URL:-http://localhost:8081}
BASE_TX=${TRANSACTION_URL:-http://localhost:8082}
BASE_AI=${AI_URL:-http://localhost:8084}

echo "Accounts:" && curl -sf "$BASE_ACC/api/accounts" | head -c 200 && echo
ACC=$(curl -sf "$BASE_ACC/api/accounts" | python -c "import sys,json; print([a['id'] for a in json.load(sys.stdin) if a['status']=='ACTIVE'][0])")
curl -sf -X POST "$BASE_TX/api/transactions" -H "Content-Type: application/json" \
  -d "{\"accountId\":\"$ACC\",\"amount\":12.3,\"currency\":\"EUR\",\"merchantCategory\":\"TEST\",\"country\":\"FR\"}" >/dev/null
echo "Transaction OK"
curl -sf -X POST "$BASE_AI/api/chat" -H "Content-Type: application/json" -d '{"message":"help"}' >/dev/null
echo "Chat OK"
echo "Smoke test passed"
