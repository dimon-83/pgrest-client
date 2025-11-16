# pgrest-client-spring-boot-starter

[English](README.en.md) | [中文](README.md)

A lightweight Spring Boot Starter to register a PostgREST service into Nacos and to access PostgREST efficiently with typed mappings and a fluent query builder.

## Overview
- Registers an external PostgREST instance to Nacos via Spring Cloud Alibaba
- Provides `PgRestClient` backed by JDK `HttpClient` (low resource footprint)
- Maps snake_case JSON fields to Java VO camelCase properties via Jackson
- Builder-style query construction for PostgREST (`eq`, `like`, `order`, `limit`, `offset`, `select`)
- Feign support: direct PostgREST and gateway routing, with optional typed adapter

## Components
- `PgRestAutoConfiguration`: auto-configures `ObjectMapper`, `HttpClient`, `PgRestClient`, optional Nacos registrar and gateway controller
- `PgRestClient`: CRUD + paging + getById
- `PgQueryBuilder`: fluent query builder
- `PgRestNacosRegistrar`: registers PostgREST IP/port as a Nacos instance
- Feign clients and config:
  - `PgRestFeignClient`: direct PostgREST by service name
  - `PgRestGatewayFeignClient`: call your gateway service and forward to PostgREST
  - `PgRestTypedClient`: typed adapter for Feign responses (`List<T>`, `PageResult<T>`)

## Quick Start
Add dependency in your application:
```xml
<dependency>
  <groupId>io.github.dimon-83</groupId>
  <artifactId>pgrest-client-spring-boot-starter</artifactId>
  <version>0.1.1-SNAPSHOT</version>
</dependency>
```

Spring Boot 3 auto-configuration is enabled via `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.

## Configuration (application.yml)
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

pgrest:
  gateway:
    enabled: true
pgrest:
  gateway-service-name: demo-app
```

- `pgrest.base-url`: PostgREST root URL
- `pgrest.service-name`: service name in Nacos for the external PostgREST
- `pgrest.gateway.enabled`: expose `/api/pg/{resource}` endpoints via gateway controller
- `pgrest.gateway-service-name`: gateway service name for Feign consumers

## Usage (PgRestClient)
```java
@Autowired
PgRestClient pgRestClient;

// List with select, filter and order
PgQueryBuilder b = new PgQueryBuilder()
  .select("id,user_name,created_at")
  .eq("status","active")
  .orderDesc("created_at")
  .limit(20);
List<UserVO> list = pgRestClient.list("users", b, UserVO.class);

// Paging (reads Content-Range for total)
PageResult<UserVO> page = pgRestClient.page("users", new PgQueryBuilder().eq("org_id", 1001), 1, 10, UserVO.class);
long total = page.getTotal();
List<UserVO> rows = page.getRecords();

// Insert, Update, Delete
List<UserVO> inserted = pgRestClient.insert("users", Map.of("user_name","alice"), UserVO.class);
List<UserVO> updated = pgRestClient.update("users", new PgQueryBuilder().eq("id", 1), Map.of("status","active"), UserVO.class);
int deleted = pgRestClient.delete("users", new PgQueryBuilder().eq("id", 1));

// Get by id
UserVO one = pgRestClient.getById("users", 1L, UserVO.class);
```

VOs should use camelCase property names (e.g., `createdAt`). Jackson `SNAKE_CASE` strategy maps `created_at` to `createdAt` automatically.

## Query Builder
- Column selection: `select("col1,col2")`
- Filters: `eq/ne/gt/gte/lt/lte/like/ilike/in/isNull/notNull`
- Sorting: `orderAsc/orderDesc`
- Paging: `limit/offset`
- Build query string: `builder.build()` → `?select=...&col=eq.xxx&order=...&limit=...&offset=...`

## Feign Options
- Direct PostgREST (`PgRestFeignClient`)
  - Name: `${pgrest.service-name}`
  - Returns `ResponseEntity<List<Map<String,Object>>>`
- Gateway Feign (`PgRestGatewayFeignClient`)
  - Name: `${pgrest.gateway-service-name}`
  - Calls `/api/pg/{resource}` on your service, then forwards to PostgREST
- Typed Adapter (`PgRestTypedClient`)
  - Strongly-typed conversions for `List<T>` and `PageResult<T>`
  - Example:
    ```java
    @Autowired PgRestTypedClient typed;
    List<UserVO> users = typed.list("users", Map.of("order","created_at.desc"), UserVO.class);
    PageResult<UserVO> p = typed.page("users", Map.of(), 2, 10, UserVO.class);
    ```
  - For pagination it automatically sets `Range-Unit: items` and `Range: start-end`, and injects `limit/offset` query parameters

## Gateway Controller
- Exposes `/api/pg/{resource}` in your service, backed by `PgRestClient`
- Accepts query parameters as `Map<String,String>` and supports range headers for pagination

## Examples
- Direct client example:
  - `examples/pgrestclient-direct/`
  - Run after installing the starter: `mvn -q -f examples/pgrestclient-direct exec:java`
- Spring Boot + Alibaba Nacos CRUD:
  - `examples/spring-boot-alicloud-crud/`
  - Start: `mvn -q -f examples/spring-boot-alicloud-crud spring-boot:run`
  - Routes: `/users` and `/feign/users` demonstrate `PgRestClient` and Feign usage

## Dependencies
- Spring Boot: 3.2.x
- Spring Cloud Alibaba Nacos: 2023.0.1.0
- Spring Cloud OpenFeign: 2023.0.4
- Jackson Databind
- JDK HttpClient (Java 17)

## Nacos Registration
- On startup, the gateway registers the external PostgREST instance (`pgrest.base-url`) into Nacos using `pgrest.service-name`
- On shutdown, it deregisters the instance

## License
- Apache License 2.0

## Publishing (optional)
- Uses OSSRH `s01.oss.sonatype.org` for snapshots and releases
- Provide Deployer Token via Maven `settings.xml` or GitHub Actions Secrets