package com.renaissance.app.payload;

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
public class RatingPayload {
	private RatingType type;
	private int score;
	private String feedback;
	private Long taskId;
	private Long ratedUserId;
	private Long ratedDepartmentId;
}
