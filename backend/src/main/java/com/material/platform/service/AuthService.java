package com.material.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.material.platform.dto.LoginUserDto;
import com.material.platform.entity.User;
import com.material.platform.mapper.UserMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    public static final String LOGIN_USER_SESSION_KEY = "LOGIN_USER";
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_USER = "USER";

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @PostConstruct
    public void initializeDefaultAdmin() {
        Long userCount = userMapper.selectCount(null);
        if (userCount != null && userCount > 0) {
            return;
        }

        User admin = new User();
        admin.setUsername("admin");
        admin.setDisplayName("系统管理员");
        admin.setRole(ROLE_ADMIN);
        admin.setPasswordHash(passwordEncoder.encode("admin123"));
        userMapper.insert(admin);
    }

    public LoginUserDto login(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new RuntimeException("用户名和密码不能为空");
        }

        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, username.trim())
                        .last("LIMIT 1")
        );

        if (user == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new RuntimeException("用户名或密码错误");
        }

        return toLoginUser(user);
    }

    public LoginUserDto getLoginUser(Long userId) {
        if (userId == null) {
            return null;
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            return null;
        }
        return toLoginUser(user);
    }

    public void requireAdmin(Long userId) {
        LoginUserDto loginUser = getLoginUser(userId);
        if (loginUser == null) {
            throw new RuntimeException("请先登录");
        }
        if (!ROLE_ADMIN.equals(loginUser.getRole())) {
            throw new RuntimeException("只有管理员可以执行该操作");
        }
    }

    private LoginUserDto toLoginUser(User user) {
        String role = user.getRole() == null || user.getRole().isBlank() ? ROLE_USER : user.getRole();
        return new LoginUserDto(user.getId(), user.getUsername(), user.getDisplayName(), role);
    }
}
