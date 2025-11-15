package com.renaissance.app.service.impl;


import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.renaissance.app.exception.BadRequestException;
import com.renaissance.app.payload.OrgDriveResult;
import com.renaissance.app.payload.UploadResult;
import com.renaissance.app.service.interfaces.IOrgDriveService;

@Service
public class UploadService {

    private final Path baseTempRoot;
    private final IOrgDriveService orgDriveService;

    public UploadService(IOrgDriveService orgDriveService) throws IOException {
        this.orgDriveService = orgDriveService;
        this.baseTempRoot = Paths.get(System.getProperty("java.io.tmpdir"), "myapp-uploads");
        Files.createDirectories(baseTempRoot);
    }

    public UploadResult storeTemporaryAndPush(MultipartFile file, Long requestId) throws IOException, BadRequestException {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is empty");
        }

        String originalFilename = Paths.get(file.getOriginalFilename() == null ? "unknown" : file.getOriginalFilename())
                                       .getFileName().toString();
        String safeFilename = sanitizeFilename(originalFilename);

        Path tmpDir = Files.createTempDirectory(baseTempRoot, "upload-");
        Path tmpFile = tmpDir.resolve(safeFilename);

        try (InputStream in = file.getInputStream()) {
            Files.copy(in, tmpFile, StandardCopyOption.REPLACE_EXISTING);
        }

        OrgDriveResult pushResult = orgDriveService.pushFile(tmpFile, requestId);

        if (pushResult.isSuccess()) {
            Files.deleteIfExists(tmpFile);
            Files.deleteIfExists(tmpDir);
        }

        return new UploadResult(pushResult.isSuccess(), pushResult.getRemoteId(), pushResult.getRemoteUrl(),
                pushResult.getMessage());
    }

    private String sanitizeFilename(String name) {
        return name.replaceAll("[\\\\/\\s]+", "_").replaceAll("[^A-Za-z0-9_.-]", "");
    }
}