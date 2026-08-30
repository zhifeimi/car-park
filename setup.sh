#!/usr/bin/env bash
set -euo pipefail

# CarPark / 停车场预约管理系统 — first-time setup
# Usage: ./setup.sh

ROOT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT_DIR"

printf '%s\n' '=== CarPark / 停车场预约管理系统 Setup ==='
printf '%s\n' "Project directory: $ROOT_DIR"
printf '%s\n' ''

printf '%s\n' 'Checking prerequisites...'

if [[ -n "${JAVA_HOME:-}" ]]; then
  export JAVA_HOME
elif [[ -d "/opt/homebrew/opt/openjdk@25" ]]; then
  export JAVA_HOME="/opt/homebrew/opt/openjdk@25"
else
  printf '%s\n' 'Error: JDK 25 is required.' >&2
  printf '%s\n' 'Set JAVA_HOME to a JDK 25 installation or install it at /opt/homebrew/opt/openjdk@25.' >&2
  exit 1
fi

if [[ ! -x "$JAVA_HOME/bin/java" ]]; then
  printf '%s\n' "Error: JAVA_HOME does not contain an executable Java runtime: $JAVA_HOME" >&2
  exit 1
fi

JAVA_VERSION_LINE="$("$JAVA_HOME/bin/java" -version 2>&1 | { IFS= read -r line; printf '%s' "$line"; })"
if [[ "$JAVA_VERSION_LINE" != *'"25'* ]]; then
  printf '%s\n' "Error: JDK 25 is required; found: $JAVA_VERSION_LINE" >&2
  exit 1
fi
printf '%s\n' "  Java: $JAVA_VERSION_LINE"

if ! command -v deno >/dev/null 2>&1; then
  printf '%s\n' 'Error: Deno is required for the web and mobile apps.' >&2
  printf '%s\n' 'Install Deno 2.x, then run this script again.' >&2
  exit 1
fi
printf '%s\n' "  Deno: $(deno --version | { IFS= read -r line; printf '%s' "$line"; })"

if [[ ! -f .env ]]; then
  cp .env.example .env
  printf '%s\n' '  Created .env from .env.example.'
  printf '%s\n' '  The dev profile works with its built-in local defaults; edit .env only when you need custom settings.'
else
  printf '%s\n' '  Keeping existing .env.'
fi

if command -v docker >/dev/null 2>&1 && docker compose version >/dev/null 2>&1; then
  if docker info >/dev/null 2>&1; then
    printf '%s\n' 'Starting local MySQL with Docker Compose...'
    docker compose up -d
    printf '%s\n' '  MySQL is available on localhost:3306.'
  else
    printf '%s\n' 'Docker is installed but its daemon is not running; skipping MySQL startup.'
    printf '%s\n' 'Start Docker and run: docker compose up -d'
  fi
else
  printf '%s\n' 'Docker Compose not available; skipping local MySQL startup.'
  printf '%s\n' 'Install/start Docker later if you want the bundled development database.'
fi

printf '%s\n' ''
printf '%s\n' 'Building the backend...'
cd "$ROOT_DIR/apps/backend"
./mvnw -DskipTests package

printf '%s\n' ''
printf '%s\n' '=== Setup complete ==='
printf '%s\n' ''
printf '%s\n' 'Next steps:'
printf '%s\n' '  1. Optional: edit .env for non-default configuration.'
printf '%s\n' '  2. Start the backend: ./scripts/dev.sh backend       # http://localhost:8080'
printf '%s\n' '  3. Start the admin console: ./scripts/dev.sh web     # http://localhost:5173'
printf '%s\n' '  4. Start the public H5: ./scripts/dev.sh mobile      # http://localhost:5174/mobile/'
printf '%s\n' ''
printf '%s\n' 'Development login: admin / admin123'
printf '%s\n' 'Change the development password before any non-development deployment.'
printf '%s\n' 'See CLAUDE.md for architecture, API contracts, and project-specific gotchas.'
