package com.renaissance.app.service.impl;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collections;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.FileContent;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.renaissance.app.payload.OrgDriveResult;
import com.renaissance.app.service.interfaces.IOrgDriveService;

@Service
public class OrgDriveServiceImpl implements IOrgDriveService {

    private final Drive driveService;

    @Value("${google.drive.folder-id}")
    private String driveFolderId;

    public OrgDriveServiceImpl(@Value("${gdrive.credentials.path}") String credentialsPath) throws Exception {
    	InputStream inputStream;

    	if (credentialsPath.startsWith("classpath:")) {
    	    String fileName = credentialsPath.replace("classpath:", "");
    	    inputStream = new ClassPathResource(fileName).getInputStream();
    	} else {
    	    inputStream = new FileInputStream(credentialsPath);
    	}

    	GoogleCredentials credentials = ServiceAccountCredentials
    	        .fromStream(inputStream)
    	        .createScoped(Collections.singleton(DriveScopes.DRIVE));

        this.driveService = new Drive.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credentials))
                .setApplicationName("Renaissance App Uploads")
                .build();
    }

    @Override
    public OrgDriveResult pushFile(Path tmpFile, Long requestId) {
        try {
            File fileMetadata = new File();
            fileMetadata.setName(tmpFile.getFileName().toString());

            // 👇 Upload inside Shared Drive folder
            if (driveFolderId != null && !driveFolderId.isEmpty()) {
                fileMetadata.setParents(Collections.singletonList(driveFolderId));
            }

            // ✅ Must tell Google API that we are using Shared Drives
            String mimeType = java.nio.file.Files.probeContentType(tmpFile);
            if (mimeType == null) mimeType = "application/octet-stream";

            FileContent mediaContent = new FileContent(mimeType, tmpFile.toFile());

            File uploadedFile = driveService.files()
                    .create(fileMetadata, mediaContent)
                    .setSupportsAllDrives(true)              // ✅ fix #1
                    .setFields("id, name, webViewLink, parents")  // optional: get folder info too
                    .execute();

            return new OrgDriveResult(
                    true,
                    uploadedFile.getId(),
                    uploadedFile.getWebViewLink(),
                    "File uploaded successfully to Shared Drive"
            );

        } catch (IOException e) {
            e.printStackTrace();
            return new OrgDriveResult(false, null, null, "Upload failed: " + e.getMessage());
        }
    }
}