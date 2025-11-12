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
	private String accessToken;
	private String refreshToken;
    private String type = "Bearer";
    private Long id;
    private String username;
    private String email;
    private Role role; // Changed to String to match frontend

    public JwtResponse(String accessToken, Long id, String username, String email, Role role, String refreshToken) {
        this.accessToken = accessToken;
        this.id = id;
        this.username = username;
        this.email = email;
        this.role = role != null ? role : null;
        this.refreshToken = refreshToken;
    }
}