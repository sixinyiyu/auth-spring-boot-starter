/**
 * Project Name: auth-spring-boot-autoconfigure
 * File Name:    HttpServiceAutoConfigure.java
 * Package Name: com.github.sixinyiyu.service.auth.spring.boot.autoconfigure
 * Date:         2020年09月18 19:11
 * Copyright (c) 2020, 思馨呓渝 All Rights Reserved.
*/

package com.github.sixinyiyu.service.auth.spring.boot.autoconfigure;

import com.github.sixinyiyu.service.auth.spring.boot.autoconfigure.annotation.RequiresPermission;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Controller;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.annotation.Resource;
import java.lang.reflect.Method;
import java.util.*;

/**
 * @description : AuthServiceAutoConfigure
 * @author    qingquanzhong
 * @create 2020年09月22 11:13
 */
@Configuration
@EnableConfigurationProperties(AuthProperties.class)
public class AuthServiceAutoConfigure implements ApplicationContextAware {
	
	protected static final Logger _LOG = LoggerFactory.getLogger(AuthServiceAutoConfigure.class);

	@Resource
	private AuthProperties authProperties;

	@Resource
	private OkHttpClient httpClient;

	private final List<String> list = new ArrayList<>(Arrays.asList("${server.error.path:${error.path:/error}}","/css/**", "/favicon.ico", "/error","/api/login.rest", "/api/user_info.rest", "/api/access_check.rest"));


	@Bean("accessWebConfigurer")
	public AccessWebConfigurer accessWebConfigurer() {
		_LOG.info("创建AccessWebConfigurer");
		return new AccessWebConfigurer(list, httpClient, authProperties.getServicePath(), authProperties.getStrategy() == 2);
	}

	private void verifyPermissionConfigure(ApplicationContext applicationContext)  {
		_LOG.info("扫描服务[{}]所有的控制器", authProperties.getAppId());

		applicationContext.getBeanNamesForAnnotation(Controller.class);
		Map<String, Object> controllerMap = applicationContext.getBeansWithAnnotation(Controller.class);
		if (!controllerMap.isEmpty()) {

			/**
			 * 1.RequiresPermission 类上有注解，默认所有控制器里的方法继承了此权限
			 * 2.类、跟方法都没有RequiresPermission 注解 默认(警告报错？还是表示不需要注解)
			 * 3. 控制器的方法扫描 需要判断(GetMapping/PostMapping/PutMapping/DeleteMapping | Mapping) 来获取对应的value
			 */
			RequestMapping classRequestMapping = null, methodRequestMapping = null;
			RequiresPermission classRequiresPermission = null, methodRequiresPermission = null;
			for(Map.Entry<String, Object> entry: controllerMap.entrySet()) {
				classRequestMapping = entry.getValue().getClass().getAnnotation(RequestMapping.class);
				String classRequestPath = "";
				if (classRequestMapping.value().length > 0) {
					classRequestPath = classRequestMapping.value()[0];
				}
				StringBuilder resourceCode = new StringBuilder();
				classRequiresPermission = entry.getValue().getClass().getAnnotation(RequiresPermission.class);
				if (null != classRequiresPermission && StringUtils.hasText(classRequiresPermission.value())) {
					resourceCode.append(classRequiresPermission.value());
				}
				Method[] methods = ReflectionUtils.getDeclaredMethods(entry.getValue().getClass());
				for(Method method: methods) {
					String methodRequestPath = "";
					methodRequestMapping= AnnotatedElementUtils.getMergedAnnotation(method, RequestMapping.class);
					if (null != methodRequestMapping) {
						if (methodRequestMapping.value().length > 0) {
							methodRequestPath = methodRequestMapping.value()[0];
						}
						// 默认所有的RequestMapping方法都需要加上Permission注解
						methodRequiresPermission = method.getAnnotation(RequiresPermission.class);
						if (null == classRequiresPermission && null == methodRequiresPermission && !this.list.contains(classRequestPath + methodRequestPath)) { // 类、方法都没启用权限,也不在白名单中
							if (authProperties.getStrategy() == 1) {
								throw new RuntimeException(String.format("%s.%s",entry.getValue().getClass().getName(), method.getName()) + "未配置权限数据");
							} else {
								_LOG.warn("类:{}配置了不需要权限，方法:{} 未配置权限(忽略权限校验)", entry.getValue().getClass().getName(), method.getName());
							}
						}
						if (null != methodRequiresPermission ) {
							/**
							 * 1.类未配置则需要方法上配置
							 * 2.类配置  a:不需要权限 则默认方法都不需要(方法配置需要则忽略方法的) b:需要权限  方法默认继承类权限
							 */
							if (null != classRequiresPermission && !classRequiresPermission.require() && methodRequiresPermission.require()) {//类不需要权限,则所有方法都不需要
								_LOG.warn("类:{}配置了不需要权限，方法:{}配置需要权限不生效,采用方法默认不需要权限策略", entry.getValue().getClass().getName(), method.getName());
							}
							if (StringUtils.hasText(methodRequiresPermission.value()) ) {
								resourceCode.append(",").append(methodRequiresPermission.value());
								if (resourceCode.toString().equals(methodRequiresPermission.value())) {
									_LOG.warn("{}方法上重复配置了权限码 {}", String.format("%s.%s",entry.getValue().getClass().getName(), method.getName()), methodRequiresPermission.value());
								}
							}
						}
						String _methods = "*";
						if (methodRequestMapping.method().length > 0) {
							StringJoiner joiner = new StringJoiner(",");
							for( RequestMethod rm: methodRequestMapping.method()) {
								joiner.add("\"" + rm.name() + "\"");
							}
							_methods = joiner.toString();
						}
						_LOG.info("[{}]{} 需要权限 {}", (classRequestPath + methodRequestPath), _methods, resourceCode);
					}
				}
			}
		}
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		if (StringUtils.isEmpty(authProperties.getServicePath())) {
			throw new RuntimeException("未配置auth.service-path 鉴权资源服务地址eg: http://gateway-api/auth_servce");
		}
		if (!Arrays.asList(1,2).contains(authProperties.getStrategy())) {
			throw new RuntimeException("auth.strategy 配置值不合法只能取值[1,2]");
		}
		if (StringUtils.hasText(authProperties.getExcludePaths())) {
			String[] paths = authProperties.getExcludePaths().split(";");
			if (paths.length > 0) {
				list.addAll(Arrays.asList(paths));
			}
		}
		verifyPermissionConfigure(applicationContext);
	}


}

