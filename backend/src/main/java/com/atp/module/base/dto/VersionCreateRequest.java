package com.atp.module.base.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 新增版本入参
 */
@Data
public class VersionCreateRequest {

    @NotNull(message = "请选择工程")
    private Long applicationId;

    @NotBlank(message = "版本名称不能为空")
    @Size(max = 100, message = "版本名称最长 100 个字符")
    private String name;

    @Size(max = 500, message = "工程描述最长 500 个字符")
    private String description;
}
