package com.atp.module.execute.service.impl;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.atp.common.exception.BizException;
import com.atp.common.result.ResultCode;
import com.atp.module.base.entity.ApiDefinition;
import com.atp.module.base.mapper.ApiDefinitionMapper;
import com.atp.module.env.entity.TestEnv;
import com.atp.module.env.mapper.TestEnvMapper;
import com.atp.module.execute.core.AssertionEngine;
import com.atp.module.execute.core.HttpExecutor;
import com.atp.module.execute.core.HttpResponse;
import com.atp.module.execute.core.VariableResolver;
import com.atp.module.execute.core.Variables;
import com.atp.module.execute.dto.CaseExecuteRequest;
import com.atp.module.execute.service.ExecuteService;
import com.atp.module.execute.vo.AssertionResultVO;
import com.atp.module.execute.vo.CaseExecuteVO;
import com.atp.module.execute.vo.RoundExecuteVO;
import com.atp.module.execute.vo.StepExecuteVO;
import com.atp.module.testcase.entity.DatasetItem;
import com.atp.module.testcase.entity.DatasetTemplate;
import com.atp.module.testcase.entity.TestCase;
import com.atp.module.testcase.mapper.CaseStepMapper;
import com.atp.module.testcase.mapper.DatasetItemMapper;
import com.atp.module.testcase.mapper.DatasetTemplateMapper;
import com.atp.module.testcase.mapper.TestCaseMapper;
import com.atp.module.testcase.vo.CaseStepVO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 用例执行服务实现：串行执行用例步骤，支持变量传递、断言校验、参数提取。
 */
@Service
@RequiredArgsConstructor
public class ExecuteServiceImpl implements ExecuteService {

    private final TestCaseMapper testCaseMapper;
    private final CaseStepMapper caseStepMapper;
    private final ApiDefinitionMapper apiDefinitionMapper;
    private final TestEnvMapper testEnvMapper;
    private final HttpExecutor httpExecutor;
    private final AssertionEngine assertionEngine;
    private final DatasetTemplateMapper datasetTemplateMapper;
    private final DatasetItemMapper datasetItemMapper;

    @Override
    public CaseExecuteVO executeCase(Long caseId, CaseExecuteRequest request) {
        TestCase testCase = testCaseMapper.selectById(caseId);
        if (testCase == null) {
            throw new BizException(ResultCode.CASE_NOT_FOUND);
        }
        TestEnv env = testEnvMapper.selectById(request.getEnvId());
        if (env == null) {
            throw new BizException(ResultCode.ENV_NOT_FOUND);
        }

        List<CaseStepVO> stepVOs = caseStepMapper.selectStepsByCaseId(caseId);

        // 读取参数化数据（多行 → 多轮）
        List<Map<String, Object>> rows = loadDatasetRows(caseId);
        int totalRounds = Math.max(1, rows.size());

        long caseStart = System.currentTimeMillis();
        List<RoundExecuteVO> rounds = new ArrayList<>();
        int passedRounds = 0;
        int failedRounds = 0;

        for (int i = 0; i < totalRounds; i++) {
            Variables variables = new Variables();
            Map<String, Object> params = new LinkedHashMap<>();
            if (i < rows.size()) {
                params = rows.get(i);
                params.forEach(variables::put);
            }
            VariableResolver resolver = new VariableResolver(variables);

            long roundStart = System.currentTimeMillis();
            List<StepExecuteVO> stepResults = new ArrayList<>();
            int passed = 0;
            int failed = 0;
            for (CaseStepVO stepVO : stepVOs) {
                StepExecuteVO stepResult = executeStep(stepVO, env, resolver, variables);
                stepResults.add(stepResult);
                if ("PASSED".equals(stepResult.getStatus())) {
                    passed++;
                } else {
                    failed++;
                }
            }
            long roundDuration = System.currentTimeMillis() - roundStart;
            boolean roundPassed = failed == 0;
            if (roundPassed) {
                passedRounds++;
            } else {
                failedRounds++;
            }

            rounds.add(RoundExecuteVO.builder()
                    .roundIndex(i + 1)
                    .params(params)
                    .passedSteps(passed)
                    .failedSteps(failed)
                    .status(roundPassed ? "SUCCESS" : "FAILED")
                    .durationMs(roundDuration)
                    .steps(stepResults)
                    .build());
        }

        long duration = System.currentTimeMillis() - caseStart;
        return CaseExecuteVO.builder()
                .caseId(caseId)
                .caseName(testCase.getName())
                .envId(env.getId())
                .totalRounds(totalRounds)
                .passedRounds(passedRounds)
                .failedRounds(failedRounds)
                .status(failedRounds == 0 ? "SUCCESS" : "FAILED")
                .durationMs(duration)
                .rounds(rounds)
                .build();
    }

    private List<Map<String, Object>> loadDatasetRows(Long caseId) {
        List<DatasetTemplate> templates = datasetTemplateMapper.selectList(
                new QueryWrapper<DatasetTemplate>().eq("case_id", caseId));
        if (templates == null || templates.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (DatasetTemplate template : templates) {
            List<DatasetItem> items = datasetItemMapper.selectList(
                    new QueryWrapper<DatasetItem>()
                            .eq("template_id", template.getId())
                            .orderByAsc("sort_order"));
            for (DatasetItem item : items) {
                if (item.getData() == null || item.getData().isBlank()) {
                    continue;
                }
                try {
                    JSONObject data = JSONUtil.parseObj(item.getData());
                    rows.add(data);
                } catch (Exception ignored) {
                    // 忽略非法 JSON
                }
            }
        }
        return rows;
    }

    private StepExecuteVO executeStep(CaseStepVO stepVO, TestEnv env,
                                      VariableResolver resolver, Variables variables) {
        long start = System.currentTimeMillis();
        StepExecuteVO.StepExecuteVOBuilder builder = StepExecuteVO.builder()
                .stepId(stepVO.getId())
                .stepName(stepVO.getStepName())
                .sortOrder(stepVO.getSortOrder())
                .method(stepVO.getApiMethod())
                .status("ERROR");

        try {
            ApiDefinition api = apiDefinitionMapper.selectById(stepVO.getApiId());
            if (api == null) {
                return builder.errorMsg("接口不存在或已被删除").durationMs(System.currentTimeMillis() - start).build();
            }

            String method = api.getMethod() == null ? "GET" : api.getMethod().toUpperCase();
            String path = resolver.resolve(api.getPath());

            // 合并请求头（环境 -> 接口 -> 步骤覆盖），并做变量替换
            Map<String, String> headers = mergeHeaders(env, api, stepVO.getRequestOverride());
            headers.replaceAll((k, v) -> resolver.resolve(v));

            // 合并请求体（接口默认 body + 步骤覆盖 body），并做变量替换
            JSONObject body = mergeBody(api, stepVO.getRequestOverride());
            resolveJsonValues(body, resolver);

            // 拼接 URL
            String baseUrl = env.getBaseUrl() == null ? "" : env.getBaseUrl();
            String url = (path != null && path.startsWith("http")) ? path : baseUrl + (path == null ? "" : path);

            // GET / DELETE 请求体转 query 参数；其余作为 JSON body
            String bodyStr = null;
            if (!body.isEmpty()) {
                if ("GET".equals(method)) {
                    url = appendQuery(url, body);
                } else {
                    bodyStr = body.toString();
                }
                if("DELETE".equals(method)){
                    for (Map.Entry<String, Object> e : body.entrySet()) {
                        String query =e.getValue() == null ? "" : String.valueOf(e.getValue());
                        url=url+"/"+query;
                    }

                }
            }

            builder.method(method)
                    .url(url)
                    .requestHeaders(JSONUtil.toJsonStr(headers))
                    .requestBody(bodyStr);

            HttpResponse resp = httpExecutor.execute(method, url, headers, bodyStr);
            builder.statusCode(resp.getStatusCode())
                    .responseHeaders(resp.getHeaders() == null ? null : JSONUtil.toJsonStr(resp.getHeaders()))
                    .responseBody(resp.getBody())
                    .durationMs(resp.getDurationMs());

            if (!resp.isSuccess()) {
                return builder.errorMsg(resp.getError()).status("ERROR").build();
            }

            // 保存响应变量（完整响应：status/header/body）
            if (StringUtils.hasText(stepVO.getResponseVar())) {
                Map<String, Object> respData = new LinkedHashMap<>();
                respData.put("status", resp.getStatusCode());
                respData.put("header", resp.getHeaders());
                respData.put("body", resp.getBody());
                variables.put(stepVO.getResponseVar(), respData);
            }

            // 断言
            List<AssertionResultVO> assertResults = runAssertions(stepVO.getAssertions(),
                    resp.getStatusCode(), resp.getHeaders(), resp.getBody());
            builder.assertResults(assertResults);

            boolean allPassed = assertResults.stream().allMatch(AssertionResultVO::getPassed);
            builder.status(allPassed ? "PASSED" : "FAILED");
            return builder.build();
        } catch (Exception e) {
            return builder.errorMsg(e.getMessage()).durationMs(System.currentTimeMillis() - start).build();
        }
    }

    // ==================== 断言 / 提取 ====================

    private List<AssertionResultVO> runAssertions(String assertionsJson, int statusCode,
                                                  Map<String, String> respHeaders, String respBody) {
        List<AssertionResultVO> results = new ArrayList<>();
        for (Map<String, Object> assertion : parseArray(assertionsJson)) {
            results.add(assertionEngine.evaluate(assertion, statusCode, respHeaders, respBody));
        }
        return results;
    }

    // ==================== 请求合并 ====================

    private Map<String, String> mergeHeaders(TestEnv env, ApiDefinition api, String requestOverride) {
        Map<String, String> headers = new LinkedHashMap<>();
        putAllString(headers, parseObject(env.getHeaders()));
        putAllString(headers, parseObject(api.getHeaders()));
        putAllString(headers, overrideObject(requestOverride, "headers"));
        return headers;
    }

    private JSONObject mergeBody(ApiDefinition api, String requestOverride) {
        JSONObject body = parseObject(api.getBody());
        if (body == null) {
            body = new JSONObject();
        }
        JSONObject overrideBody = overrideObject(requestOverride, "body");
        if (overrideBody != null && !overrideBody.isEmpty()) {
            body.putAll(overrideBody);
        }
        return body;
    }

    private void putAllString(Map<String, String> target, JSONObject obj) {
        if (obj == null) {
            return;
        }
        for (Map.Entry<String, Object> e : obj.entrySet()) {
            Object v = e.getValue();
            target.put(e.getKey(), v == null ? "" : String.valueOf(v));
        }
    }

    private JSONObject overrideObject(String requestOverride, String key) {
        JSONObject obj = parseObject(requestOverride);
        if (obj == null) {
            return null;
        }
        Object value = obj.get(key);
        return value instanceof JSONObject jo ? jo : null;
    }

    private void resolveJsonValues(JSONObject obj, VariableResolver resolver) {
        if (obj == null) {
            return;
        }
        for (Map.Entry<String, Object> e : obj.entrySet()) {
            Object v = e.getValue();
            if (v instanceof String s) {
                e.setValue(resolver.resolve(s));
            } else if (v instanceof JSONObject jo) {
                resolveJsonValues(jo, resolver);
            } else if (v instanceof JSONArray ja) {
                resolveJsonArray(ja, resolver);
            }
        }
    }

    private void resolveJsonArray(JSONArray arr, VariableResolver resolver) {
        for (int i = 0; i < arr.size(); i++) {
            Object v = arr.get(i);
            if (v instanceof String s) {
                arr.set(i, resolver.resolve(s));
            } else if (v instanceof JSONObject jo) {
                resolveJsonValues(jo, resolver);
            } else if (v instanceof JSONArray ja) {
                resolveJsonArray(ja, resolver);
            }
        }
    }

    private String appendQuery(String url, JSONObject body) {


        StringBuilder q = new StringBuilder();
        for (Map.Entry<String, Object> e : body.entrySet()) {
            if (!q.isEmpty()) {
                q.append('&');
            }
            String value = e.getValue() == null ? "" : String.valueOf(e.getValue());
            q.append(URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8))
                    .append('=')
                    .append(URLEncoder.encode(value, StandardCharsets.UTF_8));
        }
        if (q.isEmpty()) {
            return url;
        }
        String sep = url.contains("?") ? "&" : "?";
        return url + sep + q;
    }

    // ==================== JSON 解析工具 ====================

    private JSONObject parseObject(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return JSONUtil.parseObj(json);
        } catch (Exception e) {
            return null;
        }
    }

    private List<Map<String, Object>> parseArray(String json) {
        List<Map<String, Object>> list = new ArrayList<>();
        if (json == null || json.isBlank()) {
            return list;
        }
        try {
            JSONArray arr = JSONUtil.parseArray(json);
            for (Object o : arr) {
                if (o instanceof JSONObject jo) {
                    list.add(jo);
                }
            }
        } catch (Exception ignored) {
            // 非法 JSON 忽略，返回空列表
        }
        return list;
    }
}
