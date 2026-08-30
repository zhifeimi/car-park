-- CarPark schema. Derived from the legacy domain classes + mapper XMLs
-- (the old repo never contained cp_* DDL).
--
-- IMPORTANT: on the PRODUCTION database cp_park / cp_parking_booking already
-- exist with live data — run ONLY the cp_user DDL there (see README runbook).
-- This full file is for the fresh dev docker volume.

CREATE TABLE IF NOT EXISTS cp_park (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    name            VARCHAR(100) NOT NULL                COMMENT '停车场名称',
    lot_num         BIGINT       DEFAULT NULL            COMMENT '可预约车位总数',
    open_date_begin DATE         DEFAULT NULL            COMMENT '可预约起始日期',
    open_date_end   DATE         DEFAULT NULL            COMMENT '可预约结束日期',
    open_time       TIME         DEFAULT NULL            COMMENT '每天开放时间',
    close_time      TIME         DEFAULT NULL            COMMENT '每天关闭时间',
    remark          VARCHAR(500) DEFAULT NULL            COMMENT '备注',
    create_by       VARCHAR(64)  DEFAULT ''              COMMENT '创建者',
    create_time     DATETIME     DEFAULT NULL            COMMENT '创建时间',
    update_by       VARCHAR(64)  DEFAULT ''              COMMENT '更新者',
    update_time     DATETIME     DEFAULT NULL            COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='停车场信息';

CREATE TABLE IF NOT EXISTS cp_parking_booking (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    car_plate    VARCHAR(20)  DEFAULT NULL            COMMENT '车牌号',
    park         VARCHAR(100) DEFAULT NULL            COMMENT '停车场(存名称—join key，勿改名)',
    booking_date DATE         DEFAULT NULL            COMMENT '预约日期',
    cell_phone   VARCHAR(20)  DEFAULT NULL            COMMENT '手机号',
    remark       VARCHAR(500) DEFAULT NULL            COMMENT '备注',
    status       INT          DEFAULT 0               COMMENT '状态：0成功，-1取消',
    create_by    VARCHAR(64)  DEFAULT ''              COMMENT '创建者',
    create_time  DATETIME     DEFAULT NULL            COMMENT '创建时间',
    update_by    VARCHAR(64)  DEFAULT ''              COMMENT '更新者',
    update_time  DATETIME     DEFAULT NULL            COMMENT '更新时间',
    PRIMARY KEY (id),
    -- dev-only helper indexes (additive; do not require them in prod)
    KEY idx_booking_park_date (park, booking_date, status),
    KEY idx_booking_phone (cell_phone)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='停车预约记录';

CREATE TABLE IF NOT EXISTS cp_user (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    username    VARCHAR(64)  NOT NULL                COMMENT '登录名',
    password    VARCHAR(100) NOT NULL                COMMENT 'BCrypt 密码',
    nickname    VARCHAR(64)  DEFAULT NULL            COMMENT '昵称',
    remark      VARCHAR(500) DEFAULT NULL            COMMENT '备注',
    create_by   VARCHAR(64)  DEFAULT ''              COMMENT '创建者',
    create_time DATETIME     DEFAULT NULL            COMMENT '创建时间',
    update_by   VARCHAR(64)  DEFAULT ''              COMMENT '更新者',
    update_time DATETIME     DEFAULT NULL            COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='管理员';
