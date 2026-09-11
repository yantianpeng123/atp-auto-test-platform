package com.atp.module.base.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 编辑接口入参
 */
@Data
public class ApiUpdateRequest {

    @NotNull(message = "接口ID不能为空")
    private Long id;

    @NotBlank(message = "接口名称不能为空")
    @Size(max = 100, message = "接口名称最长 100 个字符")
    private String name;

    @NotBlank(message = "请求方法不能为空")
    @Size(max = 10, message = "请求方法最长 10 个字符")
    private String method;

    @NotBlank(message = "请求路径不能为空")
    @Size(max = 500, message = "请求路径最长 500 个字符")
    private String path;

    /** 请求头模板（JSON 字符串） */
    private String headers;

    /** 请求体模板（JSON 字符串） */
    private String body;

    @Size(max = 500, message = "描述最长 500 个字符")
    private String description;
}
