#!/bin/bash
set -e

echo "==============================="
echo "Installing node modules for all services..."
echo "==============================="

# --- BACKEND ---
for dir in ../backend/*; do
  if [ -f "$dir/package.json" ]; then
    echo
    echo "Installing in $dir..."
    (cd "$dir" && npm install)
  fi
done

# --- FRONTEND ---
for dir in ../frontend/*; do
  if [ -f "$dir/package.json" ]; then
    echo
    echo "Installing in $dir..."
    (cd "$dir" && npm install)
  fi
done

echo
echo "✅ All node modules installed!"