#!/bin/bash

# --- BACKEND ---
for dir in backend/*; do
  if [ -d "$dir/.git" ]; then
    echo "Pulling in $dir"
    git -C "$dir" pull origin main 2>/dev/null || git -C "$dir" pull origin master
  fi
done

# --- FRONTEND ---
for dir in frontend/*; do
  if [ -d "$dir/.git" ]; then
    echo "Pulling in $dir"
    git -C "$dir" pull origin main 2>/dev/null || git -C "$dir" pull origin master
  fi
done
