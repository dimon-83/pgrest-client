# pgrest-client

[English](README.en.md) | [中文](README.md)

PostgREST Java 客户端与 Spring Boot Starter。项目按模块拆分：核心零依赖（JDK HttpClient），Starter 负责自动装配，可选引入独立的 Feign 模块。

## 项目结构
- `pgrest-client`（父工程，packaging `pom`）
  - `pgrest-client-core`：核心库（`PgRestClient`、`PgClientConfig`、`PgQueryBuilder`、`PageResult`、HTTP 抽象：`JdkHttpExecutor`、`OkHttpExecutor`）
  - `pgrest-spring-boot-starter`：Starter（`PgRestAutoConfiguration`、`PgRestProperties`、网关控制器 `/pgrest`）
  - `pgrest-client-feign`：Feign 客户端与类型封装（`PgRestFeignClient` 网关、`PgRestDirectFeignClient` 直连、`PgRestTypedClient`）
  - `examples/pgrestclient-direct`：示例（直连/Starter 综合演示）

## 按层级划分
- 数据源访问（core）：通过 `pgrest-client-core` 直接使用 `PgRestClient`，支持默认 `ObjectMapper` 与多数据源注册
- 单一数据源服务（starter）：通过 `pgrest-spring-boot-starter` 基于 `pgrest.*` 自动装配一个 `PgRestClient` 并可暴露 `/pgrest` 网关控制器
- 服务访问（feign）：通过 `pgrest-client-feign` 按约定的 `service-name` 动态声明对应的 FeignClient Bean；支持多个服务提供方的推广配置

## Maven 引入
- 核心库（非 Spring 项目或需最小依赖）：
```xml
<dependency>
  <groupId>io.github.dimon-83</groupId>
  <artifactId>pgrest-client-core</artifactId>
  <version>0.1.6</version>
</dependency>
```

- Spring Boot Starter（自动装配 PgRestClient/ObjectMapper/HttpExecutor，网关控制器，Feign 需单独引入并开关）：
```xml
<dependency>
  <groupId>io.github.dimon-83</groupId>
  <artifactId>pgrest-spring-boot-starter</artifactId>
  <version>0.1.6</version>
</dependency>
```

- 可选 Feign 模块：
```xml
<dependency>
  <groupId>io.github.dimon-83</groupId>
  <artifactId>pgrest-client-feign</artifactId>
  <version>0.1.6</version>
</dependency>
```

## Starter 配置(application.yml)
```yaml
pgrest:
  base-url: http://127.0.0.1:3000
  # JWT 签发密钥（二选一）：
  # jwt-secret: your-256bit-secret
  # secret: @Base64EncodedSecretString
  db-role: api_user
  jwt-ttl-seconds: 3600
  auth-enabled: true
  # Feign（可选模块）显式启用
  feign:
    enabled: true
  # Feign 服务名（可选），默认取 spring.application.name
  # service-name: postgrest-service
```
- `pgrest.base-url`：PostgREST 根地址
- `pgrest.db-role`：写入 JWT 的 `role`，用于 PostgREST 切换数据库角色
- `pgrest.jwt-secret` / `pgrest.secret`：签名密钥（`secret` 以 `@` 前缀表示后续为 Base64）
- `pgrest.jwt-ttl-seconds`：JWT 过期时间（秒）
- `pgrest.auth-enabled`：是否为所有请求统一注入 `Authorization: Bearer <jwt>`
- `pgrest.feign.enabled`：启用 Feign 客户端（仅在引入 `pgrest-client-feign` 时生效）
- `pgrest.service-name`：Feign 服务名，未配置时默认使用 `spring.application.name`

## 核心（core）新特性与示例（0.1.10）
- 默认 `ObjectMapper`：Snake Case + `JavaTimeModule`（时间序列化为 ISO-8601）
- 多数据源：在 `application.yml` 下声明多个数据源，自动注册命名 Bean
```yaml
pgrest:
  datasources:
    main:
      base-url: http://localhost:3000
      db-role: web_user
      secret: "@BASE64_256BIT_KEY"
      jwt-ttl-seconds: 3600
      connect-timeout-millis: 5000
      read-timeout-millis: 10000
    audit:
      base-url: http://localhost:3001
      jwt-secret: "super-secret-plain-256-bit"
      auth-enabled: true
      jwt-issuer: myapp
      jwt-audience: audit
```
```java
@Autowired @Qualifier("pgrestClient.main") PgRestClient mainClient;
@Autowired @Qualifier("pgrestClient.audit") PgRestClient auditClient;

List<Map> list = mainClient.list("users", new PgQueryBuilder().limit(5), Map.class);
```
示例参考：`examples/spring-boot-core-multids/src/main/java/io/github/dimon83/examples/coremultids/MultiDsController.java:26`

## 使用示例（PgRestClient）
```java
@Autowired
PgRestClient pgRestClient;

PgQueryBuilder builder = new PgQueryBuilder()
    .select("id,user_name,created_at")
    .eq("status", "active")
    .orderDesc("created_at")
    .limit(20);

List<UserVO> list = pgRestClient.list("users", builder, UserVO.class);
PageResult<UserVO> page = pgRestClient.page("users", new PgQueryBuilder().eq("org_id", 1001), 1, 10, UserVO.class);
UserVO one = pgRestClient.getById("users", 1L, UserVO.class);
List<UserVO> inserted = pgRestClient.insert("users", Map.of("user_name","alice"), UserVO.class);
List<UserVO> updated = pgRestClient.update("users", new PgQueryBuilder().eq("id", 1), Map.of("status","active"), UserVO.class);
int deleted = pgRestClient.delete("users", new PgQueryBuilder().eq("id", 1));
```

## QueryBuilder 能力
- 选择列：`select("col1,col2")`
- 条件：`eq/ne/gt/gte/lt/lte/like/ilike/in/isNull/notNull`
- 排序：`orderAsc/orderDesc`
- 分页：`limit/offset`

## Feign 与 Gateway（可选）
- 网关客户端：`PgRestFeignClient`（调用你的服务的 `/pgrest/{resource}` 路由）
- 直连客户端：`PgRestDirectFeignClient`（直接调用 PostgREST 根路径 `/{resource}`）
- 强类型适配：`PgRestTypedClient` 支持 `List<T>`、`PageResult<T>`，网关分页走 `/pgrest/{resource}/page`，直连分页用 Range 头

### Feign 动态客户端配置与示例（0.1.10）
- 开启动态注册，并按服务名声明多个客户端（支持 `gateway|direct` 模式、`url` 直连、`pathPrefix` 前缀）
```yaml
pgrest:
  feign:
    enabled: true
    clients:
      A:
        serviceName: a-service
        mode: gateway
      B:
        serviceName: b-service
        mode: direct
        pathPrefix: /pgrest
```
```java
@Autowired @Qualifier("pgrestFeignClient.a-service") PgRestFeignClient aGateway;
@Autowired @Qualifier("pgrestDirectFeignClient.b-service") PgRestDirectFeignClient bDirect;

ResponseEntity<List<Map<String,Object>>> a = aGateway.list("users", Map.of("select","id,user_name,status"));
ResponseEntity<List<Map<String,Object>>> b = bDirect.list("users", Map.of("select","id,user_name,status"));
```
示例参考：
- 配置：`examples/spring-boot-alicloud-crud/src/main/resources/application.yml:24`
- 代码：`examples/spring-boot-alicloud-crud/src/main/java/io/github/dimon83/examples/bootcrud/PgRestDynamicFeignExampleController.java:22`

## 备注
- 核心库默认使用 JDK `HttpClient`；可选引入 OkHttp 适配在高并发/拦截器/WebSocket 场景增强控制
- 已移除 Nacos 强绑定，若需要服务注册请使用单独扩展或在业务层实现