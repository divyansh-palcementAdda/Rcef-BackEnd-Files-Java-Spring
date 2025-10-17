package com.renaissance.app.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.renaissance.app.exception.BadRequestException;
import com.renaissance.app.exception.ResourcesNotFoundException;
import com.renaissance.app.mapper.RatingMapper;
import com.renaissance.app.model.Department;
import com.renaissance.app.model.Rating;
import com.renaissance.app.model.Task;
import com.renaissance.app.model.User;
import com.renaissance.app.payload.RatingDTO;
import com.renaissance.app.payload.RatingPayload;
import com.renaissance.app.repository.DepartmentRepository;
import com.renaissance.app.repository.IRatingRepository;
import com.renaissance.app.repository.IUserRepository;
import com.renaissance.app.repository.TaskRepository;
import com.renaissance.app.service.interfaces.IRatingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class RatingServiceImpl implements IRatingService {

    private final IRatingRepository ratingRepository;
    private final IUserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final TaskRepository taskRepository;
    private final RatingMapper ratingMapper;

    @Override
    @PreAuthorize("hasAnyRole('TEACHER','HOD')")
    public RatingDTO giveRating(RatingPayload payload, Long givenById) throws ResourcesNotFoundException, BadRequestException {
        if (payload == null) throw new BadRequestException("Rating payload is required");
        if (givenById == null) throw new BadRequestException("GivenById is required");

        User giver = userRepository.findById(givenById)
                .orElseThrow(() -> new ResourcesNotFoundException("User (giver) not found"));

        Rating rating = new Rating();
        rating.setScore(payload.getScore());
        rating.setFeedback(payload.getFeedback());
        rating.setType(payload.getType());
        rating.setGivenBy(giver);
        rating.setCreatedAt(LocalDateTime.now());

        switch (payload.getType()) {
            case TEACHER, HOD -> {
                if (payload.getRatedUserId() == null) throw new BadRequestException("ratedUserId required for user rating");
                User ratedUser = userRepository.findById(payload.getRatedUserId())
                        .orElseThrow(() -> new ResourcesNotFoundException("Rated user not found"));
                if (ratedUser.getUserId().equals(giver.getUserId())) {
                    throw new BadRequestException("Self rating is not allowed");
                }
                rating.setRatedUser(ratedUser);
            }
            case DEPARTMENT -> {
                if (payload.getRatedDepartmentId() == null)
                    throw new BadRequestException("ratedDepartmentId required for department rating");
                Department dept = departmentRepository.findById(payload.getRatedDepartmentId())
                        .orElseThrow(() -> new ResourcesNotFoundException("Department not found"));
                rating.setRatedDepartment(dept);
            }
            case TASK -> {
                if (payload.getTaskId() == null) throw new BadRequestException("taskId required for task rating");
                Task task = taskRepository.findById(payload.getTaskId())
                        .orElseThrow(() -> new ResourcesNotFoundException("Task not found"));
                rating.setTask(task);
            }
            default -> throw new BadRequestException("Unsupported rating type: " + payload.getType());
        }

        Rating saved = ratingRepository.save(rating);
        log.info("Rating saved (id={}) by user {}", saved.getRatingId(), givenById);
        return ratingMapper.toDto(saved);
    }

    @Override
    public List<RatingDTO> getRatingsForUser(Long userId) {
        return ratingRepository.findByRatedUser_UserId(userId).stream()
                .map(ratingMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<RatingDTO> getRatingsForDepartment(Long deptId) {
        return ratingRepository.findByRatedDepartment_DepartmentId(deptId).stream()
                .map(ratingMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<RatingDTO> getRatingsForTask(Long taskId) {
        return ratingRepository.findByTask_TaskId(taskId).stream()
                .map(ratingMapper::toDto)
                .collect(Collectors.toList());
    }
}
