package com.atp.module.base.generator;

import com.atp.common.exception.BizException;
import com.atp.common.result.ResultCode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 生成表达式解析器（手写，不使用任何脚本引擎）。
 *
 * <p>职责：把字符串里的 {@code ${func(args)}} 替换为函数执行结果。
 * 只有 {@link GeneratorFunc} 白名单内登记过的函数名才会被执行，其余一律抛错。
 *
 * <p>与 {@code VariableResolver} 的分工：
 * <ul>
 *   <li>{@code VariableResolver} 的正则只匹配 {@code ${var}}，<strong>不会</strong>命中带括号的 {@code ${func()}}；</li>
 *   <li>因此先跑变量替换、再跑本解析器，两者互不干扰。</li>
 * </ul>
 *
 * <p>安全边界：无 eval / ScriptEngine；递归深度 ≤ {@value #MAX_DEPTH}；随机串长度上限见
 * {@link GeneratorFunc#MAX_STRING_LENGTH}。
 */
public final class GeneratorEngine {

    private static final int MAX_DEPTH = 10;

    private static final Map<String, GeneratorFunc> REGISTRY = new LinkedHashMap<>();

    static {
        for (GeneratorFunc func : GeneratorFunc.values()) {
            REGISTRY.put(func.funcName(), func);
        }
    }

    private GeneratorEngine() {
    }

    /** 白名单函数清单，供 {@code /functions} 接口返回给前端做语法提示 */
    public static List<GeneratorFunc> functions() {
        return List.copyOf(REGISTRY.values());
    }

    /**
     * 名字是否为已注册的生成函数。
     *
     * <p>用于给「漏写括号」的写法一个明确报错：{@code ${timestamp}} 会被
     * {@code VariableResolver} 当作未知变量替换成空字符串，而真正的写法是 {@code ${timestamp()}}。
     */
    public static boolean isFunction(String name) {
        return name != null && REGISTRY.containsKey(name);
    }

    /**
     * 解析字符串中所有 {@code ${func(args)}} 并返回替换后的结果。
     *
     * @throws BizException 遇到未注册的函数名，或参数不合法
     */
    public static String parse(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        return parse(input, 0);
    }

    /**
     * 按生成器类型试生成（供 {@code /preview} 使用）。
     *
     * <p>实现上把「类型 + 参数」翻译成一次函数调用再交给 {@link #parse}，
     * 保证预览结果与实际执行结果走同一套代码，不会出现两套逻辑不一致。
     */
    public static String generate(String type, Map<String, Object> params) {
        String t = type == null ? "" : type.trim().toUpperCase(Locale.ROOT);
        Map<String, Object> p = params == null ? Map.of() : params;
        String expr;
        switch (t) {
            case "CUSTOM" -> expr = str(p.get("template"));
            case "RANDOM" -> {
                int len = intOf(p.get("length"), 8);
                String charset = str(p.get("charset"));
                // 默认与前端 GeneratorFormDialog 的默认选项保持一致（digits）
                if (charset.isEmpty()) {
                    charset = "digits";
                }
                expr = "${randomString(" + len + "," + charset + ")}";
            }
            case "ENUM" -> expr = "${enum(" + joinCandidates(p.get("values")) + ")}";
            case "TIMESTAMP" -> {
                String format = str(p.get("format"));
                String offset = str(p.get("offset"));
                expr = "${timestamp(" + (format.isEmpty() ? "" : format)
                        + (offset.isEmpty() ? "" : "," + offset) + ")}";
            }
            case "PHONE" -> expr = "${phone()}";
            case "IDCARD" -> expr = "${idCard()}";
            case "NAME" -> expr = "${name()}";
            case "UUID" -> expr = "${uuid()}";
            default -> throw new BizException(ResultCode.BAD_REQUEST, "不支持的生成器类型：" + type);
        }
        String value = parse(expr);
        return str(p.get("prefix")) + value + str(p.get("suffix"));
    }

    private static String parse(String input, int depth) {
        if (depth > MAX_DEPTH) {
            throw new BizException(ResultCode.BAD_REQUEST, "生成表达式嵌套层级过深（最多 " + MAX_DEPTH + " 层）");
        }
        StringBuilder out = new StringBuilder();
        int i = 0;
        while (i < input.length()) {
            int start = input.indexOf("${", i);
            if (start < 0) {
                out.append(input.substring(i));
                break;
            }
            out.append(input, i, start);

            int nameStart = start + 2;
            int nameEnd = nameStart;
            while (nameEnd < input.length()
                    && (Character.isLetterOrDigit(input.charAt(nameEnd)) || input.charAt(nameEnd) == '_')) {
                nameEnd++;
            }
            String name = input.substring(nameStart, nameEnd);

            int p = nameEnd;
            while (p < input.length() && Character.isWhitespace(input.charAt(p))) {
                p++;
            }
            // 不是函数调用（如 ${varName}），原样保留，交给 VariableResolver 处理
            if (p >= input.length() || input.charAt(p) != '(') {
                out.append(input, start, nameEnd);
                i = nameEnd;
                continue;
            }

            int closeParen = findCloseParen(input, p);
            if (closeParen < 0) {
                out.append(input.substring(start));
                break;
            }
            if (closeParen + 1 >= input.length() || input.charAt(closeParen + 1) != '}') {
                out.append(input, start, closeParen + 1);
                i = closeParen + 1;
                continue;
            }

            List<String> args = new ArrayList<>();
            for (String rawArg : splitArgs(input.substring(p + 1, closeParen))) {
                // 嵌套项先递归解析，再去引号
                args.add(stripQuotes(parse(rawArg.trim(), depth + 1).trim()));
            }

            GeneratorFunc func = REGISTRY.get(name);
            if (func == null) {
                throw new BizException(ResultCode.BAD_REQUEST, "不支持的生成函数：" + name
                        + "，可用函数：" + String.join("、", REGISTRY.keySet()));
            }
            out.append(func.apply(args));
            i = closeParen + 2;
        }
        return out.toString();
    }

    /** 按顶层逗号切分参数，跳过嵌套的 ${...} 与引号内的逗号 */
    private static List<String> splitArgs(String raw) {
        List<String> out = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return out;
        }
        StringBuilder sb = new StringBuilder();
        char quoteChar = 0;
        int i = 0;
        while (i < raw.length()) {
            char c = raw.charAt(i);
            if (quoteChar != 0) {
                sb.append(c);
                if (c == quoteChar) {
                    quoteChar = 0;
                }
                i++;
                continue;
            }
            if (c == '"' || c == '\'') {
                quoteChar = c;
                sb.append(c);
                i++;
                continue;
            }
            if (c == '$' && i + 1 < raw.length() && raw.charAt(i + 1) == '{') {
                int end = findCloseBrace(raw, i + 1);
                if (end < 0) {
                    sb.append(c);
                    i++;
                    continue;
                }
                sb.append(raw, i, end + 1);
                i = end + 1;
                continue;
            }
            if (c == ',') {
                out.add(sb.toString());
                sb.setLength(0);
                i++;
                continue;
            }
            sb.append(c);
            i++;
        }
        if (sb.length() > 0 || !out.isEmpty()) {
            out.add(sb.toString());
        }
        return out;
    }

    /** s[openIdx] 为 '('，返回与之匹配的 ')' 下标；嵌套的 ${...} 整段跳过 */
    private static int findCloseParen(String s, int openIdx) {
        int depth = 0;
        int i = openIdx;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == '$' && i + 1 < s.length() && s.charAt(i + 1) == '{') {
                int end = findCloseBrace(s, i + 1);
                if (end < 0) {
                    return -1;
                }
                i = end + 1;
                continue;
            }
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
            i++;
        }
        return -1;
    }

    /** s[openIdx] 为 '{'，返回与之匹配的 '}' 下标 */
    private static int findCloseBrace(String s, int openIdx) {
        int depth = 0;
        int i = openIdx;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
            i++;
        }
        return -1;
    }

    private static String stripQuotes(String s) {
        if (s.length() >= 2 && (s.charAt(0) == '"' || s.charAt(0) == '\'')
                && s.charAt(s.length() - 1) == s.charAt(0)) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }

    private static String str(Object v) {
        return v == null ? "" : String.valueOf(v);
    }

    private static int intOf(Object v, int def) {
        if (v == null) {
            return def;
        }
        try {
            return Integer.parseInt(String.valueOf(v).trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    @SuppressWarnings("unchecked")
    private static String joinCandidates(Object values) {
        if (values == null) {
            return "";
        }
        if (values instanceof List<?> list) {
            return String.join(",", list.stream().map(String::valueOf).toList());
        }
        return String.valueOf(values);
    }
}
