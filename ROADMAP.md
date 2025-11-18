# pgrest-client 安全与功能增强路线图

## 目标
- 将 pgrest-client 定位为“仅内部使用、短效令牌、最小权限”的安全访问层，介于后台服务与 PostgREST/数据库之间。
- 在不破坏现有能力的前提下，逐步提升令牌签发安全性、声明完整性、密钥治理与网络防护。

## 优先级 P0（尽快完成）
- 非对称签名（RS256/ES256）
  - 在 `PgRestClient` 增加算法选择与私钥加载（支持 `kid`），默认从 HS256 迁移到 RS256。
  - PostgREST 改用公钥/JWK/JWKS 验证；支持多公钥轮换。
- 完整声明与短效 TTL
  - 自动填充 `exp(≤300s)`、`iat`、`nbf`、`iss`（服务标识）、`aud`（PostgREST）、`jti`（唯一 ID）。
  - 在签发侧增加 `role` 白名单校验（禁止高危角色）。
- 角色最小权限与 RLS
  - 数据库端按租户/部门声明实施 RLS；应用角色仅授予必要权限。
- 构建签名隔离
  - Maven GPG 仅在发布时启用（已完成：父 POM `signing` profile）。

## 优先级 P1（近期完成）
- 密钥治理与轮换
  - 支持从 KMS/Vault 加载私钥；实现无重启轮换与双钥过渡（`kid`）。
  - 在 Starter 中增加密钥源选择与刷新机制。
- 网络与运行态控制
  - 在内网或 mTLS 保护下访问 PostgREST；对外统一经网关鉴权与审计。
  - 禁止令牌落地与日志输出；敏感头屏蔽。
- 令牌供应策略
  - `AuthTokenSupplier` 扩展：按请求上下文动态生成，支持租户/部门映射；默认短效缓存（避免频繁签发）。

## 优先级 P2（中期完成）
- PostgREST 配置兼容性
  - 配置 `jwt-aud` 与 JWKS（含缓存与失效策略）；对多环境（dev/prod）区分密钥与 `iss/aud`。
- 可观测与审计
  - 令牌签发计数/失败指标、`kid` 命中率、RLS 拒绝统计；请求头屏蔽日志。
- OkHttp 适配增强
  - 提供连接池参数、并发 Dispatcher、拦截器（重试/日志/鉴权）、WebSocket 支持。

## 里程碑
- M1：RS256/ES256 + 完整声明（`iss/aud/nbf/jti`）+ TTL 下调到 300 秒；角色白名单；数据库 RLS；发布验证（2 周）。
- M2：KMS/Vault 集成 + 密钥轮换（`kid`）+ JWKS 拉取/缓存（2–3 周）。
- M3：网络与运行态加固 + 可观测性 + OkHttp 增强（3–4 周）。

## 技术实现纲要
- Core（pgrest-client-core）
  - 新增 `JwtAlgorithm` 与 `KeyProvider` 接口；实现 `HS256KeyProvider`、`RS256KeyProvider`。
  - `issueJwt` 增加 `kid` 与声明构建器；TTL 默认 60–300 秒；`role` 白名单校验。
  - `AuthTokenSupplier` 支持上下文感知（租户/部门），并带轻量缓存。
- Starter（pgrest-spring-boot-starter）
  - 新增属性：`pgrest.jwt.alg`、`pgrest.jwt.kid`、`pgrest.jwt.jwks-url`、`pgrest.jwt.iss`、`pgrest.jwt.aud`、`pgrest.roles.allowed`。
  - 提供 KMS/Vault 集成点（SPI/Starter 配置）；提供 OkHttp 选择开关与拦截器注册。
- 数据库与 PostgREST
  - 数据库：为表/视图配置 RLS，策略依赖令牌 `tenant_id/dept_id`；最小权限应用角色。
  - PostgREST：使用 JWKS（含轮换），设置 `jwt-aud` 验证；隔离网络访问。

## 风险与缓解
- 共享密钥泄露风险 → 迁移 RS256/ES256，公钥公开，私钥托管。
- 令牌重放风险 → 使用 `nbf/jti`，短 TTL；在网关层增加重放检查。
- 高危角色越权 → `roles.allowed` 白名单 + 数据库最小权限。

## 发布与回滚
- 发布流程：
  - 在 dev 环境启用 RS256 与 `kid` 双钥；完成 JWKS 发布。
  - 核心/Starter 升级后，灰度到一部分服务；监控失败率与 RLS 拒绝比例。
- 回滚策略：
  - 保留 HS256 路径与回退开关；确保双钥在过渡期间可用。

## 参考配置（Starter）
```yaml
pgrest:
  base-url: http://127.0.0.1:3000
  jwt:
    alg: RS256
    kid: key-2025-01
    iss: pgrest-client
    aud: postgrest
    jwks-url: https://issuer.example.com/.well-known/jwks.json
  roles:
    allowed:
      - api_user
      - reader_user
  auth-enabled: true
  jwt-ttl-seconds: 300
```

## 进度追踪
- 已完成：构建签名隔离（`-P signing` 发布时签名）。
- 待办：RS256/ES256 支持、声明完善、KMS/Vault 集成、JWKS、RLS 样例与文档、OkHttp 增强。