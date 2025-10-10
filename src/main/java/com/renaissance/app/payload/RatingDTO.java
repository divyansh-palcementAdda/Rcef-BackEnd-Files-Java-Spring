package com.renaissance.app.payload;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.renaissance.app.model.RatingType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RatingDTO {
	private Long ratingId;
	private RatingType type; // TEACHER, HOD, DEPARTMENT
	private int score;
	private String feedback;

	private Long ratedUserId;
	private String ratedUserName;

	private Long ratedDepartmentId;
	private String ratedDepartmentName;

	private Long taskId;
	private String taskTitle;

	private Long givenById;
	private String givenByName;

	private LocalDateTime createdAt;
}
