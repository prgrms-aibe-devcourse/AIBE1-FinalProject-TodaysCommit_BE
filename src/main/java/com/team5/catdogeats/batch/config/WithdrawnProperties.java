package com.team5.catdogeats.batch.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class WithdrawnProperties {
    @Value("${batch.withdrawn.cron}")
    private String cron;

    @Value("${batch.withdrawn.chunk-size}")
    private int chunkSize;
}

