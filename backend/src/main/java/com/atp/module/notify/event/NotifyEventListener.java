package com.atp.module.notify.event;

import com.atp.module.notify.service.NotifyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 通知事件监听器：在发布方事务提交之后、以独立线程异步派发，
 * 主执行流程零阻塞。派发过程自身吞掉异常，仅记录日志，避免影响业务。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotifyEventListener {

    private final NotifyService notifyService;

    // fallbackExecution=true：当发布方不在事务中（如批次执行 executeBatch 无事务）时，
    // 事件立即派发而非被静默丢弃；有事务时仍走 AFTER_COMMIT，行为不变。
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onNotifyEvent(NotifyEvent event) {
        NotifyPayload payload = event.getPayload();
        try {
            notifyService.dispatch(payload);
        } catch (Exception e) {
            log.error("通知派发失败 event={} projectId={}", payload.getEvent(), payload.getProjectId(), e);
        }
    }
}
