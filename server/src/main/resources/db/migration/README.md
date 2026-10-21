# Flyway 迁移脚本

命名约定：`V<版本>__<描述>.sql`，例如 `V1__init_schema.sql`。版本号只增不改，已上线的脚本不能再修改。

脚本需要同时兼容 MySQL 8 和 H2（`MODE=MySQL`，测试 profile 使用）。真正的建表脚本由 T2 编写。
