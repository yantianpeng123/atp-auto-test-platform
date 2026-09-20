package com.atp.module.notify.mapper;

import com.atp.module.notify.entity.NotifyLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 通知发送日志数据访问
 */
@Mapper
public interface NotifyLogMapper extends BaseMapper<NotifyLog> {
}
