package com.atp.module.notify.channel;

import com.atp.module.notify.entity.NotifyChannel;
import com.atp.module.notify.entity.NotifyMessage;
import com.atp.module.notify.event.NotifyPayload;
import com.atp.module.notify.event.NotifyRecipients;
import com.atp.module.notify.mapper.NotifyMessageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 站内信发送器：将通知写入 {@code tb_notify_message} 收件箱，
 * 供前端顶栏铃铛轮询 / 消息中心展示。收件人为项目内 OWNER/MAINTAINER/DEVELOPER 成员。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InAppSender implements NotifySender {

    private final NotifyMessageMapper messageMapper;

    @Override
    public String type() {
        return "INAPP";
    }

    @Override
    public void send(NotifyChannel channel, NotifyPayload payload, NotifyRecipients recipients) {
        List<Long> userIds = recipients.getUserIds();
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        for (Long uid : userIds) {
            NotifyMessage msg = new NotifyMessage();
            msg.setUserId(uid);
            msg.setProjectId(payload.getProjectId());
            msg.setTitle(payload.getTitle());
            msg.setContent(payload.getContent());
            msg.setIsread(0);
            msg.setLinkUrl(payload.getLinkUrl());
            messageMapper.insert(msg);
        }
        log.info("站内信已写入 channelId={} 收件人={} title={}", channel.getId(), userIds, payload.getTitle());
    }
}
