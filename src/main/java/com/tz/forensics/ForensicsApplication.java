package com.tz.forensics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ForensicsApplication {
    public static void main(String[] args) {
        SpringApplication.run(ForensicsApplication.class, args);
        System.out.println("🛡️ Cyber Forensics System → http://localhost:8080");
        System.out.println("⏰ Auto-Reply Scheduler ACTIVE (5 min wait for admin)");
    }
}
