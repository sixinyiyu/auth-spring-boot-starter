package com.github.sixinyiyu.service.auth.spring.boot.autoconfigure.annotation;

import java.lang.annotation.*;

@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface RequiresPermission {

    /**资源码 */
    String value();

    /**是否需要权限验证 默认需要校验*/
    boolean require() default  true;

}
