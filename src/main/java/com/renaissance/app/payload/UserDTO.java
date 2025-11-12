package com.renaissance.app.payload;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.renaissance.app.model.Role;
import com.renaissance.app.model.UserStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDTO {
	private Long userId;
	private String username;
	private String email;
	private String fullName;
	private Role role;
	private UserStatus status;
	private List<Long> departmentIds;
	private List<String> departmentNames;

	private boolean emailVerified; // NEW

	private Long pendingTasks;
	private Long upcomingTasks;
	private Long delayedTasks;
	private Long closedTasks;
}
