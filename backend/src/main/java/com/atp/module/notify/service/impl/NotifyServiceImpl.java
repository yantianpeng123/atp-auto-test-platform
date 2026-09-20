package com.atp.module.notify.service.impl;

import cn.hutool.json.JSONUtil;
import com.atp.common.exception.BizException;
import com.atp.common.result.PageResult;
import com.atp.common.result.ResultCode;
import com.atp.module.notify.channel.DingTalkSender;
import com.atp.module.notify.channel.InAppSender;
import com.atp.module.notify.channel.Mail163Sender;
import com.atp.module.notify.channel.NotifySender;
import com.atp.module.notify.dto.MarkReadRequest;
import com.atp.module.notify.dto.NotifyChannelSaveRequest;
import com.atp.module.notify.dto.NotifyRuleSaveRequest;
import com.atp.module.notify.entity.NotifyChannel;
import com.atp.module.notify.entity.NotifyLog;
import com.atp.module.notify.entity.NotifyMessage;
import com.atp.module.notify.entity.NotifyRule;
import com.atp.module.notify.event.NotifyPayload;
import com.atp.module.notify.event.NotifyRecipients;
import com.atp.module.notify.mapper.NotifyChannelMapper;
import com.atp.module.notify.mapper.NotifyLogMapper;
import com.atp.module.notify.mapper.NotifyMessageMapper;
import com.atp.module.notify.mapper.NotifyRuleMapper;
import com.atp.module.notify.service.NotifyService;
import com.atp.module.notify.vo.NotifyChannelVO;
import com.atp.module.notify.vo.NotifyLogVO;
import com.atp.module.notify.vo.NotifyMessageVO;
import com.atp.module.notify.vo.NotifyRuleVO;
import com.atp.module.project.entity.ProjectMember;
import com.atp.module.project.mapper.ProjectMemberMapper;
import com.atp.module.user.entity.User;
import com.atp.module.user.mapper.UserMapper;
import com.atp.security.UserPrincipal;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 通知中心服务实现。
 *
 * <p>权限：渠道/规则的管理（增删改查）仅限项目 OWNER / MAINTAINER / 全局 ADMIN；
 * 站内信的查看/已读仅限当前用户本人。
 *
 * <p>为避免与 ProjectMemberService 形成循环依赖，此处直接注入 DAO 层
 * （{@code ProjectMemberMapper} / {@code UserMapper}）做存在性与角色判断与收件人解析。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotifyServiceImpl implements NotifyService {

    private static final String ROLE_ADMIN = "ADMIN";

    private final NotifyChannelMapper channelMapper;
    private final NotifyRuleMapper ruleMapper;
    private final NotifyLogMapper logMapper;
    private final NotifyMessageMapper messageMapper;
    private final ProjectMemberMapper projectMemberMapper;
    private final UserMapper userMapper;

    private final DingTalkSender dingTalkSender;
    private final Mail163Sender mail163Sender;
    private final InAppSender inAppSender;

    // ==================== 渠道 ====================

    @Override
    public List<NotifyChannelVO> listChannels(Long projectId, UserPrincipal principal) {
        assertManageable(projectId, principal);
        List<NotifyChannel> list = channelMapper.selectList(
                new QueryWrapper<NotifyChannel>().eq("project_id", projectId).orderByDesc("create_time"));
        return list.stream().map(this::toChannelVO).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createChannel(NotifyChannelSaveRequest req, UserPrincipal principal) {
        assertManageable(req.getProjectId(), principal);
        NotifyChannel ch = new NotifyChannel();
        ch.setProjectId(req.getProjectId());
        ch.setType(req.getType());
        ch.setName(req.getName());
        ch.setEnabled(req.getEnabled() == null ? 1 : req.getEnabled());
        ch.setConfig(req.getConfig() == null ? null : JSONUtil.toJsonStr(req.getConfig()));
        channelMapper.insert(ch);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateChannel(Long id, NotifyChannelSaveRequest req, UserPrincipal principal) {
        NotifyChannel ch = channelMapper.selectById(id);
        if (ch == null || isDeleted(ch.getDeleted())) {
            throw new BizException(ResultCode.NOT_FOUND, "通知渠道不存在");
        }
        assertManageable(ch.getProjectId(), principal);
        // 部分更新：仅覆盖前端显式传回的字段（开关切换只传 enabled，编辑不传 type）
        if (req.getName() != null) {
            ch.setName(req.getName());
        }
        if (req.getEnabled() != null) {
            ch.setEnabled(req.getEnabled());
        }
        if (req.getConfig() != null) {
            ch.setConfig(JSONUtil.toJsonStr(req.getConfig()));
        }
        channelMapper.updateById(ch);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteChannel(Long id, UserPrincipal principal) {
        NotifyChannel ch = channelMapper.selectById(id);
        if (ch == null || isDeleted(ch.getDeleted())) {
            throw new BizException(ResultCode.NOT_FOUND, "通知渠道不存在");
        }
        assertManageable(ch.getProjectId(), principal);
        channelMapper.deleteById(id); // 逻辑删除
    }

    // ==================== 规则 ====================

    @Override
    public List<NotifyRuleVO> listRules(Long projectId, UserPrincipal principal) {
        assertManageable(projectId, principal);
        List<NotifyRule> list = ruleMapper.selectList(
                new QueryWrapper<NotifyRule>().eq("project_id", projectId).orderByDesc("create_time"));
        return list.stream().map(this::toRuleVO).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createRule(NotifyRuleSaveRequest req, UserPrincipal principal) {
        assertManageable(req.getProjectId(), principal);
        NotifyRule rule = new NotifyRule();
        rule.setProjectId(req.getProjectId());
        rule.setEvent(req.getEvent());
        rule.setChannelIds(JSONUtil.toJsonStr(req.getChannelIds()));
        rule.setCondition(req.getCondition() == null ? null : JSONUtil.toJsonStr(req.getCondition()));
        rule.setEnabled(req.getEnabled() == null ? 1 : req.getEnabled());
        ruleMapper.insert(rule);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRule(Long id, UserPrincipal principal) {
        NotifyRule rule = ruleMapper.selectById(id);
        if (rule == null || isDeleted(rule.getDeleted())) {
            throw new BizException(ResultCode.NOT_FOUND, "通知规则不存在");
        }
        assertManageable(rule.getProjectId(), principal);
        ruleMapper.deleteById(id); // 逻辑删除
    }

    // ==================== 发送日志 ====================

    @Override
    public PageResult<NotifyLogVO> listLogs(Long projectId, long page, long size, UserPrincipal principal) {
        assertManageable(projectId, principal);
        Page<NotifyLog> p = new Page<>(page, size);
        Page<NotifyLog> res = logMapper.selectPage(p, new QueryWrapper<NotifyLog>()
                .eq("project_id", projectId).orderByDesc("create_time"));
        List<NotifyLogVO> vos = res.getRecords().stream().map(this::toLogVO).toList();
        return PageResult.<NotifyLogVO>builder()
                .records(vos)
                .total(res.getTotal())
                .page(res.getCurrent())
                .size(res.getSize())
                .build();
    }

    // ==================== 站内信收件箱 ====================

    @Override
    public List<NotifyMessageVO> listMessages(Long projectId, Boolean unread, UserPrincipal principal) {
        QueryWrapper<NotifyMessage> qw = new QueryWrapper<NotifyMessage>()
                .eq("user_id", principal.getId())
                .eq("project_id", projectId)
                .orderByDesc("create_time");
        if (Boolean.TRUE.equals(unread)) {
            qw.eq("read", 0);
        }
        return messageMapper.selectList(qw).stream().map(this::toMessageVO).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markRead(MarkReadRequest req, UserPrincipal principal) {
        if (!req.isAll() && (req.getIds() == null || req.getIds().isEmpty())) {
            return;
        }
        QueryWrapper<NotifyMessage> qw = new QueryWrapper<NotifyMessage>()
                .eq("user_id", principal.getId())
                .eq("project_id", req.getProjectId());
        if (!req.isAll()) {
            qw.in("id", req.getIds());
        }
        NotifyMessage upd = new NotifyMessage();
        upd.setRead(1);
        messageMapper.update(upd, qw);
    }

    @Override
    public long unreadCount(Long projectId, UserPrincipal principal) {
        Long count = messageMapper.selectCount(new QueryWrapper<NotifyMessage>()
                .eq("user_id", principal.getId())
                .eq("project_id", projectId)
                .eq("read", 0));
        return count == null ? 0 : count;
    }

    // ==================== 派发（异步，@TransactionalEventListener AFTER_COMMIT 调用） ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void dispatch(NotifyPayload payload) {
        buildContent(payload);

        List<NotifyRule> rules = ruleMapper.selectList(new QueryWrapper<NotifyRule>()
                .eq("project_id", payload.getProjectId())
                .eq("enabled", 1)
                .eq("event", payload.getEvent()));
        if (rules.isEmpty()) {
            return;
        }
        for (NotifyRule rule : rules) {
            if (!matchCondition(rule, payload)) {
                continue;
            }
            for (Long cid : parseChannelIds(rule.getChannelIds())) {
                NotifyChannel ch = channelMapper.selectById(cid);
                if (ch == null || isDeleted(ch.getDeleted()) || isDisabled(ch.getEnabled())) {
                    continue;
                }
                NotifyRecipients recipients = resolveRecipients(ch, payload);
                sendWithRetry(ch, payload, recipients, rule.getId());
            }
        }
    }

    /** 发送并尝试 1 次重试，详细结果写入 tb_notify_log（无论成败都留存） */
    private void sendWithRetry(NotifyChannel ch, NotifyPayload payload,
                              NotifyRecipients recipients, Long ruleId) {
        NotifyLog log = new NotifyLog();
        log.setProjectId(payload.getProjectId());
        log.setRuleId(ruleId);
        log.setChannelId(ch.getId());
        log.setEvent(payload.getEvent());
        log.setChannelType(ch.getType());
        log.setContent(truncate(payload.getContent(), 1000));

        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                NotifySender sender = senderFor(ch.getType());
                sender.send(ch, payload, recipients);
                log.setStatus("SUCCESS");
                log.setTarget(targetOf(ch, recipients));
                log.setError(null);
                logMapper.insert(log);
                return;
            } catch (Exception e) {
                log.setTarget(targetOf(ch, recipients));
                log.setError(truncate(e.getMessage(), 1000));
                if (attempt == 1) {
                    // 第二次仍失败：落 FAILED 日志
                    log.setStatus("FAILED");
                    logMapper.insert(log);
                }
                // attempt==0 失败：循环进入重试，不落库
            }
        }
    }

    private NotifyRecipients resolveRecipients(NotifyChannel ch, NotifyPayload payload) {
        NotifyRecipients r = new NotifyRecipients();
        if ("INAPP".equals(ch.getType())) {
            r.setUserIds(projectMemberUserIds(payload.getProjectId()));
        } else if ("EMAIL_163".equals(ch.getType())) {
            r.setEmails(projectMemberEmails(payload.getProjectId()));
        }
        // DINGTALK 无需收件人
        return r;
    }

    private String targetOf(NotifyChannel ch, NotifyRecipients recipients) {
        return switch (ch.getType()) {
            case "DINGTALK" -> {
                try {
                    yield (String) JSONUtil.parseObj(ch.getConfig()).get("webhook");
                } catch (Exception e) {
                    yield "";
                }
            }
            case "EMAIL_163" -> recipients.getEmails() == null ? "" : String.join(",", recipients.getEmails());
            case "INAPP" -> recipients.getUserIds() == null ? "" : recipients.getUserIds().toString();
            default -> "";
        };
    }

    // ==================== 辅助 ====================

    private void buildContent(NotifyPayload p) {
        String verb = "EXEC_FAIL".equals(p.getEvent()) ? "失败" : "完成";
        p.setTitle(String.format("[测试通知] 用例《%s》执行%s", str(p.getCaseName()), verb));
        String link = p.getLinkUrl() == null ? "" : p.getLinkUrl();
        p.setContent(String.format(
                "**用例：** %s\n**结果：** %s\n**通过/失败轮次：** %d / %d\n**执行人：** %s\n**查看报告：** %s",
                str(p.getCaseName()), str(p.getStatus()),
                nz(p.getPassedRounds()), nz(p.getFailedRounds()),
                str(p.getExecutorName()), link));
    }

    private boolean matchCondition(NotifyRule rule, NotifyPayload payload) {
        if (rule.getCondition() == null || rule.getCondition().isBlank()) {
            return true;
        }
        Map<String, Object> c = JSONUtil.toBean(rule.getCondition(), Map.class);
        if (Boolean.TRUE.equals(c.get("onlyFail")) && !"FAILED".equals(payload.getStatus())) {
            return false;
        }
        return true;
    }

    private List<Long> parseChannelIds(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        return JSONUtil.toList(json, Long.class);
    }

    private NotifySender senderFor(String type) {
        return switch (type) {
            case "DINGTALK" -> dingTalkSender;
            case "EMAIL_163" -> mail163Sender;
            case "INAPP" -> inAppSender;
            default -> throw new BizException(ResultCode.BAD_REQUEST, "未知通知渠道类型: " + type);
        };
    }

    /** 项目内可接收站内信的成员 userId 列表 */
    private List<Long> projectMemberUserIds(Long projectId) {
        List<ProjectMember> members = projectMemberMapper.selectList(new QueryWrapper<ProjectMember>()
                .eq("project_id", projectId)
                .eq("deleted", 0)
                .in("role", "OWNER", "MAINTAINER", "DEVELOPER"));
        List<Long> ids = new ArrayList<>();
        for (ProjectMember m : members) {
            ids.add(m.getUserId());
        }
        return ids;
    }

    /** 项目内成员的邮箱列表（仅非空） */
    private List<String> projectMemberEmails(Long projectId) {
        List<Long> userIds = projectMemberUserIds(projectId);
        if (userIds.isEmpty()) {
            return List.of();
        }
        List<User> users = userMapper.selectBatchIds(userIds);
        List<String> emails = new ArrayList<>();
        for (User u : users) {
            if (u.getEmail() != null && !u.getEmail().isBlank()) {
                emails.add(u.getEmail());
            }
        }
        return emails;
    }

    private void assertManageable(Long projectId, UserPrincipal principal) {
        if (ROLE_ADMIN.equals(principal.getRole())) {
            return;
        }
        ProjectMember m = projectMemberMapper.selectByProjectAndUser(projectId, principal.getId());
        if (m == null || isDeleted(m.getDeleted())) {
            throw new BizException(ResultCode.FORBIDDEN, "您不是该项目成员，无法管理通知配置");
        }
        String role = m.getRole();
        if (!("OWNER".equals(role) || "MAINTAINER".equals(role))) {
            throw new BizException(ResultCode.FORBIDDEN, "仅项目 OWNER 或 MAINTAINER 可管理通知配置");
        }
    }

    // ==================== 转换 ====================

    private NotifyChannelVO toChannelVO(NotifyChannel ch) {
        Object cfg = null;
        if (ch.getConfig() != null && !ch.getConfig().isBlank()) {
            cfg = JSONUtil.toBean(ch.getConfig(), Map.class);
        }
        return NotifyChannelVO.builder()
                .id(ch.getId())
                .projectId(ch.getProjectId())
                .type(ch.getType())
                .name(ch.getName())
                .enabled(ch.getEnabled() != null && ch.getEnabled() == 1)
                .config(cfg)
                .createTime(ch.getCreateTime())
                .build();
    }

    private NotifyRuleVO toRuleVO(NotifyRule rule) {
        List<Long> ids = List.of();
        if (rule.getChannelIds() != null && !rule.getChannelIds().isBlank()) {
            ids = JSONUtil.toList(rule.getChannelIds(), Long.class);
        }
        Object cond = null;
        if (rule.getCondition() != null && !rule.getCondition().isBlank()) {
            cond = JSONUtil.toBean(rule.getCondition(), Map.class);
        }
        return NotifyRuleVO.builder()
                .id(rule.getId())
                .projectId(rule.getProjectId())
                .event(rule.getEvent())
                .channelIds(ids)
                .condition(cond)
                .enabled(rule.getEnabled() != null && rule.getEnabled() == 1)
                .createTime(rule.getCreateTime())
                .build();
    }

    private NotifyLogVO toLogVO(NotifyLog log) {
        return NotifyLogVO.builder()
                .id(log.getId())
                .projectId(log.getProjectId())
                .event(log.getEvent())
                .channelType(log.getChannelType())
                .target(log.getTarget())
                .status(log.getStatus())
                .content(log.getContent())
                .error(log.getError())
                .createTime(log.getCreateTime())
                .build();
    }

    private NotifyMessageVO toMessageVO(NotifyMessage msg) {
        return NotifyMessageVO.builder()
                .id(msg.getId())
                .userId(msg.getUserId())
                .projectId(msg.getProjectId())
                .title(msg.getTitle())
                .content(msg.getContent())
                .read(msg.getRead() != null && msg.getRead() == 1)
                .linkUrl(msg.getLinkUrl())
                .createTime(msg.getCreateTime())
                .build();
    }

    // ==================== 工具 ====================

    private boolean isDeleted(Integer deleted) {
        return deleted != null && deleted == 1;
    }

    private boolean isDisabled(Integer enabled) {
        return enabled == null || enabled == 0;
    }

    private String str(String s) {
        return s == null ? "" : s;
    }

    private int nz(Integer n) {
        return n == null ? 0 : n;
    }

    private String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() > max ? s.substring(0, max) : s;
    }
}
