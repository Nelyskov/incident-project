package com.example.alert_service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultEmailService implements EmailService {
    private final JavaMailSender mailSender;


    public void sendSimpleEmail(String toAddress, String subject, String message) {
        SimpleMailMessage simpleMailMessage = new SimpleMailMessage();
        simpleMailMessage.setTo(toAddress);
        simpleMailMessage.setSubject(subject);
        simpleMailMessage.setText(message);
        mailSender.send(simpleMailMessage);
        log.info("Email отправлен на {}", toAddress);
    }

    @Override
    public void sendEmailWithAttachment(String toAddress, String subject, String body, String attachmentPath)
            throws jakarta.mail.MessagingException {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
        helper.setTo(toAddress);
        helper.setSubject(subject);
        helper.setText(body, true);
        FileSystemResource file = new FileSystemResource(new File(attachmentPath));
        helper.addAttachment(file.getFilename(), file);
        mailSender.send(mimeMessage);
        log.info("Email с вложением отправлен на {}", toAddress);
    }
}
