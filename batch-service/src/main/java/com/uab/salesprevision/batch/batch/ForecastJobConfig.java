package com.uab.salesprevision.batch.batch;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Map;

@Configuration
public class ForecastJobConfig {

    @Bean
    public Job forecastJob(JobRepository jobRepository, Step forecastStep) {
        return new JobBuilder("forecastJob", jobRepository)
                .start(forecastStep)
                .build();
    }

    @Bean
    public Step forecastStep(JobRepository jobRepository,
                             PlatformTransactionManager transactionManager,
                             ForecastItemReader reader,
                             ForecastItemProcessor processor,
                             ForecastItemWriter writer) {
        return new StepBuilder("forecastStep", jobRepository)
                .<Map<String, Object>, Map<String, Object>>chunk(100, transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .build();
    }
}
