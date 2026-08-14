package com.uab.salesprevision.pipeline;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class PipelineServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(PipelineServiceApplication.class, args);
    }
}
