package com.team5.catdogeats.batch.config;

import com.team5.catdogeats.batch.mapper.UserWithdrawMapper;
import com.team5.catdogeats.users.domain.Users;
import lombok.RequiredArgsConstructor;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.batch.MyBatisPagingItemReader;
import org.mybatis.spring.batch.builder.MyBatisPagingItemReaderBuilder;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class    WithdrawnJobConfig {

    private final JobRepository jobRepo;
    private final UserWithdrawMapper mapper;
    private final PlatformTransactionManager tx;
    private final WithdrawnProperties props;
    private final SqlSessionFactory sqlSessionFactory;

    @Bean
    public Job withdrawJob() {
        return new JobBuilder("withdrawJob", jobRepo)
                .start(step())
                .build();
    }

    @Bean
    public Step step() {

        return new StepBuilder("withdrawStep", jobRepo)
                .<Users, Users>chunk(props.getChunkSize(), tx)
                .reader(pagingReader())
                .processor(item -> item)   // 그대로 전달
                .writer(items -> items.forEach(u -> {

                    mapper.withdrawUser(
                            u.getId(),
                            u.getRole().toString(),
                            OffsetDateTime.now(ZoneOffset.UTC)
                    );
                })).build();
    }

    @Bean
    public MyBatisPagingItemReader<Users> pagingReader() {
        OffsetDateTime START_DATE = OffsetDateTime.now(ZoneOffset.UTC).minusDays(7);

        Map<String, Object> params = Map.of("deleted_at", START_DATE);
        return new MyBatisPagingItemReaderBuilder<Users>()
                .sqlSessionFactory(sqlSessionFactory)
                .queryId("com.team5.catdogeats.batch.mapper.UserWithdrawMapper.selectTargets")
                .parameterValues(params)
                .pageSize(props.getChunkSize())
                .build();
    }

}
