-- DEV seed only (runs once on fresh docker volume).
--
-- Admin login: admin / admin123  ——  CHANGE IMMEDIATELY on any real deployment.
-- The BCrypt hash below is the hash of 'admin123' (regenerate at impl time with
-- BCryptPasswordEncoder and re-verify; the integration matrix asserts login works).
INSERT INTO cp_user (username, password, nickname, create_by, create_time)
VALUES ('admin', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '超级管理员', 'system', NOW())
ON DUPLICATE KEY UPDATE id = id;

-- Sample lots so the dashboard and availability math have data in dev.
INSERT INTO cp_park (name, lot_num, open_date_begin, open_date_end, open_time, close_time, remark, create_by, create_time)
SELECT * FROM (
    SELECT '黄麻停车场' AS name, 20 AS lot_num, DATE '2024-01-01' AS open_date_begin,
           DATE '2030-12-31' AS open_date_end, TIME '08:00:00' AS open_time,
           TIME '20:00:00' AS close_time, '黄麻社区访客停车' AS remark,
           'system' AS create_by, NOW() AS create_time
) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM cp_park WHERE name = '黄麻停车场');

INSERT INTO cp_park (name, lot_num, open_date_begin, open_date_end, open_time, close_time, remark, create_by, create_time)
SELECT * FROM (
    SELECT '岭头停车场', 15, DATE '2024-01-01', DATE '2030-12-31',
           TIME '08:00:00', TIME '20:00:00', '岭头社区访客停车', 'system', NOW()
) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM cp_park WHERE name = '岭头停车场');
