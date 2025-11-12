package com.renaissance.app.payload;

public class UploadResult {
    private boolean success;
    private String fileId;       // Could be org drive file ID, or temp ID
    private String fileUrl;      // Optional: link to view/download
    private String message;      // Optional: descriptive message

    public UploadResult() {}

    public UploadResult(boolean success, String fileId, String fileUrl) {
        this.success = success;
        this.fileId = fileId;
        this.fileUrl = fileUrl;
    }

    public UploadResult(boolean success, String fileId, String fileUrl, String message) {
        this.success = success;
        this.fileId = fileId;
        this.fileUrl = fileUrl;
        this.message = message;
    }

    // Getters and setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getFileId() { return fileId; }
    public void setFileId(String fileId) { this.fileId = fileId; }

    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
