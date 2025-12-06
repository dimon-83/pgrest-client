# 0.1.12 发布说明

## 新特性
- Prefer Header 全面支持：新增 `PgPrefer` 构建器，覆盖 `handling`、`timezone`、`return`、`count`、`resolution`、`missing`、`max-affected`、`tx` 等偏好，所有核心 API 提供接收 `PgPrefer` 的重载。
- 原生 Upsert：
  - `PgRestClient.upsertMerge(...)` → `Prefer: resolution=merge-duplicates`
  - `PgRestClient.upsertIgnore(...)` → `Prefer: resolution=ignore-duplicates`
  - 通过 `PgFrom.onConflict(...)` 或 `PgQueryBuilder.raw("on_conflict", ...)` 指定冲突目标（主键或唯一/排除约束）。
- Fluent 扩展：`PgFrom.prefer(PgPrefer)` 会话态应用 Prefer；`PgFrom.onConflict(...)` 生成 `?on_conflict=` 查询参数。
- Feign 增强：新增可选 `PgPreferSupplier`，用于覆盖默认 Prefer 头；未提供时保持 `count=exact`。

## 兼容性
- 旧接口与默认行为不变：
  - 读操作默认 `Prefer: count=exact`
  - 写操作默认 `Prefer: return=representation,count=exact`
  - RPC 默认 `Prefer: count=exact`
- 新能力通过重载与便捷方法提供，零侵入。

## 使用示例
```java
// 自定义 Prefer
PgPrefer prefer = PgPrefer.create()
    .handlingStrict()
    .timezone("America/Los_Angeles")
    .returnRepresentation()
    .countExact();
client.insert("projects", payload, prefer, Project.class);

// Upsert（按唯一列合并）
client.from("people")
      .onConflict("email")
      .prefer(PgPrefer.create().resolutionMergeDuplicates().returnRepresentation())
      .insert(person, Person.class);

// Upsert（忽略重复 + 缺失列用 DEFAULT）
client.from("orders")
      .onConflict("order_no")
      .prefer(PgPrefer.create().resolutionIgnoreDuplicates().missingDefault())
      .insert(order, Order.class);
```

## 注意事项
- 指定列作为冲突目标时，数据库必须存在匹配的唯一约束或唯一索引（或主键）；否则 PostgREST/数据库会报错。
- `resolution=merge-duplicates` 等价 `ON CONFLICT ... DO UPDATE`；`resolution=ignore-duplicates` 等价 `ON CONFLICT ... DO NOTHING`。

## 测试
- 新增并完善单元测试，覆盖 Prefer 组合生成、核心 API Prefer 重载、Upsert 的 `resolution` 与 `on_conflict`、`PgFrom` 会话态与语法糖、过滤器生效与异常路径等，确保行为稳定。

