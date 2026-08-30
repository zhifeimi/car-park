-- ============================================================================
-- XSS filter retirement — stored-data remediation audit (ADR-005)
--
-- Until the JAR without XssFilter/HtmlFilter is deployed, every POST/PUT body
-- was HTML-entity-encoded pre-bind: a remark typed as `1 < 2` persisted as
-- `1 &lt; 2`, and markup was silently stripped. This script finds (and, once
-- reviewed, repairs) rows whose stored text still carries that mangling.
--
-- TARGET: production cp_park only — the `name` and `remark` columns.
--         cp_parking_booking has no free-text column that reaches storage
--         (its remark field never binds on the public surface), but see the
--         optional booking audit below for completeness.
--
-- !! NEVER RUN THIS FILE WHOLESALE. !!
--   1. Run each SELECT first and read the rows.
--   2. Only if mangled rows exist, uncomment the matching UPDATE block,
--      review it against the actual values, and run it alone.
--   3. Take a backup of affected rows before any UPDATE.
--
-- Run with an explicit client charset or CJK comparisons may miss:
--   mysql --default-character-set=utf8mb4 ...
-- ============================================================================

-- ---------------------------------------------------------------------------
-- 1. AUDIT — park names carrying HTML entities introduced by HtmlFilter
--    (&lt; &gt; &amp; &quot; &#x...; numeric forms)
-- ---------------------------------------------------------------------------
SELECT id, name, remark
FROM cp_park
WHERE name REGEXP '&(lt|gt|amp|quot|#x?[0-9A-Fa-f]+);'
   OR remark REGEXP '&(lt|gt|amp|quot|#x?[0-9A-Fa-f]+);';

-- ---------------------------------------------------------------------------
-- 2. AUDIT — park remarks with angle brackets surviving un-encoded
--    (possible if rows predate the filter, or were inserted by tools that
--    bypassed the servlet stack)
-- ---------------------------------------------------------------------------
SELECT id, name, remark
FROM cp_park
WHERE name LIKE '%<%' OR name LIKE '%>%'
   OR remark LIKE '%<%' OR remark LIKE '%>%';

-- ---------------------------------------------------------------------------
-- 3. OPTIONAL AUDIT — bookings (read-only). Bookings store no free text on
--    the public surface; run only if you want belt-and-braces certainty.
-- ---------------------------------------------------------------------------
-- SELECT id, car_plate, park, remark
-- FROM cp_parking_booking
-- WHERE remark REGEXP '&(lt|gt|amp|quot|#x?[0-9A-Fa-f]+);'
--    OR remark LIKE '%<%';
-- (cp_parking_booking has no remark column in the live schema — this block
--  is intentionally inert; delete it if schema.sql ever grows one.)

-- ============================================================================
-- 4. REPAIR — commented out ON PURPOSE. Review SELECT results first.
--    Repair strategy: decode HTML entities back to the characters the citizen
--    actually typed. Rows where decoding is ambiguous must be fixed by hand.
--
--    MySQL has no built-in HTML-unescape; the practical options are:
--      a) few rows → fix each by hand from the SELECT output;
--      b) many rows → do it in application space (small one-off script) or
--         with an explicit per-entity REPLACE chain like the sketch below.
--
--    -- UPDATE cp_park SET remark = REPLACE(remark, '&lt;', '<')  WHERE ...;
--    -- UPDATE cp_park SET remark = REPLACE(remark, '&gt;', '>')  WHERE ...;
--    -- UPDATE cp_park SET remark = REPLACE(remark, '&amp;', '&') WHERE ...;  -- LAST: re-decodes &lt; → &amp;lt;
--    --
--    -- Order matters: &amp; MUST be decoded last or double-decoding corrupts
--    -- legitimately-stored "&lt;" literals.
-- ============================================================================
