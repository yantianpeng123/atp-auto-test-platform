package com.atp.module.execute.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 单条断言执行结果。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssertionResultVO {

    /** 断言类型：status / jsonPath / header / body */
    private String type;

    /** jsonPath 表达式或响应头名称 */
    private String path;

    /** 比较操作符：eq / notEq / contains / exists */
    private String operator;

    /** 期望值 */
    private String expected;

    /** 实际值 */
    private String actual;

    /** 是否通过 */
    private Boolean passed;

    private String message;
}
