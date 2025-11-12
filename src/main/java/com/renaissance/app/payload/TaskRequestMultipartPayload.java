package com.renaissance.app.payload;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.renaissance.app.model.RequestType;

import jakarta.validation.constraints.NotNull;

//Payload sent from Angular (multipart/form-data)
public record TaskRequestMultipartPayload(
     @NotNull RequestType requestType,
     String remarks,                     // required for EXTENSION
     List<MultipartFile> proofs          // required for CLOSURE, optional for EXTENSION
) {}
