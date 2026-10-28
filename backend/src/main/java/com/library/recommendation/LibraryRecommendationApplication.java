package com.library.recommendation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class LibraryRecommendationApplication {

    public static void main(String[] args) {
        SpringApplication.run(LibraryRecommendationApplication.class, args);
    }
}
