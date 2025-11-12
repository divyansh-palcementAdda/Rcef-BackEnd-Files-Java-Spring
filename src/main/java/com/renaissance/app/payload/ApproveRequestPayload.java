package com.renaissance.app.payload;

import java.time.LocalDateTime;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ApproveRequestPayload {
    @NotNull
    private Long requestId;

    @FutureOrPresent
    private LocalDateTime newDueDate; // or String if you use ISO

    // getters & setters
}