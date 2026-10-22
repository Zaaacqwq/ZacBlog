# Flyway 迁移脚本

命名约定：`V<版本>__<描述>.sql`，例如 `V2__add_xxx.sql`。版本号只增不改，已上线的脚本不能再修改。

脚本需要同时兼容 MySQL 8 和 H2（`MODE=MySQL`，测试 profile 使用）。

## 版本号段

| 版本 | 归属 |
| --- | --- |
| V1–V9 | T2：基线和前台/匿名评论相关 |
| V10 起 | T3：后台相关 |

## 规则

- **MySQL 排序规则**：线上库默认是 `utf8mb4_0900_ai_ci`，旧表是 `utf8mb4_general_ci`，V2+ 新建的表默认跟随库。
  SQL 里**不要拿新表的字符串列和旧表的字符串列直接比较**（`=`、`IN (子查询)`、JOIN 条件都算），否则 MySQL 报
  `Illegal mix of collations`，而 H2 测试发现不了。列和参数比较没有这个问题：先把一边查出来再作为参数传入
  （例如 `ReaderService` 的拉黑标记）。不要为此写迁移去改已有表的排序规则（违反“只能新增”）。

- `V1__baseline.sql` 是旧库（`zaaac_blog.sql`）中保留表的结构。线上库配置了 `baseline-on-migrate=true`、`baseline-version=1`，首次接入时直接记基线、跳过 V1。
- V2 及以后**只能新增**（可空列、新表、索引），不能删除或修改已有列和表，保证回滚到旧 jar 后仍能运行。
- 旧库里 `cms_blog.Top` 列名在 H2 中是关键字，SQL 里统一写成 `` `top` ``。
- 旧的 `sys_job`、`sys_dept`、`QRTZ_*` 等表 v2 不使用，也不要动。
