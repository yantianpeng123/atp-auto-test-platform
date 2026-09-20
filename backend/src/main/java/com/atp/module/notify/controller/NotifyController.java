package com.atp.module.notify.controller;

import com.atp.common.result.Result;
import com.atp.module.notify.dto.MarkReadRequest;
import com.atp.module.notify.dto.NotifyChannelSaveRequest;
import com.atp.module.notify.dto.NotifyRuleSaveRequest;
import com.atp.module.notify.service.NotifyService;
import com.atp.module.notify.vo.NotifyChannelVO;
import com.atp.module.notify.vo.NotifyLogVO;
import com.atp.module.notify.vo.NotifyMessageVO;
import com.atp.module.notify.vo.NotifyRuleVO;
import com.atp.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 通知中心接口（前缀 /api/notify，受全局 JWT 鉴权）
 */
@RestController
@RequestMapping("/api/notify")
@RequiredArgsConstructor
public class NotifyController {

    private final NotifyService notifyService;

    private UserPrincipal principal() {
        return (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    /* ---------------- 渠道 ---------------- */

    @GetMapping("/channel")
    public Result<List<NotifyChannelVO>> channels(@RequestParam Long projectId) {
        return Result.success(notifyService.listChannels(projectId, principal()));
    }

    @PostMapping("/channel")
    public Result<Void> createChannel(@Valid @RequestBody NotifyChannelSaveRequest req) {
        notifyService.createChannel(req, principal());
        return Result.ok("渠道已创建");
    }

    /** 部分更新：仅覆盖前端显式传回的字段（新建用全量校验，此处不用 @Valid） */
    @PutMapping("/channel/{id}")
    public Result<Void> updateChannel(@PathVariable Long id, @RequestBody NotifyChannelSaveRequest req) {
        notifyService.updateChannel(id, req, principal());
        return Result.ok("渠道已更新");
    }

    @DeleteMapping("/channel/{id}")
    public Result<Void> deleteChannel(@PathVariable Long id) {
        notifyService.deleteChannel(id, principal());
        return Result.ok("渠道已删除");
    }

    /* ---------------- 规则 ---------------- */

    @GetMapping("/rule")
    public Result<List<NotifyRuleVO>> rules(@RequestParam Long projectId) {
        return Result.success(notifyService.listRules(projectId, principal()));
    }

    @PostMapping("/rule")
    public Result<Void> createRule(@Valid @RequestBody NotifyRuleSaveRequest req) {
        notifyService.createRule(req, principal());
        return Result.ok("规则已创建");
    }

    @DeleteMapping("/rule/{id}")
    public Result<Void> deleteRule(@PathVariable Long id) {
        notifyService.deleteRule(id, principal());
        return Result.ok("规则已删除");
    }

    /* ---------------- 发送日志 ---------------- */

    @GetMapping("/log")
    public Result<com.atp.common.result.PageResult<NotifyLogVO>> logs(
            @RequestParam Long projectId,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size) {
        return Result.success(notifyService.listLogs(projectId, page, size, principal()));
    }

    /* ---------------- 站内信收件箱 ---------------- */

    @GetMapping("/messages")
    public Result<List<NotifyMessageVO>> messages(
            @RequestParam Long projectId,
            @RequestParam(required = false) Boolean unread) {
        return Result.success(notifyService.listMessages(projectId, unread, principal()));
    }

    @PostMapping("/messages/read")
    public Result<Void> markRead(@Valid @RequestBody MarkReadRequest req) {
        notifyService.markRead(req, principal());
        return Result.ok("已标记已读");
    }

    @GetMapping("/unread-count")
    public Result<Long> unreadCount(@RequestParam Long projectId) {
        return Result.success(notifyService.unreadCount(projectId, principal()));
    }
}
