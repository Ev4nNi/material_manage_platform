package com.material.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginUserDto {

    private Long id;
    private String username;
    private String displayName;
    private String role;
}
