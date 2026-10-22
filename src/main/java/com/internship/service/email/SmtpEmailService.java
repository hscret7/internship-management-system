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
import java.util.logging.Level;
import java.util.logging.Logger;

public class SmtpEmailService implements EmailService {

    private static final Logger LOGGER =
            Logger.getLogger(SmtpEmailService.class.getName());

    private static final String CONFIG_FILE = "mail.properties";

    private final Properties config;

    public SmtpEmailService() {
        if (Boolean.getBoolean("internship.mail.disabled") || "true".equalsIgnoreCase(System.getenv("INTERNSHIP_MAIL_DISABLED"))) {
            throw new EmailException("Email is explicitly disabled for this environment.");
        }
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
        String rawPassword = requireProperty("mail.password");
        String password = rawPassword.contains(" ")
                ? rawPassword.replace(" ", "")
                : rawPassword;
        String from = requireProperty("mail.from");

        Properties mailProperties = new Properties();

        for (String name : config.stringPropertyNames()) {
            if (name.startsWith("mail.smtp.") || name.startsWith("mail.transport.")) {
                mailProperties.put(name, config.getProperty(name).trim());
            }
        }

        String host = requireProperty("mail.smtp.host");
        String port = requireProperty("mail.smtp.port");
        String auth = requireProperty("mail.smtp.auth");
        String starttls = requireProperty("mail.smtp.starttls.enable");

        mailProperties.put("mail.smtp.host", host);
        mailProperties.put("mail.smtp.port", port);
        mailProperties.put("mail.smtp.auth", auth);
        mailProperties.put("mail.smtp.starttls.enable", starttls);

        mailProperties.put("mail.smtp.user", username);
        mailProperties.put("mail.user", username);
        mailProperties.putIfAbsent("mail.transport.protocol", "smtp");

        mailProperties.putIfAbsent("mail.smtp.ssl.protocols", "TLSv1.2 TLSv1.3");
        mailProperties.putIfAbsent("mail.smtp.ssl.trust", host);

        mailProperties.putIfAbsent("mail.smtp.connectiontimeout", "10000");
        mailProperties.putIfAbsent("mail.smtp.timeout", "10000");
        mailProperties.putIfAbsent("mail.smtp.writetimeout", "10000");

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
            LOGGER.log(
                    Level.WARNING,
                    "Failed to send email to {0}: [{1}] {2}",
                    new Object[]{recipient, e.getClass().getSimpleName(), e.getMessage()}
            );
            throw new EmailException(
                    "Unable to send email: " + e.getMessage(),
                    e
            );
        }
    }

    private Properties loadConfiguration() {

        Properties properties = new Properties();

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        InputStream input = classLoader != null
                ? classLoader.getResourceAsStream(CONFIG_FILE)
                : null;

        if (input == null) {
            input = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE);
        }

        if (input == null) {
            LOGGER.log(
                    Level.SEVERE,
                    "Mail configuration file not found: {0}",
                    CONFIG_FILE
            );
            throw new EmailException(
                    "Mail configuration file not found: "
                            + CONFIG_FILE
            );
        }

        try (InputStream stream = input) {
            properties.load(stream);
            return properties;

        } catch (IOException e) {
            LOGGER.log(
                    Level.SEVERE,
                    "Unable to load mail configuration: {0}",
                    e.getMessage()
            );
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
