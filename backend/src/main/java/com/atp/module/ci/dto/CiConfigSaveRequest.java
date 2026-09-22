package com.atp.module.ci.dto;

import lombok.Data;

/**
 * CI 配置保存请求（新增/更新）。令牌由后端生成，不在请求体中传入。
 */
@Data
public class CiConfigSaveRequest {

    /** 配置ID（更新时传入，新增时留空） */
    private Long id;

    /** 项目ID（必填） */
    private Long projectId;

    /** 默认执行环境ID */
    private Long defaultEnvId;

    /** 默认批次ID */
    private Long defaultBatchId;

    /** 批次完成回调地址（可选） */
    private String callbackUrl;

    /** 是否启用：true 启用 */
    private Boolean enabled;
}
