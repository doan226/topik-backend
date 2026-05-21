package com.topik.topikai.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender; // Phải khai báo ở đây để dùng

    public void sendVerificationEmail(String to, String code) {
        try {
            System.out.println("DEBUG: Bắt đầu gửi mail tới " + to + " với mã " + code);
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("Xác thực tài khoản TOPIK AI");
            message.setText("Chào bạn, mã xác thực của bạn là: " + code);

            mailSender.send(message);
            System.out.println("DEBUG: Mail đã gửi xong!");
        } catch (Exception e) {
            System.err.println("DEBUG: Lỗi khi gửi mail: " + e.getMessage());
            throw e;
        }
    }
}