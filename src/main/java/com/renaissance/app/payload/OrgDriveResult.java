package com.renaissance.app.payload;


public class OrgDriveResult {
    private boolean success;
    private String remoteId;     // e.g. Google Drive File ID, or OneDrive item ID
    private String remoteUrl;    // e.g. webViewLink or SharePoint link
    private String message;      // error or info

    public OrgDriveResult() {}

    public OrgDriveResult(boolean success, String remoteId, String remoteUrl) {
        this.success = success;
        this.remoteId = remoteId;
        this.remoteUrl = remoteUrl;
    }

    public OrgDriveResult(boolean success, String remoteId, String remoteUrl, String message) {
        this.success = success;
        this.remoteId = remoteId;
        this.remoteUrl = remoteUrl;
        this.message = message;
    }

    // Getters and setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getRemoteId() { return remoteId; }
    public void setRemoteId(String remoteId) { this.remoteId = remoteId; }

    public String getRemoteUrl() { return remoteUrl; }
    public void setRemoteUrl(String remoteUrl) { this.remoteUrl = remoteUrl; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
