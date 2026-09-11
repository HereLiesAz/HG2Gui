#!/bin/bash
# Retry git fetch with exponential backoff when DNS isn't up yet at session start.
# CCR's bootstrap runs before DNS is always ready; this hook ensures the working
# tree reflects origin even when the bootstrap fetch failed.

if ! git rev-parse --git-dir >/dev/null 2>&1; then
    exit 0
fi

remote=$(git remote | head -n1)
if [[ -z "$remote" ]]; then
    exit 0
fi

delay=2
for attempt in 1 2 3 4; do
    if git fetch "$remote" --quiet 2>/dev/null; then
        exit 0
    fi
    if [[ $attempt -lt 4 ]]; then
        sleep $delay
        delay=$((delay * 2))
    fi
done

# Non-fatal — session starts regardless; user will see stale state warning from stop hook.
exit 0
