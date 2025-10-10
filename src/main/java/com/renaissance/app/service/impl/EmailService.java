package com.renaissance.app.service.impl;

import java.io.UnsupportedEncodingException;

import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.renaissance.app.service.interfaces.IEmailService;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService implements IEmailService {
    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void sendOtpEmail(String toEmail, String otp) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        try {
            helper.setFrom("chancelloroffice@renaissance.ac.in", "Renaissance University - Chancellor Office");
        } catch (UnsupportedEncodingException e) {
            helper.setFrom("chancelloroffice@renaissance.ac.in");
        }

        helper.setTo(toEmail);
        helper.setSubject("🔐 Your Secure OTP Code from RCEF");

        String htmlContent = "<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\" />\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\" />\n" +
                "    <title>RCEF OTP Verification</title>\n" +
                "    <style>\n" +
                "        /* Reset and base styles */\n" +
                "        body, html {\n" +
                "            margin: 0;\n" +
                "            padding: 0;\n" +
                "            background: #f4f7f6;\n" +
                "            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;\n" +
                "            color: #2f2f2f;\n" +
                "            -webkit-font-smoothing: antialiased;\n" +
                "            -moz-osx-font-smoothing: grayscale;\n" +
                "        }\n" +
                "        .container {\n" +
                "            max-width: 620px;\n" +
                "            margin: 50px auto;\n" +
                "            background: #ffffff;\n" +
                "            border-radius: 18px;\n" +
                "            box-shadow: 0 14px 40px rgba(0, 0, 0, 0.1);\n" +
                "            overflow: hidden;\n" +
                "            border: 1px solid #d9e2e7;\n" +
                "        }\n" +
                "        .header {\n" +
                "            background: linear-gradient(90deg, #1b5e20, #43a047);\n" +
                "            padding: 40px 25px 35px;\n" +
                "            text-align: center;\n" +
                "            color: #fff;\n" +
                "            position: relative;\n" +
                "        }\n" +
                "        .header img {\n" +
                "            height: 300px;\n" +
                "            margin-bottom: 1px;\n" +
                "            filter: drop-shadow(0 0 4px rgba(0,0,0,0.25));\n" +
                "            pointer-events: none;\n" +
                "            user-select: none;\n" +
                "            display: inline-block;\n" +
                "        }\n" +
                "        .header h1 {\n" +
                "            font-size: 32px;\n" +
                "            font-weight: 800;\n" +
                "            letter-spacing: 3px;\n" +
                "            margin: 0;\n" +
                "            text-transform: uppercase;\n" +
                "            text-shadow: 0 2px 6px rgba(0,0,0,0.3);\n" +
                "        }\n" +
                "        .header p {\n" +
                "            font-style: italic;\n" +
                "            font-size: 18px;\n" +
                "            margin-top: 8px;\n" +
                "            color: #c8e6c9;\n" +
                "        }\n" +
                "        .content {\n" +
                "            padding: 45px 40px 50px;\n" +
                "            text-align: center;\n" +
                "        }\n" +
                "        .content h2 {\n" +
                "            font-size: 26px;\n" +
                "            color: #2e7d32;\n" +
                "            margin-bottom: 25px;\n" +
                "            font-weight: 700;\n" +
                "        }\n" +
                "        .content p {\n" +
                "            font-size: 19px;\n" +
                "            line-height: 1.7;\n" +
                "            margin-bottom: 35px;\n" +
                "            color: #444;\n" +
                "        }\n" +
                "        .otp-button {\n" +
                "            display: inline-block;\n" +
                "            background: linear-gradient(135deg, #66bb6a, #2e7d32);\n" +
                "            color: #fff;\n" +
                "            font-size: 40px;\n" +
                "            font-weight: 900;\n" +
                "            padding: 22px 65px;\n" +
                "            border-radius: 16px;\n" +
                "            letter-spacing: 10px;\n" +
                "            box-shadow: 0 10px 30px rgba(46, 125, 50, 0.5);\n" +
                "            user-select: all;\n" +
                "            cursor: default;\n" +
                "            transition: background 0.3s ease;\n" +
                "            margin-bottom: 40px;\n" +
                "            text-shadow: 0 3px 8px rgba(0,0,0,0.25);\n" +
                "        }\n" +
                "        .otp-button:hover {\n" +
                "            background: linear-gradient(135deg, #81c784, #388e3c);\n" +
                "        }\n" +
                "        .expiry-note {\n" +
                "            font-size: 17px;\n" +
                "            color: #666;\n" +
                "            margin-bottom: 50px;\n" +
                "        }\n" +
                "        .warning {\n" +
                "            background: #fff4e5;\n" +
                "            border-left: 7px solid #ffb300;\n" +
                "            padding: 22px 30px;\n" +
                "            font-size: 16px;\n" +
                "            color: #6d4c41;\n" +
                "            border-radius: 12px;\n" +
                "            max-width: 540px;\n" +
                "            margin: 0 auto 50px auto;\n" +
                "            box-shadow: 0 6px 18px rgba(255, 152, 0, 0.2);\n" +
                "            line-height: 1.5;\n" +
                "        }\n" +
                "        .warning a {\n" +
                "            color: wheat;\n" +
                "            font-weight: 700;\n" +
                "            text-decoration: none;\n" +
                "            transition: color 0.3s ease;\n" +
                "        }\n" +
                "        .warning a:hover {\n" +
                "            color: #bf360c;\n" +
                "            text-decoration: underline;\n" +
                "        }\n" +
                "        .support-button {\n" +
                "            display: inline-block;\n" +
                "            background-color: #2e7d32;\n" +
                "            color: #fff;\n" +
                "            padding: 14px 36px;\n" +
                "            border-radius: 35px;\n" +
                "            font-weight: 700;\n" +
                "            font-size: 17px;\n" +
                "            text-decoration: none;\n" +
                "            box-shadow: 0 8px 20px rgba(46, 125, 50, 0.5);\n" +
                "            transition: background-color 0.3s ease;\n" +
                "            margin-top: 15px;\n" +
                "            cursor: pointer;\n" +
                "        }\n" +
                "        .support-button:hover {\n" +
                "            background-color: #1b5e20;\n" +
                "        }\n" +
                "        .footer {\n" +
                "            background: #1b5e20;\n" +
                "            color: #c8e6c9;\n" +
                "            text-align: center;\n" +
                "            padding: 22px 20px;\n" +
                "            font-size: 14px;\n" +
                "            border-top: 1px solid rgba(255, 255, 255, 0.15);\n" +
                "            letter-spacing: 0.6px;\n" +
                "        }\n" +
                "        @media (max-width: 640px) {\n" +
                "            .container {\n" +
                "                margin: 25px 15px;\n" +
                "            }\n" +
                "            .content {\n" +
                "                padding: 35px 25px 45px;\n" +
                "            }\n" +
                "            .otp-button {\n" +
                "                font-size: 32px;\n" +
                "                padding: 18px 50px;\n" +
                "                letter-spacing: 8px;\n" +
                "            }\n" +
                "            .header img {\n" +
                "                height: 140px;\n" +
                "            }\n" +
                "            .header h1 {\n" +
                "                font-size: 26px;\n" +
                "            }\n" +
                "            .header p {\n" +
                "                font-size: 15px;\n" +
                "            }\n" +
                "        }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <div class=\"container\" role=\"main\">\n" +
                "        <div class=\"header\">\n" +
                "            <img src=\"cid:logoImage\" alt=\"Renaissance University Logo\" aria-label=\"Renaissance University Logo\" />\n" +
                "            <h1>RCEF</h1>\n" +
                "            <p>Secure Access Verification</p>\n" +
                "        </div>\n" +
                "        <div class=\"content\">\n" +
                "            <h2>Dear User,</h2>\n" +
                "            <p>To ensure the security of your account, please use the One-Time Password (OTP) below to verify your identity.</p>\n" +
                "            <div class=\"otp-button\" aria-live=\"polite\" aria-label=\"Your One Time Password\">" + otp + "</div>\n" +
                "            <p class=\"expiry-note\">This OTP is valid for <strong>5 minutes</strong>. Please enter it promptly to avoid expiration.</p>\n" +
                "            <div class=\"warning\" role=\"alert\">\n" +
                "                <p>⚠️ <strong>Important:</strong> Never share this OTP with anyone. If you did not request this code, please contact our support team immediately.</p>\n" +
                "                <a href=\"mailto:support@rcef.com\" class=\"support-button\"  aria-label=\"Contact RCEF Support\">Contact Support</a>\n" +
                "            </div>\n" +
                "        </div>\n" +
                "        <div class=\"footer\">\n" +
                "            <p>&copy; 2025 Renaissance University - Chancellor Office | All rights reserved</p>\n" +
                "        </div>\n" +
                "    </div>\n" +
                "</body>\n" +
                "</html>";

        helper.setText(htmlContent, true);

        try {
            helper.addInline("logoImage", new ClassPathResource("static/images/renaissance-logo.png"));
        } catch (Exception ex) {
            // Optionally log or handle image loading failure
        }

        mailSender.send(message);
    }

    @Override
    public void sendTaskAssignedReminderEmail(String toEmail, String taskName, String dueDate, String timeLimit) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        try {
            helper.setFrom("chancelloroffice@renaissance.ac.in", "Renaissance University - Chancellor Office");
        } catch (UnsupportedEncodingException e) {
            helper.setFrom("chancelloroffice@renaissance.ac.in");
        }

        helper.setTo(toEmail);
        helper.setSubject("📅 New Task Assigned - Action Required");

        String htmlContent = "<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\" />\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\" />\n" +
                "    <title>RCEF Task Assignment</title>\n" +
                "    <style>\n" +
                "        body, html {\n" +
                "            margin: 0;\n" +
                "            padding: 0;\n" +
                "            background: #f4f7f6;\n" +
                "            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;\n" +
                "            color: #2f2f2f;\n" +
                "            -webkit-font-smoothing: antialiased;\n" +
                "            -moz-osx-font-smoothing: grayscale;\n" +
                "        }\n" +
                "        .container {\n" +
                "            max-width: 620px;\n" +
                "            margin: 50px auto;\n" +
                "            background: #ffffff;\n" +
                "            border-radius: 18px;\n" +
                "            box-shadow: 0 14px 40px rgba(0, 0, 0, 0.1);\n" +
                "            overflow: hidden;\n" +
                "            border: 1px solid #d9e2e7;\n" +
                "        }\n" +
                "        .header {\n" +
                "            background: linear-gradient(90deg, #1b5e20, #43a047);\n" +
                "            padding: 40px 25px 35px;\n" +
                "            text-align: center;\n" +
                "            color: #fff;\n" +
                "        }\n" +
                "        .header img {\n" +
                "            height: 300px;\n" +
                "            margin-bottom: 1px;\n" +
                "            filter: drop-shadow(0 0 4px rgba(0,0,0,0.25));\n" +
                "            pointer-events: none;\n" +
                "            user-select: none;\n" +
                "            display: inline-block;\n" +
                "        }\n" +
                "        .header h1 {\n" +
                "            font-size: 32px;\n" +
                "            font-weight: 800;\n" +
                "            letter-spacing: 3px;\n" +
                "            margin: 0;\n" +
                "            text-transform: uppercase;\n" +
                "            text-shadow: 0 2px 6px rgba(0,0,0,0.3);\n" +
                "        }\n" +
                "        .header p {\n" +
                "            font-style: italic;\n" +
                "            font-size: 18px;\n" +
                "            margin-top: 8px;\n" +
                "            color: #c8e6c9;\n" +
                "        }\n" +
                "        .content {\n" +
                "            padding: 45px 40px 50px;\n" +
                "            text-align: center;\n" +
                "        }\n" +
                "        .content h2 {\n" +
                "            font-size: 26px;\n" +
                "            color: #2e7d32;\n" +
                "            margin-bottom: 25px;\n" +
                "            font-weight: 700;\n" +
                "        }\n" +
                "        .content p {\n" +
                "            font-size: 19px;\n" +
                "            line-height: 1.7;\n" +
                "            margin-bottom: 35px;\n" +
                "            color: #444;\n" +
                "        }\n" +
                "        .task-details {\n" +
                "            background: #f9fafb;\n" +
                "            padding: 20px 25px;\n" +
                "            border-radius: 12px;\n" +
                "            border-left: 4px solid #2e7d32;\n" +
                "            text-align: left;\n" +
                "            max-width: 540px;\n" +
                "            margin: 0 auto 35px;\n" +
                "            box-shadow: 0 6px 18px rgba(0, 0, 0, 0.05);\n" +
                "        }\n" +
                "        .task-details p {\n" +
                "            margin: 8px 0;\n" +
                "            font-size: 18px;\n" +
                "            color: #555;\n" +
                "        }\n" +
                "        .task-details strong {\n" +
                "            color: #2e7d32;\n" +
                "        }\n" +
                "        .warning {\n" +
                "            background: #fff4e5;\n" +
                "            border-left: 7px solid #ffb300;\n" +
                "            padding: 22px 30px;\n" +
                "            font-size: 16px;\n" +
                "            color: #6d4c41;\n" +
                "            border-radius: 12px;\n" +
                "            max-width: 540px;\n" +
                "            margin: 0 auto 50px auto;\n" +
                "            box-shadow: 0 6px 18px rgba(255, 152, 0, 0.2);\n" +
                "            line-height: 1.5;\n" +
                "        }\n" +
                "        .warning a {\n" +
                "            color: wheat;\n" +
                "            font-weight: 700;\n" +
                "            text-decoration: none;\n" +
                "            transition: color 0.3s ease;\n" +
                "        }\n" +
                "        .warning a:hover {\n" +
                "            color: #bf360c;\n" +
                "            text-decoration: underline;\n" +
                "        }\n" +
                "        .support-button {\n" +
                "            display: inline-block;\n" +
                "            background-color: #2e7d32;\n" +
                "            color: #fff;\n" +
                "            padding: 14px 36px;\n" +
                "            border-radius: 35px;\n" +
                "            font-weight: 700;\n" +
                "            font-size: 17px;\n" +
                "            text-decoration: none;\n" +
                "            box-shadow: 0 8px 20px rgba(46, 125, 50, 0.5);\n" +
                "            transition: background-color 0.3s ease;\n" +
                "            margin-top: 15px;\n" +
                "            cursor: pointer;\n" +
                "        }\n" +
                "        .support-button:hover {\n" +
                "            background-color: #1b5e20;\n" +
                "        }\n" +
                "        .footer {\n" +
                "            background: #1b5e20;\n" +
                "            color: #c8e6c9;\n" +
                "            text-align: center;\n" +
                "            padding: 22px 20px;\n" +
                "            font-size: 14px;\n" +
                "            border-top: 1px solid rgba(255, 255, 255, 0.15);\n" +
                "            letter-spacing: 0.6px;\n" +
                "        }\n" +
                "        @media (max-width: 640px) {\n" +
                "            .container {\n" +
                "                margin: 25px 15px;\n" +
                "            }\n" +
                "            .content {\n" +
                "                padding: 35px 25px 45px;\n" +
                "            }\n" +
                "            .header img {\n" +
                "                height: 140px;\n" +
                "            }\n" +
                "            .header h1 {\n" +
                "                font-size: 26px;\n" +
                "            }\n" +
                "            .header p {\n" +
                "                font-size: 15px;\n" +
                "            }\n" +
                "            .task-details {\n" +
                "                padding: 15px 20px;\n" +
                "            }\n" +
                "            .task-details p {\n" +
                "                font-size: 16px;\n" +
                "            }\n" +
                "        }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <div class=\"container\" role=\"main\">\n" +
                "        <div class=\"header\">\n" +
                "            <img src=\"cid:logoImage\" alt=\"Renaissance University Logo\" aria-label=\"Renaissance University Logo\" />\n" +
                "            <h1>RCEF</h1>\n" +
                "            <p>Task Management</p>\n" +
                "        </div>\n" +
                "        <div class=\"content\">\n" +
                "            <h2>Dear User,</h2>\n" +
                "            <p>A new task has been assigned to you. Please review the details below and take action accordingly.</p>\n" +
                "            <div class=\"task-details\">\n" +
                "                <p><strong>Task Name:</strong> " + taskName + "</p>\n" +
                "                <p><strong>Due Date:</strong> " + dueDate + "</p>\n" +
                "                <p><strong>Time Limit:</strong> " + timeLimit + "</p>\n" +
                "            </div>\n" +
                "            <p>Please ensure you complete the task within the specified time frame. Your prompt attention is appreciated.</p>\n" +
                "            <div class=\"warning\" role=\"alert\">\n" +
                "                <p>⚠️ <strong>Important:</strong> Failure to complete the task on time may impact project progress. For assistance, contact our support team.</p>\n" +
                "                <a href=\"mailto:support@rcef.com\" class=\"support-button\" aria-label=\"Contact RCEF Support\">Contact Support</a>\n" +
                "            </div>\n" +
                "        </div>\n" +
                "        <div class=\"footer\">\n" +
                "            <p>&copy; 2025 Renaissance University - Chancellor Office | All rights reserved</p>\n" +
                "        </div>\n" +
                "    </div>\n" +
                "</body>\n" +
                "</html>";

        helper.setText(htmlContent, true);

        try {
            helper.addInline("logoImage", new ClassPathResource("static/images/renaissance-logo.png"));
        } catch (Exception ex) {
            // Optionally log or handle image loading failure
        }

        mailSender.send(message);
    }

    @Override
    public void sendTaskDeadlineNearReminderEmail(String toEmail, String taskName, String dueDate, String timeRemaining) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        try {
            helper.setFrom("chancelloroffice@renaissance.ac.in", "Renaissance University - Chancellor Office");
        } catch (UnsupportedEncodingException e) {
            helper.setFrom("chancelloroffice@renaissance.ac.in");
        }

        helper.setTo(toEmail);
        helper.setSubject("⏰ Task Deadline Approaching - " + taskName);

        String htmlContent = "<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\" />\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\" />\n" +
                "    <title>RCEF Task Deadline Reminder</title>\n" +
                "    <style>\n" +
                "        body, html {\n" +
                "            margin: 0;\n" +
                "            padding: 0;\n" +
                "            background: #f4f7f6;\n" +
                "            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;\n" +
                "            color: #2f2f2f;\n" +
                "            -webkit-font-smoothing: antialiased;\n" +
                "            -moz-osx-font-smoothing: grayscale;\n" +
                "        }\n" +
                "        .container {\n" +
                "            max-width: 620px;\n" +
                "            margin: 50px auto;\n" +
                "            background: #ffffff;\n" +
                "            border-radius: 18px;\n" +
                "            box-shadow: 0 14px 40px rgba(0, 0, 0, 0.1);\n" +
                "            overflow: hidden;\n" +
                "            border: 1px solid #d9e2e7;\n" +
                "        }\n" +
                "        .header {\n" +
                "            background: linear-gradient(90deg, #1b5e20, #43a047);\n" +
                "            padding: 40px 25px 35px;\n" +
                "            text-align: center;\n" +
                "            color: #fff;\n" +
                "        }\n" +
                "        .header img {\n" +
                "            height: 300px;\n" +
                "            margin-bottom: 1px;\n" +
                "            filter: drop-shadow(0 0 4px rgba(0,0,0,0.25));\n" +
                "            pointer-events: none;\n" +
                "            user-select: none;\n" +
                "            display: inline-block;\n" +
                "        }\n" +
                "        .header h1 {\n" +
                "            font-size: 32px;\n" +
                "            font-weight: 800;\n" +
                "            letter-spacing: 3px;\n" +
                "            margin: 0;\n" +
                "            text-transform: uppercase;\n" +
                "            text-shadow: 0 2px 6px rgba(0,0,0,0.3);\n" +
                "        }\n" +
                "        .header p {\n" +
                "            font-style: italic;\n" +
                "            font-size: 18px;\n" +
                "            margin-top: 8px;\n" +
                "            color: #c8e6c9;\n" +
                "        }\n" +
                "        .content {\n" +
                "            padding: 45px 40px 50px;\n" +
                "            text-align: center;\n" +
                "        }\n" +
                "        .content h2 {\n" +
                "            font-size: 26px;\n" +
                "            color: #d32f2f;\n" +
                "            margin-bottom: 25px;\n" +
                "            font-weight: 700;\n" +
                "        }\n" +
                "        .content p {\n" +
                "            font-size: 19px;\n" +
                "            line-height: 1.7;\n" +
                "            margin-bottom: 35px;\n" +
                "            color: #444;\n" +
                "        }\n" +
                "        .task-details {\n" +
                "            background: #ffebee;\n" +
                "            padding: 20px 25px;\n" +
                "            border-radius: 12px;\n" +
                "            border-left: 4px solid #d32f2f;\n" +
                "            text-align: left;\n" +
                "            max-width: 540px;\n" +
                "            margin: 0 auto 35px;\n" +
                "            box-shadow: 0 6px 18px rgba(211, 47, 47, 0.1);\n" +
                "        }\n" +
                "        .task-details p {\n" +
                "            margin: 8px 0;\n" +
                "            font-size: 18px;\n" +
                "            color: #555;\n" +
                "        }\n" +
                "        .task-details strong {\n" +
                "            color: #d32f2f;\n" +
                "        }\n" +
                "        .warning {\n" +
                "            background: #ffebee;\n" +
                "            border-left: 7px solid #ef5350;\n" +
                "            padding: 22px 30px;\n" +
                "            font-size: 16px;\n" +
                "            color: #6d4c41;\n" +
                "            border-radius: 12px;\n" +
                "            max-width: 540px;\n" +
                "            margin: 0 auto 50px auto;\n" +
                "            box-shadow: 0 6px 18px rgba(239, 83, 80, 0.2);\n" +
                "            line-height: 1.5;\n" +
                "        }\n" +
                "        .warning a {\n" +
                "            color: wheat;\n" +
                "            font-weight: 700;\n" +
                "            text-decoration: none;\n" +
                "            transition: color 0.3s ease;\n" +
                "        }\n" +
                "        .warning a:hover {\n" +
                "            color: #bf360c;\n" +
                "            text-decoration: underline;\n" +
                "        }\n" +
                "        .support-button {\n" +
                "            display: inline-block;\n" +
                "            background-color: #d32f2f;\n" +
                "            color: #fff;\n" +
                "            padding: 14px 36px;\n" +
                "            border-radius: 35px;\n" +
                "            font-weight: 700;\n" +
                "            font-size: 17px;\n" +
                "            text-decoration: none;\n" +
                "            box-shadow: 0 8px 20px rgba(211, 47, 47, 0.5);\n" +
                "            transition: background-color 0.3s ease;\n" +
                "            margin-top: 15px;\n" +
                "            cursor: pointer;\n" +
                "        }\n" +
                "        .support-button:hover {\n" +
                "            background-color: #b71c1c;\n" +
                "        }\n" +
                "        .footer {\n" +
                "            background: #1b5e20;\n" +
                "            color: #c8e6c9;\n" +
                "            text-align: center;\n" +
                "            padding: 22px 20px;\n" +
                "            font-size: 14px;\n" +
                "            border-top: 1px solid rgba(255, 255, 255, 0.15);\n" +
                "            letter-spacing: 0.6px;\n" +
                "        }\n" +
                "        @media (max-width: 640px) {\n" +
                "            .container {\n" +
                "                margin: 25px 15px;\n" +
                "            }\n" +
                "            .content {\n" +
                "                padding: 35px 25px 45px;\n" +
                "            }\n" +
                "            .header img {\n" +
                "                height: 140px;\n" +
                "            }\n" +
                "            .header h1 {\n" +
                "                font-size: 26px;\n" +
                "            }\n" +
                "            .header p {\n" +
                "                font-size: 15px;\n" +
                "            }\n" +
                "            .task-details {\n" +
                "                padding: 15px 20px;\n" +
                "            }\n" +
                "            .task-details p {\n" +
                "                font-size: 16px;\n" +
                "            }\n" +
                "        }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <div class=\"container\" role=\"main\">\n" +
                "        <div class=\"header\">\n" +
                "            <img src=\"cid:logoImage\" alt=\"Renaissance University Logo\" aria-label=\"Renaissance University Logo\" />\n" +
                "            <h1>RCEF</h1>\n" +
                "            <p>Task Management</p>\n" +
                "        </div>\n" +
                "        <div class=\"content\">\n" +
                "            <h2>Urgent: Deadline Approaching</h2>\n" +
                "            <p>Your task deadline is nearing. Please review the details below and take immediate action.</p>\n" +
                "            <div class=\"task-details\">\n" +
                "                <p><strong>Task Name:</strong> " + taskName + "</p>\n" +
                "                <p><strong>Due Date:</strong> " + dueDate + "</p>\n" +
                "                <p><strong>Time Remaining:</strong> " + timeRemaining + "</p>\n" +
                "            </div>\n" +
                "            <p>Please prioritize this task to ensure timely completion. Contact support if you need assistance.</p>\n" +
                "            <div class=\"warning\" role=\"alert\">\n" +
                "                <p>⚠️ <strong>Critical:</strong> Failure to meet the deadline may affect project timelines. Seek help if needed.</p>\n" +
                "                <a href=\"mailto:support@rcef.com\" class=\"support-button\" aria-label=\"Contact RCEF Support\">Contact Support</a>\n" +
                "            </div>\n" +
                "        </div>\n" +
                "        <div class=\"footer\">\n" +
                "            <p>&copy; 2025 Renaissance University - Chancellor Office | All rights reserved</p>\n" +
                "        </div>\n" +
                "    </div>\n" +
                "</body>\n" +
                "</html>";

        helper.setText(htmlContent, true);

        try {
            helper.addInline("logoImage", new ClassPathResource("static/images/renaissance-logo.png"));
        } catch (Exception ex) {
            // Optionally log or handle image loading failure
        }

        mailSender.send(message);
    }
}