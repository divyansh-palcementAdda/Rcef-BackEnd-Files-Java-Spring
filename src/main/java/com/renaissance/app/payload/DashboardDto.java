package com.renaissance.app.payload;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DashboardDto {
	private String email;
	private String userName;
    private Long totalTask;
    private Long pendingTask;
    private Long delayedTask;
    private Long completedTask;
    private Long upcomingTask;
    private Long requestForClosure;
    private Long requestForExtension;

    private Long activeUsers;       // visible only to Admin/HOD
    private Long totalUsers;        // visible only to Admin/HOD
    private Long selfTask;
    private Long activeDepartments; // visible only to Admin
    private Long totalDepartments;  // visible only to Admin

    private String loggedInRole;
    private String departmentName;
}
