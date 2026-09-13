package com.atp.module.plan.service.impl;

import com.atp.common.exception.BizException;
import com.atp.common.result.PageResult;
import com.atp.common.result.ResultCode;
import com.atp.module.env.entity.TestEnv;
import com.atp.module.env.mapper.TestEnvMapper;
import com.atp.module.execute.dto.CaseExecuteRequest;
import com.atp.module.execute.entity.Execution;
import com.atp.module.execute.mapper.ExecutionMapper;
import com.atp.module.execute.service.ExecuteService;
import com.atp.module.execute.vo.CaseExecuteVO;
import com.atp.module.execute.vo.RoundExecuteVO;
import com.atp.module.plan.dto.PlanExecuteResult;
import com.atp.module.plan.dto.PlanExecuteResult.PlanExecuteCaseResult;
import com.atp.module.plan.dto.TestPlanForm;
import com.atp.module.plan.entity.TestPlan;
import com.atp.module.plan.entity.TestPlanCase;
import com.atp.module.plan.mapper.TestPlanCaseMapper;
import com.atp.module.plan.mapper.TestPlanMapper;
import com.atp.module.plan.service.TestPlanService;
import com.atp.module.plan.vo.TestPlanVO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 测试计划服务实现：计划 CRUD + 按计划执行。
 *
 * <p>「按计划执行」复用既有的 {@link ExecuteService#executeCase(Long, CaseExecuteRequest)}，
 * 透传 planId，使执行记录自动标记 trigger_type=SCHEDULED 并回填 plan_id，
 * 与单用例手动执行共用同一套落库逻辑，耦合最小。
 */
@Service
@RequiredArgsConstructor
public class TestPlanServiceImpl implements TestPlanService {

    private final TestPlanMapper testPlanMapper;
    private final TestPlanCaseMapper testPlanCaseMapper;
    private final TestEnvMapper testEnvMapper;
    private final ExecutionMapper executionMapper;
    private final ExecuteService executeService;

    @Override
    public PageResult<TestPlanVO> list(Long projectId, String name, Boolean enabled, long page, long size) {
        Page<TestPlan> p = new Page<>(page, size);
        QueryWrapper<TestPlan> qw = new QueryWrapper<>();
        if (projectId != null) {
            qw.eq("project_id", projectId);
        }
        if (name != null && !name.isBlank()) {
            qw.like("name", name);
        }
        if (enabled != null) {
            qw.eq("enabled", enabled ? 1 : 0);
        }
        qw.orderByDesc("update_time");
        Page<TestPlan> result = testPlanMapper.selectPage(p, qw);

        List<TestPlan> records = result.getRecords();
        // 批量查环境名称，避免 N+1
        Map<Long, String> envNameMap = records.stream()
                .map(TestPlan::getEnvId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet())
                .stream()
                .map(testEnvMapper::selectById)
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(TestEnv::getId, TestEnv::getName, (a, b) -> a));

        List<TestPlanVO> vos = new ArrayList<>();
        for (TestPlan plan : records) {
            TestPlanVO vo = new TestPlanVO();
            vo.setId(plan.getId());
            vo.setProjectId(plan.getProjectId());
            vo.setEnvId(plan.getEnvId());
            vo.setEnvName(envNameMap.get(plan.getEnvId()));
            vo.setName(plan.getName());
            vo.setCron(plan.getCron());
            vo.setEnabled(plan.getEnabled() != null && plan.getEnabled() == 1);
            vo.setLastRunId(plan.getLastRunId());
            vo.setCreateTime(plan.getCreateTime());
            vo.setUpdateTime(plan.getUpdateTime());

            Long cc = testPlanCaseMapper.selectCount(
                    new QueryWrapper<TestPlanCase>().eq("plan_id", plan.getId()));
            vo.setCaseCount(cc == null ? 0 : cc.intValue());

            if (plan.getLastRunId() != null) {
                Execution ex = executionMapper.selectById(plan.getLastRunId());
                vo.setLastRunTime(ex == null ? null : ex.getEndTime());
            }
            vos.add(vo);
        }

        return PageResult.<TestPlanVO>builder()
                .records(vos)
                .total(result.getTotal())
                .page(result.getCurrent())
                .size(result.getSize())
                .build();
    }

    @Override
    public void create(TestPlanForm form) {
        TestPlan plan = new TestPlan();
        plan.setProjectId(form.getProjectId());
        plan.setName(form.getName());
        plan.setEnvId(form.getEnvId());
        plan.setCron(normalizeCron(form.getCron()));
        plan.setEnabled(Boolean.TRUE.equals(form.getEnabled()) ? 1 : 0);
        testPlanMapper.insert(plan);
        saveLinks(plan.getId(), form.getCaseIds());
    }

    @Override
    public void update(TestPlanForm form) {
        if (form.getId() == null) {
            throw new BizException(ResultCode.BAD_REQUEST, "计划ID不能为空");
        }
        TestPlan plan = testPlanMapper.selectById(form.getId());
        if (plan == null) {
            throw new BizException(ResultCode.PLAN_NOT_FOUND);
        }
        plan.setProjectId(form.getProjectId());
        plan.setName(form.getName());
        plan.setEnvId(form.getEnvId());
        plan.setCron(normalizeCron(form.getCron()));
        plan.setEnabled(Boolean.TRUE.equals(form.getEnabled()) ? 1 : 0);
        testPlanMapper.updateById(plan);
        saveLinks(plan.getId(), form.getCaseIds());
    }

    /**
     * 重建关联用例：物理删除旧关联后重新插入。
     * tb_plan_case 无 deleted 列，全局逻辑删除会自动跳过它，此处即为物理删除（期望行为）。
     */
    private void saveLinks(Long planId, List<Long> caseIds) {
        testPlanCaseMapper.delete(new QueryWrapper<TestPlanCase>().eq("plan_id", planId));
        if (caseIds == null || caseIds.isEmpty()) {
            return;
        }
        int order = 1;
        for (Long caseId : caseIds) {
            TestPlanCase link = new TestPlanCase();
            link.setPlanId(planId);
            link.setCaseId(caseId);
            link.setSortOrder(order++);
            testPlanCaseMapper.insert(link);
        }
    }

    @Override
    public void delete(Long id) {
        TestPlan plan = testPlanMapper.selectById(id);
        if (plan == null) {
            throw new BizException(ResultCode.PLAN_NOT_FOUND);
        }
        testPlanMapper.deleteById(id); // @TableLogic 逻辑删除
        testPlanCaseMapper.delete(new QueryWrapper<TestPlanCase>().eq("plan_id", id)); // 物理删除关联
    }

    @Override
    public void toggleEnabled(Long id, Boolean enabled) {
        TestPlan plan = testPlanMapper.selectById(id);
        if (plan == null) {
            throw new BizException(ResultCode.PLAN_NOT_FOUND);
        }
        plan.setEnabled(Boolean.TRUE.equals(enabled) ? 1 : 0);
        testPlanMapper.updateById(plan);
    }

    @Override
    public PlanExecuteResult executePlan(Long id) {
        TestPlan plan = testPlanMapper.selectById(id);
        if (plan == null) {
            throw new BizException(ResultCode.PLAN_NOT_FOUND);
        }
        List<TestPlanCase> links = testPlanCaseMapper.selectList(
                new QueryWrapper<TestPlanCase>().eq("plan_id", id).orderByAsc("sort_order"));

        PlanExecuteResult result = new PlanExecuteResult();
        result.setPlanId(id);
        result.setPlanName(plan.getName());
        result.setTotalCases(links.size());

        int passed = 0;
        int failed = 0;
        long start = System.currentTimeMillis();
        List<PlanExecuteCaseResult> caseResults = new ArrayList<>();

        for (TestPlanCase link : links) {
            PlanExecuteCaseResult cr = new PlanExecuteCaseResult();
            cr.setCaseId(link.getCaseId());
            try {
                CaseExecuteRequest req = new CaseExecuteRequest();
                req.setEnvId(plan.getEnvId());
                req.setPlanId(id);
                req.setDebug(Boolean.FALSE);
                CaseExecuteVO vo = executeService.executeCase(link.getCaseId(), req);
                cr.setCaseName(vo.getCaseName());
                cr.setStatus(vo.getStatus());
                cr.setDurationMs(vo.getDurationMs());
                int ps = 0, fs = 0;
                if (vo.getRounds() != null) {
                    for (RoundExecuteVO r : vo.getRounds()) {
                        ps += r.getPassedSteps() == null ? 0 : r.getPassedSteps();
                        fs += r.getFailedSteps() == null ? 0 : r.getFailedSteps();
                    }
                }
                cr.setPassedSteps(ps);
                cr.setFailedSteps(fs);
                if ("SUCCESS".equals(vo.getStatus())) {
                    passed++;
                } else {
                    failed++;
                }
            } catch (Exception e) {
                cr.setStatus("FAILED");
                cr.setDurationMs(0L);
                cr.setPassedSteps(0);
                cr.setFailedSteps(0);
                failed++;
            }
            caseResults.add(cr);
        }

        result.setPassedCases(passed);
        result.setFailedCases(failed);
        result.setDurationMs(System.currentTimeMillis() - start);
        result.setCases(caseResults);

        // 回填最近执行ID（取该计划最新一条执行记录）
        Execution latest = executionMapper.selectOne(
                new QueryWrapper<Execution>().eq("plan_id", id).orderByDesc("id").last("LIMIT 1"));
        if (latest != null) {
            plan.setLastRunId(latest.getId());
            testPlanMapper.updateById(plan);
        }
        return result;
    }

    private String normalizeCron(String cron) {
        return (cron == null || cron.isBlank()) ? null : cron.trim();
    }
}
