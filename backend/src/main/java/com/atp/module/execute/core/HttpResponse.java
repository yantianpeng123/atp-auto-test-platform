package com.atp.module.execute.core;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * HTTP 执行结果（由 HttpExecutor 返回）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HttpResponse {

    /** 是否成功拿到响应（false 表示连接/请求异常） */
    private boolean success;

    private int statusCode;

    private Map<String, String> headers;

    private String body;

    private long durationMs;

    private String error;
}
