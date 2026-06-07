package com.astraNotes.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.astraNotes")
public class AstraNotesWebApplication {
    public static void main(String[] args) {
        SpringApplication.run(AstraNotesWebApplication.class, args);
    }
}
