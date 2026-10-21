# Flyway 迁移脚本

命名约定：`V<版本>__<描述>.sql`，例如 `V2__add_xxx.sql`。版本号只增不改，已上线的脚本不能再修改。

脚本需要同时兼容 MySQL 8 和 H2（`MODE=MySQL`，测试 profile 使用）。

## 版本号段

| 版本 | 归属 |
| --- | --- |
| V1–V9 | T2：基线和前台/匿名评论相关 |
| V10 起 | T3：后台相关 |

## 规则

- `V1__baseline.sql` 是旧库（`zaaac_blog.sql`）中保留表的结构。线上库配置了 `baseline-on-migrate=true`、`baseline-version=1`，首次接入时直接记基线、跳过 V1。
- V2 及以后**只能新增**（可空列、新表、索引），不能删除或修改已有列和表，保证回滚到旧 jar 后仍能运行。
- 旧库里 `cms_blog.Top` 列名在 H2 中是关键字，SQL 里统一写成 `` `top` ``。
- 旧的 `sys_job`、`sys_dept`、`QRTZ_*` 等表 v2 不使用，也不要动。
