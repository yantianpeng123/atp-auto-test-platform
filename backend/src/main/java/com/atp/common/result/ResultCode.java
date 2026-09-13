package com.atp.common.result;

import lombok.Getter;

/**
 * 统一响应码
 *
 * <p>分段规则：
 * <ul>
 *   <li>200      成功</li>
 *   <li>400-499  客户端错误</li>
 *   <li>1001+    各业务模块错误（1001 用户 / 2001 项目 / 3001 用例）</li>
 *   <li>500      服务端异常</li>
 * </ul>
 */
@Getter
public enum ResultCode {

    SUCCESS(200, "操作成功"),

    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "没有操作权限"),
    NOT_FOUND(404, "资源不存在"),

    USERNAME_EXISTS(1001, "用户名已被注册"),
    EMAIL_EXISTS(1002, "邮箱已被注册"),
    USER_NOT_FOUND(1003, "用户不存在"),
    PASSWORD_ERROR(1004, "账号或密码错误"),
    ACCOUNT_DISABLED(1005, "账号已被禁用"),
    PASSWORD_NOT_MATCH(1006, "两次输入的密码不一致"),
    CAPTCHA_ERROR(1007, "验证码错误或已过期"),
    TOKEN_INVALID(1008, "令牌无效"),

    PROJECT_NOT_FOUND(2001, "项目不存在"),
    PROJECT_NAME_EXISTS(2002, "工程名称已存在"),
    VERSION_NOT_FOUND(2003, "版本不存在"),
    VERSION_NAME_EXISTS(2004, "版本名称已存在"),
    MODULE_NOT_FOUND(2005, "模块不存在"),
    MODULE_NAME_EXISTS(2006, "模块名称已存在"),
    ENV_NOT_FOUND(2101, "环境不存在"),
    ENV_NAME_EXISTS(2102, "同一项目下环境名称已存在"),
    CASE_NOT_FOUND(3001, "用例不存在"),

    PLAN_NOT_FOUND(3101, "测试计划不存在"),
    CASE_LINKED(3102, "用例不存在或不属于当前项目"),
    PLAN_NAME_EXISTS(3103, "测试计划名称已存在"),

    BATCH_NOT_FOUND(3201, "定时任务批次不存在"),
    BATCH_NAME_EXISTS(3201,"定时任务已存在"),
    API_NAME_EXISTS(4001,"请求路径和请求方式已存在"),


    ERROR(500, "服务器内部错误");

    private final Integer code;
    private final String message;

    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
