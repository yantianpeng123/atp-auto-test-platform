-- =============================================
-- 自动化测试平台（ATP）数据库初始化脚本
-- MySQL 8.0  charset: utf8mb4
-- 第一阶段：用户与权限
-- =============================================

CREATE DATABASE IF NOT EXISTS `atp`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_general_ci;

USE `atp`;

-- ----------------------------
-- 用户表
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`
(
    `id`               BIGINT                                                        NOT NULL COMMENT '主键ID',
    `username`         VARCHAR(50)                                                   NOT NULL COMMENT '登录账号',
    `password`         VARCHAR(100)                                                  NOT NULL COMMENT 'BCrypt 密码',
    `nickname`         VARCHAR(50)  DEFAULT NULL COMMENT '昵称',
    `email`            VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    `phone`            VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
    `avatar`           VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
    `status`           TINYINT      DEFAULT 1 COMMENT '状态 0-禁用 1-正常',
    `role`             VARCHAR(20)  DEFAULT 'TESTER' COMMENT '角色 ADMIN/TESTER/VIEWER',
    `last_login_ip`    VARCHAR(50)  DEFAULT NULL COMMENT '最近登录IP',
    `last_login_time`  DATETIME     DEFAULT NULL COMMENT '最近登录时间',
    `create_time`      DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`          TINYINT      DEFAULT 0 COMMENT '逻辑删除 0-未删 1-已删',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_email` (`email`),
    KEY `idx_status` (`status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='系统用户表';

-- 初始管理员：admin / admin123
INSERT INTO `sys_user`
    (`id`, `username`, `password`, `nickname`, `email`, `status`, `role`)
VALUES (1, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '平台管理员',
        'admin@atp.local', 1, 'ADMIN')
    ON DUPLICATE KEY UPDATE `username` = `username`;


-- =============================================
-- 以下为后续阶段预留表结构，本期仅建表
-- =============================================

-- 项目表（基础数据管理）
DROP TABLE IF EXISTS `tb_project`;
CREATE TABLE `tb_project`
(
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '项目ID',
    `name`        VARCHAR(100) NOT NULL COMMENT '项目名称',
    `team`        VARCHAR(100) DEFAULT NULL COMMENT '所属团队',
    `owner_id`    BIGINT       DEFAULT NULL COMMENT '负责人ID（创建者）',
    `create_by`   BIGINT       DEFAULT NULL COMMENT '创建人ID',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     TINYINT      DEFAULT 0 COMMENT '逻辑删除 0-未删 1-已删',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='项目表';

-- 工程表（基础数据管理）
DROP TABLE IF EXISTS `tb_application`;
CREATE TABLE `tb_application`
(
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `project_id`   BIGINT       DEFAULT NULL COMMENT '所属项目ID',
    `name`         VARCHAR(100) NOT NULL COMMENT '工程名称',
    `description`  VARCHAR(500) DEFAULT NULL COMMENT '工程描述',
    `create_by`    BIGINT       DEFAULT NULL COMMENT '创建人ID',
    `create_time`  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`      TINYINT      DEFAULT 0 COMMENT '逻辑删除 0-未删 1-已删',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='工程表';

-- 版本表（基础数据管理）
DROP TABLE IF EXISTS `tb_application_version`;
CREATE TABLE `tb_application_version`
(
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `application_id` BIGINT       NOT NULL COMMENT '工程ID',
    `name`           VARCHAR(100) NOT NULL COMMENT '版本名称',
    `create_by`      BIGINT       DEFAULT NULL COMMENT '创建人ID',
    `create_time`    DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`        TINYINT      DEFAULT 0 COMMENT '逻辑删除 0-未删 1-已删',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_app_name` (`application_id`, `name`),
    KEY `idx_application` (`application_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='工程版本表';

-- 模块表（基础数据管理）
DROP TABLE IF EXISTS `tb_application_module`;
CREATE TABLE `tb_application_module`
(
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `version_id`  BIGINT       NOT NULL COMMENT '版本ID',
    `name`        VARCHAR(100) NOT NULL COMMENT '模块名称',
    `create_by`   BIGINT       DEFAULT NULL COMMENT '创建人ID',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     TINYINT      DEFAULT 0 COMMENT '逻辑删除 0-未删 1-已删',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_version_name` (`version_id`, `name`),
    KEY `idx_version` (`version_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='工程模块表';

-- 环境表（第二阶段）
DROP TABLE IF EXISTS `tb_test_env`;
CREATE TABLE `tb_test_env`
(
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `project_id`  BIGINT       NOT NULL COMMENT '项目ID',
    `name`        VARCHAR(50)  NOT NULL COMMENT '环境名称',
    `base_url`    VARCHAR(255) DEFAULT NULL COMMENT '基础域名',
    `headers`     JSON         DEFAULT NULL COMMENT '全局请求头',
    `db_config`   JSON         DEFAULT NULL COMMENT '数据库配置',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`     TINYINT      DEFAULT 0,
    PRIMARY KEY (`id`),
    -- deleted 纳入唯一键：逻辑删除后同名环境可再次创建（deleted=1 与 deleted=0 不冲突）
    UNIQUE KEY `uk_project_name_deleted` (`project_id`, `name`, `deleted`),
    KEY `idx_project` (`project_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='测试环境表';

-- 接口定义表（第二阶段）
DROP TABLE IF EXISTS `tb_api_definition`;
CREATE TABLE `tb_api_definition`
(
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `module_id`   BIGINT       NOT NULL COMMENT '模块ID',
    `name`        VARCHAR(100) NOT NULL COMMENT '接口名称',
    `method`      VARCHAR(10)  NOT NULL COMMENT 'GET/POST/PUT/DELETE',
    `path`        VARCHAR(500) NOT NULL COMMENT '接口路径',
    `headers`     JSON         DEFAULT NULL COMMENT '请求头模板',
    `body`        JSON         DEFAULT NULL COMMENT '请求体模板',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '描述',
    `create_by`   BIGINT       DEFAULT NULL,
    `source_flag` VARCHAR(500) DEFAULT NULL COMMENT '接口来源标志',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`     TINYINT      DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_module` (`module_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='接口定义表';

-- 用例表（第二阶段）
DROP TABLE IF EXISTS `tb_test_case`;
CREATE TABLE `tb_test_case`
(
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `project_id`      BIGINT       NOT NULL COMMENT '项目ID',
    `application_id`  BIGINT       NOT NULL COMMENT '工程ID',
    `version_id`      BIGINT       NOT NULL COMMENT '版本ID',
    `module_id`       BIGINT       NOT NULL COMMENT '模块ID',
    `api_id`          BIGINT       DEFAULT NULL COMMENT '关联接口ID',
    `name`            VARCHAR(200) NOT NULL COMMENT '用例名称',
    `creator_name`    VARCHAR(50)  DEFAULT NULL COMMENT '创建人名称',
    `level`           TINYINT      DEFAULT 2 COMMENT '优先级 1-P0 2-P1 3-P2',
    `request`         JSON         DEFAULT NULL COMMENT '请求内容',
    `assertions`      JSON         DEFAULT NULL COMMENT '断言规则',
    `setup_script`    TEXT         DEFAULT NULL COMMENT '前置脚本',
    `status`          TINYINT      DEFAULT 1 COMMENT '0-停用 1-启用',
    `create_by`       BIGINT       DEFAULT NULL,
    `create_time`     DATETIME     DEFAULT CURRENT_TIMESTAMP,
    `update_time`     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`         TINYINT      DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_project` (`project_id`),
    KEY `idx_application` (`application_id`),
    KEY `idx_version` (`version_id`),
    KEY `idx_module` (`module_id`),
    KEY `idx_api` (`api_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='测试用例表';

-- 测试计划表（第三阶段）
DROP TABLE IF EXISTS `tb_test_plan`;
CREATE TABLE `tb_test_plan`
(
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `project_id`  BIGINT       NOT NULL COMMENT '项目ID',
    `env_id`      BIGINT       DEFAULT NULL COMMENT '环境ID',
    `name`        VARCHAR(100) NOT NULL COMMENT '计划名称',
    `cron`        VARCHAR(100) DEFAULT NULL COMMENT 'Cron 表达式',
    `enabled`     TINYINT      DEFAULT 0 COMMENT '0-关闭 1-启用',
    `last_run_id` BIGINT       DEFAULT NULL COMMENT '最近执行ID',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`     TINYINT      DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_project` (`project_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='测试计划表';

-- 计划-用例关联表（第三阶段）
DROP TABLE IF EXISTS `tb_plan_case`;
CREATE TABLE `tb_plan_case`
(
    `id`         BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `plan_id`    BIGINT NOT NULL COMMENT '计划ID',
    `case_id`    BIGINT NOT NULL COMMENT '用例ID',
    `sort_order` INT    DEFAULT 0 COMMENT '执行顺序',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_plan_case` (`plan_id`, `case_id`),
    KEY `idx_plan` (`plan_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='计划用例关联表';

-- 用例步骤表（第二阶段扩展：多接口串行+参数传递）
DROP TABLE IF EXISTS `tb_case_step`;
CREATE TABLE `tb_case_step`
(
    `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `case_id`          BIGINT       NOT NULL COMMENT '用例ID',
    `api_id`           BIGINT       NOT NULL COMMENT '关联接口ID',
    `sort_order`       INT          NOT NULL DEFAULT 0 COMMENT '执行顺序(从1开始)',
    `step_name`        VARCHAR(200) DEFAULT NULL COMMENT '步骤名称',
    `request_override` JSON         DEFAULT NULL COMMENT '请求覆盖内容(headers/body/params)',
    `assertions`       JSON         DEFAULT NULL COMMENT '步骤断言规则',
    `response_var`     VARCHAR(100) DEFAULT NULL COMMENT '响应变量名',
    `create_time`      DATETIME     DEFAULT CURRENT_TIMESTAMP,
    `update_time`      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`          TINYINT      DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_case` (`case_id`),
    KEY `idx_api` (`api_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='用例步骤表';

-- 用例参数化数据集表（第三阶段）
DROP TABLE IF EXISTS `tb_dataset_template`;
CREATE TABLE `tb_dataset_template`
(
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `case_id`      BIGINT       NOT NULL COMMENT '关联用例ID',
    `name`         VARCHAR(100) NOT NULL COMMENT '数据源名称',
    `keys`         JSON         DEFAULT NULL COMMENT '字段名列表(JSON数组)',
    `creator_name` VARCHAR(50)  DEFAULT NULL COMMENT '添加人',
    `create_time`  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    `update_time`  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`      TINYINT      DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_case` (`case_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='数据源模板表';

-- 数据项表（第三阶段：保存具体数据，每行一个 JSON 对象）
DROP TABLE IF EXISTS `tb_dataset_item`;
CREATE TABLE `tb_dataset_item`
(
    `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `template_id` BIGINT   NOT NULL COMMENT '关联数据源模板ID',
    `data`        JSON     DEFAULT NULL COMMENT '数据行(JSON对象)',
    `sort_order`  INT      DEFAULT 0 COMMENT '执行顺序',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`     TINYINT  DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_template` (`template_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='数据项表';

-- 执行记录表（头表：一次执行动作的汇总。手动执行=1个用例；计划执行=1个计划，可派生多条记录）
DROP TABLE IF EXISTS `tb_execution`;
CREATE TABLE `tb_execution`
(
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `plan_id`       BIGINT       DEFAULT NULL COMMENT '计划ID（手动执行为NULL）',
    `project_id`    BIGINT       NOT NULL COMMENT '项目ID',
    `case_id`       BIGINT       DEFAULT NULL COMMENT '用例ID（手动执行填）',
    `case_name`     VARCHAR(200) DEFAULT NULL COMMENT '用例名称快照',
    `env_id`        BIGINT       DEFAULT NULL COMMENT '执行环境ID',
    `env_name`      VARCHAR(50)  DEFAULT NULL COMMENT '执行环境名称快照',
    `trigger_type`  VARCHAR(20)  DEFAULT 'MANUAL' COMMENT 'MANUAL/SCHEDULED/CI',
    `executor_id`   BIGINT       DEFAULT NULL COMMENT '执行人ID',
    `status`        VARCHAR(20)  DEFAULT NULL COMMENT 'RUNNING/SUCCESS/FAILED',
    `total_rounds`  INT          DEFAULT 0 COMMENT '总轮次（参数化多轮）',
    `passed_rounds` INT          DEFAULT 0 COMMENT '通过轮次',
    `failed_rounds` INT          DEFAULT 0 COMMENT '失败轮次',
    `total_steps`   INT          DEFAULT 0 COMMENT '总步骤数',
    `passed_steps`  INT          DEFAULT 0 COMMENT '通过步骤数',
    `failed_steps`  INT          DEFAULT 0 COMMENT '失败步骤数',
    `duration_ms`   BIGINT       DEFAULT 0 COMMENT '耗时毫秒',
    `start_time`    DATETIME     DEFAULT NULL COMMENT '开始时间',
    `end_time`      DATETIME     DEFAULT NULL COMMENT '结束时间',
    `create_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`       TINYINT      DEFAULT 0 COMMENT '逻辑删除 0-未删 1-已删',
    PRIMARY KEY (`id`),
    KEY `idx_project` (`project_id`),
    KEY `idx_plan` (`plan_id`),
    KEY `idx_case` (`case_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='执行记录表';

-- 执行明细表（每轮每步一行：execution_id + round_index + step_index 唯一定位一个步骤的实际请求/响应）
DROP TABLE IF EXISTS `tb_execution_detail`;
CREATE TABLE `tb_execution_detail`
(
    `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `execution_id`     BIGINT       NOT NULL COMMENT '执行记录ID',
    `case_id`          BIGINT       DEFAULT NULL COMMENT '用例ID（冗余，便于按用例查历史）',
    `round_index`      INT          NOT NULL DEFAULT 1 COMMENT '轮次（参数化多轮从1起）',
    `step_index`       INT          NOT NULL DEFAULT 0 COMMENT '步骤序号（同轮内排序）',
    `step_id`          BIGINT       DEFAULT NULL COMMENT '步骤ID快照',
    `step_name`        VARCHAR(200) DEFAULT NULL COMMENT '步骤名称快照',
    `method`           VARCHAR(10)  DEFAULT NULL COMMENT '实际请求方法',
    `url`              VARCHAR(1000) DEFAULT NULL COMMENT '实际请求URL',
    `request_headers`  TEXT         DEFAULT NULL COMMENT '实际请求头',
    `request_body`     LONGTEXT     DEFAULT NULL COMMENT '实际请求体',
    `response_headers` TEXT         DEFAULT NULL COMMENT '实际响应头',
    `response_body`    LONGTEXT     DEFAULT NULL COMMENT '实际响应体',
    `status_code`      INT          DEFAULT NULL COMMENT '响应状态码',
    `status`           VARCHAR(20)  DEFAULT NULL COMMENT 'PASSED/FAILED/ERROR',
    `error_msg`        VARCHAR(2000) DEFAULT NULL COMMENT '错误信息',
    `duration_ms`      BIGINT       DEFAULT 0 COMMENT '耗时毫秒',
    `create_time`      DATETIME     DEFAULT CURRENT_TIMESTAMP,
    `update_time`      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`          TINYINT      DEFAULT 0 COMMENT '逻辑删除 0-未删 1-已删',
    PRIMARY KEY (`id`),
    KEY `idx_execution` (`execution_id`),
    KEY `idx_case` (`case_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='执行明细表';

-- 断言结果表（每条断言一行，归属某一步骤，便于按接口/断言维度做通过率统计）
DROP TABLE IF EXISTS `tb_execution_assertion`;
CREATE TABLE `tb_execution_assertion`
(
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `execution_id` BIGINT       NOT NULL COMMENT '执行记录ID（冗余，便于单表统计）',
    `detail_id`    BIGINT       NOT NULL COMMENT '执行明细ID',
    `round_index`  INT          NOT NULL DEFAULT 1 COMMENT '轮次',
    `step_index`   INT          NOT NULL DEFAULT 0 COMMENT '步骤序号',
    `type`         VARCHAR(20)  DEFAULT NULL COMMENT '断言类型 status/jsonPath/header/body',
    `path`         VARCHAR(200) DEFAULT NULL COMMENT 'jsonPath 或响应头名',
    `operator`     VARCHAR(20)  DEFAULT NULL COMMENT 'eq/notEq/contains/exists',
    `expected`     VARCHAR(500) DEFAULT NULL COMMENT '期望值',
    `actual`       VARCHAR(500) DEFAULT NULL COMMENT '实际值',
    `passed`       TINYINT(1)   DEFAULT 0 COMMENT '是否通过 0-否 1-是',
    `message`      VARCHAR(500) DEFAULT NULL COMMENT '失败原因',
    `create_time`  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    `update_time`  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`      TINYINT      DEFAULT 0 COMMENT '逻辑删除 0-未删 1-已删',
    PRIMARY KEY (`id`),
    KEY `idx_detail` (`detail_id`),
    KEY `idx_execution` (`execution_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='断言结果表';

-- =============================================
-- 第四阶段：定时任务批次（Batch）
-- 一个批次编排多个测试计划，可定时 / 手动批量执行
-- =============================================

-- 批次头表（批次定义 + 启用状态 + 定时表达式）
DROP TABLE IF EXISTS `tb_plan_batch`;
CREATE TABLE `tb_plan_batch`
(
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `project_id`      BIGINT       NOT NULL COMMENT '项目ID（项目隔离）',
    `name`            VARCHAR(100) NOT NULL COMMENT '批次名称',
    `strategy`        VARCHAR(16)  DEFAULT 'SERIAL' COMMENT '执行策略 SERIAL/PARALLEL',
    `fail_continue`   TINYINT      DEFAULT 0 COMMENT '串行时失败后是否继续 0-否 1-是',
    `max_concurrency` INT          DEFAULT 3 COMMENT '并行最大并发数',
    `cron`            VARCHAR(100) DEFAULT NULL COMMENT 'Cron 表达式（NULL/空表示不定时）',
    `enabled`         TINYINT      DEFAULT 0 COMMENT '0-关闭 1-启用定时',
    `last_run_id`     BIGINT       DEFAULT NULL COMMENT '最近一次运行实例ID',
    `create_time`     DATETIME     DEFAULT CURRENT_TIMESTAMP,
    `update_time`     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`         TINYINT      DEFAULT 0 COMMENT '逻辑删除 0-未删 1-已删',
    PRIMARY KEY (`id`),
    KEY `idx_project` (`project_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='定时任务批次头表';

-- 批次-计划关联表（无 deleted 列：物理删除，重建关联时直接清掉旧数据）
DROP TABLE IF EXISTS `tb_plan_batch_item`;
CREATE TABLE `tb_plan_batch_item`
(
    `id`         BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `batch_id`   BIGINT NOT NULL COMMENT '批次ID',
    `plan_id`    BIGINT NOT NULL COMMENT '测试计划ID',
    `sort_order` INT    DEFAULT 0 COMMENT '执行顺序',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_batch_plan` (`batch_id`, `plan_id`),
    KEY `idx_batch` (`batch_id`),
    KEY `idx_plan` (`plan_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='批次计划关联表';

-- 批次运行实例表（一次运行的汇总）
DROP TABLE IF EXISTS `tb_plan_batch_run`;
CREATE TABLE `tb_plan_batch_run`
(
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `batch_id`     BIGINT       NOT NULL COMMENT '批次ID',
    `trigger_type` VARCHAR(16)  DEFAULT 'MANUAL' COMMENT '触发方式 MANUAL/SCHEDULED',
    `status`       VARCHAR(16)  DEFAULT 'RUNNING' COMMENT 'RUNNING/SUCCESS/PARTIAL_FAILED/FAILED',
    `total`        INT          DEFAULT 0 COMMENT '计划总数',
    `passed`       INT          DEFAULT 0 COMMENT '成功数',
    `failed`       INT          DEFAULT 0 COMMENT '失败数（含被跳过）',
    `running`      INT          DEFAULT 0 COMMENT '进行中数',
    `queued`       INT          DEFAULT 0 COMMENT '排队中数',
    `start_time`   DATETIME     DEFAULT NULL COMMENT '开始时间',
    `end_time`     DATETIME     DEFAULT NULL COMMENT '结束时间',
    `duration_ms`  BIGINT       DEFAULT 0 COMMENT '耗时毫秒',
    `create_time`  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    `update_time`  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_batch` (`batch_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='批次运行实例表';

-- 批次运行明细表（每个计划的执行结果；execution_id 复用 tb_execution）
DROP TABLE IF EXISTS `tb_plan_batch_run_item`;
CREATE TABLE `tb_plan_batch_run_item`
(
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `run_id`       BIGINT       NOT NULL COMMENT '运行实例ID',
    `plan_id`      BIGINT       NOT NULL COMMENT '计划ID',
    `plan_name`    VARCHAR(100) DEFAULT NULL COMMENT '计划名称快照',
    `sort_order`   INT          DEFAULT 0 COMMENT '顺序快照',
    `status`       VARCHAR(16)  DEFAULT 'QUEUED' COMMENT 'QUEUED/RUNNING/SUCCESS/FAILED/SKIPPED',
    `execution_id` BIGINT       DEFAULT NULL COMMENT '关联执行记录ID（复用 tb_execution）',
    `duration_ms`  BIGINT       DEFAULT NULL COMMENT '耗时毫秒',
    `error_msg`    TEXT         DEFAULT NULL COMMENT '失败原因',
    `start_time`   DATETIME     DEFAULT NULL COMMENT '开始时间',
    `end_time`     DATETIME     DEFAULT NULL COMMENT '结束时间',
    `create_time`  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    `update_time`  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_run` (`run_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='批次运行明细表';
