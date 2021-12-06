package com.glory.springbatch.batch.tasklet;

import com.glory.springbatch.batch.tasklet.step.LinesProcessor;
import com.glory.springbatch.batch.tasklet.step.LinesReader;
import com.glory.springbatch.batch.tasklet.step.LinesWriter;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableBatchProcessing
public class TaskletsConfig {

    @Autowired
    private JobBuilderFactory jobs;

    @Autowired
    private StepBuilderFactory steps;

    @Bean
    public JobLauncherTestUtils jobLauncherTestUtils() {
        return new JobLauncherTestUtils();
    }

    @Bean
    public LinesReader linesReader() {
        return new LinesReader();
    }

    @Bean
    public LinesProcessor linesProcessor() {
        return new LinesProcessor();
    }

    @Bean
    public LinesWriter linesWriter() {
        return new LinesWriter();
    }

    @Bean
    protected Step readLines() {
        return steps
                .get("readLines")
                .tasklet(linesReader())
                .build();
    }

    @Bean
    protected Step processLines() {
        return steps
                .get("processLines")
                .tasklet(linesProcessor())
                .build();
    }

    @Bean
    protected Step writeLines() {
        return steps
                .get("writeLines")
                .tasklet(linesWriter())
                .build();
    }

    @Bean
    public Job job() {
        return jobs
                .get("taskletsJob")
                .start(readLines())
                .next(processLines())
                .next(writeLines())
                .build();
    }

}
