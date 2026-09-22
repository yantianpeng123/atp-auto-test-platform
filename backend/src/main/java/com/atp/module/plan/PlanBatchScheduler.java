package com.atp.module.plan;

import com.atp.module.plan.entity.PlanBatch;
import com.atp.module.plan.mapper.PlanBatchMapper;
import com.atp.module.plan.service.PlanBatchService;
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
 * 批次定时调度：周期性扫描「已启用且有 cron」的批次，到点触发执行。
 *
 * <p>复用 {@link PlanBatchService#executeBatch(Long, String)}，与手动点「立即执行」共用同一内核。
 * 内存锁 Set 防同一批次重入，避免上一次未结束又被触发。
 */
@Component
@RequiredArgsConstructor
public class PlanBatchScheduler {

    private final PlanBatchMapper planBatchMapper;
    private final PlanBatchService planBatchService;

    /** 执行中批次的防重入锁 */
    private Set<Long> running = ConcurrentHashMap.newKeySet();

    @Scheduled(fixedRate = 60_000)
    public void scan() {
        List<PlanBatch> batches = planBatchMapper.selectList(
                new QueryWrapper<PlanBatch>().eq("enabled", 1).isNotNull("cron").ne("cron", ""));
        LocalDateTime now = LocalDateTime.now();
        for (PlanBatch batch : batches) {
            if (!shouldRun(batch.getCron(), now)) {
                continue;
            }
            if (!running.add(batch.getId())) {
                continue; // 上一次还在跑，跳过本次
            }
            try {
                planBatchService.executeBatch(batch.getId(), "SCHEDULED", null);
            } catch (Exception ignored) {
                // 单个批次失败不影响其他批次
            } finally {
                running.remove(batch.getId());
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
