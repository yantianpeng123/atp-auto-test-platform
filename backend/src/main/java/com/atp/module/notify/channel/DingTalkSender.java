package com.atp.module.notify.channel;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.atp.module.notify.entity.NotifyChannel;
import com.atp.module.notify.event.NotifyPayload;
import com.atp.module.notify.event.NotifyRecipients;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

/**
 * 钉钉群机器人 Webhook 发送器。
 *
 * <p>config JSON 结构：{ webhook, secret(可选, 加签), atMobiles(可选, @手机号), msgtype }
 * 加签算法：HMAC-SHA256(timestamp + "\n" + secret) → Base64 → URL encode，拼到 webhook 后。
 */
@Slf4j
@Component
public class DingTalkSender implements NotifySender {

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public String type() {
        return "DINGTALK";
    }

    @Override
    public void send(NotifyChannel channel, NotifyPayload payload, NotifyRecipients recipients) throws Exception {
        JSONObject cfg = JSONUtil.parseObj(channel.getConfig());
        String webhook = cfg.getStr("webhook");
        if (webhook == null || webhook.isBlank()) {
            throw new IllegalArgumentException("钉钉渠道未配置 webhook");
        }
        String secret = cfg.getStr("secret", "");
        List<String> atMobiles = List.of();
        if (cfg.get("atMobiles") instanceof JSONArray arr) {
            atMobiles = arr.toList(String.class);
        }

        String url = webhook;
        if (secret != null && !secret.isBlank()) {
            long timestamp = Instant.now().toEpochMilli();
            String sign = sign(timestamp, secret);
            url = webhook + (webhook.contains("?") ? "&" : "?")
                    + "timestamp=" + timestamp + "&sign=" + URLEncoder.encode(sign, StandardCharsets.UTF_8);
        }

        JSONObject markdown = new JSONObject()
                .set("title", payload.getTitle())
                .set("text", payload.getContent());
        JSONObject body = new JSONObject().set("msgtype", "markdown").set("markdown", markdown);
        if (!atMobiles.isEmpty()) {
            body.set("at", new JSONObject().set("atMobiles", atMobiles));
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(body.toString(), headers);

        String resp = restTemplate.postForObject(url, entity, String.class);
        if (resp != null) {
            JSONObject r = JSONUtil.parseObj(resp);
            if (r.getInt("errcode", 0) != 0) {
                throw new RuntimeException("钉钉推送失败: " + r.getStr("errmsg", resp));
            }
        }
        log.info("钉钉通知已推送 channelId={} title={}", channel.getId(), payload.getTitle());
    }

    private String sign(long timestamp, String secret) throws NoSuchAlgorithmException, InvalidKeyException {
        String stringToSign = timestamp + "\n" + secret;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] signData = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(signData);
    }
}
