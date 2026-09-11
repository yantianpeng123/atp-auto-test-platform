package com.atp.module.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.atp.module.user.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户数据访问
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
