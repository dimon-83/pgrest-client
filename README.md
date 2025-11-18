# pgrest-client

[English](README.en.md) | [中文](README.md)

PostgREST Java 客户端与 Spring Boot Starter。当前项目已按模块拆分：核心零依赖（JDK HttpClient），Starter 负责自动装配与可选的 Feign/Gateway 集成。

## 项目结构
- `pgrest-client`（父工程，packaging `pom`）
  - `pgrest-client-core`：核心库，包含 `PgRestClient`、`PgClientConfig`、`PgQueryBuilder`、`PageResult`、HTTP 抽象与实现（`JdkHttpExecutor`、`OkHttpExecutor`）
  - `pgrest-client-spring-boot-starter`：Starter，提供 `PgRestAutoConfiguration`、`PgRestProperties`，以及可选的 Feign 与 Gateway 组件
  - `examples/pgrestclient-direct`：示例，直接使用核心能力/Starter 访问 PostgREST

## Maven 引入
- 核心库（非 Spring 项目或需最小依赖）：
```xml
<dependency>
  <groupId>io.github.dimon-83</groupId>
  <artifactId>pgrest-client-core</artifactId>
  <version>0.1.3</version>
</dependency>
```

- Spring Boot Starter（自动装配 PgRestClient/ObjectMapper/HttpExecutor，并可启用 Feign/Gateway）：
```xml
<dependency>
  <groupId>io.github.dimon-83</groupId>
  <artifactId>pgrest-spring-boot-starter</artifactId>
  <version>0.1.3</version>
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

pgrest:
  gateway:
    enabled: true
```
- `pgrest.base-url`：PostgREST 根地址
- `pgrest.db-role`：写入 JWT 的 `role`，用于 PostgREST 切换数据库角色
- `pgrest.jwt-secret` / `pgrest.secret`：签名密钥（`secret` 以 `@` 前缀表示后续为 Base64）
- `pgrest.jwt-ttl-seconds`：JWT 过期时间（秒）
- `pgrest.auth-enabled`：是否为所有请求统一注入 `Authorization: Bearer <jwt>`
- `pgrest.gateway.enabled`：开启网关控制器 `/api/pg/{resource}`

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
- Feign 客户端：`PgRestFeignClient`（直连 PostgREST）、`PgRestGatewayFeignClient`（调用你的网关服务转发）
- 强类型适配：`PgRestTypedClient` 支持 `List<T>`、`PageResult<T>` 并自动处理分页头（`Range-Unit: items`、`Range: start-end`）与查询参数注入

## 备注
- 核心库默认使用 JDK `HttpClient`；可选引入 OkHttp 适配在高并发/拦截器/WebSocket 场景增强控制
- 已移除 Nacos 强绑定，若需要服务注册请使用单独扩展或在业务层实现