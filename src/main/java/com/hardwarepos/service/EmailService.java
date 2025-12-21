package com.hardwarepos.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendEmailWithAttachment(String to, String subject, String text, byte[] attachment, String filename) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();

        // Pass true for multipart
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(text);

        // Add attachment
        helper.addAttachment(filename, new ByteArrayResource(attachment));

        mailSender.send(message);
        System.out.println("Email sent successfully to " + to);
    }
}
