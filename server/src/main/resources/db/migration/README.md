# Flyway 迁移脚本

命名约定：`V<版本>__<描述>.sql`，例如 `V2__add_xxx.sql`。版本号只增不改，已上线的脚本不能再修改。

脚本需要同时兼容 MySQL 8 和 H2（`MODE=MySQL`，测试 profile 使用）。

## 版本号段

| 版本 | 归属 |
| --- | --- |
| V1–V9 | T2：基线和前台/匿名评论相关 |
| V10 起 | T3：后台相关 |

## 数据库专用脚本

`db/migration/` 下的脚本两种数据库都执行。确实无法兼容时（例如 MySQL 的排序规则转换），把脚本放到
`db/vendor/mysql/` 和 `db/vendor/h2/`，**两边用同一个版本号**（H2 没有对应操作时放一个等价的空操作脚本）。
`application.yml` 的 `spring.flyway.locations` 包含 `classpath:db/vendor/{vendor}`，Spring Boot 按数据源自动选目录。
不要把 vendor 目录放到 `db/migration/` 下面：Flyway 递归扫描，会扫到两份同版本脚本而报错。

| 版本 | 位置 | 说明 |
| --- | --- | --- |
| V11 | `db/vendor/{mysql,h2}/` | 黑名单表的排序规则改成与旧表一致（MySQL）；H2 为空操作 |

## 规则

- **MySQL 排序规则**：线上库默认是 `utf8mb4_0900_ai_ci`，旧表是 `utf8mb4_general_ci`，新建表默认跟随库。
  新表里要和旧表做“列 = 列”比较的字符串列，必须和旧表排序规则一致（参照 V11），否则 MySQL 报
  `Illegal mix of collations`，而 H2 测试发现不了。列和参数比较没有这个问题。

- `V1__baseline.sql` 是旧库（`zaaac_blog.sql`）中保留表的结构。线上库配置了 `baseline-on-migrate=true`、`baseline-version=1`，首次接入时直接记基线、跳过 V1。
- V2 及以后**只能新增**（可空列、新表、索引），不能删除或修改已有列和表，保证回滚到旧 jar 后仍能运行。
- 旧库里 `cms_blog.Top` 列名在 H2 中是关键字，SQL 里统一写成 `` `top` ``。
- 旧的 `sys_job`、`sys_dept`、`QRTZ_*` 等表 v2 不使用，也不要动。
