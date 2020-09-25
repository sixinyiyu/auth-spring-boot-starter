package com.github.sixinyiyu.service.auth.spring.boot.autoconfigure;
/**
 * Project Name: IntelliJ IDEA
 * Package Name: com.github.sixinyiyu.service.auth.spring.boot.autoconfigure
 * Date:          2020年09月22 11:13: 47
 * Copyright (c) 2020, sixinyiyu All Rights Reserved.
 */

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.sixinyiyu.service.auth.spring.boot.autoconfigure.annotation.RequiresPermission;
import com.github.sixinyiyu.service.auth.spring.boot.autoconfigure.dto.AuthResultDTO;
import com.github.sixinyiyu.service.auth.spring.boot.autoconfigure.dto.Constants;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

/**
 * @description 认证/权限验证拦截器
 * @author qingquanzhong
 * @create 2020年09月22 11:13
 **/
public class AccessInterceptor extends HandlerInterceptorAdapter  {

    private static final Logger _LOG = LoggerFactory.getLogger(AccessInterceptor.class);

    private final OkHttpClient httpClient;
    private final boolean ignoreNoMatch;
    private final String authService;

    public AccessInterceptor(OkHttpClient httpClient, String authService, boolean ignoreNoMatch) {
        _LOG.info("Initializing AccessInterceptor");
        this.httpClient = httpClient;
        this.ignoreNoMatch = ignoreNoMatch;
        this.authService = authService;
    }

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
//        HttpServletRequest httpServletRequest= ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        String bestMatchingPattern = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        _LOG.info("try to access  path ===> {}", bestMatchingPattern);
        if ("/**".equals(bestMatchingPattern)) return true;
        final String token = getToken(request);
        if (StringUtils.isEmpty(token)) {
            response.getOutputStream().write(objectMapper.writeValueAsBytes(AuthResultDTO.failed(String.valueOf(HttpStatus.UNAUTHORIZED), "未登录")));
            response.setCharacterEncoding("UTF-8");
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(HttpStatus.OK.value());
            return false;
        }
        Set<String> resourceCodes = new HashSet<>();
        RequiresPermission classRequiresPermission = ((HandlerMethod) handler).getBean().getClass().getAnnotation(RequiresPermission.class);
        if (null != classRequiresPermission && classRequiresPermission.require() && StringUtils.hasText(classRequiresPermission.value())) {
            resourceCodes.add(classRequiresPermission.value());
        }
        Method method = ((HandlerMethod) handler).getMethod();
        RequiresPermission permission = AnnotationUtils.findAnnotation(method, RequiresPermission.class);
        if (null != permission && permission.require() && StringUtils.hasText(permission.value())) {
            resourceCodes.add(permission.value());
        }

        if ( (resourceCodes.isEmpty() &&  !ignoreNoMatch) || ( resourceCodes.size() > 0 && !invokeCheckPermission(token, String.join(",",resourceCodes))) ) {
            response.getOutputStream().write(objectMapper.writeValueAsBytes(AuthResultDTO.failed(String.valueOf(HttpStatus.FORBIDDEN), "没有访问权限")));
            response.setCharacterEncoding("UTF-8");
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(HttpStatus.OK.value());
            return false;
        }

        return true;
    }

    // 校验access_token 是否有权限操作资源 多个资源码用,分割
    private boolean invokeCheckPermission(final String token, final String resourceCode) {
        boolean accessible = true;
        _LOG.info("try to check permission for access [{}-{}]", token, resourceCode);

        // 这里走网关？还是走内网服务
        _LOG.info("调用认证服务,权限校验： token={}, resource_code={}", token, resourceCode);
        Request request = new Request.Builder().url(String.format("%s/access_check.rest?token=%s&resource_code=%s", authService , token, resourceCode)).get().build();
        try {
            Response response = httpClient.newCall(request).execute();
            accessible = response.isSuccessful();
            ResponseBody responseBody = null;
            if (response.isSuccessful() && null != (responseBody = response.body())) {
                String responseText = response.body().string();
                if (StringUtils.hasText(responseText)) {
                    accessible = Boolean.parseBoolean(responseText.trim());
                } else {
                    accessible = false;
                }
            } else {
                accessible = false;
            }
        } catch (IOException exception) {
            accessible = false;
            _LOG.warn("调用认证服务,权限校验接口异常" + exception.getMessage(), exception);
        }
        return accessible;
    }

    //请求参数、请求头[Authorization/x-rbac-token]、请求cookie
    private String getToken(HttpServletRequest request) {
        String token = request.getParameter(Constants.TOKEN_PARAM);
        if (StringUtils.isEmpty(token)) {
            token = request.getHeader(Constants.TOKEN_AUTHORIZATION);
            if (StringUtils.isEmpty(token)) {
                token = request.getHeader(Constants.TOKEN_HEADER);
            }
        }
        if (StringUtils.isEmpty(token)) {
            Cookie[] cookies = request.getCookies();
            if (null != cookies) {
                for(Cookie cookie : cookies) {
                    if (Constants.TOKEN_COOKIE.equals(cookie.getName())) {
                        token = cookie.getValue();
                        break;
                    }
                }
            }
        }
        return token;
    }
}
