package com.atp.module.user.controller;

import com.atp.common.result.Result;
import com.atp.common.result.ResultCode;
import com.atp.module.user.entity.User;
import com.atp.module.user.service.UserService;
import com.atp.module.user.service.impl.AuthServiceImpl;
import com.atp.module.user.vo.UserInfoVO;
import com.atp.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户接口
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 当前登录用户信息
     */
    @GetMapping("/info")
    public Result<UserInfoVO> info() {
        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        User user = userService.getById(principal.getId());
        if (user == null) {
            return Result.failure(ResultCode.USER_NOT_FOUND);
        }
        return Result.success(AuthServiceImpl.toUserInfoVO(user));
    }

    /**
     * 用户名是否可用（true 表示未被占用）
     */
    @GetMapping("/check-username")
    public Result<Boolean> checkUsername(@RequestParam String username) {
        return Result.success(userService.getByUsername(username) == null);
    }

    /**
     * 平台所有用户列表（用于「添加成员」等需要选择用户的下拉场景）
     */
    @GetMapping("/list")
    public Result<List<UserInfoVO>> list() {
        List<User> users = userService.list();
        List<UserInfoVO> vos = users.stream()
                .map(AuthServiceImpl::toUserInfoVO)
                .collect(Collectors.toList());
        return Result.success(vos);
    }
}
