package com.atp.module.notify.event;

import lombok.Data;

import java.util.List;

/**
 * 通知收件人（按渠道类型取所需部分）：
 * <ul>
 *   <li>INAPP：userIds（写站内信收件箱）</li>
 *   <li>EMAIL_163：emails（发邮件）</li>
 *   <li>DINGTALK：无需收件人（群机器人 webhook）</li>
 * </ul>
 */
@Data
public class NotifyRecipients {

    private List<Long> userIds;

    private List<String> emails;
}
