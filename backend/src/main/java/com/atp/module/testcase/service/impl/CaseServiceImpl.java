package com.atp.module.testcase.service.impl;

import com.atp.common.exception.BizException;
import com.atp.common.result.ResultCode;
import com.atp.module.base.entity.ApiDefinition;
import com.atp.module.base.entity.Application;
import com.atp.module.base.entity.ApplicationModule;
import com.atp.module.base.entity.ApplicationVersion;
import com.atp.module.base.generator.GeneratorEngine;
import com.atp.module.base.mapper.ApiDefinitionMapper;
import com.atp.module.base.mapper.DataGeneratorMapper;
import com.atp.module.base.mapper.ApplicationMapper;
import com.atp.module.base.mapper.ApplicationModuleMapper;
import com.atp.module.base.mapper.ApplicationVersionMapper;
import com.atp.module.testcase.dto.CaseCreateRequest;
import com.atp.module.testcase.dto.CaseUpdateRequest;
import com.atp.module.testcase.dto.StepDTO;
import com.atp.module.testcase.entity.CaseStep;
import com.atp.module.testcase.entity.TestCase;
import com.atp.module.testcase.mapper.CaseStepMapper;
import com.atp.module.testcase.mapper.TestCaseMapper;
import com.atp.module.testcase.parser.HarEntryDTO;
import com.atp.module.testcase.parser.HarParser;
import com.atp.module.testcase.service.CaseService;
import com.atp.module.testcase.service.HarImportResult;
import com.atp.module.testcase.vo.CaseStepVO;
import com.atp.module.testcase.vo.CaseVO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 测试用例服务实现
 */
@Service
@RequiredArgsConstructor
public class CaseServiceImpl extends ServiceImpl<TestCaseMapper, TestCase> implements CaseService {

    private static final int LEVEL_MIN = 1;
    private static final int LEVEL_MAX = 3;
    private static final int DEFAULT_LEVEL = 2;
    private static final int STATUS_DISABLED = 0;
    private static final int STATUS_ENABLED = 1;

    /** 接口定义来源标识：HAR 包导入 */
    private static final String SOURCE_FLAG_HAR = "har包导入";

    private final ObjectMapper objectMapper;
    private final CaseStepMapper caseStepMapper;
    private final HarParser harParser;
    private final ApiDefinitionMapper apiDefinitionMapper;
    private final ApplicationModuleMapper applicationModuleMapper;
    private final ApplicationVersionMapper applicationVersionMapper;
    private final ApplicationMapper applicationMapper;
    private final DataGeneratorMapper dataGeneratorMapper;

    @Override
    public IPage<CaseVO> selectCasePage(long page, long size, Long projectId, Long apiId,
                                        String name, Integer level, Integer status) {
        return baseMapper.selectCasePage(new Page<>(page, size), projectId, apiId, name, level, status);
    }

    @Override
    public CaseVO getCaseDetail(Long caseId) {
        TestCase testCase = getById(caseId);
        if (testCase == null) {
            throw new BizException(ResultCode.CASE_NOT_FOUND);
        }
        // 查询步骤列表
        List<CaseStepVO> steps = caseStepMapper.selectStepsByCaseId(caseId);

        return CaseVO.builder()
                .id(testCase.getId())
                .projectId(testCase.getProjectId())
                .applicationId(testCase.getApplicationId())
                .versionId(testCase.getVersionId())
                .moduleId(testCase.getModuleId())
                .apiId(testCase.getApiId())
                .name(testCase.getName())
                .creatorName(testCase.getCreatorName())
                .level(testCase.getLevel())
                .request(testCase.getRequest())
                .assertions(testCase.getAssertions())
                .setupScript(testCase.getSetupScript())
                .status(testCase.getStatus())
                .createTime(testCase.getCreateTime())
                .updateTime(testCase.getUpdateTime())
                .steps(steps)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createCase(CaseCreateRequest request, Long userId) {
        TestCase testCase = new TestCase();
        testCase.setProjectId(request.getProjectId());
        testCase.setApplicationId(request.getApplicationId());
        testCase.setVersionId(request.getVersionId());
        testCase.setModuleId(request.getModuleId());
        testCase.setApiId(request.getApiId());
        testCase.setName(request.getName().trim());
        testCase.setCreatorName(trimToNull(request.getCreatorName()));
        testCase.setLevel(normalizeLevel(request.getLevel()));
        testCase.setRequest(normalizeJson(request.getRequest(), "请求内容"));
        testCase.setAssertions(normalizeJson(request.getAssertions(), "断言规则"));
        testCase.setSetupScript(trimToNull(request.getSetupScript()));
        testCase.setStatus(normalizeStatus(request.getStatus()));
        testCase.setCreateBy(userId);
        save(testCase);

        // 保存步骤
        saveSteps(testCase.getId(), request.getSteps());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCase(CaseUpdateRequest request) {
        TestCase testCase = getById(request.getId());
        if (testCase == null) {
            throw new BizException(ResultCode.CASE_NOT_FOUND);
        }
        testCase.setApplicationId(request.getApplicationId());
        testCase.setVersionId(request.getVersionId());
        testCase.setModuleId(request.getModuleId());
        testCase.setApiId(request.getApiId());
        testCase.setName(request.getName().trim());
        testCase.setCreatorName(trimToNull(request.getCreatorName()));
        testCase.setLevel(normalizeLevel(request.getLevel()));
        testCase.setRequest(normalizeJson(request.getRequest(), "请求内容"));
        testCase.setAssertions(normalizeJson(request.getAssertions(), "断言规则"));
        testCase.setSetupScript(trimToNull(request.getSetupScript()));
        testCase.setStatus(normalizeStatus(request.getStatus()));
        updateById(testCase);

        // 替换步骤：先逻辑删除旧步骤，再插入新步骤
        replaceSteps(testCase.getId(), request.getSteps());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCase(Long id) {
        TestCase testCase = getById(id);
        if (testCase == null) {
            throw new BizException(ResultCode.CASE_NOT_FOUND);
        }
        removeById(id);
        // 逻辑删除该用例下的所有步骤
        List<CaseStep> oldSteps = caseStepMapper.selectList(
                new QueryWrapper<CaseStep>().eq("case_id", id)
        );
        for (CaseStep step : oldSteps) {
            caseStepMapper.deleteById(step.getId());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        TestCase testCase = getById(id);
        if (testCase == null) {
            throw new BizException(ResultCode.CASE_NOT_FOUND);
        }
        testCase.setStatus(normalizeStatus(status));
        updateById(testCase);
    }

    @Override
    public List<CaseStepVO> getCaseSteps(Long caseId) {
        return caseStepMapper.selectStepsByCaseId(caseId);
    }

    // ==================== HAR 导入 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HarImportResult importHar(Long moduleId, String caseName, MultipartFile har, Long userId) {
        if (moduleId == null) {
            throw new BizException(ResultCode.BAD_REQUEST, "模块ID不能为空");
        }
        if (!StringUtils.hasText(caseName)) {
            throw new BizException(ResultCode.BAD_REQUEST, "用例名称不能为空");
        }
        if (har == null || har.isEmpty()) {
            throw new BizException(ResultCode.BAD_REQUEST, "HAR 文件不能为空");
        }
        String filename = har.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".har")) {
            throw new BizException(ResultCode.BAD_REQUEST, "仅支持 .har 格式文件");
        }

        // 1) 模块反查：拿到 applicationId / versionId / projectId（tb_test_case 四个外键）
        ModuleContext ctx = resolveModule(moduleId);

        // 2) 解析 HAR
        byte[] bytes;
        try {
            bytes = har.getBytes();
        } catch (Exception e) {
            throw new BizException(ResultCode.BAD_REQUEST, "读取 HAR 文件失败: " + e.getMessage());
        }
        List<HarEntryDTO> entries = harParser.parse(bytes);
        if (entries.isEmpty()) {
            throw new BizException(ResultCode.BAD_REQUEST, "HAR 文件未包含有效的 HTTP 请求");
        }

        // 3) 预载模块下现有接口，按 method+path 查重
        Map<String, Long> apiMap = preLoadModuleApis(moduleId);

        // 4) 阶段一：插入缺失的接口定义
        int created = 0;
        int skipped = 0;
        for (HarEntryDTO entry : entries) {
            String key = apiKey(entry.getMethod(), entry.getPath());
            if (apiMap.containsKey(key)) {
                skipped++;
                continue;
            }
            ApiDefinition api = new ApiDefinition();
            api.setModuleId(moduleId);
            api.setName(""); // HAR 导入：name 留空
            api.setMethod(entry.getMethod());
            api.setPath(entry.getPath());
            api.setHeaders(entry.getHeaders());
            api.setBody(entry.getBody());
            api.setDescription(buildApiDescription(entry));
            api.setSourceFlag(SOURCE_FLAG_HAR);
            api.setCreateBy(userId);
            apiDefinitionMapper.insert(api);
            apiMap.put(key, api.getId());
            created++;
        }

        // 5) 阶段二：创建一个用例
        TestCase testCase = new TestCase();
        testCase.setProjectId(ctx.projectId);
        testCase.setApplicationId(ctx.applicationId);
        testCase.setVersionId(ctx.versionId);
        testCase.setModuleId(moduleId);
        testCase.setName(caseName.trim());
        testCase.setLevel(DEFAULT_LEVEL);
        testCase.setStatus(STATUS_ENABLED);
        testCase.setCreateBy(userId);
        save(testCase);

        // 6) 阶段二：按 startedDateTime 顺序插入步骤（parser 已排好）
        for (int i = 0; i < entries.size(); i++) {
            HarEntryDTO entry = entries.get(i);
            String key = apiKey(entry.getMethod(), entry.getPath());
            Long apiId = apiMap.get(key);
            if (apiId == null) {
                // 理论不会发生：阶段一已确保 apiMap 含所有 entry
                continue;
            }
            CaseStep step = new CaseStep();
            step.setCaseId(testCase.getId());
            step.setApiId(apiId);
            step.setPhase("main");
            step.setStepType(1);
            step.setSortOrder(i + 1);
            step.setRequestOverride(null);
            step.setAssertions(buildDefaultStatusAssertion(entry.getResponseStatus()));
            step.setIsDisabled(0);
            step.setPromoteGlobal(0);
            step.setContinueOnFail(0);
            caseStepMapper.insert(step);
        }

        return HarImportResult.builder()
                .apiCreatedCount(created)
                .apiSkippedCount(skipped)
                .caseId(testCase.getId())
                .stepCount(entries.size())
                .build();
    }

    /** 模块反查上下文（四个外键） */
    private ModuleContext resolveModule(Long moduleId) {
        ApplicationModule module = applicationModuleMapper.selectById(moduleId);
        if (module == null) {
            throw new BizException(ResultCode.MODULE_NOT_FOUND);
        }
        ApplicationVersion version = applicationVersionMapper.selectById(module.getVersionId());
        if (version == null) {
            throw new BizException(ResultCode.VERSION_NOT_FOUND);
        }
        Application application = applicationMapper.selectById(version.getApplicationId());
        if (application == null) {
            throw new BizException(ResultCode.PROJECT_NOT_FOUND);
        }
        return new ModuleContext(application.getProjectId(), version.getApplicationId(), version.getId());
    }

    /** 预载模块下所有未删除接口的 method+path → apiId */
    private Map<String, Long> preLoadModuleApis(Long moduleId) {
        List<ApiDefinition> existing = apiDefinitionMapper.selectList(
                new QueryWrapper<ApiDefinition>()
                        .eq("module_id", moduleId)
                        .select("id", "method", "path")
        );
        Map<String, Long> map = new HashMap<>(existing.size() * 2);
        for (ApiDefinition a : existing) {
            map.put(apiKey(a.getMethod(), a.getPath()), a.getId());
        }
        return map;
    }

    private static String apiKey(String method, String path) {
        return method + " " + path;
    }

    /** 接口定义描述：HAR 导入的备注 + 主机 */
    private String buildApiDescription(HarEntryDTO entry) {
        StringBuilder sb = new StringBuilder("HAR包导入");
        if (StringUtils.hasText(entry.getHost())) {
            sb.append(" 源:").append(entry.getHost());
        }
        if (StringUtils.hasText(entry.getNote())) {
            sb.append(" (").append(entry.getNote()).append(")");
        }
        return sb.toString();
    }

    /**
     * 生成默认的响应状态码断言，格式与前端一致：
     * <pre>[{"type":"status","expected":200}]</pre>
     */
    private String buildDefaultStatusAssertion(Integer responseStatus) {
        int expected = responseStatus != null ? responseStatus : 200;
        try {
            return objectMapper.writeValueAsString(java.util.List.of(
                    java.util.Map.of("type", "status", "expected", expected)
            ));
        } catch (Exception e) {
            return "[]";
        }
    }

    /** HAR 导入时的模块反查结果 */
    private record ModuleContext(Long projectId, Long applicationId, Long versionId) {
    }

    // ==================== 步骤管理 ====================

    /** 新增时保存步骤列表 */
    private void saveSteps(Long caseId, List<StepDTO> steps) {
        if (steps == null || steps.isEmpty()) {
            return;
        }
        for (int i = 0; i < steps.size(); i++) {
            StepDTO dto = steps.get(i);
            Integer stepType = dto.getStepType() != null ? dto.getStepType() : 1;
            // 生成变量步骤（stepType=3）：校验生成器存在 + 变量名合法；结果在 saveSteps 末尾统一落库
            String variableName = null;
            Integer regenEachRun = null;
            if (stepType == 2) {
                if (dto.getComponentId() == null) {
                    throw new BizException(ResultCode.BAD_REQUEST, "组合组件步骤必须选择引用的组件");
                }
            } else if (stepType == 3) {
                if (dto.getGeneratorId() == null) {
                    throw new BizException(ResultCode.BAD_REQUEST, "生成变量步骤必须选择数据生成器");
                }
                if (dataGeneratorMapper.selectById(dto.getGeneratorId()) == null) {
                    throw new BizException(ResultCode.NOT_FOUND, "数据生成器不存在或已被删除");
                }
                variableName = GeneratorEngine.checkVariableName(dto.getVariableName());
                regenEachRun = dto.getRegenEachRun() != null ? dto.getRegenEachRun() : 1;
            } else {
                if (dto.getApiId() == null) {
                    throw new BizException(ResultCode.BAD_REQUEST, "请选择步骤关联接口");
                }
            }
            CaseStep step = new CaseStep();
            step.setCaseId(caseId);
            step.setApiId(dto.getApiId());
            step.setPhase(dto.getPhase() != null ? dto.getPhase() : "main");
            step.setStepType(stepType);
            step.setComponentId(dto.getComponentId());
            step.setGeneratorId(stepType == 3 ? dto.getGeneratorId() : null);
            step.setVariableName(variableName);
            step.setRegenEachRun(stepType == 3 ? regenEachRun : 1);
            step.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : i + 1);
            step.setStepName(trimToNull(dto.getStepName()));
            step.setRequestOverride(normalizeJson(dto.getRequestOverride(), "步骤请求覆盖内容"));
            step.setAssertions(normalizeJson(dto.getAssertions(), "步骤断言规则"));
            step.setResponseVar(trimToNull(dto.getResponseVar()));
            step.setIsDisabled(dto.getIsDisabled() != null ? dto.getIsDisabled() : 0);
            step.setPromoteGlobal(dto.getPromoteGlobal() != null ? dto.getPromoteGlobal() : 0);
            step.setContinueOnFail(dto.getContinueOnFail() != null ? dto.getContinueOnFail() : 0);
            step.setDescription(trimToNull(dto.getDescription()));
            caseStepMapper.insert(step);
        }
    }

    /** 编辑时替换步骤（逻辑删除旧的，插入新的） */
    private void replaceSteps(Long caseId, List<StepDTO> steps) {
        // 逻辑删除旧步骤
        List<CaseStep> oldSteps = caseStepMapper.selectList(
                new QueryWrapper<CaseStep>().eq("case_id", caseId)
        );
        for (CaseStep old : oldSteps) {
            caseStepMapper.deleteById(old.getId());
        }
        // 插入新步骤
        saveSteps(caseId, steps);
    }

    // ==================== 工具方法 ====================

    private int normalizeLevel(Integer level) {
        if (level == null) {
            return DEFAULT_LEVEL;
        }
        if (level < LEVEL_MIN || level > LEVEL_MAX) {
            throw new BizException(ResultCode.BAD_REQUEST, "优先级取值只能为 1(P0) / 2(P1) / 3(P2)");
        }
        return level;
    }

    private int normalizeStatus(Integer status) {
        if (status == null) {
            return STATUS_ENABLED;
        }
        if (status != STATUS_DISABLED && status != STATUS_ENABLED) {
            throw new BizException(ResultCode.BAD_REQUEST, "状态取值只能为 0(停用) / 1(启用)");
        }
        return status;
    }

    private String normalizeJson(String json, String fieldName) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            objectMapper.readTree(json);
        } catch (Exception e) {
            throw new BizException(ResultCode.BAD_REQUEST, fieldName + "不是合法的 JSON");
        }
        return json.trim();
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
