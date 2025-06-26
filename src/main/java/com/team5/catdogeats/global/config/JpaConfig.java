package com.team5.catdogeats.global.config;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
@EnableJpaRepositories(
        basePackages = {"com.team5.catdogeats.users.repository",
                "com.team5.catdogeats.addresses.repository",
                "com.team5.catdogeats.orders.repository",
                "com.team5.catdogeats.products.repository",
                "com.team5.catdogeats.pets.repository",
                "com.team5.catdogeats.carts.repository",
                "com.team5.catdogeats.payments.repository"},
        entityManagerFactoryRef = "entityManagerFactory",
        transactionManagerRef = "jpaTransactionManager"
)

public class JpaConfig {

        @Value("${spring.jpa.hibernate.ddl-auto}")
        private String ddlAuto;

        @Value("${spring.jpa.show-sql}")
        private boolean showSql;

        @Value("${spring.jpa.properties.hibernate.format_sql}")
        private boolean formatSql;

        @Value("${spring.jpa.database-platform}")
        private String databasePlatform;

        @Bean
        public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
            LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
            emf.setDataSource(dataSource);
            emf.setPackagesToScan("com.team5.catdogeats");

            HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
            vendorAdapter.setGenerateDdl(true);
            vendorAdapter.setShowSql(showSql);
            vendorAdapter.setDatabasePlatform(databasePlatform);
            emf.setJpaVendorAdapter(vendorAdapter);

            java.util.Properties jpaProperties = new java.util.Properties();
            jpaProperties.put("hibernate.hbm2ddl.auto", ddlAuto);
            jpaProperties.put("hibernate.format_sql", String.valueOf(formatSql));
            emf.setJpaProperties(jpaProperties);

            return emf;
        }


    @Bean(name = {"jpaTransactionManager", "transactionManager"})
    public PlatformTransactionManager jpaTransactionManager(EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}
