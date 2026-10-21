#!/usr/bin/env bash
set -euo pipefail

# Generate 32 cryptographically secure random bytes, encoded as hexadecimal.
od -An -N32 -tx1 /dev/urandom | tr -d ' \n'
