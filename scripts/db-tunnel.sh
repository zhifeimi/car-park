#!/usr/bin/env bash
set -e

# Remote Dev MySQL SSH Tunnel Helper
# Usage:
#   ./scripts/db-tunnel.sh

CARPARK_DB_HOST="${CARPARK_DB_HOST:-dev-db.example.com}"
echo "Opening SSH tunnel to remote dev MySQL on ${CARPARK_DB_HOST} (:3306 -> 127.0.0.1:3306)..."
echo "Press Ctrl+C to stop the tunnel."
ssh -N -L 127.0.0.1:3306:127.0.0.1:3306 "${CARPARK_DB_HOST}"
