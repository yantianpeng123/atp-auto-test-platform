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

    /** 所属工程ID，可空；为空时沿用原工程（用于重新归属） */
    private Long applicationId;

    /** 工程描述，可空；不为 null 时同步覆盖所属工程的 description */
    private String description;
}
