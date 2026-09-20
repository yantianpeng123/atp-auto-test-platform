package com.atp.module.notify.service;

import com.atp.common.result.PageResult;
import com.atp.module.notify.dto.MarkReadRequest;
import com.atp.module.notify.dto.NotifyChannelSaveRequest;
import com.atp.module.notify.dto.NotifyRuleSaveRequest;
import com.atp.module.notify.event.NotifyPayload;
import com.atp.module.notify.vo.NotifyChannelVO;
import com.atp.module.notify.vo.NotifyLogVO;
import com.atp.module.notify.vo.NotifyMessageVO;
import com.atp.module.notify.vo.NotifyRuleVO;
import com.atp.security.UserPrincipal;

import java.util.List;

/**
 * 通知中心服务
 */
public interface NotifyService {

    /** 渠道列表（OWNER/MAINTAINER/ADMIN 可见） */
    List<NotifyChannelVO> listChannels(Long projectId, UserPrincipal principal);

    void createChannel(NotifyChannelSaveRequest req, UserPrincipal principal);

    void updateChannel(Long id, NotifyChannelSaveRequest req, UserPrincipal principal);

    void deleteChannel(Long id, UserPrincipal principal);

    /** 规则列表（OWNER/MAINTAINER/ADMIN 可见） */
    List<NotifyRuleVO> listRules(Long projectId, UserPrincipal principal);

    void createRule(NotifyRuleSaveRequest req, UserPrincipal principal);

    void deleteRule(Long id, UserPrincipal principal);

    /** 发送日志（分页） */
    PageResult<NotifyLogVO> listLogs(Long projectId, long page, long size, UserPrincipal principal);

    /** 我的站内信（全员，仅本人） */
    List<NotifyMessageVO> listMessages(Long projectId, Boolean unread, UserPrincipal principal);

    void markRead(MarkReadRequest req, UserPrincipal principal);

    /** 未读角标数（当前用户） */
    long unreadCount(Long projectId, UserPrincipal principal);

    /**
     * 派发通知：按 项目 + 事件 + 启用规则 命中渠道并发送，逐渠道写发送日志（失败重试 1 次）。
     * 由 NotifyEventListener 在事务提交后异步调用，内部吞掉异常，保证主流程零阻塞。
     */
    void dispatch(NotifyPayload payload);
}
