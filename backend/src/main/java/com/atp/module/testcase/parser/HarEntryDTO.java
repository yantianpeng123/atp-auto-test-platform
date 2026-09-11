package com.atp.module.testcase.parser;

import lombok.Builder;
import lombok.Data;

/**
 * HAR 解析中间模型
 *
 * <p>由 {@link HarParser} 将 HAR JSON 中的单个 entry 转换而来，供 {@code CaseImportService}
 * 入库阶段使用，避免直接操作原始 HAR JSON。
 */
@Data
@Builder
public class HarEntryDTO {

    /** HTTP 方法（大写，如 GET/POST），原样保留 PATCH/HEAD 等 */
    private String method;

    /** 仅路径部分（不含协议/域名/端口/queryString），如 {@code /api/user/login} */
    private String path;

    /** 原始主机（含协议与端口），用于来源记录与问题排查 */
    private String host;

    /** 请求头（已剥离敏感信息），JSON 字符串，存 tb_api_definition.headers */
    private String headers;

    /** 请求体（已按 mimeType 转换），JSON 字符串或 null（无法解析时） */
    private String body;

    /** HAR 中的开始时间（ISO8601 字符串），用于步骤排序 */
    private String startedDateTime;

    /** 响应状态码（可能为 null，比如无响应或抓包时连接失败） */
    private Integer responseStatus;

    /** 解析过程中追加的备注（如"含二进制请求体"），最终拼到接口定义 description */
    private String note;
}
