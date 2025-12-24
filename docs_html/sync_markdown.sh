#!/bin/bash

# Script to sync markdown files from parent directory to docs_html

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PARENT_DIR="$(dirname "$SCRIPT_DIR")"

echo "🔄 Syncing markdown files from $PARENT_DIR to $SCRIPT_DIR"

# Copy all markdown files
cp "$PARENT_DIR"/*.md "$SCRIPT_DIR/" 2>/dev/null

# Count files
FILE_COUNT=$(ls -1 "$SCRIPT_DIR"/*.md 2>/dev/null | wc -l)

echo "✅ Synced $FILE_COUNT markdown files"
echo "📁 Files in docs_html:"
ls -1 "$SCRIPT_DIR"/*.md | xargs -n1 basename

