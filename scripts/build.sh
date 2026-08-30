#!/usr/bin/env bash
set -e

# Monorepo Production Build Script
# Builds all apps:
#   1. Backend -> apps/backend/target/car-park-server-*.jar
#   2. Web Admin -> apps/web/dist/
#   3. Mobile H5 -> apps/mobile/dist/

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
export JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@25}"

echo "==========================================="
echo " Building Backend JAR..."
echo "==========================================="
cd "$ROOT_DIR/apps/backend"
./mvnw clean package -DskipTests
echo "✓ Backend built: $(ls target/car-park-server*.jar | grep -v original)"

echo "==========================================="
echo " Building Web Admin..."
echo "==========================================="
cd "$ROOT_DIR/apps/web"
deno task build
echo "✓ Web Admin built to apps/web/dist/"

echo "==========================================="
echo " Building Mobile H5..."
echo "==========================================="
cd "$ROOT_DIR/apps/mobile"
deno task build
echo "✓ Mobile H5 built to apps/mobile/dist/"

echo "==========================================="
echo " All builds completed successfully!"
echo "==========================================="
