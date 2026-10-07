#!/usr/bin/env bash
# End-to-end check of a running dev node with the Ethereum registry:
# users -> on-chain group -> service publish/deploy -> faucet -> reputation profile -> rating.
set -uo pipefail
B=${BASE_URL:-http://localhost:8085}/las2peer
JAR=${SERVICE_JAR:-"$(cd "$(dirname "$0")" && pwd)/fixtures/i5.las2peer.services.templateService-1.0.0.jar"}
TMP=$(mktemp -d); trap 'rm -rf "$TMP"' EXIT
FAIL=0

check() { # name, expected substring, actual
  if [[ "$3" == *"$2"* ]]; then echo "ok   $1"; else echo "FAIL $1: ${3:0:300}"; FAIL=1; fi
}
login() { curl -s -c "$TMP/$1" -u "$1:$2" "$B/auth/login" >/dev/null; }

S=$RANDOM # unique suffix, so the test can run repeatedly against the same chain
U1=alice$S; U2=bobby$S
check "create $U1" '"code":200' "$(curl -s -m 60 -F username=$U1 -F email=$U1@example.org -F password=pw $B/agents/createAgent)"
check "create $U2" '"code":200' "$(curl -s -m 60 -F username=$U2 -F email=$U2@example.org -F password=pw $B/agents/createAgent)"
login $U1 pw; login $U2 pw
A1=$(curl -s -u $U1:pw $B/currentagent); A2=$(curl -s -u $U2:pw $B/currentagent)

# the user list is served from a cache the chain observer fills asynchronously
for _ in $(seq 1 15); do
  USERS=$(curl -s -m 30 -b $TMP/$U1 -X POST $B/eth/listAgents); [[ "$USERS" == *"$U1"* ]] && break; sleep 1
done
check "users on chain" "$U1" "$USERS"
check "create group" '"code":200' "$(curl -s -m 90 -b $TMP/$U1 -F name=group$S \
  -F "members=[{\"agentid\":\"$A1\"},{\"agentid\":\"$A2\"}]" $B/agents/createGroup)"

if [[ -f "$JAR" ]]; then
  UP=$(curl -s -m 120 -b $TMP/$U1 -F "jarfile=@$JAR" -F 'supplement={"name":"Template Service"}' $B/services/upload)
  [[ "$UP" == *"already known"* ]] && UP='"code":200 (already published)'
  check "publish service" '"code":200' "$UP"
  for _ in $(seq 1 30); do # filled asynchronously by the chain observer, like the user list
    RELEASES=$(curl -s -m 30 $B/services/releases); [[ "$RELEASES" == *templateService* ]] && break; sleep 1
  done
  check "service release on chain" "templateService" "$RELEASES"
fi

for u in $U1 $U2; do
  check "faucet $u" '"code":200' "$(curl -s -m 90 -b $TMP/$u -F groupID= $B/eth/requestFaucet)"
done
for u in $U1 $U2; do # rater and recipient both need a reputation profile
  check "register profile $u" '"code":200' "$(curl -s -m 90 -b $TMP/$u -X POST $B/eth/registerProfile)"
done
check "$U1 rates $U2" '"code":200' "$(curl -s -m 90 -b $TMP/$U1 -F agentid=$A2 -F rating=5 $B/eth/rateAgent)"

exit $FAIL
