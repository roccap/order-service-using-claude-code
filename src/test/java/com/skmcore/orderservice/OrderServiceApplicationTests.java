package com.skmcore.orderservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("dev")
class OrderServiceApplicationTests {

    @Test
    void contextLoads() {
        // Verifies the Spring context starts cleanly with the dev profile (H2 + Liquibase)
    }
}
