package com.atp.module.base.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 下拉选项出参
 */
@Data
@Builder
public class OptionVO {

    private Long id;

    private String name;
}
