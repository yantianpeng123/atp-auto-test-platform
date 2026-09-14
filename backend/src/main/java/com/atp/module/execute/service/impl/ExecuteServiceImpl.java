package com.atp.module.execute.service.impl;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.atp.common.exception.BizException;
import com.atp.common.result.ResultCode;
import com.atp.common.result.PageResult;
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
import com.atp.module.execute.vo.ExecutionReportVO;
import com.atp.module.execute.vo.ExecutionSummaryVO;
import com.atp.module.plan.entity.TestPlan;
import com.atp.module.plan.mapper.TestPlanMapper;
import com.atp.module.user.entity.User;
import com.atp.module.user.mapper.UserMapper;
import com.atp.module.testcase.entity.DatasetItem;
import com.atp.module.testcase.entity.DatasetTemplate;
import com.atp.module.testcase.entity.TestCase;
import com.atp.module.testcase.mapper.CaseStepMapper;
import com.atp.module.testcase.mapper.DatasetItemMapper;
import com.atp.module.testcase.mapper.DatasetTemplateMapper;
import com.atp.module.testcase.mapper.TestCaseMapper;
import com.atp.module.testcase.vo.CaseStepVO;
import com.atp.module.execute.entity.Execution;
import com.atp.module.execute.entity.ExecutionAssertion;
import com.atp.module.execute.entity.ExecutionDetail;
import com.atp.module.execute.mapper.ExecutionAssertionMapper;
import com.atp.module.execute.mapper.ExecutionDetailMapper;
import com.atp.module.execute.mapper.ExecutionMapper;
import com.atp.security.UserPrincipal;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Objects;
import java.util.stream.Collectors;
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
@Transactional(rollbackFor = Exception.class)
public class ExecuteServiceImpl implements ExecuteService {

    private final TestCaseMapper testCaseMapper;
    private final CaseStepMapper caseStepMapper;
    private final ApiDefinitionMapper apiDefinitionMapper;
    private final TestEnvMapper testEnvMapper;
    private final HttpExecutor httpExecutor;
    private final AssertionEngine assertionEngine;
    private final DatasetTemplateMapper datasetTemplateMapper;
    private final DatasetItemMapper datasetItemMapper;
    private final ExecutionMapper executionMapper;
    private final ExecutionDetailMapper executionDetailMapper;
    private final ExecutionAssertionMapper executionAssertionMapper;
    private final TestPlanMapper testPlanMapper;
    private final UserMapper userMapper;

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

        CaseExecuteVO vo = CaseExecuteVO.builder()
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

        // ============ 持久化执行记录（头表 + 每轮每步明细 + 断言） ============
        // 调试运行不落库，仅正式「执行」才记录
        if (!Boolean.TRUE.equals(request.getDebug())) {
            persistExecution(caseId, testCase, env, vo, duration, request.getPlanId());
        }

        return vo;
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

    // ==================== 执行记录持久化 ====================

    /** 将一次执行结果落库：头表 + 每轮每步明细 + 每条断言 */
    private void persistExecution(Long caseId, TestCase testCase, TestEnv env,
                                  CaseExecuteVO vo, long duration, Long planId) {
        LocalDateTime start = LocalDateTime.now();
        Execution exec = new Execution();
        exec.setProjectId(testCase.getProjectId());
        exec.setPlanId(planId);
        exec.setCaseId(caseId);
        exec.setCaseName(testCase.getName());
        exec.setEnvId(env.getId());
        exec.setEnvName(env.getName());
        exec.setTriggerType(planId != null ? "SCHEDULED" : "MANUAL");
        exec.setExecutorId(currentUserId());
        exec.setStatus(vo.getStatus());
        exec.setTotalRounds(vo.getTotalRounds());
        exec.setPassedRounds(vo.getPassedRounds());
        exec.setFailedRounds(vo.getFailedRounds());

        int totalSteps = 0, passedSteps = 0, failedSteps = 0;
        for (RoundExecuteVO r : vo.getRounds()) {
            totalSteps += r.getSteps().size();
            passedSteps += r.getPassedSteps();
            failedSteps += r.getFailedSteps();
        }
        exec.setTotalSteps(totalSteps);
        exec.setPassedSteps(passedSteps);
        exec.setFailedSteps(failedSteps);
        exec.setDurationMs(duration);
        exec.setStartTime(start);
        exec.setEndTime(LocalDateTime.now());
        executionMapper.insert(exec);

        for (RoundExecuteVO round : vo.getRounds()) {
            for (int si = 0; si < round.getSteps().size(); si++) {
                StepExecuteVO s = round.getSteps().get(si);
                ExecutionDetail d = new ExecutionDetail();
                d.setExecutionId(exec.getId());
                d.setCaseId(caseId);
                d.setRoundIndex(round.getRoundIndex());
                d.setStepIndex(si);
                d.setStepId(s.getStepId());
                d.setStepName(s.getStepName());
                d.setMethod(s.getMethod());
                d.setUrl(s.getUrl());
                d.setRequestHeaders(s.getRequestHeaders());
                d.setRequestBody(s.getRequestBody());
                d.setResponseHeaders(s.getResponseHeaders());
                d.setResponseBody(s.getResponseBody());
                d.setStatusCode(s.getStatusCode());
                d.setStatus(s.getStatus());
                d.setErrorMsg(s.getErrorMsg());
                d.setDurationMs(s.getDurationMs());
                executionDetailMapper.insert(d);

                if (s.getAssertResults() != null) {
                    for (AssertionResultVO a : s.getAssertResults()) {
                        ExecutionAssertion ea = new ExecutionAssertion();
                        ea.setExecutionId(exec.getId());
                        ea.setDetailId(d.getId());
                        ea.setRoundIndex(round.getRoundIndex());
                        ea.setStepIndex(si);
                        ea.setType(a.getType());
                        ea.setPath(a.getPath());
                        ea.setOperator(a.getOperator());
                        ea.setExpected(a.getExpected());
                        ea.setActual(a.getActual());
                        ea.setPassed(a.getPassed() != null && a.getPassed() ? 1 : 0);
                        ea.setMessage(a.getMessage());
                        executionAssertionMapper.insert(ea);
                    }
                }
            }
        }
    }

    /** 取当前登录用户 ID（无登录上下文时返回 null） */
    private Long currentUserId() {
        try {
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (principal instanceof UserPrincipal up) {
                return up.getId();
            }
        } catch (Exception ignored) {
            // 非 Web 上下文或尚未认证
        }
        return null;
    }

    // ==================== 历史查询 / 报告 ====================

    @Override
    @Transactional(readOnly = true)
    public CaseExecuteVO getLatestExecution(Long caseId) {
        Execution exec = executionMapper.selectOne(new QueryWrapper<Execution>()
                .eq("case_id", caseId)
                .orderByDesc("create_time")
                .last("LIMIT 1"));
        if (exec == null) {
            return null;
        }
        List<RoundExecuteVO> rounds = buildRounds(exec.getId());
        return CaseExecuteVO.builder()
                .caseId(exec.getCaseId())
                .caseName(exec.getCaseName())
                .envId(exec.getEnvId())
                .totalRounds(exec.getTotalRounds())
                .passedRounds(exec.getPassedRounds())
                .failedRounds(exec.getFailedRounds())
                .status(exec.getStatus())
                .durationMs(exec.getDurationMs())
                .rounds(rounds)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ExecutionReportVO getExecutionReport(Long id) {
        Execution exec = executionMapper.selectById(id);
        if (exec == null) {
            throw new BizException(ResultCode.EXECUTION_NOT_FOUND);
        }
        List<RoundExecuteVO> rounds = buildRounds(exec.getId());

        String planName = null;
        if (exec.getPlanId() != null) {
            TestPlan plan = testPlanMapper.selectById(exec.getPlanId());
            planName = plan == null ? null : plan.getName();
        }
        String executorName = null;
        if (exec.getExecutorId() != null) {
            User user = userMapper.selectById(exec.getExecutorId());
            executorName = user == null ? null : user.getUsername();
        }

        return ExecutionReportVO.builder()
                .executionId(exec.getId())
                .planId(exec.getPlanId())
                .planName(planName)
                .caseId(exec.getCaseId())
                .caseName(exec.getCaseName())
                .envId(exec.getEnvId())
                .envName(exec.getEnvName())
                .triggerType(exec.getTriggerType())
                .executorId(exec.getExecutorId())
                .executorName(executorName)
                .status(exec.getStatus())
                .startTime(exec.getStartTime())
                .endTime(exec.getEndTime())
                .durationMs(exec.getDurationMs())
                .totalRounds(exec.getTotalRounds())
                .passedRounds(exec.getPassedRounds())
                .failedRounds(exec.getFailedRounds())
                .rounds(rounds)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<ExecutionSummaryVO> getReportPage(Long projectId, String keyword, String status,
                                                       long page, long size) {
        Page<Execution> p = new Page<>(page, size);
        QueryWrapper<Execution> qw = new QueryWrapper<>();
        if (projectId != null) {
            qw.eq("project_id", projectId);
        }
        if (status != null && !status.isBlank()) {
            qw.eq("status", status);
        }
        if (keyword != null && !keyword.isBlank()) {
            // 用例名模糊 OR 所属计划名模糊（先按计划名查 planId 集合）
            List<Long> planIds = testPlanMapper.selectList(
                    new QueryWrapper<TestPlan>().like("name", keyword))
                    .stream().map(TestPlan::getId).toList();
            qw.and(w -> {
                w.like("case_name", keyword);
                if (!planIds.isEmpty()) {
                    w.or().in("plan_id", planIds);
                }
            });
        }
        qw.orderByDesc("start_time");
        Page<Execution> result = executionMapper.selectPage(p, qw);

        List<Execution> records = result.getRecords();
        // 批量查计划名与执行人名，避免 N+1
        Map<Long, String> planNameMap = records.stream()
                .map(Execution::getPlanId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet())
                .stream()
                .map(testPlanMapper::selectById)
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(TestPlan::getId, TestPlan::getName, (a, b) -> a));
        Map<Long, String> userNameMap = records.stream()
                .map(Execution::getExecutorId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet())
                .stream()
                .map(userMapper::selectById)
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(User::getId, User::getUsername, (a, b) -> a));

        List<ExecutionSummaryVO> vos = new ArrayList<>();
        for (Execution exec : records) {
            vos.add(ExecutionSummaryVO.builder()
                    .executionId(exec.getId())
                    .planId(exec.getPlanId())
                    .planName(exec.getPlanId() == null ? null : planNameMap.get(exec.getPlanId()))
                    .caseId(exec.getCaseId())
                    .caseName(exec.getCaseName())
                    .envId(exec.getEnvId())
                    .envName(exec.getEnvName())
                    .triggerType(exec.getTriggerType())
                    .executorName(exec.getExecutorId() == null ? null : userNameMap.get(exec.getExecutorId()))
                    .status(exec.getStatus())
                    .startTime(exec.getStartTime())
                    .durationMs(exec.getDurationMs())
                    .totalRounds(exec.getTotalRounds())
                    .passedRounds(exec.getPassedRounds())
                    .failedRounds(exec.getFailedRounds())
                    .build());
        }
        return PageResult.<ExecutionSummaryVO>builder()
                .records(vos)
                .total(result.getTotal())
                .page(result.getCurrent())
                .size(result.getSize())
                .build();
    }

    /** 将一次执行的明细（detail + assertion）组装为按轮次分组的步骤列表 */
    private List<RoundExecuteVO> buildRounds(Long executionId) {
        List<ExecutionDetail> details = executionDetailMapper.selectList(new QueryWrapper<ExecutionDetail>()
                .eq("execution_id", executionId)
                .orderByAsc("round_index")
                .orderByAsc("step_index"));
        List<ExecutionAssertion> assertions = executionAssertionMapper.selectList(
                new QueryWrapper<ExecutionAssertion>().eq("execution_id", executionId));

        Map<Long, List<ExecutionAssertion>> assertByDetail = assertions.stream()
                .collect(Collectors.groupingBy(ExecutionAssertion::getDetailId));

        Map<Integer, List<StepExecuteVO>> stepsByRound = new LinkedHashMap<>();
        Map<Integer, Integer> roundPassed = new HashMap<>();
        Map<Integer, Integer> roundFailed = new HashMap<>();
        Map<Integer, Long> roundDuration = new HashMap<>();

        for (ExecutionDetail d : details) {
            List<ExecutionAssertion> ads = assertByDetail.get(d.getId());
            List<AssertionResultVO> assertResults = (ads == null ? List.of()
                    : ads.stream().map(a -> AssertionResultVO.builder()
                            .type(a.getType())
                            .path(a.getPath())
                            .operator(a.getOperator())
                            .expected(a.getExpected())
                            .actual(a.getActual())
                            .passed(a.getPassed() != null && a.getPassed() == 1)
                            .message(a.getMessage())
                            .build()).toList());

            StepExecuteVO step = StepExecuteVO.builder()
                    .stepId(d.getStepId())
                    .stepName(d.getStepName())
                    .sortOrder(d.getStepIndex())
                    .method(d.getMethod())
                    .url(d.getUrl())
                    .requestHeaders(d.getRequestHeaders())
                    .requestBody(d.getRequestBody())
                    .statusCode(d.getStatusCode())
                    .responseHeaders(d.getResponseHeaders())
                    .responseBody(d.getResponseBody())
                    .status(d.getStatus())
                    .errorMsg(d.getErrorMsg())
                    .durationMs(d.getDurationMs())
                    .assertResults(assertResults)
                    .build();
            stepsByRound.computeIfAbsent(d.getRoundIndex(), k -> new ArrayList<>()).add(step);
            if ("PASSED".equals(d.getStatus())) {
                roundPassed.merge(d.getRoundIndex(), 1, Integer::sum);
            } else {
                roundFailed.merge(d.getRoundIndex(), 1, Integer::sum);
            }
            roundDuration.merge(d.getRoundIndex(), d.getDurationMs() == null ? 0L : d.getDurationMs(), Long::sum);
        }

        List<RoundExecuteVO> rounds = new ArrayList<>();
        stepsByRound.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e -> {
            int idx = e.getKey();
            List<StepExecuteVO> steps = e.getValue();
            int passed = roundPassed.getOrDefault(idx, 0);
            int failed = roundFailed.getOrDefault(idx, 0);
            rounds.add(RoundExecuteVO.builder()
                    .roundIndex(idx)
                    .params(new LinkedHashMap<>())
                    .passedSteps(passed)
                    .failedSteps(failed)
                    .status(failed == 0 ? "SUCCESS" : "FAILED")
                    .durationMs(roundDuration.getOrDefault(idx, 0L))
                    .steps(steps)
                    .build());
        });
        return rounds;
    }
}
