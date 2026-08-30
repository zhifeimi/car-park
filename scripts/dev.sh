#!/usr/bin/env bash
set -e

# Monorepo Development Runner
# Usage:
#   ./scripts/dev.sh backend   - Start Spring Boot backend (:8080)
#   ./scripts/dev.sh web       - Start Web Admin console (:5173)
#   ./scripts/dev.sh mobile    - Start Mobile H5 SPA (:5174)

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
TARGET="${1:-all}"

export JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@25}"

case "$TARGET" in
  backend)
    echo "Starting Backend on :8080..."
    cd "$ROOT_DIR/apps/backend" && ./mvnw spring-boot:run
    ;;
  web)
    echo "Starting Web Admin on :5173..."
    cd "$ROOT_DIR/apps/web" && deno task dev
    ;;
  mobile)
    echo "Starting Mobile H5 on :5174..."
    cd "$ROOT_DIR/apps/mobile" && deno task dev
    ;;
  *)
    echo "Usage: ./scripts/dev.sh [backend|web|mobile]"
    exit 1
    ;;
esac
