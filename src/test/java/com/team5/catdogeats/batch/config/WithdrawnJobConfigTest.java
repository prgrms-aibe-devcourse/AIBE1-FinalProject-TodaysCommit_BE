package com.team5.catdogeats.batch.config;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class WithdrawnJobConfigTest {
    @Autowired
    JobLauncher jobLauncher;
    @Autowired
    Job withdrawJob;

    @Test
    void runOnce() throws Exception {
        JobParameters params = new JobParametersBuilder()
                .addLong("run.id", System.currentTimeMillis())
                .toJobParameters();

        JobExecution exec = jobLauncher.run(withdrawJob, params);
        assertEquals(BatchStatus.COMPLETED, exec.getStatus());
    }

}