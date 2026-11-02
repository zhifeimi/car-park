-- ============================================================================
-- cp_park.country — add the parking lot's country/region (2026-11-02)
--
-- ISO 3166-1 alpha-2, upper case. CN = mainland China (HK, MO and TW have codes
-- of their own). The public H5 shows the province plate keyboard only for a CN
-- lot; every other lot, and a lot with no country, gets free-text plate entry.
--
-- TARGET: any database that already has cp_park — production, an existing dev
--         volume, the *_test database. A fresh docker volume does not need it
--         (schema.sql already has the column). Deploy order: run this BEFORE
--         starting the new JAR, which selects cp.country.
--
-- Idempotent: the ADD COLUMN runs only when the column is missing, and the
-- backfill only touches rows whose country is still NULL. Safe to re-run.
--
-- Run with an explicit client charset:
--   mysql --default-character-set=utf8mb4 ... < scripts/ops/park-country-migration.sql
-- ============================================================================

-- 1. Add the column if it is not there yet (MySQL has no ADD COLUMN IF NOT EXISTS).
SET @add_country = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE cp_park ADD COLUMN country CHAR(2) DEFAULT NULL COMMENT ''国家/地区 ISO 3166-1 alpha-2 (CN=中国大陆)'' AFTER name',
              'DO 0')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'cp_park' AND COLUMN_NAME = 'country');
PREPARE add_country FROM @add_country;
EXECUTE add_country;
DEALLOCATE PREPARE add_country;

-- 2. Backfill. Every lot deployed so far is in Guangzhou (Changling Street), so
--    existing rows are mainland China and keep the province keyboard they have
--    today. A deployment with lots elsewhere: change 'CN' (or set each lot in
--    the admin console afterwards — a lot left NULL must get a country on its
--    next edit).
UPDATE cp_park SET country = 'CN' WHERE country IS NULL;
