package com.betobanco;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BetoBancoApplication {

    public static void main(String[] args) {
        SpringApplication.run(BetoBancoApplication.class, args);
    }
}
