package com.topik.topikai.config; // Tùy thuộc vào package của bạn

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
public class MailConfig {

    @Bean
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost("smtp.gmail.com");

        // 🎯 ÉP CỨNG CỔNG 465 ĐỂ VƯỢT TƯỜNG LỬA RENDER
        mailSender.setPort(465);

        // Lấy username và password từ biến môi trường của Render
        mailSender.setUsername(System.getenv("support.topikai@gmail.com"));
        mailSender.setPassword(System.getenv("cqllipktzwhdymzj"));

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");

        // 🎯 TẮT STARTTLS (chỉ dùng cho 587) VÀ BẬT SSL CHUYÊN DỤNG CHO 465
        props.put("mail.smtp.starttls.enable", "false");
        props.put("mail.smtp.ssl.enable", "true");
        props.put("mail.smtp.socketFactory.port", "465");
        props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        props.put("mail.debug", "true"); // Để in log ra xem nếu có lỗi

        return mailSender;
    }
}