package com.atp.module.ci.vo;

import lombok.Builder;
import lombok.Data;

/**
 * CI 触发返回：立即返回运行实例ID与轮询地址，批次在后台异步执行。
 */
@Data
@Builder
public class CiTriggerResultVO {

    /** 批次运行实例ID（用于轮询） */
    private Long runId;

    /** 轮询状态地址（供 CI 调用 GET /api/ci/result/{runId}） */
    private String statusUrl;

    /** 当前状态（刚触发时为 RUNNING） */
    private String status;
}
