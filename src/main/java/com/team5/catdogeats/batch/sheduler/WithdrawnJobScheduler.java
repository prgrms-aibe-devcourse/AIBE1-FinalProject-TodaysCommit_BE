package com.team5.catdogeats.batch.sheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WithdrawnJobScheduler {

    private final Job withdrawJob;
    private final JobLauncher jobLauncher;

    @Scheduled(cron = "${batch.withdrawn.cron}")
    public void runWithdrawJob() {
        try {
            log.debug("스케줄러 시작");

            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();

            jobLauncher.run(withdrawJob, jobParameters);

        } catch (Exception e) {
            log.error("예기치 못한 스케줄러 예외", e);
        }
    }
}