# pgrest-client

[English](README.en.md) | [中文](README.md)

PostgREST Java client and Spring Boot Starter. The project has been split into modules: a zero-dependency core (JDK HttpClient) and a Starter for auto-configuration with optional Feign/Gateway.

## Project Layout
- `pgrest-client` (parent, packaging `pom`)
  - `pgrest-client-core`: core library (`PgRestClient`, `PgClientConfig`, `PgQueryBuilder`, `PageResult`, HTTP abstraction and implementations: `JdkHttpExecutor`, `OkHttpExecutor`)
  - `pgrest-client-spring-boot-starter`: Starter (`PgRestAutoConfiguration`, `PgRestProperties`, optional Feign and Gateway)
  - `examples/pgrestclient-direct`: example using core/starter

## Maven Dependencies
- Core (for non-Spring apps or minimal footprint):
```xml
<dependency>
  <groupId>io.github.dimon-83</groupId>
  <artifactId>pgrest-client-core</artifactId>
  <version>0.1.3</version>
</dependency>
```

- Spring Boot Starter (auto-configures PgRestClient/ObjectMapper/HttpExecutor, optional Feign/Gateway):
```xml
<dependency>
  <groupId>io.github.dimon-83</groupId>
  <artifactId>pgrest-spring-boot-starter</artifactId>
  <version>0.1.3</version>
</dependency>
```

## Starter Configuration (application.yml)
```yaml
pgrest:
  base-url: http://127.0.0.1:3000
  # JWT secret (choose one):
  # jwt-secret: your-256bit-secret
  # secret: @Base64EncodedSecretString
  db-role: api_user
  jwt-ttl-seconds: 3600
  auth-enabled: true

pgrest:
  gateway:
    enabled: true
```
- `pgrest.base-url`: PostgREST root URL
- `pgrest.db-role`: JWT `role` claim for PostgREST role switching
- `pgrest.jwt-secret` / `pgrest.secret`: signing key (`secret` with `@` prefix means Base64-encoded key)
- `pgrest.jwt-ttl-seconds`: token TTL in seconds
- `pgrest.auth-enabled`: auto inject `Authorization: Bearer <jwt>` on all requests
- `pgrest.gateway.enabled`: enable `/api/pg/{resource}` gateway controller

## Usage (PgRestClient)
```java
@Autowired
PgRestClient pgRestClient;

PgQueryBuilder b = new PgQueryBuilder()
  .select("id,user_name,created_at")
  .eq("status","active")
  .orderDesc("created_at")
  .limit(20);
List<UserVO> list = pgRestClient.list("users", b, UserVO.class);

PageResult<UserVO> page = pgRestClient.page("users", new PgQueryBuilder().eq("org_id", 1001), 1, 10, UserVO.class);
long total = page.getTotal();
List<UserVO> rows = page.getItems();

List<UserVO> inserted = pgRestClient.insert("users", Map.of("user_name","alice"), UserVO.class);
List<UserVO> updated = pgRestClient.update("users", new PgQueryBuilder().eq("id", 1), Map.of("status","active"), UserVO.class);
int deleted = pgRestClient.delete("users", new PgQueryBuilder().eq("id", 1));

UserVO one = pgRestClient.getById("users", 1L, UserVO.class);
```

VOs should use camelCase property names (e.g., `createdAt`). Jackson `SNAKE_CASE` strategy maps `created_at` to `createdAt` automatically.

## Query Builder
- Columns: `select("col1,col2")`
- Filters: `eq/ne/gt/gte/lt/lte/like/ilike/in/isNull/notNull`
- Sorting: `orderAsc/orderDesc`
- Paging: `limit/offset`
- Build query string: `builder.build()` → `?select=...&col=eq.xxx&order=...&limit=...&offset=...`

## Feign & Gateway (optional)
- Feign clients: `PgRestFeignClient` (direct), `PgRestGatewayFeignClient` (your gateway)
- Typed adapter: `PgRestTypedClient` for `List<T>` and `PageResult<T>`, auto sets `Range-Unit: items` and `Range: start-end` and injects `limit/offset`

## Notes
- Core defaults to JDK `HttpClient`; OkHttp adapter can be used for high concurrency/interceptors/WebSocket scenarios
- Nacos integration has been removed from the core; use separate extensions if needed