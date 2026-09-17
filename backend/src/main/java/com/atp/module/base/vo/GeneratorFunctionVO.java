package com.atp.module.base.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 生成器函数帮助信息（供前端做语法提示/函数清单展示）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneratorFunctionVO {

    /** 函数名，如 randomInt */
    private String name;

    /** 参数说明，如 min,max */
    private String args;

    /** 中文说明 */
    private String desc;

    /** 示例，如 ${randomInt(1000,9999)} */
    private String example;
}
