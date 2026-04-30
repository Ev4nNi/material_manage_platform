package com.material.platform.controller;

import com.material.platform.common.Result;
import com.material.platform.dto.LoginUserDto;
import com.material.platform.dto.UserSummaryDto;
import com.material.platform.service.AuthService;
import com.material.platform.service.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final AuthService authService;
    private final UserService userService;

    @GetMapping
    public Result<List<UserSummaryDto>> listUsers(HttpSession session) {
        authService.requireAdmin(getSessionUserId(session));
        return Result.success(userService.listUsers());
    }

    @PostMapping
    public Result<UserSummaryDto> createUser(@RequestBody Map<String, String> params, HttpSession session) {
        try {
            authService.requireAdmin(getSessionUserId(session));
            return Result.success(userService.createUser(
                    params.get("username"),
                    params.get("displayName"),
                    params.get("password"),
                    params.get("role")
            ));
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public Result<UserSummaryDto> updateUser(@PathVariable Long id,
                                             @RequestBody Map<String, String> params,
                                             HttpSession session) {
        try {
            Long operatorUserId = getSessionUserId(session);
            authService.requireAdmin(operatorUserId);
            return Result.success(userService.updateUser(
                    id,
                    params.get("displayName"),
                    params.get("role")
            ));
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/{id}/password")
    public Result<Void> resetPassword(@PathVariable Long id,
                                      @RequestBody Map<String, String> params,
                                      HttpSession session) {
        try {
            authService.requireAdmin(getSessionUserId(session));
            userService.resetPassword(id, params.get("password"));
            return Result.success(null);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteUser(@PathVariable Long id, HttpSession session) {
        try {
            Long operatorUserId = getSessionUserId(session);
            authService.requireAdmin(operatorUserId);
            userService.deleteUser(id, operatorUserId);
            return Result.success(null);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    private Long getSessionUserId(HttpSession session) {
        Object loginUser = session.getAttribute(AuthService.LOGIN_USER_SESSION_KEY);
        if (loginUser instanceof LoginUserDto loginUserDto) {
            return loginUserDto.getId();
        }
        if (loginUser instanceof Long userId) {
            return userId;
        }
        return null;
    }
}
