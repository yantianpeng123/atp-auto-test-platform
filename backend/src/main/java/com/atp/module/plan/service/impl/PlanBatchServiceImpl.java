package com.atp.module.plan.service.impl;

import com.atp.common.exception.BizException;
import com.atp.common.result.PageResult;
import com.atp.common.result.ResultCode;
import com.atp.module.plan.dto.PlanBatchForm;
import com.atp.module.plan.dto.PlanExecuteResult;
import com.atp.module.plan.entity.PlanBatch;
import com.atp.module.plan.entity.PlanBatchItem;
import com.atp.module.plan.entity.PlanBatchRun;
import com.atp.module.plan.entity.PlanBatchRunItem;
import com.atp.module.plan.entity.TestPlan;
import com.atp.module.plan.mapper.PlanBatchItemMapper;
import com.atp.module.plan.mapper.PlanBatchMapper;
import com.atp.module.plan.mapper.PlanBatchRunItemMapper;
import com.atp.module.plan.mapper.PlanBatchRunMapper;
import com.atp.module.plan.mapper.TestPlanMapper;
import com.atp.module.plan.service.PlanBatchService;
import com.atp.module.plan.service.TestPlanService;
import com.atp.module.plan.vo.PlanBatchDetailVO;
import com.atp.module.plan.vo.PlanBatchRunItemVO;
import com.atp.module.plan.vo.PlanBatchRunVO;
import com.atp.module.plan.vo.PlanBatchVO;
import com.atp.module.notify.event.NotifyEvent;
import com.atp.module.notify.event.NotifyPayload;
import com.atp.module.user.entity.User;
import com.atp.module.user.mapper.UserMapper;
import com.atp.security.UserPrincipal;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * 定时任务批次服务实现：批次 CRUD + 批量执行（串行/并行）+ 运行看板。
 *
 * <p>「单个计划执行」复用既有的 {@link TestPlanService#executePlan(Long)}，
 * 与测试计划的手动/定时执行共用同一套内核，耦合最小。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PlanBatchServiceImpl implements PlanBatchService {

    private final PlanBatchMapper planBatchMapper;
    private final PlanBatchItemMapper planBatchItemMapper;
    private final PlanBatchRunMapper planBatchRunMapper;
    private final PlanBatchRunItemMapper planBatchRunItemMapper;
    private final TestPlanMapper testPlanMapper;
    private final TestPlanService testPlanService;
    private final UserMapper userMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public PageResult<PlanBatchVO> list(Long projectId, String name, Boolean enabled, long page, long size) {
        Page<PlanBatch> p = new Page<>(page, size);
        QueryWrapper<PlanBatch> qw = new QueryWrapper<>();
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
        Page<PlanBatch> result = planBatchMapper.selectPage(p, qw);

        List<PlanBatchVO> vos = new ArrayList<>();
        for (PlanBatch b : result.getRecords()) {
            vos.add(toVO(b));
        }
        return PageResult.<PlanBatchVO>builder()
                .records(vos)
                .total(result.getTotal())
                .page(result.getCurrent())
                .size(result.getSize())
                .build();
    }

    private PlanBatchVO toVO(PlanBatch b) {
        PlanBatchVO vo = new PlanBatchVO();
        vo.setId(b.getId());
        vo.setProjectId(b.getProjectId());
        vo.setName(b.getName());
        vo.setStrategy(b.getStrategy());
        vo.setFailContinue(b.getFailContinue() != null && b.getFailContinue() == 1);
        vo.setMaxConcurrency(b.getMaxConcurrency());
        vo.setCron(b.getCron());
        vo.setEnabled(b.getEnabled() != null && b.getEnabled() == 1);
        vo.setLastRunId(b.getLastRunId());
        vo.setCreateTime(b.getCreateTime());
        vo.setUpdateTime(b.getUpdateTime());
        if (b.getLastRunId() != null) {
            PlanBatchRun run = planBatchRunMapper.selectById(b.getLastRunId());
            if (run != null) {
                vo.setLastRunTime(run.getEndTime());
                vo.setLastRunStatus(run.getStatus());
            }
        }
        return vo;
    }

    @Override
    public void create(PlanBatchForm form) {

        //新增根据项目id和名称 无法新增
       long exist= planBatchMapper.selectCount(new QueryWrapper<PlanBatch>()
                .eq("project_id",form.getProjectId())
                .eq("name",form.getName()));
       if(exist>0){
           throw new BizException(ResultCode.PLAN_NAME_EXISTS);
       }

        PlanBatch b = new PlanBatch();
        b.setProjectId(form.getProjectId());
        b.setName(form.getName());
        b.setStrategy(normalizeStrategy(form.getStrategy()));
        b.setFailContinue(Boolean.TRUE.equals(form.getFailContinue()) ? 1 : 0);
        b.setMaxConcurrency(normalizeConcurrency(form.getMaxConcurrency()));
        b.setCron(normalizeCron(form.getCron()));
        b.setEnabled(Boolean.TRUE.equals(form.getEnabled()) ? 1 : 0);
        planBatchMapper.insert(b);
        saveItems(b.getId(), form.getPlanIds());
    }

    @Override
    public void update(PlanBatchForm form) {
        if (form.getId() == null) {
            throw new BizException(ResultCode.BAD_REQUEST, "批次ID不能为空");
        }
        PlanBatch b = planBatchMapper.selectById(form.getId());
        if (b == null) {
            throw new BizException(ResultCode.BATCH_NOT_FOUND);
        }
        long exist= planBatchMapper.selectCount(new QueryWrapper<PlanBatch>()
                .eq("project_id",form.getProjectId())
                .eq("name",form.getName())
                .ne("id",form.getId()));
        if(exist>0){
            throw new BizException(ResultCode.BATCH_NAME_EXISTS);
        }



        b.setProjectId(form.getProjectId());
        b.setName(form.getName());
        b.setStrategy(normalizeStrategy(form.getStrategy()));
        b.setFailContinue(Boolean.TRUE.equals(form.getFailContinue()) ? 1 : 0);
        b.setMaxConcurrency(normalizeConcurrency(form.getMaxConcurrency()));
        b.setCron(normalizeCron(form.getCron()));
        b.setEnabled(Boolean.TRUE.equals(form.getEnabled()) ? 1 : 0);
        planBatchMapper.updateById(b);
        saveItems(b.getId(), form.getPlanIds());
    }

    /**
     * 重建关联计划：物理删除旧关联后重新插入（tb_plan_batch_item 无 deleted 列，即为物理删除）。
     */
    private void saveItems(Long batchId, List<Long> planIds) {
        planBatchItemMapper.delete(new QueryWrapper<PlanBatchItem>().eq("batch_id", batchId));
        if (planIds == null || planIds.isEmpty()) {
            return;
        }
        int order = 1;
        for (Long planId : planIds) {
            PlanBatchItem item = new PlanBatchItem();
            item.setBatchId(batchId);
            item.setPlanId(planId);
            item.setSortOrder(order++);
            planBatchItemMapper.insert(item);
        }
    }

    @Override
    public void delete(Long id) {
        PlanBatch b = planBatchMapper.selectById(id);
        if (b == null) {
            throw new BizException(ResultCode.BATCH_NOT_FOUND);
        }
        planBatchMapper.deleteById(id); // @TableLogic 逻辑删除
        planBatchItemMapper.delete(new QueryWrapper<PlanBatchItem>().eq("batch_id", id)); // 物理删除关联
    }

    @Override
    public void toggleEnabled(Long id, Boolean enabled) {
        PlanBatch b = planBatchMapper.selectById(id);
        if (b == null) {
            throw new BizException(ResultCode.BATCH_NOT_FOUND);
        }
        b.setEnabled(Boolean.TRUE.equals(enabled) ? 1 : 0);
        planBatchMapper.updateById(b);
    }

    @Override
    public PlanBatchRunVO executeBatch(Long id, String triggerType) {
        PlanBatch batch = planBatchMapper.selectById(id);
        if (batch == null) {
            throw new BizException(ResultCode.BATCH_NOT_FOUND);
        }
        List<PlanBatchItem> items = planBatchItemMapper.selectList(
                new QueryWrapper<PlanBatchItem>().eq("batch_id", id).orderByAsc("sort_order"));

        // 运行实例头
        PlanBatchRun run = new PlanBatchRun();
        run.setBatchId(id);
        run.setTriggerType(triggerType == null ? "MANUAL" : triggerType);
        run.setStatus("RUNNING");
        int total = items.size();
        run.setTotal(total);
        run.setPassed(0);
        run.setFailed(0);
        run.setRunning(0);
        run.setQueued(total);
        run.setStartTime(LocalDateTime.now());
        planBatchRunMapper.insert(run);

        // 预创建明细（QUEUED）
        List<PlanBatchRunItem> runItems = new ArrayList<>();
        for (PlanBatchItem item : items) {
            PlanBatchRunItem ri = new PlanBatchRunItem();
            ri.setRunId(run.getId());
            ri.setPlanId(item.getPlanId());
            TestPlan tp = testPlanMapper.selectById(item.getPlanId());
            ri.setPlanName(tp == null ? null : tp.getName());
            ri.setSortOrder(item.getSortOrder());
            ri.setStatus("QUEUED");
            planBatchRunItemMapper.insert(ri);
            runItems.add(ri);
        }

        boolean parallel = "PARALLEL".equals(batch.getStrategy());
        boolean failContinue = batch.getFailContinue() != null && batch.getFailContinue() == 1;

        if (!parallel) {
            // 串行：前一个失败且不允许继续则后续跳过
            boolean stop = false;
            for (PlanBatchRunItem ri : runItems) {
                if (stop) {
                    markSkipped(ri);
                    continue;
                }
                boolean ok = runOne(ri);
                if (!ok && !failContinue) {
                    stop = true;
                }
            }
        } else {
            // 并行：按最大并发数提交任务
            int concurrency = normalizeConcurrency(batch.getMaxConcurrency());
            ExecutorService pool = Executors.newFixedThreadPool(concurrency);
            try {
                List<Future<?>> futures = new ArrayList<>();
                for (PlanBatchRunItem ri : runItems) {
                    futures.add(pool.submit(() -> runOne(ri)));
                }
                for (Future<?> f : futures) {
                    try {
                        f.get();
                    } catch (Exception ignored) {
                        // 单个计划异常已在 runOne 内记录，这里忽略
                    }
                }
            } finally {
                pool.shutdown();
            }
        }

        // 汇总并落库最终状态
        recountRun(run);
        batch.setLastRunId(run.getId());
        planBatchMapper.updateById(batch);

        // 批次整体执行完成后发布 BATCH_DONE 通知事件（供「批次执行完成」规则触发）
        publishBatchDone(batch, run);

        return toRunVO(run);
    }

    /** 批次执行完成后发布 BATCH_DONE 事件，供通知中心「批次执行完成」规则使用 */
    private void publishBatchDone(PlanBatch batch, PlanBatchRun run) {
        try {
            Long executorId = currentUserId();
            String executorName = null;
            if (executorId != null) {
                User u = userMapper.selectById(executorId);
                if (u != null) {
                    executorName = u.getUsername();
                }
            }
            NotifyPayload p = new NotifyPayload();
            p.setProjectId(batch.getProjectId());
            p.setEvent("BATCH_DONE");
            p.setExecutionId(run.getId());
            p.setCaseName(batch.getName());
            p.setStatus(run.getFailed() == null || run.getFailed() == 0 ? "SUCCESS" : "FAILED");
            p.setTotalRounds(run.getTotal());
            p.setPassedRounds(run.getPassed());
            p.setFailedRounds(run.getFailed());
            p.setExecutorId(executorId);
            p.setExecutorName(executorName);
            p.setLinkUrl("/plan/batch/" + batch.getId());
            eventPublisher.publishEvent(new NotifyEvent(this, p));
        } catch (Exception e) {
            log.warn("批次完成通知事件发布失败 batchId={}", batch.getId(), e);
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

    /** 执行单个计划，更新其运行明细，返回是否成功 */
    private boolean runOne(PlanBatchRunItem ri) {
        ri.setStatus("RUNNING");
        ri.setStartTime(LocalDateTime.now());
        planBatchRunItemMapper.updateById(ri);
        boolean success;
        try {
            PlanExecuteResult res = testPlanService.executePlan(ri.getPlanId());
            ri.setStatus("SUCCESS");
            ri.setDurationMs(res.getDurationMs());
            // 取该计划最新一条执行记录作为报告入口（复用 tb_execution）
            TestPlan tp = testPlanMapper.selectById(ri.getPlanId());
            if (tp != null) {
                ri.setExecutionId(tp.getLastRunId());
            }
            success = true;
        } catch (Exception e) {
            ri.setStatus("FAILED");
            ri.setErrorMsg(truncate(e.getMessage()));
            success = false;
        }
        ri.setEndTime(LocalDateTime.now());
        planBatchRunItemMapper.updateById(ri);
        return success;
    }

    /** 标记未执行的计划为跳过 */
    private void markSkipped(PlanBatchRunItem ri) {
        ri.setStatus("SKIPPED");
        ri.setEndTime(LocalDateTime.now());
        planBatchRunItemMapper.updateById(ri);
    }

    /** 按明细实时聚合计数，并落库最终状态（仅在本次执行结束时调用） */
    private void recountRun(PlanBatchRun run) {
        List<PlanBatchRunItem> items = planBatchRunItemMapper.selectList(
                new QueryWrapper<PlanBatchRunItem>().eq("run_id", run.getId()));
        int passed = 0;
        int failed = 0;
        int running = 0;
        int queued = 0;
        for (PlanBatchRunItem ri : items) {
            switch (ri.getStatus()) {
                case "SUCCESS" -> passed++;
                case "RUNNING" -> running++;
                case "QUEUED" -> queued++;
                default -> failed++; // FAILED + SKIPPED
            }
        }
        run.setPassed(passed);
        run.setFailed(failed);
        run.setRunning(running);
        run.setQueued(queued);
        run.setEndTime(LocalDateTime.now());
        run.setDurationMs(Duration.between(run.getStartTime(), run.getEndTime()).toMillis());
        String status;
        if (failed == 0) {
            status = "SUCCESS";
        } else if (passed == 0) {
            status = "FAILED";
        } else {
            status = "PARTIAL_FAILED";
        }
        run.setStatus(status);
        planBatchRunMapper.updateById(run);
    }

    @Override
    public PlanBatchRunVO getRun(Long runId) {
        PlanBatchRun run = planBatchRunMapper.selectById(runId);
        if (run == null) {
            throw new BizException(ResultCode.NOT_FOUND, "运行实例不存在");
        }
        return toRunVO(run);
    }

    @Override
    public List<PlanBatchRunVO> getRuns(Long batchId) {
        List<PlanBatchRun> runs = planBatchRunMapper.selectList(
                new QueryWrapper<PlanBatchRun>().eq("batch_id", batchId).orderByDesc("id"));
        List<PlanBatchRunVO> vos = new ArrayList<>();
        for (PlanBatchRun run : runs) {
            vos.add(toRunVO(run));
        }
        return vos;
    }

    @Override
    public PlanBatchDetailVO getDetail(Long id) {
        PlanBatch b = planBatchMapper.selectById(id);
        if (b == null) {
            throw new BizException(ResultCode.BATCH_NOT_FOUND);
        }
        PlanBatchDetailVO vo = new PlanBatchDetailVO();
        BeanUtils.copyProperties(toVO(b), vo);

        List<PlanBatchItem> items = planBatchItemMapper.selectList(
                new QueryWrapper<PlanBatchItem>().eq("batch_id", id).orderByAsc("sort_order"));
        List<PlanBatchDetailVO.PlanBrief> plans = new ArrayList<>();
        for (PlanBatchItem it : items) {
            TestPlan tp = testPlanMapper.selectById(it.getPlanId());
            PlanBatchDetailVO.PlanBrief pb = new PlanBatchDetailVO.PlanBrief();
            pb.setId(it.getPlanId());
            pb.setName(tp == null ? null : tp.getName());
            pb.setSortOrder(it.getSortOrder());
            plans.add(pb);
        }
        vo.setPlans(plans);
        return vo;
    }

    /** 运行实例 → 出参，并实时聚合明细计数（不修改运行实例本身） */
    private PlanBatchRunVO toRunVO(PlanBatchRun run) {
        PlanBatchRunVO vo = new PlanBatchRunVO();
        vo.setId(run.getId());
        vo.setBatchId(run.getBatchId());
        vo.setTriggerType(run.getTriggerType());
        vo.setStartTime(run.getStartTime());
        vo.setEndTime(run.getEndTime());
        vo.setDurationMs(run.getDurationMs());
        vo.setStatus(run.getStatus());

        List<PlanBatchRunItem> items = planBatchRunItemMapper.selectList(
                new QueryWrapper<PlanBatchRunItem>().eq("run_id", run.getId()).orderByAsc("sort_order"));
        List<PlanBatchRunItemVO> ivos = new ArrayList<>();
        int passed = 0;
        int failed = 0;
        int running = 0;
        int queued = 0;
        for (PlanBatchRunItem ri : items) {
            PlanBatchRunItemVO ivo = new PlanBatchRunItemVO();
            ivo.setId(ri.getId());
            ivo.setRunId(ri.getRunId());
            ivo.setPlanId(ri.getPlanId());
            ivo.setPlanName(ri.getPlanName());
            ivo.setSortOrder(ri.getSortOrder());
            ivo.setStatus(ri.getStatus());
            ivo.setExecutionId(ri.getExecutionId());
            ivo.setDurationMs(ri.getDurationMs());
            ivo.setErrorMsg(ri.getErrorMsg());
            ivo.setStartTime(ri.getStartTime());
            ivo.setEndTime(ri.getEndTime());
            ivos.add(ivo);
            switch (ri.getStatus()) {
                case "SUCCESS" -> passed++;
                case "RUNNING" -> running++;
                case "QUEUED" -> queued++;
                default -> failed++;
            }
        }
        vo.setItems(ivos);
        vo.setTotal(items.size());
        vo.setPassed(passed);
        vo.setFailed(failed);
        vo.setRunning(running);
        vo.setQueued(queued);
        return vo;
    }

    private String normalizeStrategy(String s) {
        return "PARALLEL".equals(s) ? "PARALLEL" : "SERIAL";
    }

    private int normalizeConcurrency(Integer c) {
        return (c == null || c < 1) ? 3 : c;
    }

    private String normalizeCron(String cron) {
        return (cron == null || cron.isBlank()) ? null : cron.trim();
    }

    private String truncate(String msg) {
        if (msg == null) {
            return null;
        }
        return msg.length() > 2000 ? msg.substring(0, 2000) : msg;
    }
}
