package com.atp.module.ci.dto;

import lombok.Data;

/**
 * CI 触发请求：外部 CI（Jenkins 等）携带令牌调用 /api/ci/trigger。
 * projectId 用于定位 CI 配置；batchId / envId 缺省时取配置中的默认值。
 */
@Data
public class CiTriggerRequest {

    /** 项目ID（必填，用于定位 CI 配置） */
    private Long projectId;

    /** 要执行的批次ID（可选，缺省取配置 defaultBatchId） */
    private Long batchId;

    /** 执行环境ID（可选，缺省取配置 defaultEnvId） */
    private Long envId;
}
