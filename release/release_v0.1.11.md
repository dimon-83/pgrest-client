# pgrest-client v0.1.11 发布说明

发布日期：2025-11-26

## 概览
- 新增链式 RPC 调用：`client.fn(function)` / `client.rpc(function)`，支持 `params/select/eq/order/limit/single/object/invokeVoid`。
- 新增多 schema Profile 头支持：`setProfile(schema)` 一次性设置读/写 Profile，读请求自动加 `Accept-Profile`，写/变更/RPC 自动加 `Content-Profile`。
- 新增 `groupBy` 查询能力与链式方法，构建 `group=<columns>` 查询参数。
- 移除客户端侧 `distinct/distinctOn` 生成，避免与服务端实现不一致；建议使用视图或 RPC 在服务端进行去重与规范化。

## 新增功能
### 链式 RPC（pgrest-client-core）
- 新类：`PgFn`，提供 RPC 的链式构建与执行：
  - 构造：`client.fn("function")` 或别名 `client.rpc("function")`
  - 传参：`params(Object payload)`（命名参数 Map 或 POJO）
  - 查询：`select/eq/ne/gt/gte/lt/lte/like/ilike/in/isNull/notNull/orderAsc/orderDesc/groupBy/limit/offset/raw`
  - 执行：
    - `list(Class<T>)`（返回列表）
    - `single(Class<T>)`（限制 1 条并返回首项）
    - `object(Class<T>)`（函数直接返回对象体）
    - `invokeVoid()`（无返回过程）
- 客户端入口：`PgRestClient.fn(String)` 与 `PgRestClient.rpc(String)`。
- RPC 请求自动携带认证与 Profile 头（详见下文）。

### 多 schema Profile 头支持
- 新增方法：
  - `PgRestClient.setProfile(String schema)`：同时设置读/写 Profile，支持链式返回 `PgRestClient`
  - 亦保留 `setReadProfile(String)` 与 `setWriteProfile(String)`（同样返回 `PgRestClient`）
- 头注入行为：
  - 读（GET）：`Accept-Profile: <readProfile>`
  - 写/变更/RPC（POST/PATCH/DELETE/RPC）：`Content-Profile: <writeProfile>`
- 作用范围：`from(...)` 所有查询/写操作与 `fn/rpc(...)` 的 RPC 调用。

### 查询构造器增强
- 新增 `groupBy(String... columns)`：
  - 构造器：`PgQueryBuilder.groupBy(...)` 输出 `group=<encoded columns>`
  - 链式：`PgFrom.groupBy(...)`、`PgFn.groupBy(...)`

## 使用示例
### 登录（返回单对象）
```java
Map<String,Object> args = Map.of("user_email", email, "user_password", password);
User user = client.setProfile("security")
    .rpc("user_login")
    .params(args)
    .select("user_id,name,email,role")
    .single(User.class);
```

### 搜索用户（返回列表）
```java
List<User> users = client.setProfile("directory")
    .rpc("search_users")
    .params(criteria)
    .select("id,name,email,created_at")
    .eq("is_active", true)
    .orderDesc("created_at")
    .limit(50)
    .list(User.class);
```

### 无返回过程（RETURNS void）
```java
Map<String,Object> payload = Map.of(
    "payloads", Map.of("N","sensor_001","V","12.34","T","2025-11-25T10:00:00Z","Q","1","Err","")
);
client.setProfile("ia_csc")
    .rpc("upsert_ia_node_value")
    .params(payload)
    .invokeVoid();
```

### 分组聚合示例
```java
List<Map> rows = client.setProfile("analytics")
    .from("metrics")
    .select("group_name,count:count(id)")
    .groupBy("group_name")
    .list(Map.class);
```

## 兼容性与迁移指南（0.1.10 → 0.1.11）
- 无破坏性变更：新增功能均为增量；现有查询与写操作不受影响
- 多 schema 建议：
  - 统一使用 `setProfile(schema)` 简化路径与头部设置，避免在资源名/函数名中重复书写 schema 前缀

## 注意事项
- RPC 命名参数需与函数签名一致（例如：`payloads jsonb` 需传 `Map.of("payloads", ...)`）
- 当服务端关闭匿名访问时，需携带有效认证头，否则会返回 `PGRST302`（如：`{"code":"PGRST302","message":"Anonymous access is disabled"}`）
- 安全与序列化行为同 0.1.10：默认 `ObjectMapper` 与 JWT 相关约束保持不变

