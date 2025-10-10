package com.renaissance.app.model;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ratings")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Rating {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long ratingId;

    @NotNull(message = "Type cannot be null")
    @Enumerated(EnumType.STRING)
    private RatingType type; // TEACHER, HOD, DEPARTMENT

    @Min(value = 1, message = "Score must be at least 1")
    @Max(value = 5, message = "Score cannot exceed 5")
    private int score; // e.g., 1–5 stars

    @Size(max = 1000, message = "Feedback cannot exceed 1000 characters")
    private String feedback;

    @ManyToOne
    @JoinColumn(name = "rated_user_id")
    private User ratedUser; // Teacher/HOD rated

    @ManyToOne
    @JoinColumn(name = "rated_department_id")
    private Department ratedDepartment;

    @ManyToOne
    @JoinColumn(name = "task_id")
    private Task task;

    @NotNull(message = "Given by user cannot be null")
    @ManyToOne
    @JoinColumn(name = "given_by")
    private User givenBy; // Admin who gave rating

    @NotNull(message = "Created at date cannot be null")
    @PastOrPresent(message = "Created at date must be in the past or present")
    private LocalDateTime createdAt;
}