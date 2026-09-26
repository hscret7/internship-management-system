package com.internship.service.email;

public interface EmailService {

    void sendEmail(
            String recipient,
            String subject,
            String content
    );
}