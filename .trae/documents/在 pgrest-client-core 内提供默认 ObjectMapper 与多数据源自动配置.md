## 背景现状
- Starter 已声明 `ObjectMapper` Bean 并启用 `SNAKE_CASE`，但未注册 `JavaTimeModule`：`pgrest-client-starter/src/main/java/com/github/pgrest/client/PgRestAutoConfiguration.java:19-24`。
- Feign 模块也单独声明了一个 `ObjectMapper`（同样仅 `SNAKE_CASE`），并用于其编解码器。
- 核心模块 `pgrest-client-core` 目前不含 Spring Boot 自动配置；`PgRestClient` 构造签名为 `PgRestClient(PgClientConfig, HttpExecutor, ObjectMapper)`（`pgrest-client-core/src/main/java/com/github/pgrest/client/PgRestClient.java:21`），Starter 通过 `PgRestProperties` 仅支持单一数据源（`base-url`）。

## 目标与范围
1. 在 `pgrest-client-core` 内置一个默认 `ObjectMapper` Bean，配置 `SNAKE_CASE`、注册 `JavaTimeModule`，并合理的日期时间序列化设置。
2. 在仅引入 `pgrest-client-core` 的场景下，允许通过 `application.yml` 声明多个 PostgREST 数据源，并为每个数据源注册独立的 `PgRestClient` 实例 Bean。
3. 与现有 Starter/Feign 保持兼容，避免 Bean 冲突。

## 技术方案
### 默认 ObjectMapper
- 在 `pgrest-client-core` 新增 `@AutoConfiguration` 类：`PgRestCoreAutoConfiguration`。
- 声明 `@Bean ObjectMapper pgRestObjectMapper()` 并加 `@ConditionalOnMissingBean(ObjectMapper.class)`：
  - `PropertyNamingStrategies.SNAKE_CASE`
  - `registerModule(new JavaTimeModule())`
  - `disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)`
  - 可选：`configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)`
- 统一命名为 `pgRestObjectMapper`，供核心与 Feign 复用（Feign 后续改为注入该 Bean）。

### 多数据源属性模型
- 在 `pgrest-client-core` 新增属性类 `PgRestCoreProperties`（`@ConfigurationProperties(prefix="pgrest")`）：
  - 顶层保留兼容字段：`baseUrl`、`connectTimeoutMillis`、`readTimeoutMillis`、`dbRole`、`jwtSecret`、`secret`、`jwtTtlSeconds`、`authEnabled`。
  - 新增 `Map<String, DataSource> datasources`，`DataSource` 含上述同构字段，并扩展 `jwtIssuer`、`jwtAudience`、`defaultUser`、`addNbf`、`addJti`（与 `PgClientConfig` 对齐）。
- 在 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 注册该自动配置类。

### 自动注册 PgRestClient（支持多数据源）
- 在 `PgRestCoreAutoConfiguration` 中启用 `@EnableConfigurationProperties(PgRestCoreProperties.class)`。
- 当存在 `pgrest.datasources`：
  - 遍历 Map，按数据源名构造 `PgClientConfig`（复制和映射属性），并为每个数据源创建独立 `HttpClient`（使用其 `connectTimeoutMillis`）与 `JdkHttpExecutor`。
  - 以命名 Bean 注册：`@Bean(name = "pgrestClient.<name>") PgRestClient ...`
  - 额外暴露 `@Bean(name = "pgrestClients") Map<String, PgRestClient>` 以便批量注入。
- 仅存在单一 `pgrest.base-url`（且无 `datasources`）时，保持向后兼容：注册一个 `@Bean(name = "pgRestClient") PgRestClient`。

### 依赖与打包
- 在 `pgrest-client-core` 的 `pom.xml` 添加可选依赖：
  - `spring-boot-autoconfigure`（optional）
  - `spring-boot` 的 `spring-boot-configuration-processor`（optional，提供编译期元数据）
- 添加自动配置导入文件：`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`。

### 兼容性与冲突避免
- 核心的 `ObjectMapper` Bean 使用 `@ConditionalOnMissingBean`，与 Starter/Feign 共存时不产生冲突。
- 如后续调整 Feign：将 `PgRestFeignConfig` 的 `ObjectMapper` 改为注入已有 Bean，并增加 `@ConditionalOnMissingBean` 以避免重复定义。
- 单数据源旧配置仍受支持；多数据源优先级更高。

## YAML 示例
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

## 使用示例
- 注入命名单例：
```java
@Autowired @Qualifier("pgrestClient.main") PgRestClient mainClient;
@Autowired @Qualifier("pgrestClient.audit") PgRestClient auditClient;
```
- 批量注入：
```java
@Autowired @Qualifier("pgrestClients") Map<String, PgRestClient> clients;
```

## 验证与测试
- 在 `examples` 新增或复用现有 Spring Boot 示例，声明两个数据源，启动后检查：
  - 所有命名 Bean 已注册（`pgrestClient.main` / `pgrestClient.audit`）。
  - 时间字段序列化为 ISO-8601（非时间戳），`snake_case` 字段名。
- 添加一个轻量级单元测试：加载 `ApplicationContext`，断言多个 `PgRestClient` Bean 存在且属性正确。
