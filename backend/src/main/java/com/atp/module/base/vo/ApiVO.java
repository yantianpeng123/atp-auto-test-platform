package com.atp.module.base.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 接口列表出参（接口 → 模块 → 版本 → 工程 联查结果）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiVO {

    private Long id;

    private String name;

    private String method;

    private String path;

    /** 请求头模板（JSON 字符串） */
    private String headers;

    /** 请求体模板（JSON 字符串） */
    private String body;

    private String description;

    private Long moduleId;

    private String moduleName;

    private Long versionId;

    private String versionName;

    private String sourceFlag;

    private Long applicationId;

    private String applicationName;

    private LocalDateTime updateTime;
}
