package com.internship.service.email;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class SmtpEmailService implements EmailService {

    private static final String CONFIG_FILE = "mail.properties";

    private final Properties config;

    public SmtpEmailService() {
        this.config = loadConfiguration();
    }

    @Override
    public void sendEmail(
            String recipient,
            String subject,
            String content
    ) {

        validateInput(recipient, subject, content);

        String username = requireProperty("mail.username");
        String password = requireProperty("mail.password");
        String from = requireProperty("mail.from");

        Properties mailProperties = new Properties();

        mailProperties.put(
                "mail.smtp.host",
                requireProperty("mail.smtp.host")
        );

        mailProperties.put(
                "mail.smtp.port",
                requireProperty("mail.smtp.port")
        );

        mailProperties.put(
                "mail.smtp.auth",
                requireProperty("mail.smtp.auth")
        );

        mailProperties.put(
                "mail.smtp.starttls.enable",
                requireProperty("mail.smtp.starttls.enable")
        );

        Session session = Session.getInstance(
                mailProperties,
                new Authenticator() {
                    @Override
                    protected PasswordAuthentication
                    getPasswordAuthentication() {

                        return new PasswordAuthentication(
                                username,
                                password
                        );
                    }
                }
        );

        try {
            MimeMessage message = new MimeMessage(session);

            String fromName =
                    config.getProperty(
                            "mail.from.name",
                            "Internship Management System"
                    );

            message.setFrom(
                    new InternetAddress(
                            from,
                            fromName,
                            StandardCharsets.UTF_8.name()
                    )
            );

            message.setRecipients(
                    Message.RecipientType.TO,
                    InternetAddress.parse(recipient)
            );

            message.setSubject(
                    subject,
                    StandardCharsets.UTF_8.name()
            );

            message.setText(
                    content,
                    StandardCharsets.UTF_8.name()
            );

            Transport.send(message);

        } catch (MessagingException | IOException e) {
            throw new EmailException(
                    "Unable to send email.",
                    e
            );
        }
    }

    private Properties loadConfiguration() {

        Properties properties = new Properties();

        try (InputStream input =
                     getClass()
                             .getClassLoader()
                             .getResourceAsStream(CONFIG_FILE)) {

            if (input == null) {
                throw new EmailException(
                        "Mail configuration file not found: "
                                + CONFIG_FILE
                );
            }

            properties.load(input);

            return properties;

        } catch (IOException e) {
            throw new EmailException(
                    "Unable to load mail configuration.",
                    e
            );
        }
    }

    private String requireProperty(String key) {

        String value = config.getProperty(key);

        if (value == null || value.isBlank()) {
            throw new EmailException(
                    "Missing mail configuration: " + key
            );
        }

        return value.trim();
    }

    private void validateInput(
            String recipient,
            String subject,
            String content
    ) {

        if (recipient == null || recipient.isBlank()) {
            throw new IllegalArgumentException(
                    "Recipient must not be blank."
            );
        }

        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException(
                    "Subject must not be blank."
            );
        }

        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException(
                    "Content must not be blank."
            );
        }
    }
}