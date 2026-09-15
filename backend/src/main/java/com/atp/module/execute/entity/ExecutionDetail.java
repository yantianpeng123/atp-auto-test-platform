package com.atp.module.execute.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 执行明细表：每轮每步一行（execution_id + round_index + step_index 唯一定位一个步骤的实际请求/响应）。
 * 对应 tb_execution_detail。
 */
@Data
@TableName("tb_execution_detail")
public class ExecutionDetail {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 执行记录ID */
    private Long executionId;

    /** 用例ID（冗余，便于按用例查历史） */
    private Long caseId;

    /** 轮次（参数化多轮从1起） */
    private Integer roundIndex;

    /** 步骤序号（同轮内排序） */
    private Integer stepIndex;

    /** 步骤ID快照 */
    private Long stepId;

    /** 所属组合组件ID（组件展开子步骤时填写） */
    private Long componentId;

    /** 父步骤ID（组件展开时为容器步骤ID） */
    private Long parentStepId;

    /** 嵌套层级：0-用例直接步骤 1-组件内 2-嵌套组件内 */
    private Integer nestLevel;

    /** 步骤名称快照 */
    private String stepName;

    /** 实际请求方法 */
    private String method;

    /** 实际请求URL */
    private String url;

    /** 实际请求头（JSON 字符串） */
    private String requestHeaders;

    /** 实际请求体（JSON 字符串） */
    private String requestBody;

    /** 实际响应头（JSON 字符串） */
    private String responseHeaders;

    /** 实际响应体（JSON 字符串） */
    private String responseBody;

    /** 响应状态码 */
    private Integer statusCode;

    /** 步骤状态 PASSED/FAILED/ERROR */
    private String status;

    /** 错误信息 */
    private String errorMsg;

    private Long durationMs;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
