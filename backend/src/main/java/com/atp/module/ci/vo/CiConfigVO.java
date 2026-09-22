package com.atp.module.ci.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * CI 配置出参。token 仅在「新建 / 重新生成」时返回一次明文，之后查询不再返回。
 */
@Data
@Builder
public class CiConfigVO {

    private Long id;
    private Long projectId;
    private Long defaultEnvId;
    private Long defaultBatchId;
    private String callbackUrl;
    private Integer enabled;

    /** 一次性明文令牌（仅新建/重新生成时非空） */
    private String token;

    /** 令牌后缀提示（如 ...a1b2），便于在无明文时辨认 */
    private String tokenHint;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
