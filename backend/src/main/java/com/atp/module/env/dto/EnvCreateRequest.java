package com.atp.module.env.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 新增环境入参
 */
@Data
public class EnvCreateRequest {

    @NotNull(message = "请选择所属项目")
    private Long projectId;

    @NotBlank(message = "请输入环境名称")
    @Size(max = 50, message = "环境名称最长 50 个字符")
    private String name;

    @Size(max = 255, message = "基础域名最长 255 个字符")
    private String baseUrl;

    /** 全局请求头，JSON 字符串，如 {"token":"xxx"} */
    private String headers;

    /** 数据库配置，JSON 字符串 */
    private String dbConfig;
}
