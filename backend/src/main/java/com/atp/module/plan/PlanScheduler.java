package com.atp.module.plan;

import com.atp.module.plan.entity.TestPlan;
import com.atp.module.plan.mapper.TestPlanMapper;
import com.atp.module.plan.service.TestPlanService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 测试计划定时调度：周期性扫描「已启用且有 cron」的计划，到点触发执行。
 *
 * <p>复用 {@link TestPlanService#executePlan(Long)}，与手动点「执行」共用同一执行内核。
 * 内存锁 Set 防同一计划重入，避免上一次未结束又被触发。
 */
@Component
@RequiredArgsConstructor
public class PlanScheduler {

    private final TestPlanMapper testPlanMapper;
    private final TestPlanService testPlanService;

    /** 执行中计划的防重入锁 */
    private Set<Long> running = ConcurrentHashMap.newKeySet();

    @Scheduled(fixedRate = 60_000)
    public void scan() {
        List<TestPlan> plans = testPlanMapper.selectList(
                new QueryWrapper<TestPlan>().eq("enabled", 1).isNotNull("cron").ne("cron", ""));
        LocalDateTime now = LocalDateTime.now();
        for (TestPlan plan : plans) {
            if (!shouldRun(plan.getCron(), now)) {
                continue;
            }
            if (!running.add(plan.getId())) {
                continue; // 上一次还在跑，跳过本次
            }
            try {
                testPlanService.executePlan(plan.getId());
            } catch (Exception ignored) {
                // 单个计划失败不影响其他计划
            } finally {
                running.remove(plan.getId());
            }
        }
    }

    /** 判断给定 cron 在最近 1 分钟内是否应触发一次 */
    private boolean shouldRun(String cron, LocalDateTime now) {
        try {
            CronExpression expr = CronExpression.parse(cron);
            LocalDateTime after = now.minusSeconds(61);
            LocalDateTime next = expr.next(after);
            return next != null && next.isAfter(after) && !next.isAfter(now);
        } catch (Exception e) {
            return false;
        }
    }
}
