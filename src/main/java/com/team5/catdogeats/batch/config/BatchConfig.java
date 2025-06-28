package com.team5.catdogeats.batch.config;

import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
public class BatchConfig {

    // 순환 참조 오류 발생으로 반드시 필요한 설정 절대 지우면 안됩니다
    @Bean
    public JobRepository jobRepository(
            DataSource dataSource,
            @Qualifier("batchTransactionManager") PlatformTransactionManager transactionManager // 반드시 MyBatis용!
    )  {
        try {
            JobRepositoryFactoryBean factory = new JobRepositoryFactoryBean();
            factory.setDataSource(dataSource);
            factory.setTransactionManager(transactionManager);
            // 필수 옵션 (Spring Boot 3.x 이상, JDBC 기반 테이블 스키마)
            factory.setIsolationLevelForCreate("ISOLATION_DEFAULT");
            factory.afterPropertiesSet();
            return factory.getObject();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    @Bean(name = "batchTransactionManager")
    public PlatformTransactionManager batchTransactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}
