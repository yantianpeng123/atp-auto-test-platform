package com.atp.module.notify.channel;

import cn.hutool.json.JSONUtil;
import com.atp.module.notify.entity.NotifyChannel;
import com.atp.module.notify.event.NotifyPayload;
import com.atp.module.notify.event.NotifyRecipients;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Properties;

/**
 * 网易 163 邮箱 SMTP 发送器。
 *
 * <p>config JSON 结构：{ host, port, username, authCode(授权码非登录密码), from, ssl, to(可选) }
 * 收件人优先级：recipients.emails（dispatch 阶段解析好的）→ 回落到 config.to（逗号/数组）→ 再回落到项目成员邮箱。
 */
@Slf4j
@Component
public class Mail163Sender implements NotifySender {

    @Override
    public String type() {
        return "EMAIL_163";
    }

    @Override
    public void send(NotifyChannel channel, NotifyPayload payload, NotifyRecipients recipients) throws Exception {
        cn.hutool.json.JSONObject cfg = JSONUtil.parseObj(channel.getConfig());
        String host = cfg.getStr("host", "smtp.163.com");
        int port = cfg.getInt("port", 465);
        String username = cfg.getStr("username");
        String authCode = cfg.getStr("authCode");
        String from = cfg.getStr("from", username);
        boolean ssl = cfg.getBool("ssl", true);
        if (username == null || authCode == null) {
            throw new IllegalArgumentException("163 邮件渠道未配置用户名/授权码");
        }

        List<String> toList = recipients.getEmails();
        if (toList == null || toList.isEmpty()) {
            toList = parseTo(cfg.getStr("to"));
        }
        if (toList == null || toList.isEmpty()) {
            throw new RuntimeException("163 邮件无有效收件人（请配置 to 或确保项目成员有邮箱）");
        }

        JavaMailSenderImpl mail = new JavaMailSenderImpl();
        mail.setHost(host);
        mail.setPort(port);
        mail.setUsername(username);
        mail.setPassword(authCode);
        Properties props = mail.getJavaMailProperties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.timeout", "15000");
        if (ssl) {
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.socketFactory.port", String.valueOf(port));
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        } else {
            props.put("mail.smtp.starttls.enable", "true");
        }

        MimeMessage message = mail.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(from);
        helper.setTo(toList.toArray(new String[0]));
        helper.setSubject(payload.getTitle());
        helper.setText(payload.getContent(), false);
        mail.send(message);
        log.info("163 邮件已发送 channelId={} to={} title={}", channel.getId(), toList, payload.getTitle());
    }

    private List<String> parseTo(String to) {
        if (to == null || to.isBlank()) {
            return List.of();
        }
        List<String> list = new java.util.ArrayList<>();
        for (String s : to.split("[,;\\s]+")) {
            if (!s.isBlank()) {
                list.add(s.trim());
            }
        }
        return list;
    }
}
