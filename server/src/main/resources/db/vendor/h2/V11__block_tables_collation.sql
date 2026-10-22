-- 对应 db/vendor/mysql/V11__block_tables_collation.sql。H2 不区分列的排序规则，不需要转换；
-- 保留同一版本号，让两种数据库的迁移历史一致。
SELECT 1;
