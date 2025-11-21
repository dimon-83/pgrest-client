# pgrest-client v0.1.10 发布说明

发布日期：2025-11-21

## 概览
- 核心（core）内置默认 `ObjectMapper`（Snake Case + JavaTime），统一时间序列化为 ISO-8601。
- 仅引入 core 时支持通过 `application.yml` 声明多个 PostgREST 数据源，并注册命名的 `PgRestClient` Bean。
- Feign 模块增加“按服务名动态注册 FeignClient”能力，支持多个服务提供方，适配网关与直连两种模式。
- 示例工程补充：新增多数据源示例；动态 Feign 客户端示例在启动后直接进行调用演示。

## 核心模块（pgrest-client-core）
### 默认 ObjectMapper
- 自动配置类：`PgRestCoreAutoConfiguration`
- 配置：
  - `PropertyNamingStrategies.SNAKE_CASE`
  - 注册 `JavaTimeModule`
  - `SerializationFeature.WRITE_DATES_AS_TIMESTAMPS` 禁用
  - `DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES` 关闭

### 多数据源支持
- 属性类：`PgRestCoreProperties`（前缀 `pgrest`），新增 `datasources: Map<String, DataSource>`
- 每个数据源支持：`baseUrl/connectTimeoutMillis/readTimeoutMillis/dbRole/jwtSecret/secret/jwtTtlSeconds/authEnabled/jwtIssuer/jwtAudience/defaultUser/addNbf/addJti`
- 自动注册命名 Bean：
  - `pgrestClient.<name>`（`PgRestClient`）
  - 相关依赖 Bean：`pgrestConfig.<name>`、`pgrestHttpClient.<name>`、`pgrestExecutor.<name>`

### YAML 与注入示例
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
```

## Feign 模块（pgrest-client-feign）
### 动态客户端注册
- 属性类：`PgRestFeignProperties`（前缀 `pgrest.feign`）
- 开关：`pgrest.feign.enabled: true`
- 多服务声明：
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
      C:
        serviceName: c-service
        mode: direct
        url: http://c-host:3000
```
- 自动注册器：`PgRestFeignDynamicClientsRegistrar`（通过 `FeignClientFactoryBean` 注册）
  - 按 `serviceName` 注册两类客户端：
    - 网关：`pgrestFeignClient.<serviceName>`（`PgRestFeignClient`）
    - 直连：`pgrestDirectFeignClient.<serviceName>`（`PgRestDirectFeignClient`）
  - 支持 `url`（直连固定地址）、`pathPrefix`（直连路径前缀）、`decode404`（可选）
  - 复用 `PgRestFeignConfig` 的编解码配置，优先使用核心的 `ObjectMapper`

### 注入与使用示例
```java
@Autowired @Qualifier("pgrestFeignClient.a-service") PgRestFeignClient aGateway;
@Autowired @Qualifier("pgrestDirectFeignClient.b-service") PgRestDirectFeignClient bDirect;
```

## 示例工程
### 新增：spring-boot-core-multids
- 仅引入 core，声明两个数据源，注入命名 `PgRestClient` 并分别调用：
  - 控制器：`MultiDsController`（`/core-multids/{resource}/main` 与 `/audit`）
  - 配置：`application.yml`（如上示例）

### 更新：spring-boot-alicloud-crud（动态 Feign）
- 新增启动即执行的调用示例（`ApplicationRunner`），使用 `aGateway` 与 `bDirect` 分别调用 `users` 资源，打印结果数量。
- 配置新增 `pgrest.feign.clients` 声明多服务。

## 兼容性与注意事项
- 与 Starter：`pgrest-spring-boot-starter` 仍提供单数据源与网关控制器，core 的默认 `ObjectMapper` 使用 `@ConditionalOnMissingBean`，不会与 Starter 冲突。
- 与现有 Feign：默认的 `PgRestFeignAutoConfiguration` 保持不变；动态注册是增量能力，不影响原有单客户端。
- 安全：未引入敏感信息；JWT 密钥长度与来源保持原约束（至少 256 bit）。

## 迁移指南（0.1.9 → 0.1.10）
- 仅使用 core：如果需要多数据源，添加 `pgrest.datasources.*` 配置并注入命名 Bean；无需其他改动。
- 使用 Feign：如需多服务消费，在 `pgrest.feign` 下开启 `enabled: true` 并声明 `clients`；在代码中注入对应命名 Bean 即可。
- 无破坏性变更；现有单数据源与单客户端配置继续有效。

