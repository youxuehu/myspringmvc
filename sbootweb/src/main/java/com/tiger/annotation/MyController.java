package com.tiger.annotation;

import java.lang.annotation.*;

/**
 * Created by Administrator on 2018/8/1.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MyController {
    String value() default "" ;
}
