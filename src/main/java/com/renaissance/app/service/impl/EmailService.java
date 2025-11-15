package com.renaissance.app.service.impl;

import java.io.UnsupportedEncodingException;
import java.util.Objects;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.renaissance.app.service.interfaces.IEmailService;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService implements IEmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final Executor emailExecutor;
    // configurable sender values — consider moving to properties
    private final String fromAddress = "chancelloroffice@renaissance.ac.in";
    private final String fromName = "Renaissance University - Chancellor Office";

    public EmailService(JavaMailSender mailSender,  @Qualifier("taskExecutor") Executor emailExecutor) {
        this.mailSender = Objects.requireNonNull(mailSender, "mailSender required");
        this.emailExecutor = Objects.requireNonNull(emailExecutor, "emailExecutor required");
    }

    @Override
    public void sendOtpEmail(String toEmail, String otp) throws MessagingException {
        validateEmailParams(toEmail, otp);
        String subject = "🔐 Your Secure OTP Code from RCEF";
        String body = buildOtpHtml(otp);

        // Send asynchronously using provided executor — caller can choose to block if desired
        emailExecutor.execute(() -> {
            try {
                sendHtmlEmailWithInlineLogo(toEmail, subject, body);
                log.info("OTP email sent to {}", toEmail);
            } catch (Exception e) {
                // Log error - do not throw from async thread
                log.error("Failed to send OTP email to {} : {}", toEmail, e.getMessage(), e);
            }
        });
    }

    @Override
    public void sendTaskAssignedReminderEmail(String toEmail, String taskName, String dueDate, String timeLimit)
            throws MessagingException {
        validateEmailParams(toEmail, taskName);
        String subject = "📅 New Task Assigned - Action Required";
        String body = buildTaskAssignedHtml(taskName, dueDate, timeLimit);

        emailExecutor.execute(() -> {
            try {
                sendHtmlEmailWithInlineLogo(toEmail, subject, body);
                log.info("Task-assigned email sent to {}", toEmail);
            } catch (Exception e) {
                log.error("Failed to send task-assigned email to {} : {}", toEmail, e.getMessage(), e);
            }
        });
    }

    @Override
    public void sendTaskDeadlineNearReminderEmail(String toEmail, String taskName, String dueDate, String timeRemaining)
            throws MessagingException {
        validateEmailParams(toEmail, taskName);
        String subject = "⏰ Task Deadline Approaching - " + taskName;
        String body = buildDeadlineHtml(taskName, dueDate, timeRemaining);

        emailExecutor.execute(() -> {
            try {
                sendHtmlEmailWithInlineLogo(toEmail, subject, body);
                log.info("Deadline reminder email sent to {}", toEmail);
            } catch (Exception e) {
                log.error("Failed to send deadline reminder to {} : {}", toEmail, e.getMessage(), e);
            }
        });
    }

    // ----------------------- internal helpers -----------------------

    private void validateEmailParams(String toEmail, String required) {
        if (toEmail == null || toEmail.isBlank()) {
            throw new IllegalArgumentException("Recipient email is required");
        }
        if (required == null || required.isBlank()) {
            throw new IllegalArgumentException("Email body parameter is required");
        }
    }

    private void sendHtmlEmailWithInlineLogo(String toEmail, String subject, String htmlContent) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        try {
            helper.setFrom(fromAddress, fromName);
        } catch (UnsupportedEncodingException e) {
            helper.setFrom(fromAddress);
        }

        helper.setTo(toEmail);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);

        // try to inline logo; if missing don't fail the entire send
        try {
            ClassPathResource logo = new ClassPathResource("static/images/renaissance-logo.png");
            if (logo.exists()) {
                helper.addInline("logoImage", logo);
            } else {
                log.debug("Logo resource not found in classpath: static/images/renaissance-logo.png");
            }
        } catch (Exception ex) {
            log.warn("Failed to attach inline logo: {}", ex.getMessage());
        }

        mailSender.send(message);
    }

    // Builders for HTML content (kept your original templates, simplified here)
    private String buildOtpHtml(String otp) {
        // keep your original full HTML but for readability keep it short here.
        // In production you can load an external template (Thymeleaf, FreeMarker) and substitute values.
        return "<html><body><div style='font-family:Segoe UI'><h2>RCEF OTP</h2>"
                + "<div style='font-size:40px;font-weight:700;padding:20px;background:#2e7d32;color:#fff;border-radius:10px;display:inline-block'>"
                + otp + "</div><p>Valid for 5 minutes.</p></div></body></html>";
    }

    private String buildTaskAssignedHtml(String taskName, String dueDate, String timeLimit) {
        return "<html><body><h2>New Task Assigned</h2><p><b>Task:</b> " + escape(taskName) + "</p>"
                + "<p><b>Due:</b> " + escape(dueDate) + "</p><p><b>Time Limit:</b> " + escape(timeLimit) + "</p></body></html>";
    }

    private String buildDeadlineHtml(String taskName, String dueDate, String timeRemaining) {
        return "<html><body><h2>Deadline Approaching</h2><p><b>Task:</b> " + escape(taskName) + "</p>"
                + "<p><b>Due:</b> " + escape(dueDate) + "</p><p><b>Remaining:</b> " + escape(timeRemaining) + "</p></body></html>";
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

	@Override
	public void sendTaskApprovalEmail(String toEmail, String taskName, boolean approved, String reason)
			throws MessagingException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void sendTaskUpdateEmail(String toEmail, String taskName, String updateDetails) throws MessagingException {
		// TODO Auto-generated method stub
		
	}
}