package com.atp.module.user.service.impl;

import com.atp.module.user.entity.User;
import com.atp.module.user.mapper.UserMapper;
import com.atp.module.user.service.UserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 用户服务实现
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Override
    public User getByUsername(String username) {
        if (!StringUtils.hasText(username)) {
            return null;
        }
        return lambdaQuery().eq(User::getUsername, username).one();
    }

    @Override
    public void updateLoginInfo(Long userId, String ip) {
        lambdaUpdate()
                .eq(User::getId, userId)
                .set(User::getLastLoginIp, ip)
                .set(User::getLastLoginTime, LocalDateTime.now())
                .update();
    }
}
