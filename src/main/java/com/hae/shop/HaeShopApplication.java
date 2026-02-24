package com.hae.shop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HaeShopApplication {
    public static void main(String[] args) {
        SpringApplication.run(HaeShopApplication.class, args);
    }
}
