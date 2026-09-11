package com.atp.module.execute.core;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * HTTP 执行器：基于 OkHttp 发起实际请求并返回响应信息。
 */
@Component
public class HttpExecutor {

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final OkHttpClient client;

    public HttpExecutor() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .followRedirects(false)
                .followSslRedirects(false)
                .build();
    }

    /**
     * 发起 HTTP 请求。
     *
     * @param method  请求方法（大写）
     * @param url     完整 URL
     * @param headers 请求头
     * @param body    请求体（JSON 字符串，可为 null，GET/DELETE 应为 null）
     */
    public HttpResponse execute(String method, String url, Map<String, String> headers, String body) {
        long start = System.currentTimeMillis();
        try {
            Request.Builder builder = new Request.Builder().url(url);
            if (headers != null) {
                headers.forEach(builder::header);
            }
            RequestBody requestBody = null;
            if (body != null && !body.isEmpty()) {
                requestBody = RequestBody.create(body, JSON);
            }
            builder.method(method, requestBody);

            try (okhttp3.Response response = client.newCall(builder.build()).execute()) {
                String respBody = response.body() != null ? response.body().string() : "";
                Map<String, String> respHeaders = new LinkedHashMap<>();
                for (String name : response.headers().names()) {
                    respHeaders.put(name, response.header(name));
                }
                return HttpResponse.builder()
                        .success(true)
                        .statusCode(response.code())
                        .headers(respHeaders)
                        .body(respBody)
                        .durationMs(System.currentTimeMillis() - start)
                        .build();
            }
        } catch (Exception e) {
            return HttpResponse.builder()
                    .success(false)
                    .durationMs(System.currentTimeMillis() - start)
                    .error(e.getMessage())
                    .build();
        }
    }
}
