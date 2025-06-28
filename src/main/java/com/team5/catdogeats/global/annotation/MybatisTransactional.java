package com.team5.catdogeats.global.annotation;


import org.springframework.core.annotation.AliasFor;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Transactional(transactionManager = "mybatisTransactionManager")
public @interface MybatisTransactional {

    /** 전파 옵션 (기본: Propagation.REQUIRED) */
    @AliasFor(annotation = Transactional.class, attribute = "propagation")
    Propagation propagation() default Propagation.REQUIRED;

    /** 격리 수준 (기본: Isolation.DEFAULT) */
    @AliasFor(annotation = Transactional.class, attribute = "isolation")
    Isolation isolation() default Isolation.DEFAULT;

    /** 타임아웃(초) (기본: -1, 무제한) */
    @AliasFor(annotation = Transactional.class, attribute = "timeout")
    int timeout() default -1;

    /** 읽기 전용 여부 (기본: false) */
    @AliasFor(annotation = Transactional.class, attribute = "readOnly")
    boolean readOnly() default false;

    /** 롤백 대상 예외 지정 */
    @AliasFor(annotation = Transactional.class, attribute = "rollbackFor")
    Class<? extends Throwable>[] rollbackFor() default {};

    /** 롤백 제외 예외 지정 */
    @AliasFor(annotation = Transactional.class, attribute = "noRollbackFor")
    Class<? extends Throwable>[] noRollbackFor() default {};
}
