package com.atp.module.notify.event;

import org.springframework.context.ApplicationEvent;

/**
 * 通知触发事件。由执行引擎在「执行记录落库后」（事务内）发布，
 * 由 {@link NotifyEventListener} 在事务提交后异步派发。
 */
public class NotifyEvent extends ApplicationEvent {

    private final NotifyPayload payload;

    public NotifyEvent(Object source, NotifyPayload payload) {
        super(source);
        this.payload = payload;
    }

    public NotifyPayload getPayload() {
        return payload;
    }
}
