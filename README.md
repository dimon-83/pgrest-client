# pgrest-client-spring-boot-starter

快速将 PostgREST 服务注册到 Nacos，并提供高效的 PostgREST 访问与查询条件构造能力。

## 组件关系
- `spring-boot-autoconfigure`: 提供自动装配，暴露 `PgRestClient`、`ObjectMapper`、`HttpClient` 等 Bean。
- `spring-cloud-starter-alibaba-nacos-discovery`: 用于将外部的 PostgREST 实例注册为 Nacos 服务实例。
- `jackson-databind`: 负责 JSON 映射，默认使用 `SNAKE_CASE` 命名策略，将下划线字段映射到 VO 的驼峰属性。

## 引入方式
- Maven 依赖：
```xml
<dependency>
  <groupId>com.github.pgrest</groupId>
  <artifactId>pgrest-client-spring-boot-starter</artifactId>
  <version>0.1.0</version>
</dependency>
```

- Spring Boot 3 自动装配通过 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 生效，无需额外配置。

## 必要配置(application.yml)
```yaml
spring:
  application:
    name: demo-app
  cloud:
    nacos:
      discovery:
        server-addr: 127.0.0.1:8848
        namespace: public
        group: DEFAULT_GROUP

pgrest:
  base-url: http://127.0.0.1:3000
  service-name: postgrest-service
  group: DEFAULT_GROUP
  register-to-nacos: true
  connect-timeout-millis: 5000
  read-timeout-millis: 10000
  metadata:
    version: v1
    owner: data-team
```

- `pgrest.base-url`: PostgREST 根地址
- `pgrest.service-name`: 在 Nacos 中的服务名
- `pgrest.group`: 注册到的 Nacos 分组，缺省取 `spring.cloud.nacos.discovery.group`
- `pgrest.register-to-nacos`: 启用注册

## 使用示例
- 注入与查询：
```java
@Autowired
PgRestClient pgRestClient;

PgQueryBuilder builder = new PgQueryBuilder()
        .select("id,name,created_at")
        .eq("status", "active")
        .orderDesc("created_at")
        .limit(20);

List<UserVO> list = pgRestClient.list("users", builder, UserVO.class);

UserVO one = pgRestClient.getById("users", 1L, UserVO.class);
UserVO one2 = pgRestClient.getById("users", "user_id", 1L, new PgQueryBuilder().select("id,user_name"), UserVO.class);
```

- 分页查询：
```java
PageResult<UserVO> page = pgRestClient.page("users", new PgQueryBuilder().eq("org_id", 1001), 1, 10, UserVO.class);
long total = page.getTotal();
List<UserVO> rows = page.getRecords();
```

`UserVO` 的属性使用驼峰命名，如 `createdAt`，会自动由 PostgREST 的 `created_at` 字段映射。

## QueryBuilder 能力
- 选择列：`select("col1,col2")`
- 条件：`eq/ne/gt/gte/lt/lte/like/ilike/in/isNull/notNull`
- 排序：`orderAsc/orderDesc`
- 分页：`limit/offset`

## Nacos 注册行为
- 启动后按照 `pgrest.base-url` 解析出 `ip:port`，以 `pgrest.service-name` 注册到指定 `group`。
- 应用停止时自动注销实例。

## 依赖版本
- Spring Boot: 3.2.x
- Spring Cloud Alibaba Nacos: 2023.0.1.0
 - Spring Cloud OpenFeign: 2023.0.4

## 线程与资源占用
- 使用 JDK `HttpClient`，连接池与异步能力由 JDK 管理，资源占用低，适合作为轻量转发客户端。

## 对外暴露（Feign Client）
- Starter 自动启用 `@EnableFeignClients` 并注册两类 Feign 客户端：
  - 直接访问 PostgREST：`PgRestFeignClient`（name 使用 `${pgrest.service-name}`）
  - 访问当前服务网关：`PgRestGatewayFeignClient`（name 使用 `${pgrest.gateway-service-name}`）
  两者均带默认头 `Accept: application/json`、`Prefer: count=exact`，并使用 `SNAKE_CASE` ObjectMapper。

示例（在调用方服务）
```java
@Autowired PgRestGatewayFeignClient gateway;
ResponseEntity<List<Map<String,Object>>> resp = gateway.list("users", Map.of("order","created_at.desc","limit","10"));
List<Map<String,Object>> rows = resp.getBody();
```

如需返回强类型 `List<UserVO>`，推荐在调用方定义资源专属接口：
```java
@FeignClient(name = "your-gateway-service-name", configuration = PgRestFeignConfig.class, contextId = "usersFeign")
public interface UsersFeign {
  @GetMapping("/users")
  List<UserVO> list(@RequestParam Map<String,String> query);
}
```

或使用 Typed 适配器：
```java
@Autowired PgRestTypedClient typed;
List<UserVO> users = typed.list("users", Map.of("order","created_at.desc"), UserVO.class);
List<UserVO> inserted = typed.insert("users", Map.of("user_name","alice"), Map.of(), UserVO.class);
List<UserVO> updated = typed.update("users", Map.of("status","active"), Map.of("id","eq.1"), UserVO.class);
int deleted = typed.delete("users", Map.of("id","eq.1"));
PageResult<UserVO> page = typed.page("users", Map.of("order","created_at.desc"), 1, 10, UserVO.class);
```

分页请求头
- Typed 适配器在分页时会自动设置 `Range-Unit: items` 与 `Range: start-end`，并同步注入 `limit/offset` 查询参数，确保 PostgREST 返回 `Content-Range` 用于总数统计。

## 网关控制器
- 自动启用网关：设置 `pgrest.gateway.enabled: true`（缺省即为 true）
- 路径：`/api/pg/{resource}`，内部使用 `PgRestClient` 转发到 PostgREST，并支持 Map 形式查询参数

示例配置（application.yml）
```yaml
pgrest:
  base-url: http://127.0.0.1:3000
  service-name: postgrest-service
pgrest:
  gateway:
    enabled: true
pgrest:
  gateway-service-name: your-gateway-service-name
```