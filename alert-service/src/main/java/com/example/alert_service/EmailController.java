package com.example.alert_service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.mail.MailMessage;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.messaging.MessagingException;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.util.Map;

@RestController
@RequestMapping("/api/alert-service")
@RequiredArgsConstructor
@Slf4j
public class EmailController {
    private final EmailService emailService;

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        log.info("GET /health ");
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "alert-service"
        ));
    }

    @PostMapping("/test-email")
    public ResponseEntity<String> sendTestEmail(
            @RequestParam String to,
            @RequestParam(defaultValue = "Test Alert") String subject,
            @RequestParam(defaultValue = "Test message from alert-service") String message) {
        try {
            log.info("POST /test-email. Попытка отправки тестового email. to {}, subject {}, message {}",to, subject, message);
            emailService.sendSimpleEmail(to, subject, message);
            return ResponseEntity.ok("Email отправлен на " + to);
        } catch (MailException e) {
            log.error("Ошибка при отправке email: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Ошибка: " + e.getMessage());
        }
    }
}
