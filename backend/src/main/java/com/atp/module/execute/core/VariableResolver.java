package com.atp.module.execute.core;

import cn.hutool.json.JSONUtil;
import com.atp.common.exception.BizException;
import com.atp.common.result.ResultCode;
import com.atp.module.base.generator.GeneratorEngine;
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
     *
     * <p>两遍解析：
     * <ol>
     *   <li>第一遍：把 {@code ${varName}} / {@code ${varName.path}} 换成变量池当前值；</li>
     *   <li>第二遍：把 {@code ${func(args)}} 交给 {@link GeneratorEngine} 现生成
     *       （如 {@code ${phone()}}、{@code ${randomInt(1000,9999)}}）。</li>
     * </ol>
     * 本类的正则只匹配 {@code ${var}}，不会命中带括号的 {@code ${func()}}，两遍互不干扰。
     */
    public String resolve(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        String result = input;
        for (int depth = 0; depth < MAX_DEPTH; depth++) {
            Matcher matcher = PATTERN.matcher(result);
            if (!matcher.find()) {
                // 变量已替换完毕，再跑一遍生成函数
                return GeneratorEngine.parse(result);
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
        return GeneratorEngine.parse(result);
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
            // 变量不存在，且名字正好是某个生成函数 —— 几乎可以肯定是漏写了括号：
            // ${timestamp} 会被当未知变量替换成空字符串，而真正想要的是 ${timestamp()}。
            // 这条路径原本就只会静默返回空串，抛错不影响任何正常行为。
            if (path == null && GeneratorEngine.isFunction(varName)) {
                throw new BizException(ResultCode.BAD_REQUEST, "生成函数必须写成调用形式：${" + varName
                        + "()}，可用函数见 /api/base/generator/functions");
            }
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
