package com.team5.catdogeats.batch.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team5.catdogeats.batch.dto.WithdrawBatchTargetRow;
import com.team5.catdogeats.batch.mapper.UserWithdrawMapper;
import com.team5.catdogeats.users.domain.Users;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.batch.MyBatisPagingItemReader;
import org.mybatis.spring.batch.builder.MyBatisPagingItemReaderBuilder;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;

@Slf4j
@Configuration
public class    WithdrawnJobConfig {

    private final JobRepository jobRepo;
    private final UserWithdrawMapper userWithdrawMapper;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final PlatformTransactionManager batchTransactionManager;
    private final WithdrawnProperties props;
    private final SqlSessionFactory sqlSessionFactory;



    // Lombok은 @Qualifier와 같은 Spring 어노테이션을 자동으로 생성자에 복사하지않아 명시적으로 생성자 선언
    public WithdrawnJobConfig(JobRepository jobRepo,
                              UserWithdrawMapper userWithdrawMapper,
                              RedisTemplate<String, String> redisTemplate,
                              ObjectMapper objectMapper,
                              @Qualifier("batchTransactionManager") PlatformTransactionManager batchTransactionManager,
                              WithdrawnProperties props, SqlSessionFactory sqlSessionFactory) {
        this.jobRepo = jobRepo;
        this.userWithdrawMapper = userWithdrawMapper;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.batchTransactionManager = batchTransactionManager;
        this.props = props;
        this.sqlSessionFactory = sqlSessionFactory;
    }

    @Bean
    public Job withdrawJob() {
        return new JobBuilder("withdrawJob", jobRepo)
                .start(readTargetUserStep(jobRepo, batchTransactionManager))
                .next(withdrawUserStep(jobRepo, batchTransactionManager))
                .next(cleanupRedisStep(jobRepo, batchTransactionManager))
                .build();
    }


    // Step 1: 탈퇴 대상 users를 redis 저장
    @Bean
    public Step readTargetUserStep(JobRepository jobRepo,
                                   PlatformTransactionManager batchTransactionManager) {
        try {
            return new StepBuilder("readTargetUserStep", jobRepo)
                    .<Users, Users>chunk(props.getChunkSize(), batchTransactionManager)
                    .reader(pagingReader())
                    .writer(items -> {
                        for (Users u : items) {
                            WithdrawBatchTargetRow row = new WithdrawBatchTargetRow(
                                    u.getId(),
                                    u.getRole().toString(),
                                    u.getDeletedAt()
                            );
                            String json = objectMapper.writeValueAsString(row);
                            redisTemplate.opsForList().rightPush("withdrawBatchTargets", json);
                        }
                    })
                    .build();
        } catch (Exception e) {
            log.error("step1 예상치 못한 에러 발생");
            throw e;
        }
    }

    // Step 2: 임시 테이블에서 읽어서 탈퇴 처리
    @Bean
    public Step withdrawUserStep(JobRepository jobRepo, PlatformTransactionManager batchTransactionManager) {
        try {
            return new StepBuilder("withdrawUserStep", jobRepo)
                    .<WithdrawBatchTargetRow, WithdrawBatchTargetRow>chunk(props.getChunkSize(), batchTransactionManager)
                    .reader(() -> {
                        String json = redisTemplate.opsForList().leftPop("withdrawBatchTargets");
                        if (json == null || json.isBlank()) return null;
                        return objectMapper.readValue(json, WithdrawBatchTargetRow.class);
                    })
                    .writer(items -> {
                        for (WithdrawBatchTargetRow row : items) {
                            userWithdrawMapper.withdrawUser(row.id(), row.role(), row.deletedAt());
                        }
                    })
                    .build();
        } catch (Exception e) {
            log.error("step2 예상치 못한 에러 발생");
            throw e;
        }
    }

    @Bean
    public Step cleanupRedisStep(JobRepository jobRepo, PlatformTransactionManager txManager) {
        return new StepBuilder("cleanupRedisStep", jobRepo)
                .tasklet((contribution, context) -> {
                    redisTemplate.delete("withdrawBatchTargets");
                    return RepeatStatus.FINISHED;
                }, txManager)
                .build();
    }


    @Bean
    public MyBatisPagingItemReader<Users> pagingReader() {
        OffsetDateTime cutoffDate = OffsetDateTime.now(ZoneOffset.UTC).minusDays(7);

        Map<String, Object> params = Map.of("deletedAt", cutoffDate);
        return new MyBatisPagingItemReaderBuilder<Users>()
                .sqlSessionFactory(sqlSessionFactory)
                .queryId("com.team5.catdogeats.batch.mapper.UserWithdrawMapper.selectTargets")
                .parameterValues(params)
                .pageSize(props.getChunkSize())
                .build();
    }

}
