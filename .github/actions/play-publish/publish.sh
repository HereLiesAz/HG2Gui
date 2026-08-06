#!/usr/bin/env bash
set -euo pipefail

package="$1"
aab_glob="$2"
token_file="$3"
expected_vc="$4"
completed_track="$5"
draft_tracks="$6"
release_name="$7"
release_notes="$8"
out_file="$9"

# Resolve aab (glob)
aab=""
for f in $aab_glob; do
  if [ -f "$f" ]; then aab="$f"; break; fi
done
if [ -z "$aab" ]; then
  echo "::error::AAB not found (tried glob: $aab_glob)"
  exit 1
fi

if [ ! -f "$token_file" ]; then
  echo "::error::Token file not found: $token_file"
  exit 1
fi
token="$(cat "$token_file")"
if [ -z "$token" ]; then
  echo "::error::Token file is empty"
  exit 1
fi

api="https://androidpublisher.googleapis.com/androidpublisher/v3/applications/$package"

# helpers
req() {
  # $1 = method, $2 = url, $3 = out-file (or -), $4 = stdin-file (optional), $5 = content-type optional
  method="$1"
  url="$2"
  out="$3"
  datafile="${4:-}"
  ctype="${5:-application/json}"
  if [ -n "$datafile" ]; then
    http=$(curl -sS -w "%{http_code}" -o "$out" -X "$method" \
      -H "Authorization: Bearer $token" -H "Content-Type: $ctype" --data-binary @"$datafile" "$url")
  else
    http=$(curl -sS -w "%{http_code}" -o "$out" -X "$method" -H "Authorization: Bearer $token" -H "Content-Type: $ctype" "$url")
  fi
  echo "$http"
}

tmp="$RUNNER_TEMP/play-pub"
mkdir -p "$tmp"
edit_json="$tmp/edit.json"
upload_resp="$tmp/upload.json"

# 1) Create an edit
http=$(req POST "$api/edits" "$edit_json")
if [ "$http" != "200" ] && [ "$http" != "201" ]; then
  echo "::error::Play API edits.insert -> HTTP $http"
  echo "Response:"
  cat "$edit_json"
  exit 1
fi
edit_id=$(jq -r '.id // empty' "$edit_json")
if [ -z "$edit_id" ]; then
  echo "::error::could not read edit id"
  cat "$edit_json"
  exit 1
fi

# 2) Upload the AAB (media upload) - use Google's upload host
upload_base="https://www.googleapis.com/upload/androidpublisher/v3/applications/$package"
upload_url="$upload_base/edits/${edit_id}/bundles?uploadType=media"
http=$(curl -sS -w "%{http_code}" -o "$upload_resp" -X POST \
  -H "Authorization: Bearer $token" \
  -H "Content-Type: application/octet-stream" \
  --data-binary @"$aab" \
  "$upload_url")
if [ "$http" != "200" ] && [ "$http" != "201" ]; then
  echo "::error::Bundle upload -> HTTP $http"
  echo "Response:"
  cat "$upload_resp"
  # attempt to delete edit to avoid leaking partial state
  curl -sS -o /dev/null -X DELETE -H "Authorization: Bearer $token" "$api/edits/${edit_id}" || true
  exit 1
fi

version_code="$(jq -r '.versionCode // empty' "$upload_resp")"
if [ -z "$version_code" ]; then
  echo "::error::Could not find versionCode in upload response"
  cat "$upload_resp"
  curl -sS -o /dev/null -X DELETE -H "Authorization: Bearer $token" "$api/edits/${edit_id}" || true
  exit 1
fi

# Optional sanity check
if [ -n "$expected_vc" ] && [ "$expected_vc" != "$version_code" ]; then
  echo "::error::Expected VERSION_CODE $expected_vc but upload produced $version_code"
  curl -sS -o /dev/null -X DELETE -H "Authorization: Bearer $token" "$api/edits/${edit_id}" || true
  exit 1
fi

# Build release JSON helper (status = completed/draft)
make_release() {
  status="$1"   # completed or draft
  ver="$2"
  name="$3"
  notes="$4"
  # releaseNotes array with en-US only
  if [ -n "$notes" ]; then
    printf '{"name":%s,"versionCodes":[%s],"status":"%s","releaseNotes":[{"language":"en-US","text":%s}]}' \
      "$(jq -R -s . <<< "$name")" "$ver" "$status" "$(jq -R -s . <<< "$notes")"
  else
    printf '{"name":%s,"versionCodes":[%s],"status":"%s"}' "$(jq -R -s . <<< "$name")" "$ver" "$status"
  fi
}

drafted_arr=()
rolled_out=""

# 3) Put draft tracks
if [ -n "$draft_tracks" ]; then
  for tr in $draft_tracks; do
    release_json="$tmp/release-${tr}.json"
    rel=$(make_release draft "$version_code" "$release_name" "$release_notes")
    jq -n --argjson r "$rel" '{"releases": [$r]}' > "$release_json" 2>/dev/null || echo "{\"releases\":[$rel]}" > "$release_json"
    url="$api/edits/${edit_id}/tracks/${tr}"
    http=$(req PUT "$url" "$tmp/track-${tr}.json" "$release_json")
    if [ "$http" != "200" ]; then
      echo "::warning::Could not stage draft on $tr (HTTP $http). Response:"
      cat "$tmp/track-${tr}.json"
      # do not fail whole run on some drafts; warn and continue
    else
      drafted_arr+=("$tr")
    fi
  done
fi

# 4) Put completed track (rollout)
if [ -n "$completed_track" ]; then
  release_json="$tmp/release-${completed_track}.json"
  rel=$(make_release completed "$version_code" "$release_name" "$release_notes")
  jq -n --argjson r "$rel" '{"releases": [$r]}' > "$release_json" 2>/dev/null || echo "{\"releases\":[$rel]}" > "$release_json"
  url="$api/edits/${edit_id}/tracks/${completed_track}"
  http=$(req PUT "$url" "$tmp/track-${completed_track}.json" "$release_json")
  if [ "$http" != "200" ]; then
    echo "::error::Failed to set completed track $completed_track (HTTP $http)"
    echo "Response:"
    cat "$tmp/track-${completed_track}.json"
    # cleanup
    curl -sS -o /dev/null -X DELETE -H "Authorization: Bearer $token" "$api/edits/${edit_id}" || true
    exit 1
  else
    rolled_out="$completed_track"
  fi
fi

# 5) Commit the edit
commit_out="$tmp/commit.json"
http=$(req POST "$api/edits/${edit_id}:commit" "$commit_out")
if [ "$http" != "200" ] && [ "$http" != "201" ]; then
  echo "::error::Failed to commit edit (HTTP $http)"
  echo "Response:"
  cat "$commit_out"
  exit 1
fi

# Write structured output
jq -n --arg vc "$version_code" --arg rolled "$rolled_out" --argjson drafted "$(jq -R -s -c 'split(" ") | map(select(length>0))' <<< "${drafted_arr[*]}")" \
  '{versionCode: ($vc|tonumber), rolledOut: $rolled, drafted: $drafted}' > "$out_file"

echo "Published versionCode $version_code; rolled out: $rolled_out; drafted: ${drafted_arr[*]}"
