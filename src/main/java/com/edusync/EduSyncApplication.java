package com.edusync;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;


/**
 * EduSync College Digital Platform.
 *
 * Main Spring Boot application entry point.
 */
@SpringBootApplication
public class EduSyncApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(EduSyncApplication.class, args);
        String port = context.getEnvironment().getProperty("server.port", "8080");

        System.out.println();
        System.out.println("========================================");
        System.out.println("EduSync Backend is RUNNING");
        System.out.println("http://localhost:" + port);
        System.out.println("TKMIT College Digital Platform");
        System.out.println("========================================");
        System.out.println();
    }
}