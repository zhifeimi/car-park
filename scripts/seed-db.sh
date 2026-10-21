#!/usr/bin/env bash
set -e

# Seed a fresh dev MySQL with db/schema.sql + db/data.sql (both idempotent).
# DEV ONLY — never point this at the production database (see CLAUDE.md).
# Usage:
#   ./scripts/seed-db.sh
# Defaults target the dev DB [via your SSH tunnel].

CARPARK_DB_HOST="${CARPARK_DB_HOST:-127.0.0.1}"
CARPARK_DB_PORT="${CARPARK_DB_PORT:-3306}"
CARPARK_DB_PASSWORD_FILE="${CARPARK_DB_PASSWORD_FILE:-$PWD/secrets/carpark.txt}"
DB_DIR="$(cd "$(dirname "$0")/.." && pwd)/apps/backend/src/main/resources/db"

CARPARK_DB_PASSWORD="$(sed -n 's/^password=//p' "$CARPARK_DB_PASSWORD_FILE")"
export CARPARK_DB_HOST CARPARK_DB_PORT CARPARK_DB_PASSWORD

# No local mysql client; PyMySQL via uv. utf8mb4 is explicit so CJK isn't double-encoded.
uv run -q --with pymysql python3 -I - "$DB_DIR/schema.sql" "$DB_DIR/data.sql" <<'PY'
import os, sys, pymysql
from pymysql.constants import CLIENT
c = pymysql.connect(host=os.environ["CARPARK_DB_HOST"], port=int(os.environ["CARPARK_DB_PORT"]),
                    user="carpark", password=os.environ["CARPARK_DB_PASSWORD"], database="carpark",
                    charset="utf8mb4", client_flag=CLIENT.MULTI_STATEMENTS, autocommit=True,
                    # carpark is REQUIRE SSL; no CA → encrypted but unverified (JDBC verifyServerCertificate=false)
                    ssl={"check_hostname": True})
with c.cursor() as cur:
    cur.execute("SHOW SESSION STATUS LIKE 'Ssl_cipher'")
    print("ssl:", cur.fetchone()[1] or "NONE")
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
