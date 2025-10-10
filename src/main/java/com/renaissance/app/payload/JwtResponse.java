package com.renaissance.app.payload;

import com.renaissance.app.model.Role;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@Builder
public class JwtResponse {
    private String token;
    private String type = "Bearer";
    private Long id;
    private String username;
    private String email;
    private Role role; // Changed to String to match frontend

    public JwtResponse(String token, Long id, String username, String email, Role role) {
        this.token = token;
        this.id = id;
        this.username = username;
        this.email = email;
        this.role = role != null ? role : null;
    }
}