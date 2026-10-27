package com.atp.module.base.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 版本重命名入参
 */
@Data
public class VersionUpdateRequest {

    @NotNull(message = "版本ID不能为空")
    private Long id;

    @NotBlank(message = "版本名称不能为空")
    private String name;
}
