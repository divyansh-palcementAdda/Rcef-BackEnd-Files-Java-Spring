package com.renaissance.app.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

@Service
@Transactional
public class RatingServiceImpl implements IRatingService {

    private final IRatingRepository ratingRepository;
    private final IUserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final TaskRepository taskRepository;
    private final ModelMapper modelMapper;

    public RatingServiceImpl(IRatingRepository ratingRepository,
                             IUserRepository userRepository,
                             DepartmentRepository departmentRepository,
                             TaskRepository taskRepository,
                             ModelMapper modelMapper) {
        this.ratingRepository = ratingRepository;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.taskRepository = taskRepository;
        this.modelMapper = modelMapper;
    }

    @Override
    @PreAuthorize("hasAnyRole('TEACHER','HOD')")
    public RatingDTO giveRating(RatingPayload payload, Long givenById) {
        User giver = userRepository.findById(givenById)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Rating rating = new Rating();
        rating.setScore(payload.getScore());
        rating.setFeedback(payload.getFeedback());
        rating.setType(payload.getType());
        rating.setGivenBy(giver);
        rating.setCreatedAt(LocalDateTime.now());

        switch (payload.getType()) {
            case TEACHER, HOD -> {
                User ratedUser = userRepository.findById(payload.getRatedUserId())
                        .orElseThrow(() -> new RuntimeException("Rated user not found"));
                if (ratedUser.getUserId().equals(giver.getUserId())) throw new RuntimeException("Self rating not allowed");
                rating.setRatedUser(ratedUser);
            }
            case DEPARTMENT -> {
                Department dept = departmentRepository.findById(payload.getRatedDepartmentId())
                        .orElseThrow(() -> new RuntimeException("Department not found"));
                rating.setRatedDepartment(dept);
            }
            case TASK -> {
                Task task = taskRepository.findById(payload.getTaskId())
                        .orElseThrow(() -> new RuntimeException("Task not found"));
                rating.setTask(task);
            }
        }

        return modelMapper.map(ratingRepository.save(rating), RatingDTO.class);
    }

    @Override
    public List<RatingDTO> getRatingsForUser(Long userId) {
        return ratingRepository.findByRatedUser_UserId(userId).stream()
                .map(r -> modelMapper.map(r, RatingDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    public List<RatingDTO> getRatingsForDepartment(Long deptId) {
        return ratingRepository.findByRatedDepartment_DepartmentId(deptId).stream()
                .map(r -> modelMapper.map(r, RatingDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    public List<RatingDTO> getRatingsForTask(Long taskId) {
        return ratingRepository.findByTask_TaskId(taskId).stream()
                .map(r -> modelMapper.map(r, RatingDTO.class))
                .collect(Collectors.toList());
    }
}
