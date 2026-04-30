package com.material.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.material.platform.dto.UserSummaryDto;
import com.material.platform.entity.User;
import com.material.platform.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public List<UserSummaryDto> listUsers() {
        return userMapper.selectList(new LambdaQueryWrapper<User>().orderByAsc(User::getCreatedAt, User::getId))
                .stream()
                .map(this::toSummary)
                .toList();
    }

    public UserSummaryDto createUser(String username, String displayName, String password, String role) {
        String normalizedUsername = normalizeRequiredValue(username, "用户名");
        String normalizedDisplayName = normalizeRequiredValue(displayName, "显示名称");
        String normalizedPassword = normalizeRequiredValue(password, "密码");
        String normalizedRole = normalizeRole(role);

        User existingUser = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, normalizedUsername)
                        .last("LIMIT 1")
        );
        if (existingUser != null) {
            throw new RuntimeException("用户名已存在");
        }

        User user = new User();
        user.setUsername(normalizedUsername);
        user.setDisplayName(normalizedDisplayName);
        user.setRole(normalizedRole);
        user.setPasswordHash(passwordEncoder.encode(normalizedPassword));
        userMapper.insert(user);
        return toSummary(user);
    }

    public UserSummaryDto updateUser(Long targetUserId, String displayName, String role) {
        User user = requireUser(targetUserId);
        user.setDisplayName(normalizeRequiredValue(displayName, "显示名称"));

        String normalizedRole = normalizeRole(role);
        if ("ADMIN".equals(user.getRole()) && !"ADMIN".equals(normalizedRole)) {
            ensureAnotherAdminExists(targetUserId);
        }

        user.setRole(normalizedRole);
        userMapper.updateById(user);
        return toSummary(user);
    }

    public void resetPassword(Long targetUserId, String newPassword) {
        User user = requireUser(targetUserId);
        user.setPasswordHash(passwordEncoder.encode(normalizeRequiredValue(newPassword, "新密码")));
        userMapper.updateById(user);
    }

    public void deleteUser(Long targetUserId, Long operatorUserId) {
        User targetUser = requireUser(targetUserId);
        if (targetUserId.equals(operatorUserId)) {
            throw new RuntimeException("不能删除当前登录用户");
        }

        if ("ADMIN".equals(targetUser.getRole())) {
            ensureAnotherAdminExists(targetUserId);
        }

        userMapper.deleteById(targetUserId);
    }

    private void ensureAnotherAdminExists(Long excludedUserId) {
        Long adminCount = userMapper.selectCount(
                new LambdaQueryWrapper<User>()
                        .eq(User::getRole, "ADMIN")
                        .ne(User::getId, excludedUserId)
        );
        if (adminCount == null || adminCount == 0) {
            throw new RuntimeException("至少保留一个管理员账号");
        }
    }

    private User requireUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        return user;
    }

    private UserSummaryDto toSummary(User user) {
        return new UserSummaryDto(user.getId(), user.getUsername(), user.getDisplayName(), user.getRole());
    }

    private String normalizeRequiredValue(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new RuntimeException(fieldName + "不能为空");
        }
        return value.trim();
    }

    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return "USER";
        }
        String normalizedRole = role.trim().toUpperCase();
        if (!"ADMIN".equals(normalizedRole) && !"USER".equals(normalizedRole)) {
            throw new RuntimeException("角色不合法");
        }
        return normalizedRole;
    }
}
