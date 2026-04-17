#!/usr/bin/env bash

OUTPUT_FILE="${RUNNER_TEMP:-.}/RELEASE_NOTES.md"

LATEST_TAG=$(git describe --tags --abbrev=0 2>/dev/null)

if [ -z "$LATEST_TAG" ]; then
  echo "Error: Could not find any tag"
  exit 1
fi

PREVIOUS_TAG=$(git describe --tags --abbrev=0 "${LATEST_TAG}^" 2>/dev/null)

echo "# What's changed in ${LATEST_TAG}" > "$OUTPUT_FILE"

echo "" >> "$OUTPUT_FILE"

if [ -z "$PREVIOUS_TAG" ]; then
  git log "$LATEST_TAG" --reverse --pretty=format:"- %s" >> "$OUTPUT_FILE"
else
  git log "${PREVIOUS_TAG}..${LATEST_TAG}" --reverse --pretty=format:"- %s" >> "$OUTPUT_FILE"
fi

echo "" >> "$OUTPUT_FILE"

echo "Release Notes created."
