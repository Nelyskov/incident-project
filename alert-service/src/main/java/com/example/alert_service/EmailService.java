package com.example.alert_service;



public interface EmailService {
    void sendSimpleEmail(String toAddress, String subject, String message);

    void sendEmailWithAttachment(String toAddress, String subject, String body, String attachmentPath)
            throws jakarta.mail.MessagingException;
}
