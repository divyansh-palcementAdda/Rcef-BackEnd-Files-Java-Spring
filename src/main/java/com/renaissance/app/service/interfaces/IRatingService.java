package com.renaissance.app.service.interfaces;

import java.util.List;

import com.renaissance.app.exception.BadRequestException;
import com.renaissance.app.exception.ResourcesNotFoundException;
import com.renaissance.app.payload.RatingDTO;
import com.renaissance.app.payload.RatingPayload;

public interface IRatingService {

	RatingDTO giveRating(RatingPayload payload, Long givenById) throws ResourcesNotFoundException, BadRequestException;

	List<RatingDTO> getRatingsForUser(Long userId);

	List<RatingDTO> getRatingsForDepartment(Long departmentId);

	List<RatingDTO> getRatingsForTask(Long taskId);
}
