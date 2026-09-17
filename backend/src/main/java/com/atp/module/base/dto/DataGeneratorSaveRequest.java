package com.atp.module.base.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Map;

/**
 * 数据生成器新增 / 编辑入参（对齐前端 {@code DataGeneratorSaveParams}）
 *
 * <p>新增时 {@code id} 为空，编辑时必填。
 */
@Data
public class DataGeneratorSaveRequest {

    private Long id;

    @NotNull(message = "项目不能为空")
    private Long projectId;

    @NotBlank(message = "生成器名称不能为空")
    @Size(max = 100, message = "生成器名称不能超过 100 个字符")
    private String name;

    /** RANDOM/PHONE/IDCARD/NAME/ENUM/TIMESTAMP/UUID/CUSTOM */
    @NotBlank(message = "生成器类型不能为空")
    private String type;

    /** 结构化参数，随 type 而定；CUSTOM 时为 { template: "..." }。无参类型可传空 */
    private Map<String, Object> params;

    @Size(max = 500, message = "说明不能超过 500 个字符")
    private String description;
}
