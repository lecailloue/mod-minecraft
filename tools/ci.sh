#!/bin/bash
# Attend la fin du dernier run CI puis recupere la branche ci-results-barad dans /tmp/ci
cd /home/claude/mod-minecraft
SHA=$(git rev-parse HEAD)
for i in $(seq 1 110); do
  R=$(gh api "repos/lecailloue/mod-minecraft/actions/runs?head_sha=$SHA" --jq '.workflow_runs[0] | "\(.status) \(.conclusion)"' 2>/dev/null)
  case "$R" in completed*) break;; esac
  sleep 15
done
echo "run: $R"
rm -rf /tmp/ci && git fetch -q origin ci-results-barad 2>/dev/null && git worktree prune && git worktree add -q -f /tmp/ci FETCH_HEAD 2>/dev/null
cat /tmp/ci/STATUS 2>/dev/null; echo "commit: $(cat /tmp/ci/COMMIT 2>/dev/null) (head $SHA)"; ls /tmp/ci 2>/dev/null

if [ -f /tmp/ci/build.log ]; then grep -E "error:|FAILED" /tmp/ci/build.log | head -${1:-60}; fi
