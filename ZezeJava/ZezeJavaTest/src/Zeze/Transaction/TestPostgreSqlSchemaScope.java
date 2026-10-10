package Zeze.Transaction;

import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * PG迁移幂等判定的物理元数据查询曾只按table_name过滤——多schema部署下
 * 同名表会混入他表的列集合/主键约束，tryAlter据此误判"已迁移"，跳过本表缺失列的
 * ADD。修复后两条查询都以table_schema=current_schema限定作用域（与建表DDL及
 * tableExists的to_regclass解析一致），表名走PreparedStatement参数。
 * <p>
 * 字符串契约钉板（判例：TestSetInUseConcurrentGlobalCheck对PROC_SET_IN_USE_SQL的
 * 契约断言）：真实PG集成不可得，钉住SQL形态防回归。
 */
@Fast
public class TestPostgreSqlSchemaScope {

	@Test
	public void testPhysicalColumnsSqlSchemaScoped() {
		var sql = DatabasePostgreSQL.SQL_PHYSICAL_COLUMNS;
		Assertions.assertTrue(sql.contains("information_schema.columns"), "查询列目录");
		Assertions.assertTrue(sql.contains("table_schema = current_schema"),
				"必须限定schema：只按table_name会命中所有可见schema的同名表");
		Assertions.assertTrue(sql.contains("table_name = ?"), "表名必须参数化");
		Assertions.assertFalse(sql.contains("table_catalog"), "不按catalog放大作用域");
	}

	@Test
	public void testPkConstraintSqlSchemaScoped() {
		var sql = DatabasePostgreSQL.SQL_PK_CONSTRAINT;
		Assertions.assertTrue(sql.contains("information_schema.table_constraints"), "查询约束目录");
		Assertions.assertTrue(sql.contains("table_schema = current_schema"),
				"主键约束查询同样必须限定schema（同名表跨schema混判）");
		Assertions.assertTrue(sql.contains("constraint_type = 'PRIMARY KEY'"), "只取主键约束");
		Assertions.assertTrue(sql.contains("table_name = ?"), "表名必须参数化（修复前为字符串拼接）");
	}
}
