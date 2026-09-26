package com.internship.service.email;

public class SmtpEmailServiceTest {

    public static void main(String[] args) {

        String recipient = System.getenv("INTERNSHIP_TEST_EMAIL");

        if (recipient == null || recipient.isBlank()) {
            System.err.println(
                    "Missing environment variable: INTERNSHIP_TEST_EMAIL"
            );
            System.exit(1);
        }

        EmailService emailService = new SmtpEmailService();

        emailService.sendEmail(
                recipient,
                "Internship Management System - SMTP Test",
                """
                Đây là email kiểm thử từ Internship Management System.

                Nếu bạn nhận được email này thì EmailService đã hoạt động thành công.
                """
        );

        System.out.println("PASS: Test email sent successfully.");
    }
}