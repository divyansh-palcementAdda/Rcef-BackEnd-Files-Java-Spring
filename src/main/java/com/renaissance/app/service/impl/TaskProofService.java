package com.renaissance.app.service.impl;


import java.io.IOException;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.renaissance.app.exception.BadRequestException;
import com.renaissance.app.model.Task;
import com.renaissance.app.model.TaskProof;
import com.renaissance.app.model.TaskRequest;
import com.renaissance.app.model.User;
import com.renaissance.app.payload.UploadResult;
import com.renaissance.app.repository.TaskProofRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class TaskProofService {

    private final TaskProofRepository repo;
    private final UploadService uploadService;

    public TaskProof uploadProof(MultipartFile file,
                                 Long taskId,
                                 Long requestId,
                                 User uploadedBy) throws IOException, BadRequestException {

        UploadResult result = uploadService.storeTemporaryAndPush(file, requestId);
        log.info("Uploaded proof: {}", result.getFileUrl());

        TaskProof proof = TaskProof.builder()
                .task(Task.builder().taskId(taskId).build())
                .taskRequest(TaskRequest.builder().requestId(requestId).build())
                .fileUrl(result.getFileUrl())
                .fileType(resolveFileType(file.getContentType()))
                .uploadedBy(uploadedBy)
                .uploadedAt(LocalDateTime.now())
                .build();

        return repo.save(proof);
    }

    private String resolveFileType(String mime) {
        if (mime == null) return "FILE";
        return switch (mime.toLowerCase()) {
            case "image/jpeg", "image/jpg" -> "JPG";
            case "image/png" -> "PNG";
            case "application/pdf" -> "PDF";
            default -> "FILE";
        };
    }
}