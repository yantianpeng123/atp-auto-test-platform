package com.atp.module.base.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 数据生成器出参（对齐前端 {@code DataGeneratorInfo}）
 *
 * <p>{@code params} 以结构化对象返回给前端，前端按 {@code Record<string, unknown>} 透传。
 */
@Data
@Builder
public class DataGeneratorVO {

    private Long id;

    private Long projectId;

    private String name;

    private String type;

    private Map<String, Object> params;

    private String description;

    private LocalDateTime createTime;
}
