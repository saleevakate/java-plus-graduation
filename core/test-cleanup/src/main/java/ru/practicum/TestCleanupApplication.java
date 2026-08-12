package ru.practicum;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class TestCleanupApplication {
    public static void main(String[] args) {
        SpringApplication.run(TestCleanupApplication.class, args);
    }
}
