package com.atp.module.notify.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通知规则：绑定「事件 + 渠道集合 + 附加条件」。
 *
 * <p>{@code channelIds} / {@code condition} 均为 JSON 列，实体按字符串存取。
 */
@Data
@TableName("tb_notify_rule")
public class NotifyRule {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long projectId;

    private String name;

    /** 触发事件：EXEC_DONE / EXEC_FAIL / BATCH_DONE */
    private String event;

    /** JSON 数组字符串：[channelId1, channelId2] */
    private String channelIds;

    /** JSON 字符串：如 {"onlyFail":true} */
    private String condition;

    /** 0-停用 1-启用 */
    private Integer enabled;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
