package com.atp.module.testcase.parser;

import com.atp.common.exception.BizException;
import com.atp.common.result.ResultCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * HAR 包解析器
 *
 * <p>职责：HAR JSON 字节流 → List&lt;{@link HarEntryDTO}&gt;，过程中完成：
 * <ul>
 *   <li>URL 拆解（剥离协议/域名/端口/queryString）</li>
 *   <li>敏感请求头剥离（Authorization/Cookie/Proxy-Authorization，大小写不敏感）</li>
 *   <li>请求体三分支处理（JSON / urlencoded / 二进制置 null）</li>
 *   <li>无效 entry 过滤（无 request、CONNECT、缺关键字段）</li>
 * </ul>
 *
 * <p>无状态组件，可独立单测；不访问数据库。
 */
@Component
public class HarParser {

    /** 敏感 header 黑名单（小写比较） */
    private static final Set<String> SENSITIVE_HEADERS = Set.of(
            "authorization",
            "cookie",
            "proxy-authorization"
    );

    /** tb_api_definition.path 列宽 500 */
    private static final int PATH_MAX_LENGTH = 500;

    private final ObjectMapper objectMapper;

    public HarParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 解析 HAR 字节流
     *
     * @param harBytes HAR 文件原始字节
     * @return 解析后的 entry 列表（已按 startedDateTime 升序）
     * @throws BizException 文件不是合法 JSON 或不含 log.entries
     */
    public List<HarEntryDTO> parse(byte[] harBytes) {
        if (harBytes == null || harBytes.length == 0) {
            throw new BizException(ResultCode.BAD_REQUEST, "HAR 文件为空");
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(harBytes);
        } catch (Exception e) {
            throw new BizException(ResultCode.BAD_REQUEST, "HAR 文件不是合法 JSON: " + e.getMessage());
        }
        JsonNode logNode = root.get("log");
        if (logNode == null || !logNode.has("entries") || !logNode.get("entries").isArray()) {
            throw new BizException(ResultCode.BAD_REQUEST, "HAR 文件缺少 log.entries 节点");
        }

        List<HarEntryDTO> result = new ArrayList<>();
        JsonNode entries = logNode.get("entries");
        for (JsonNode entry : entries) {
            HarEntryDTO dto = parseEntry(entry);
            if (dto != null) {
                result.add(dto);
            }
        }

        // 按 startedDateTime 升序排序
        result.sort((a, b) -> {
            if (a.getStartedDateTime() == null) return -1;
            if (b.getStartedDateTime() == null) return 1;
            return a.getStartedDateTime().compareTo(b.getStartedDateTime());
        });

        return result;
    }

    /** 解析单个 entry；无效时返回 null（被静默过滤） */
    private HarEntryDTO parseEntry(JsonNode entry) {
        JsonNode request = entry.get("request");
        if (request == null || request.isNull()) {
            return null;
        }

        String method = textOrNull(request.get("method"));
        if (method == null) {
            return null;
        }
        method = method.toUpperCase();

        String rawUrl = textOrNull(request.get("url"));
        if (rawUrl == null) {
            return null;
        }
        URLParts parts = splitUrl(rawUrl);
        if (parts == null) {
            return null;
        }
        String path = parts.path;
        if (path.length() > PATH_MAX_LENGTH) {
            // 路径过长，丢弃（防止入库截断/异常）
            return null;
        }

        // 请求头剥离
        String headersJson = buildHeadersJson(request.get("headers"));

        // 请求体
        BodyResult bodyResult = buildBody(request.get("postData"));
        String bodyJson = bodyResult.json;

        // 响应状态码
        Integer responseStatus = null;
        JsonNode response = entry.get("response");
        if (response != null && response.has("status") && response.get("status").isInt()) {
            responseStatus = response.get("status").asInt();
        }

        String startedDateTime = textOrNull(entry.get("startedDateTime"));

        return HarEntryDTO.builder()
                .method(method)
                .path(path)
                .host(parts.host)
                .headers(headersJson)
                .body(bodyJson)
                .startedDateTime(startedDateTime)
                .responseStatus(responseStatus)
                .note(bodyResult.note)
                .build();
    }

    /** URL 拆解 */
    private URLParts splitUrl(String rawUrl) {
        try {
            URI uri = new URI(rawUrl);
            String path = uri.getRawPath();
            if (!StringUtils.hasText(path)) {
                path = "/";
            }
            // 端口：显式指定则带上，否则不写
            String host = uri.getScheme() + "://" + uri.getHost();
            if (uri.getPort() > 0) {
                host = host + ":" + uri.getPort();
            }
            return new URLParts(path, host);
        } catch (URISyntaxException e) {
            return null;
        }
    }

    /**
     * 构造请求头 JSON 字符串
     *
     * <p>headers 节点结构：[{name, value, ...}, ...] → 转为 {name: value, ...} 后序列化为 JSON
     * 剥离大小写不敏感命中的敏感头
     */
    private String buildHeadersJson(JsonNode headersNode) {
        if (headersNode == null || !headersNode.isArray() || headersNode.size() == 0) {
            return null;
        }
        // 使用 TreeMap 让 JSON 中 key 有序，便于后续人工查看
        Map<String, String> map = new TreeMap<>();
        for (JsonNode h : headersNode) {
            String name = textOrNull(h.get("name"));
            if (name == null) continue;
            if (SENSITIVE_HEADERS.contains(name.toLowerCase())) {
                continue;
            }
            String value = textOrNull(h.get("value"));
            if (value == null) {
                value = "";
            }
            // 同名 header：后者覆盖前者（HAR 中罕见，但防御性处理）
            map.put(name, value);
        }
        if (map.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 请求体三分支处理
     *
     * <ul>
     *   <li>JSON 类（mimeType 含 json）且 text 可解析 → 序列化解析后的对象</li>
     *   <li>urlencoded → 拆解为键值对象</li>
     *   <li>其他（binary、text/plain 不可解析等）→ 返回 null，并把备注拼到 description</li>
     * </ul>
     */
    private BodyResult buildBody(JsonNode postData) {
        if (postData == null || postData.isNull()) {
            return new BodyResult(null, null);
        }
        String text = textOrNull(postData.get("text"));
        if (!StringUtils.hasText(text)) {
            return new BodyResult(null, null);
        }
        String mime = textOrNull(postData.get("mimeType"));
        if (mime == null) {
            mime = "";
        }
        String mimeLower = mime.toLowerCase();

        // 分支1：JSON
        if (mimeLower.contains("json")) {
            try {
                JsonNode parsed = objectMapper.readTree(text);
                return new BodyResult(objectMapper.writeValueAsString(parsed), null);
            } catch (Exception ignored) {
                return new BodyResult(null, "JSON 请求体解析失败");
            }
        }

        // 分支2：urlencoded
        if (mimeLower.contains("x-www-form-urlencoded")) {
            return new BodyResult(parseUrlEncoded(text), null);
        }

        // 分支3：其他（二进制、纯文本等无法结构化）→ 不入库
        if (mimeLower.startsWith("text/")) {
            // 纯文本：包成 {"raw": "..."} 保留可读性
            ObjectNode obj = objectMapper.createObjectNode();
            obj.put("raw", text);
            try {
                return new BodyResult(objectMapper.writeValueAsString(obj), null);
            } catch (Exception ignored) {
                return new BodyResult(null, "纯文本请求体序列化失败");
            }
        }
        return new BodyResult(null, "含无法解析的请求体（" + mime + "）");
    }

    /** 解析 application/x-www-form-urlencoded 文本为 JSON 字符串 */
    private String parseUrlEncoded(String text) {
        Map<String, String> map = new java.util.LinkedHashMap<>();
        for (String pair : text.split("&")) {
            if (pair.isEmpty()) continue;
            int eq = pair.indexOf('=');
            String k, v;
            if (eq < 0) {
                k = decode(pair);
                v = "";
            } else {
                k = decode(pair.substring(0, eq));
                v = decode(pair.substring(eq + 1));
            }
            map.put(k, v);
        }
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            return null;
        }
    }

    private static String decode(String s) {
        try {
            return java.net.URLDecoder.decode(s, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return s;
        }
    }

    private String textOrNull(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) return null;
        if (node.isTextual()) {
            String v = node.asText();
            return v.isEmpty() ? null : v;
        }
        return node.asText();
    }

    private static class URLParts {
        final String path;
        final String host;
        URLParts(String path, String host) {
            this.path = path;
            this.host = host;
        }
    }

    private static class BodyResult {
        final String json;
        final String note;
        BodyResult(String json, String note) {
            this.json = json;
            this.note = note;
        }
    }
}
