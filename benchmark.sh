#!/usr/bin/env bash
# Search latency benchmark. Run after the load generator has filled the index.
#   ./benchmark.sh [runs]
set -euo pipefail

API="${API:-http://localhost:8080}"
RUNS="${1:-30}"

QUERIES=(
  'level:ERROR AND service:billing-api AND response_time > 1000'
  'level:ERROR'
  'deadlock'
  '(level:ERROR OR level:WARN) AND NOT service:search-api'
  'status_code:500 AND response_time > 2000'
  '"connection pool exhausted"'
)

echo "Refreshing the searcher..."
curl -s -X POST "$API/api/refresh" | tr -d '\n'; echo
echo

for q in "${QUERIES[@]}"; do
  times=()
  total=""
  for _ in $(seq "$RUNS"); do
    body=$(curl -s -G "$API/api/search" \
      --data-urlencode "q=$q" \
      --data-urlencode "size=50" \
      --data-urlencode "histogram=false")
    t=$(printf '%s' "$body" | grep -o '"tookMs":[0-9.]*' | cut -d: -f2)
    total=$(printf '%s' "$body" | grep -o '"total":[0-9]*' | cut -d: -f2)
    times+=("$t")
  done

  printf '%s\n' "${times[@]}" | sort -n > /tmp/ls_times
  p50=$(awk 'NR==int(0.50*n)+1' n="$RUNS" /tmp/ls_times)
  p95=$(awk 'NR==int(0.95*n)+1' n="$RUNS" /tmp/ls_times)
  max=$(tail -1 /tmp/ls_times)

  printf 'query   : %s\n' "$q"
  printf 'matches : %s\n' "${total:-0}"
  printf 'p50     : %s ms\n' "${p50:-?}"
  printf 'p95     : %s ms\n' "${p95:-?}"
  printf 'max     : %s ms\n\n' "${max:-?}"
done

echo "Index stats:"
curl -s "$API/api/stats"; echo
