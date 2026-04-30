package com.material.platform.controller;

import com.material.platform.common.Result;
import com.material.platform.dto.LoginUserDto;
import com.material.platform.service.AuthService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public Result<LoginUserDto> login(@RequestBody Map<String, String> params, HttpSession session) {
        try {
            LoginUserDto loginUser = authService.login(params.get("username"), params.get("password"));
            session.setAttribute(AuthService.LOGIN_USER_SESSION_KEY, loginUser);
            return Result.success(loginUser);
        } catch (RuntimeException e) {
            return Result.error(401, e.getMessage());
        }
    }

    @PostMapping("/logout")
    public Result<Void> logout(HttpSession session) {
        session.invalidate();
        return Result.success(null);
    }

    @GetMapping("/me")
    public Result<LoginUserDto> currentUser(HttpSession session) {
        LoginUserDto loginUser = (LoginUserDto) session.getAttribute(AuthService.LOGIN_USER_SESSION_KEY);
        if (loginUser == null) {
            return Result.error(401, "请先登录");
        }
        return Result.success(loginUser);
    }
}
