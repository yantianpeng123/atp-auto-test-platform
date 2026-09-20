-- =============================================
-- 第五阶段：通知中心（渠道 / 规则 / 发送日志 / 站内信）
-- 2026-09-20 新增；仅新增 4 张表，不触碰其它表
-- 单独执行本文件即可完成建表，不影响既有业务表数据
-- =============================================

DROP TABLE IF EXISTS `tb_notify_channel`;
CREATE TABLE `tb_notify_channel`
(
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `project_id`  BIGINT       NOT NULL COMMENT '项目ID（项目隔离）',
    `type`        VARCHAR(20)  NOT NULL COMMENT '渠道类型 INAPP/DINGTALK/EMAIL_163',
    `name`        VARCHAR(100) NOT NULL COMMENT '渠道显示名（如「钉钉-交易群」）',
    `config`      JSON         DEFAULT NULL COMMENT '渠道配置JSON：DINGTALK=webhook/secret/atMobiles；EMAIL_163=host/port/username/authCode/from/ssl/to；INAPP 无',
    `enabled`     TINYINT      DEFAULT 1 COMMENT '是否启用 0-否 1-是',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     TINYINT      DEFAULT 0 COMMENT '逻辑删除 0-未删 1-已删',
    PRIMARY KEY (`id`),
    KEY `idx_project` (`project_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='通知渠道表';

DROP TABLE IF EXISTS `tb_notify_rule`;
CREATE TABLE `tb_notify_rule`
(
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `project_id`  BIGINT       NOT NULL COMMENT '项目ID（项目隔离）',
    `name`        VARCHAR(100) DEFAULT NULL COMMENT '规则名称（可选）',
    `event`       VARCHAR(20)  NOT NULL COMMENT '触发事件 EXEC_DONE/EXEC_FAIL/BATCH_DONE',
    `channel_ids` JSON         DEFAULT NULL COMMENT '命中的渠道ID列表（JSON 数组）',
    `condition`   JSON         DEFAULT NULL COMMENT '附加条件 如 {"onlyFail":true}',
    `enabled`     TINYINT      DEFAULT 1 COMMENT '是否启用 0-否 1-是',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     TINYINT      DEFAULT 0 COMMENT '逻辑删除 0-未删 1-已删',
    PRIMARY KEY (`id`),
    KEY `idx_project` (`project_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='通知规则表';

DROP TABLE IF EXISTS `tb_notify_log`;
CREATE TABLE `tb_notify_log`
(
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `project_id`   BIGINT       DEFAULT NULL COMMENT '项目ID',
    `rule_id`      BIGINT       DEFAULT NULL COMMENT '关联规则ID',
    `channel_id`   BIGINT       DEFAULT NULL COMMENT '关联渠道ID',
    `event`        VARCHAR(20)  DEFAULT NULL COMMENT '触发事件',
    `channel_type` VARCHAR(20)  DEFAULT NULL COMMENT '渠道类型 INAPP/DINGTALK/EMAIL_163',
    `target`       VARCHAR(255) DEFAULT NULL COMMENT '发送目标（webhook url / 收件人 / 站内信用户）',
    `status`       VARCHAR(20)  DEFAULT NULL COMMENT 'SUCCESS/FAILED',
    `content`      TEXT         DEFAULT NULL COMMENT '发送内容摘要',
    `error`        VARCHAR(2000) DEFAULT NULL COMMENT '失败原因',
    `create_time`  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
    PRIMARY KEY (`id`),
    KEY `idx_project` (`project_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='通知发送日志表';

DROP TABLE IF EXISTS `tb_notify_message`;
CREATE TABLE `tb_notify_message`
(
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`     BIGINT       NOT NULL COMMENT '接收用户ID',
    `project_id`  BIGINT       NOT NULL COMMENT '项目ID',
    `title`       VARCHAR(200) DEFAULT NULL COMMENT '站内信标题',
    `content`     TEXT         DEFAULT NULL COMMENT '站内信内容',
    `read`        TINYINT      DEFAULT 0 COMMENT '是否已读 0-未读 1-已读',
    `link_url`    VARCHAR(500) DEFAULT NULL COMMENT '点击跳转的报告/详情URL',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user` (`user_id`),
    KEY `idx_project` (`project_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='站内信收件箱表';
