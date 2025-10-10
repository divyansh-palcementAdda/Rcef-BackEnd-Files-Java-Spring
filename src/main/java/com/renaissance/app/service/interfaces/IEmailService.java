package com.renaissance.app.service.interfaces;

import jakarta.mail.MessagingException;

public interface IEmailService {
	public void sendOtpEmail(String toEmail, String otp) throws MessagingException;
	public void sendTaskAssignedReminderEmail(String toEmail, String taskName, String dueDate, String timeLimit) throws MessagingException;
    public void sendTaskDeadlineNearReminderEmail(String toEmail, String taskName, String dueDate, String timeRemaining) throws MessagingException ;
}
