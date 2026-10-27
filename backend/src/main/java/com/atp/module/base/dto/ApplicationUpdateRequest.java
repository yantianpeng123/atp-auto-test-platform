package com.atp.module.base.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 工程重命名入参
 */
@Data
public class ApplicationUpdateRequest {

    @NotNull(message = "工程ID不能为空")
    private Long id;

    @NotBlank(message = "工程名称不能为空")
    private String name;
}
