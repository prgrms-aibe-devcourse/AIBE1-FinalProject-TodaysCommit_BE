package com.team5.catdogeats.global.config;

import org.springframework.transaction.annotation.Transactional;

import java.lang.annotation.*;

@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Transactional(transactionManager = "mongoTransactionManager")
public @interface MongoTransactional {
}
