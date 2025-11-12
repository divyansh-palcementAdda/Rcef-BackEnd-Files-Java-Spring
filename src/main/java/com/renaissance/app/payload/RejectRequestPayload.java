package com.renaissance.app.payload;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RejectRequestPayload {
    @NotBlank(message = "Rejection reason is required")
    private String reason;
    
    @NotNull
    private Long requestId;
}