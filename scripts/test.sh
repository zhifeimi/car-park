#!/usr/bin/env bash
set -e

# Monorepo Test Runner
# Runs:
#   1. Backend unit and context tests (excluding @Tag("db"))
#   2. Deno lint across all TypeScript & Vue packages
#   3. Web Vite regression tests

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
export JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@25}"

echo "==========================================="
echo " Running Backend Tests..."
echo "==========================================="
cd "$ROOT_DIR/apps/backend"
./mvnw test
echo "✓ Backend tests passed!"

echo "==========================================="
echo " Running Deno Lint..."
echo "==========================================="
cd "$ROOT_DIR"
deno lint
echo "✓ Deno lint passed!"

echo "==========================================="
echo " Running Utils Envelope Tests..."
echo "==========================================="
cd "$ROOT_DIR/packages/utils"
deno task test
echo "✓ Utils envelope tests passed!"

echo "==========================================="
echo " Running Web Regression Tests..."
echo "==========================================="
cd "$ROOT_DIR/apps/web"
deno task test
echo "✓ Web regression tests passed!"

echo "==========================================="
echo " All tests passed!"
echo "==========================================="
