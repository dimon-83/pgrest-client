## 目标
- 提供可运行的示例代码，展示：
  1) 仅引入 `pgrest-client-core` 通过 `yml` 声明多个数据源并注入命名 `PgRestClient` Bean；
  2) 在现有 Spring Boot 示例中展示基于 `service-name` 的 Feign 动态客户端注册与注入使用。

## 示例模块与改动
- 新增 `examples/spring-boot-core-multids`：
  - 依赖：`spring-boot-starter-web`、`pgrest-client-core`（使用核心自动配置）；
  - `application.yml`：声明 `pgrest.datasources.main` 和 `pgrest.datasources.audit`；
  - 控制器：注入 `@Qualifier("pgrestClient.main")` / `@Qualifier("pgrestClient.audit")`，演示 `list/page/insert/update/delete` 基本用法。
- 扩展 `examples/spring-boot-alicloud-crud`：
  - 在 `application.yml` 增加 `pgrest.feign.enabled: true` 与 `pgrest.feign.clients`（多个服务）；
  - 新增控制器 `PgRestDynamicFeignExampleController`：
    - 注入 `@Qualifier("pgrestFeignClient.A") PgRestFeignClient`（网关）
    - 注入 `@Qualifier("pgrestDirectFeignClient.B") PgRestDirectFeignClient`（直连）
    - 演示 `list/insert/update/delete` 调用；

## 说明与验证
- 示例不改动父模块的构建顺序，保持与现有 `examples` 一致（独立构建即可）。
- 示例代码按约定路径与 Bean 命名，确保直接可用；若无真实 PostgREST 服务，示例仍可编译运行，调用路径可按需替换。
- 保持不创建文档文件，仅提供代码与 `application.yml` 配置示例。