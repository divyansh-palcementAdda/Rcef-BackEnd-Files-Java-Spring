package com.renaissance.app.payload;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.renaissance.app.model.Role;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserRequest {
	private String username;
	private String password;
	private String email;
	private String fullName;
	private Role role;
	private Long departmentId;
}
