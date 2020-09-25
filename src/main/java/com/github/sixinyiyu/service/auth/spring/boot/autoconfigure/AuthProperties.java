package com.github.sixinyiyu.service.auth.spring.boot.autoconfigure;
/**
 * Project Name: IntelliJ IDEA
 * Package Name: com.github.sixinyiyu.service.auth.spring.boot.autoconfigure
 * Date:          2020年09月22 18:56: 57
 * Copyright (c) 2020, sixinyiyu All Rights Reserved.
 */


import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @description
 * @author qingquanzhong
 * @create 2020年09月22 18:56
 **/
@ConfigurationProperties("auth")
public class AuthProperties {

    /**服务名*/
    private String appId;

    /**权限校验白名单*/
    private String excludePaths;

    /**授权服务地址*/
    private String servicePath;

    public String getServicePath() {
        return servicePath;
    }

    public void setServicePath(String servicePath) {
        this.servicePath = servicePath;
    }

    /**认证策略(非白名单path) 方法以及所在类都未配置权限处理策略 1 报错  2 默认不进行权限验证*/
    private Integer strategy = 1;

    public Integer getStrategy() {
        return strategy;
    }

    public void setStrategy(Integer strategy) {
        this.strategy = strategy;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getExcludePaths() {
        return excludePaths;
    }

    public void setExcludePaths(String excludePaths) {
        this.excludePaths = excludePaths;
    }
}
