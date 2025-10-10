package com.renaissance.app.service.interfaces;

import java.util.List;

import com.renaissance.app.payload.RatingDTO;
import com.renaissance.app.payload.RatingPayload;

public interface IRatingService {

	RatingDTO giveRating(RatingPayload payload, Long givenById);

	List<RatingDTO> getRatingsForUser(Long userId);

	List<RatingDTO> getRatingsForDepartment(Long departmentId);

	List<RatingDTO> getRatingsForTask(Long taskId);
}
