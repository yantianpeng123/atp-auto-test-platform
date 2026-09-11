package com.atp.module.execute.core;

import cn.hutool.json.JSONUtil;
import com.jayway.jsonpath.JsonPath;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 变量解析器：把字符串中的 ${...} 替换为变量上下文中的实际值（支持嵌套替换，限制递归深度防循环）。
 *
 * <p>支持两种引用：
 * <ul>
 *   <li>{@code ${varName}}：引用变量原始值（字符串，或整个响应对象 JSON）</li>
 *   <li>{@code ${varName.path}}：从响应变量按路径解析——默认从 body 根解析 JSONPath；
 *       {@code ${varName.header.x}} 取响应头；{@code ${varName.status}} 取状态码</li>
 * </ul>
 */
public class VariableResolver {

    private static final Pattern PATTERN = Pattern.compile("\\$\\{([\\w.\\[\\]-]+)\\}");
    private static final int MAX_DEPTH = 10;

    private final Variables variables;

    public VariableResolver(Variables variables) {
        this.variables = variables;
    }

    /**
     * 替换字符串中所有 ${...} 占位符。
     */
    public String resolve(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        String result = input;
        for (int depth = 0; depth < MAX_DEPTH; depth++) {
            Matcher matcher = PATTERN.matcher(result);
            if (!matcher.find()) {
                return result;
            }
            matcher.reset();
            StringBuilder sb = new StringBuilder();
            while (matcher.find()) {
                String name = matcher.group(1);
                String replacement = resolveValue(name);
                matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
            }
            matcher.appendTail(sb);
            result = sb.toString();
        }
        return result;
    }

    /**
     * 解析单个引用表达式（如 loginResp / loginResp.data.token / loginResp.header.Authorization）。
     */
    private String resolveValue(String name) {
        int dot = name.indexOf('.');
        String varName = dot <= 0 ? name : name.substring(0, dot);
        String path = dot <= 0 ? null : name.substring(dot + 1);

        Object value = variables.get(varName);
        if (value == null) {
            return "";
        }
        if (path == null || path.isEmpty()) {
            return stringify(value);
        }
        if (value instanceof Map<?, ?> map) {
            return resolveFromMap(map, path);
        }
        // 简单值 + 路径：从字符串 JSONPath 解析（兼容旧逻辑）
        return jsonPath(String.valueOf(value), path);
    }

    private String resolveFromMap(Map<?, ?> resp, String path) {
        if ("status".equals(path)) {
            return stringify(resp.get("status"));
        }
        if (path.startsWith("header.")) {
            String headerName = path.substring("header.".length());
            Object headers = resp.get("header");
            if (headers instanceof Map<?, ?> headerMap) {
                return stringify(headerMap.get(headerName));
            }
            return "";
        }
        // 默认从 body 根解析 JSONPath
        String bodyPath = path.startsWith("body.") ? path.substring("body.".length()) : path;
        Object body = resp.get("body");
        return body == null ? "" : jsonPath(String.valueOf(body), bodyPath);
    }

    private String jsonPath(String json, String path) {
        try {
            Object result = JsonPath.read(json, "$." + path);
            return result == null ? "" : String.valueOf(result);
        } catch (Exception e) {
            return "";
        }
    }

    private String stringify(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof String s) {
            return s;
        }
        if (value instanceof Number || value instanceof Boolean || value instanceof Character) {
            return String.valueOf(value);
        }
        return JSONUtil.toJsonStr(value);
    }
}
