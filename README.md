- ```
   auth-spring-boot-start
   主要是来做token认证、权限校验功能
  ```

- 引入pom

```
<dependency>
    <groupId>com.github.sixinyiyu.service</groupId>
    <artifactId>auth-spring-boot-start</artifactId>
    <version>${recently.version}</version>
</dependency>
```

- 编写配置

```
--服务名实例
auth.appId=user-service_0004 
-- token/权限黑名单
auth.exclude-paths=/api/dept/hello  
-- 认证策略(非白名单path) 方法以及所在类都未配置权限处理策略 1 报错  2 默认不进行权限验证
auth.strategy=2  
-- 提供资源认证服务器
auth.service-path=http://192.168.1.130:9080/user-service
```

- 具体认证服务器(需要提供下面三个接口)

```
-- 登录
{
    "appId":"1",
    "username":"",
    "password":""
}
-- 返回结果
{
   "token":"xxx",
   "user_info": {
       "user_name":"", --必须
       "nick_name":"", --必须
       "role":""
       -- 其他
   }
}
/api/login.rest 

--根据token获取用户信息
/api/user_info.rest

/api/access_check.rest?token=?&resource_code=code1,code2  返回 true/false
```

