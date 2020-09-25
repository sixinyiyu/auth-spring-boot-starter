package com.github.sixinyiyu.service.auth.spring.boot.autoconfigure;
/**
 * Project Name: IntelliJ IDEA
 * Package Name: com.github.sixinyiyu.service.auth.spring.boot.autoconfigure
 * Date:          2020年09月22 18:44: 12
 * Copyright (c) 2020, sixinyiyu All Rights Reserved.
 */

import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.*;

/**
 * @description
 * @author qingquanzhong
 * @create 2020年09月22 18:44
 **/
public class AccessWebConfigurer implements WebMvcConfigurer {

    private static final Logger _LOG = LoggerFactory.getLogger(AccessWebConfigurer.class);

    private final List<String> excludePath;
    private final OkHttpClient httpClient;
    private final boolean ignoreNoMatch;
    private final String authService;

    public List<String> getExcludePath() {
        return excludePath;
    }

    /**排除拦截地址 多个用;分割*/
    public AccessWebConfigurer(List<String> excludePaths, OkHttpClient httpClient, String authService, boolean ignoreNoMatch) {
        this.excludePath = Collections.unmodifiableList(excludePaths);
        this.httpClient = httpClient;
        this.ignoreNoMatch = ignoreNoMatch;
        this.authService = authService;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        _LOG.info("Access exclude Path : {}", excludePath);
        registry.addInterceptor(new AccessInterceptor(this.httpClient, this.authService, this.ignoreNoMatch)).addPathPatterns("/**")
                .excludePathPatterns(excludePath).order(0);
    }
}
