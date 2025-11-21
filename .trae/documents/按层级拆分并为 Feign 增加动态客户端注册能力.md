## 分层设计
- 数据源访问：由 `pgrest-client-core` 提供 `PgRestClient` 与默认 `ObjectMapper`（snake_case + JavaTime）。
- 单一数据源服务：由 `pgrest-spring-boot-starter` 基于 `pgrest.*` 属性创建一个 `PgRestClient` 并可暴露网关控制器。
- 服务访问（多服务方）：由 `pgrest-client-feign` 提供 Feign 集成，新增“按 service-name 动态注册 FeignClient Bean”的能力，支持多个服务提供方。

## 动态 Feign 客户端（目标）
- 支持通过 `application.yml` 声明多个服务方，每个服务根据约定的 `service-name` 自动生成对应的 `FeignClient` Bean。
- 每个服务可选择访问模式：`gateway`（调用网关 `/pgrest/*`）或 `direct`（直连 PostgREST 基础路径）。
- 兼容服务发现（Nacos/Eureka/Consul）：当仅指定 `serviceName` 时使用负载均衡与发现；可选提供 `url` 用于直连。

## 属性模型
- 新增 `PgRestFeignProperties`（前缀 `pgrest.feign`）：
  - `enabled: true|false`
  - `clients: Map<String, Client>`，其中 `Client` 字段：
    - `serviceName`（必填）
    - `mode`：`gateway|direct`（默认 `gateway`）
    - `url`（可选，直连时使用，提供固定地址）
    - `pathPrefix`（可选，`direct` 模式下为服务基础路径前缀）
    - `decode404`（可选）
- 示例：
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

## 自动配置与注册
- 新增 `PgRestFeignDynamicClientsAutoConfiguration`：
  - `@AutoConfiguration`, `@EnableConfigurationProperties(PgRestFeignProperties)`
  - `@ConditionalOnProperty(prefix = "pgrest.feign", name = "enabled", havingValue = "true")`
  - `@Import(PgRestFeignDynamicClientsRegistrar.class)`
- 新增 `PgRestFeignDynamicClientsRegistrar`：
  - 读取 `pgrest.feign.clients`，为每个服务注册一个 `FeignClientFactoryBean`：
    - `name=contextId=serviceName`
    - `type`：`gateway` 使用 `PgRestFeignClient`，`direct` 使用 `PgRestDirectFeignClient`
    - `url`（若指定则直连，不走发现）
    - `path`（来自 `pathPrefix`，可为空）
    - `decode404`（可选）
  - Bean 命名：`pgrestFeignClient.<serviceName>` 或 `pgrestDirectFeignClient.<serviceName>`
- 复用 `PgRestFeignConfig` 的 `Encoder/Decoder/RequestInterceptor`，并改造为复用核心的 `pgRestObjectMapper`（若存在则使用）。

## 与现有 AutoConfiguration 的关系
- 保持现有 `PgRestFeignAutoConfiguration` 与 `@EnableFeignClients(basePackageClasses = PgRestFeignClient.class)` 不变，作为默认单服务支持。
- 当声明 `pgrest.feign.clients` 时，动态注册会额外生成多组客户端，不影响已有单客户端。

## 使用方式
- 服务提供方：在 `starter` 内配置 `pgrest.service-name` 完成注册与网关暴露（如需）。
- 服务消费方：仅引入 `pgrest-client-feign`，在 `yml` 中声明上述 `clients`，即可注入：
```java
@Autowired @Qualifier("pgrestFeignClient.A") PgRestFeignClient aGateway;
@Autowired @Qualifier("pgrestDirectFeignClient.B") PgRestDirectFeignClient bDirect;
```
- 若偏好类型化：可扩展注册 `PgRestTypedClient` 的命名 Bean（例如 `pgrestTypedClient.A`），封装两类接口的选择逻辑。

## 兼容性与安全
- 使用 `ObjectMapper` 时优先复用核心 Bean（`@ConditionalOnMissingBean` 避免冲突）。
- 遵循 Spring Cloud OpenFeign 的注册方式，确保与负载均衡和服务发现兼容。
- 不修改当前接口路径约定，避免破坏网关/直连既有路由。

## 实施步骤
1. 在 `feign` 模块新增 `PgRestFeignProperties` 与 `PgRestFeignDynamicClientsAutoConfiguration`。
2. 实现 `PgRestFeignDynamicClientsRegistrar`，用 `FeignClientFactoryBean` 按配置注册 Bean。
3. 将 `PgRestFeignConfig` 的 `ObjectMapper` 改为优先注入核心的 `pgRestObjectMapper`，若不存在再本地创建。
4. 增加使用示例与简单的上下文加载测试，验证多客户端注册与注入成功。
