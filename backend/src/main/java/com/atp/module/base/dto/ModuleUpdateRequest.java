package com.atp.module.base.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 模块重命名入参
 */
@Data
public class ModuleUpdateRequest {

    @NotNull(message = "模块ID不能为空")
    private Long id;

    @NotBlank(message = "模块名称不能为空")
    private String name;
}
