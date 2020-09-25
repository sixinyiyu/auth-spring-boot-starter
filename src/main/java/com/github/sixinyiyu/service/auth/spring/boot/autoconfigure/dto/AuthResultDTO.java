package com.github.sixinyiyu.service.auth.spring.boot.autoconfigure.dto;
/**
 * Project Name: IntelliJ IDEA
 * Package Name: com.github.sixinyiyu.service.auth.spring.boot.autoconfigure.dto
 * Date:          2020年09月22 17:34: 43
 * Copyright (c) 2020, sixinyiyu All Rights Reserved.
 */

import java.io.Serializable;

/**
 * @description 认证结果
 * @author qingquanzhong
 * @create 2020年09月22 17:34
 **/
public class AuthResultDTO implements Serializable {

    /**认证结果*/
    private Boolean authSuccess;
    /** 401 未登录、403 无权限 */
    private String code;

    /**认证信息*/
    private String message;

    public AuthResultDTO() {
    }

    public static AuthResultDTO failed(String code, String message) {
        return new AuthResultDTO(Boolean.FALSE, code, message);
    }

    public AuthResultDTO(Boolean authSuccess, String code, String message) {
        super();
        this.code = code;
        this.authSuccess = authSuccess;
        this.message = message;
    }

    public Boolean getAuthSuccess() {
        return authSuccess;
    }

    public void setAuthSuccess(Boolean authSuccess) {
        this.authSuccess = authSuccess;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
