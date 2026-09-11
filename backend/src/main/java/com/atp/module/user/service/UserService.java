package com.atp.module.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.atp.module.user.entity.User;

/**
 * 用户服务
 */
public interface UserService extends IService<User> {

    /**
     * 按用户名查询
     */
    User getByUsername(String username);

    /**
     * 更新最近登录信息
     */
    void updateLoginInfo(Long userId, String ip);
}
