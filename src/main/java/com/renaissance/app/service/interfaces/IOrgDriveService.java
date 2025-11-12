package com.renaissance.app.service.interfaces;


import java.nio.file.Path;
import java.security.Principal;

import com.renaissance.app.payload.OrgDriveResult;

public interface IOrgDriveService {
    /**
     * Pushes a file to the organization's Drive.
     * 
     * @param tmpFile   Path of the local temporary file
     * @param requestId Optional request identifier (for metadata)
     * @param principal Authenticated user (can be null for system)
     * @return OrgDriveResult containing success flag, drive file ID, and web link
     */
    OrgDriveResult pushFile(Path tmpFile, Long requestId);
}

