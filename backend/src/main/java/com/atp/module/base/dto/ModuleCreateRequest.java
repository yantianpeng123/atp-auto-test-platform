package com.atp.module.base.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 新增模块入参（支持批量）
 */
@Data
public class ModuleCreateRequest {

    @NotNull(message = "请选择版本")
    private Long versionId;

    @NotEmpty(message = "模块名称不能为空")
    @Valid
    private List<@NotBlank(message = "模块名称不能为空") @Size(max = 100, message = "模块名称最长 100 个字符") String> names;

    @Size(max = 500, message = "工程描述最长 500 个字符")
    private String description;
}
