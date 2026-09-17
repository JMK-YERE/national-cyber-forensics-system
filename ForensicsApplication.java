package com.tz.forensics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ForensicsApplication {
    public static void main(String[] args) {
        SpringApplication.run(ForensicsApplication.class, args);
        System.out.println("🇹🇿 National Cyber Security & Digital Forensics System");
        System.out.println("🔐 Server running on → http://localhost:8080");
    }
}
