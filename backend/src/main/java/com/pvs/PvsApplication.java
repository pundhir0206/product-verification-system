package com.pvs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class PvsApplication {
    public static void main(String[] args) {
        SpringApplication.run(PvsApplication.class, args);
    }
}
