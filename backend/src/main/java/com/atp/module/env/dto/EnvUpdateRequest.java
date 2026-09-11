package com.atp.module.env.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 编辑环境入参
 */
@Data
public class EnvUpdateRequest {

    @NotNull(message = "环境ID不能为空")
    private Long id;

    @NotBlank(message = "请输入环境名称")
    @Size(max = 50, message = "环境名称最长 50 个字符")
    private String name;

    @Size(max = 255, message = "基础域名最长 255 个字符")
    private String baseUrl;

    /** 全局请求头，JSON 字符串 */
    private String headers;

    /** 数据库配置，JSON 字符串 */
    private String dbConfig;
}
