-- =============================================
-- 迁移脚本 v3：前置/后置扩展 + 组合组件 + 执行明细嵌套
-- 适用：已存在旧表结构的运行数据库（新建库请直接使用 schema.sql）
-- 注意：请对运行库执行一次；脚本对不存在的约束做了幂等处理，但 ALTER ADD COLUMN 重复执行会报错，请勿重复运行。
-- =============================================

-- 1) 用例步骤表扩展
ALTER TABLE `tb_case_step`
    ADD COLUMN `phase` VARCHAR(10) NOT NULL DEFAULT 'main' COMMENT '步骤阶段 pre-前置/main-主步骤/post-后置' AFTER `api_id`,
    ADD COLUMN `step_type` TINYINT NOT NULL DEFAULT 1 COMMENT '步骤类型 1-单接口 2-组合组件 3-其他类型' AFTER `phase`,
    ADD COLUMN `component_id` BIGINT DEFAULT NULL COMMENT '组合组件ID(step_type=2时引用)' AFTER `step_type`,
    ADD COLUMN `is_disabled` TINYINT NOT NULL DEFAULT 0 COMMENT '是否禁用 0-否 1-是' AFTER `response_var`,
    ADD COLUMN `promote_global` TINYINT NOT NULL DEFAULT 0 COMMENT '提升为全局变量 0-否 1-是' AFTER `is_disabled`,
    ADD COLUMN `continue_on_fail` TINYINT NOT NULL DEFAULT 0 COMMENT '失败后继续执行 0-否 1-是' AFTER `promote_global`,
    ADD COLUMN `description` VARCHAR(500) DEFAULT NULL COMMENT '扩展说明' AFTER `continue_on_fail`;

-- 历史数据：原步骤统一归为 main 阶段、单接口类型
UPDATE `tb_case_step` SET `phase` = 'main', `step_type` = 1 WHERE `phase` IS NULL OR `phase` = '';

-- api_id 改为可空（组合组件步骤无单接口）
ALTER TABLE `tb_case_step` MODIFY COLUMN `api_id` BIGINT DEFAULT NULL COMMENT '关联接口ID(step_type=1时必填)';
ALTER TABLE `tb_case_step` ADD KEY `idx_component` (`component_id`);

-- 2) 新增组合组件表
CREATE TABLE IF NOT EXISTS `tb_api_component`
(
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `project_id`  BIGINT       NOT NULL COMMENT '项目ID(项目隔离)',
    `module_id`   BIGINT       DEFAULT NULL COMMENT '所属模块ID(模块树定位)',
    `name`        VARCHAR(100) NOT NULL COMMENT '组件名称',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '组件描述',
    `create_by`   BIGINT       DEFAULT NULL COMMENT '创建人ID',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`     TINYINT      DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_project` (`project_id`),
    KEY `idx_module` (`module_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='组合组件表';

-- 3) 新增组合组件步骤表
CREATE TABLE IF NOT EXISTS `tb_api_component_step`
(
    `id`                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `component_id`      BIGINT       NOT NULL COMMENT '所属组件ID',
    `step_type`         TINYINT      NOT NULL DEFAULT 1 COMMENT '步骤类型 1-单接口 2-嵌套组件',
    `api_id`            BIGINT       DEFAULT NULL COMMENT '关联接口ID(step_type=1时必填)',
    `child_component_id` BIGINT      DEFAULT NULL COMMENT '嵌套组件ID(step_type=2时引用)',
    `sort_order`        INT          NOT NULL DEFAULT 0 COMMENT '执行顺序(从1开始)',
    `step_name`         VARCHAR(200) DEFAULT NULL COMMENT '步骤名称',
    `request_override`  JSON         DEFAULT NULL COMMENT '请求覆盖内容(headers/body/params)',
    `assertions`        JSON         DEFAULT NULL COMMENT '步骤断言规则',
    `response_var`      VARCHAR(100) DEFAULT NULL COMMENT '响应变量名',
    `is_disabled`       TINYINT      NOT NULL DEFAULT 0 COMMENT '是否禁用 0-否 1-是',
    `continue_on_fail`  TINYINT      NOT NULL DEFAULT 0 COMMENT '失败后继续执行 0-否 1-是',
    `description`       VARCHAR(500) DEFAULT NULL COMMENT '步骤说明',
    `create_time`       DATETIME     DEFAULT CURRENT_TIMESTAMP,
    `update_time`       DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`           TINYINT      DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_component` (`component_id`),
    KEY `idx_api` (`api_id`),
    KEY `idx_child_component` (`child_component_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='组合组件步骤表';

-- 4) 执行明细表增加嵌套三列
ALTER TABLE `tb_execution_detail`
    ADD COLUMN `component_id` BIGINT DEFAULT NULL COMMENT '所属组合组件ID(组件展开子步骤时填写)' AFTER `step_id`,
    ADD COLUMN `parent_step_id` BIGINT DEFAULT NULL COMMENT '父步骤ID(组件展开时为容器步骤ID)' AFTER `component_id`,
    ADD COLUMN `nest_level` INT NOT NULL DEFAULT 0 COMMENT '嵌套层级(0-用例直接步骤 1-组件内 2-嵌套组件内)' AFTER `parent_step_id`;
