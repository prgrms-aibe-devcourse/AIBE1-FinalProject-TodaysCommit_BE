package com.team5.catdogeats.global.config;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
@MapperScan(
        basePackages = {"com.team5.catdogeats.users.mapper,",
                        "com.team5.catdogeats.products.mapper"}, // @Mapper 인터페이스가 있는 패키지
        sqlSessionFactoryRef = "sqlSessionFactory"
)

public class MyBatisConfig {

        @Value("${mybatis.type-aliases-package}")
        private String typeAliasesPackage;

        @Bean
        public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
            SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
            factoryBean.setDataSource(dataSource);

            // YAML 설정에서 가져온 type-aliases-package 적용
            factoryBean.setTypeAliasesPackage(typeAliasesPackage);
            return factoryBean.getObject();
        }

    @Bean
    public SqlSessionTemplate sqlSessionTemplate(SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }
}
