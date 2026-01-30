#!/usr/bin/env bash
set -euo pipefail

# run-haitale.sh
# Small helper to run the HaiTale JAR downloaded from Releases.
# Usage:
#   ./run-haitale.sh [path-to-jar] <haitale-args...>
# If no JAR path is provided, the script will try to find a haitale-*.jar in the current dir.

# Prompt for API key if not set
if [ -z "${OPENROUTER_API_KEY:-}" ]; then
  read -s -p "Enter your OpenRouter API key (input hidden): " OPENROUTER_API_KEY
  echo
  export OPENROUTER_API_KEY
fi

# Determine JAR
if [ "$#" -ge 1 ] && [[ "${1}" == *.jar ]]; then
  JAR="$1"
  shift
else
  # try to find a haitale-*.jar first
  JAR=$(ls haitale-*.jar 2>/dev/null | head -n 1 || true)
  if [ -z "$JAR" ]; then
    JAR=$(ls *.jar 2>/dev/null | head -n 1 || true)
  fi
fi

if [ -z "${JAR:-}" ]; then
  echo "No JAR found in current directory. Download the release JAR from GitHub Releases and place it here, or pass the path as the first argument."
  exit 1
fi

# Run the JAR with any remaining arguments
exec java -jar "$JAR" "$@"
