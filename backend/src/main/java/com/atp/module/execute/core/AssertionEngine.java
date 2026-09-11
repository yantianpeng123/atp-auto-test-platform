package com.atp.module.execute.core;

import com.atp.module.execute.vo.AssertionResultVO;
import com.jayway.jsonpath.JsonPath;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 断言引擎：对响应执行断言规则。
 *
 * <p>断言类型：status（状态码）、jsonPath（JSONPath 取值）、header（响应头）、body（响应体文本）。
 * 操作符：eq / notEq / contains / exists。
 */
@Component
public class AssertionEngine {

    public AssertionResultVO evaluate(Map<String, Object> assertion, int statusCode,
                                      Map<String, String> respHeaders, String respBody) {
        String type = str(assertion.get("type"));
        String operator = str(assertion.getOrDefault("operator", "eq"));
        String path = assertion.get("path") == null ? null : String.valueOf(assertion.get("path"));
        String expected = assertion.get("expected") == null ? null : String.valueOf(assertion.get("expected"));

        AssertionResultVO.AssertionResultVOBuilder builder = AssertionResultVO.builder()
                .type(type).path(path).operator(operator).expected(expected);

        switch (type == null ? "" : type) {
            case "status":
                return finish(builder, String.valueOf(statusCode), compare(String.valueOf(statusCode), operator, expected), expected);
            case "header":
                String headerValue = respHeaders.get(path);
                return finish(builder, headerValue, compare(headerValue, operator, expected), expected);
            case "jsonPath":
                String jsonValue = readJsonPath(respBody, path);
                return finish(builder, jsonValue, compare(jsonValue, operator, expected), expected);
            case "body":
                boolean passed;
                if ("contains".equals(operator)) {
                    passed = respBody != null && expected != null && respBody.contains(expected);
                } else if ("eq".equals(operator)) {
                    passed = expected != null && expected.equals(respBody);
                } else if ("exists".equals(operator)) {
                    passed = respBody != null && !respBody.isEmpty();
                } else {
                    passed = false;
                }
                return finish(builder, null, passed, expected);
            default:
                return builder.actual(null).passed(false).message("未知断言类型：" + type).build();
        }
    }

    private AssertionResultVO finish(AssertionResultVO.AssertionResultVOBuilder builder, String actual,
                                     boolean passed, String expected) {
        String message = passed ? "断言通过" : "断言失败：期望 " + expected + "，实际 " + actual;
        return builder.actual(actual).passed(passed).message(message).build();
    }

    private boolean compare(String actual, String operator, String expected) {
        if (operator == null) {
            return false;
        }
        switch (operator) {
            case "eq":
                return expected != null && expected.equals(actual);
            case "notEq":
                return actual != null && !actual.equals(expected);
            case "contains":
                return actual != null && expected != null && actual.contains(expected);
            case "exists":
                return actual != null && !actual.isEmpty();
            default:
                return false;
        }
    }

    private String readJsonPath(String body, String path) {
        if (body == null || path == null) {
            return null;
        }
        try {
            Object value = JsonPath.read(body, path);
            return value == null ? null : String.valueOf(value);
        } catch (Exception e) {
            return null;
        }
    }

    private String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }
}
