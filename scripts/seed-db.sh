#!/usr/bin/env bash
set -euo pipefail

# Seed a fresh dev MySQL with db/schema.sql + db/data.sql (both idempotent).
# DEV ONLY — never point this at the production database (see CLAUDE.md).
# Usage:
#   ./scripts/db-tunnel.sh     # in another terminal
#   ./scripts/seed-db.sh
# Defaults target the dev DB through the tunnel's local end (127.0.0.1:3306).
# CARPARK_DB_HOST is deliberately NOT read: in .env / db-tunnel.sh it names the SSH host,
# and dialing it directly would bypass the tunnel.
# CARPARK_DB_SSL_CA=<server CA pem> verifies the server certificate chain; unset, the
# connection is encrypted but unverified (same as JDBC verifyServerCertificate=false).

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
SEED_DB_HOST="${CARPARK_SEED_HOST:-127.0.0.1}"
SEED_DB_PORT="${CARPARK_SEED_PORT:-3306}"
SEED_DB_SSL_CA="${CARPARK_DB_SSL_CA:-}"
CARPARK_DB_PASSWORD_FILE="${CARPARK_DB_PASSWORD_FILE:-$ROOT_DIR/secrets/carpark.txt}"
DB_DIR="$ROOT_DIR/apps/backend/src/main/resources/db"

SEED_DB_PASSWORD="$(sed -n 's/^password=//p' "$CARPARK_DB_PASSWORD_FILE")"
if [ -z "$SEED_DB_PASSWORD" ]; then
  echo "no password= line in $CARPARK_DB_PASSWORD_FILE" >&2
  exit 1
fi
export SEED_DB_HOST SEED_DB_PORT SEED_DB_SSL_CA SEED_DB_PASSWORD

# No local mysql client; PyMySQL via uv. utf8mb4 is explicit so CJK isn't double-encoded.
uv run -q --with pymysql python3 -I - "$DB_DIR/schema.sql" "$DB_DIR/data.sql" <<'PY'
import os, sys, pymysql
from pymysql.constants import CLIENT
ca = os.environ["SEED_DB_SSL_CA"]
# carpark is REQUIRE SSL. With a CA the chain is verified; the hostname is not, because through
# the tunnel it is 127.0.0.1, which the server certificate never names.
ssl = {"ca": ca, "check_hostname": False} if ca else {"check_hostname": False}
c = pymysql.connect(host=os.environ["SEED_DB_HOST"], port=int(os.environ["SEED_DB_PORT"]),
                    user="carpark", password=os.environ["SEED_DB_PASSWORD"], database="carpark",
                    charset="utf8mb4", client_flag=CLIENT.MULTI_STATEMENTS, autocommit=True, ssl=ssl)
with c.cursor() as cur:
    cur.execute("SHOW SESSION STATUS LIKE 'Ssl_cipher'")
    print("ssl:", cur.fetchone()[1] or "NONE", "(cert verified)" if ca else "(cert NOT verified)")
    for path in sys.argv[1:]:
        cur.execute(open(path, encoding="utf-8").read())
        while cur.nextset():
            pass
        print("loaded", os.path.basename(path))
    cur.execute("SELECT name, CHAR_LENGTH(name) FROM cp_park")
    print("cp_park:", cur.fetchall())
    cur.execute("SELECT username FROM cp_user")
    print("cp_user:", cur.fetchall())
PY
