package com.atp.module.base.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

/**
 * 生成器试生成入参。
 *
 * <p>{@code type=CUSTOM} 时 params 里传 {@code template}（形如 {@code NO-${randomInt(6)}}）；
 * 其余类型传各自结构化参数，由服务端翻译成函数调用后执行。
 */
@Data
public class GeneratorPreviewRequest {

    private Long projectId;

    /** RANDOM/PHONE/IDCARD/NAME/ENUM/TIMESTAMP/UUID/CUSTOM */
    @NotBlank(message = "生成器类型不能为空")
    private String type;

    private Map<String, Object> params;
}
