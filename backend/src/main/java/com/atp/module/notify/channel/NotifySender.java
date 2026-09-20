package com.atp.module.notify.channel;

import com.atp.module.notify.entity.NotifyChannel;
import com.atp.module.notify.event.NotifyPayload;
import com.atp.module.notify.event.NotifyRecipients;

/**
 * 通知发送器统一接口。每种渠道一个实现，由 NotifyService 按渠道 type 路由。
 */
public interface NotifySender {

    /** 渠道类型，对应 {@link NotifyChannel#getType()} 的取值 */
    String type();

    /**
     * 发送通知。
     *
     * @param channel    渠道配置（含 config JSON）
     * @param payload    事件载荷（title/content 已组装好）
     * @param recipients 收件人（按渠道类型取所需字段）
     */
    void send(NotifyChannel channel, NotifyPayload payload, NotifyRecipients recipients) throws Exception;
}
