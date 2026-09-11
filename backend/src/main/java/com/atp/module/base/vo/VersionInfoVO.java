package com.atp.module.base.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工程版本信息出参（模块 → 版本 → 工程 联查结果）
 */
@Data
@Builder
public class VersionInfoVO {

    private Long moduleId;

    private String moduleName;

    private Long versionId;

    private String versionName;

    private Long applicationId;

    private String applicationName;

    private LocalDateTime updateTime;
}
