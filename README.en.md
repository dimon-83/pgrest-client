# pgrest-client

[English](README.en.md) | [中文](README.md)

PostgREST Java client and Spring Boot Starter. The project is split into modules: a zero-dependency core (JDK HttpClient), a Starter for auto-configuration, and an optional standalone Feign module.

## Project Layout
- `pgrest-client` (parent, packaging `pom`)
  - `pgrest-client-core`: core library (`PgRestClient`, `PgClientConfig`, `PgQueryBuilder`, `PageResult`, HTTP abstraction: `JdkHttpExecutor`, `OkHttpExecutor`)
  - `pgrest-spring-boot-starter`: Starter (`PgRestAutoConfiguration`, `PgRestProperties`, gateway controller `/pgrest`)
  - `pgrest-client-feign`: Feign clients and typed adapter (`PgRestFeignClient` gateway, `PgRestDirectFeignClient` direct, `PgRestTypedClient`)
  - `examples/pgrestclient-direct`: example (direct/starter usage)

## Layered Architecture
- Data source access (core): use `pgrest-client-core` with `PgRestClient`; core provides a default `ObjectMapper` and supports multiple data sources registration
- Single data source service (starter): `pgrest-spring-boot-starter` auto-configures one `PgRestClient` from `pgrest.*` and can expose `/pgrest` gateway controller
- Service access (feign): `pgrest-client-feign` dynamically declares FeignClient beans per `service-name`; supports multiple providers via configuration

## Maven Dependencies
- Core (for non-Spring apps or minimal footprint):
```xml
<dependency>
  <groupId>io.github.dimon-83</groupId>
  <artifactId>pgrest-client-core</artifactId>
  <version>0.1.6</version>
</dependency>
```

- Spring Boot Starter (auto-configures PgRestClient/ObjectMapper/HttpExecutor; Feign is separate and opt-in):
```xml
<dependency>
  <groupId>io.github.dimon-83</groupId>
  <artifactId>pgrest-spring-boot-starter</artifactId>
  <version>0.1.6</version>
</dependency>
```

- Optional Feign module:
```xml
<dependency>
  <groupId>io.github.dimon-83</groupId>
  <artifactId>pgrest-client-feign</artifactId>
  <version>0.1.6</version>
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
  # Feign (optional module) explicit enable switch
  feign:
    enabled: true
  # Feign service name (optional), defaults to spring.application.name
  # service-name: postgrest-service
```
- `pgrest.base-url`: PostgREST root URL
- `pgrest.db-role`: JWT `role` claim for PostgREST role switching
- `pgrest.jwt-secret` / `pgrest.secret`: signing key (`secret` with `@` prefix means Base64-encoded key)
- `pgrest.jwt-ttl-seconds`: token TTL in seconds
- `pgrest.auth-enabled`: auto inject `Authorization: Bearer <jwt>` on all requests
- `pgrest.feign.enabled`: enable Feign clients (only effective if `pgrest-client-feign` is on the classpath)
- `pgrest.service-name`: Feign service name; defaults to `spring.application.name` when omitted

## Core new features (0.1.10)
- Default `ObjectMapper`: Snake Case + `JavaTimeModule` (dates serialized as ISO-8601)
- Multiple data sources: declare multiple sources under `application.yml` and get named beans
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
Example reference: `examples/spring-boot-core-multids/src/main/java/io/github/dimon83/examples/coremultids/MultiDsController.java:26`

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

## Prefer Header & Upsert (0.1.12)
- New `PgPrefer` builder covering `handling/timezone/return/count/resolution/missing/max-affected/tx`; all core APIs have overloads accepting a `PgPrefer`.
- Native upsert:
  - `upsertMerge(...)` merges duplicates (`Prefer: resolution=merge-duplicates`)
  - `upsertIgnore(...)` ignores duplicates (`Prefer: resolution=ignore-duplicates`)
  - Use `PgFrom.onConflict(...)` or `PgQueryBuilder.raw("on_conflict", ...)` to specify conflict target (primary key or unique/exclusion constraint).

```java
// Custom Prefer
PgPrefer prefer = PgPrefer.create()
    .handlingStrict()
    .timezone("America/Los_Angeles")
    .returnRepresentation()
    .countExact();
pgRestClient.insert("projects", Map.of("name","x"), prefer, Map.class);

// Upsert: merge on unique column
pgRestClient.from("people")
    .onConflict("email")
    .prefer(PgPrefer.create().resolutionMergeDuplicates().returnRepresentation())
    .insert(Map.of("email","a@b","name","Alice"), Map.class);

// Upsert: ignore duplicates + fill missing with DEFAULT
pgRestClient.from("orders")
    .onConflict("order_no")
    .prefer(PgPrefer.create().resolutionIgnoreDuplicates().missingDefault())
    .insert(Map.of("order_no","A1"), Map.class);
```

> Note: When specifying columns as conflict target, a matching unique constraint or unique index must exist in the database, otherwise upsert fails.

## Query Builder
- Columns: `select("col1,col2")`
- Filters: `eq/ne/gt/gte/lt/lte/like/ilike/in/isNull/notNull`
- Sorting: `orderAsc/orderDesc`
- Paging: `limit/offset`
- Build query string: `builder.build()` → `?select=...&col=eq.xxx&order=...&limit=...&offset=...`

## Feign & Gateway (optional)
- Gateway client: `PgRestFeignClient` calls your service `/pgrest/{resource}`
- Direct client: `PgRestDirectFeignClient` calls PostgREST root `/{resource}`
- Typed adapter: `PgRestTypedClient` for `List<T>` and `PageResult<T>`; gateway paging via `/pgrest/{resource}/page`, direct paging via Range headers

### Dynamic Feign clients (0.1.10)
- Enable dynamic registration and declare multiple clients by service name (`gateway|direct` modes, optional `url` for fixed direct, `pathPrefix` for direct route prefix)
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
References:
- Config: `examples/spring-boot-alicloud-crud/src/main/resources/application.yml:24`
- Code: `examples/spring-boot-alicloud-crud/src/main/java/io/github/dimon83/examples/bootcrud/PgRestDynamicFeignExampleController.java:22`

### Feign Prefer injection (0.1.12)
- Optionally provide a `PgPreferSupplier` bean to override the default Prefer; when absent, defaults to `count=exact`.
```java
@Bean
PgPreferSupplier preferSupplier() { return () -> "handling=strict,count=exact"; }
```

## Notes
- Core defaults to JDK `HttpClient`; OkHttp adapter can be used for high concurrency/interceptors/WebSocket scenarios
- Nacos integration has been removed from the core; use separate extensions if needed

## Code Coverage (JaCoCo)
- Run: `mvn clean verify` (executes tests and generates coverage reports)
- Module report: `pgrest-client-*/target/site/jacoco/index.html`
- Aggregate report: `target/site/jacoco-aggregate/index.html`
- Thresholds can be tuned in the parent POM (currently non-blocking build, `LINE` coverage minimum `0.30`; adjust as needed or enable strict checks)
