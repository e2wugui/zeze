package Zeze.Transaction;

import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * 所有维护 dropped 状态的 Database.Table 实现都必须将该字段声明为 volatile，
 * 确保业务/运维线程 dropTable() 后，Checkpoint 刷盘线程与其他读线程立即可见，
 * 避免向已删除的物理表/集合继续写入或通过 upsert 重建已删集合。
 */
@Fast
public class TestDatabaseTableDroppedVolatile {
	@Test
	public void testAllDatabaseTableDroppedFieldsAreVolatile() throws Exception {
		var databaseClasses = List.of(
				DatabaseMySql.class,
				DatabasePostgreSQL.class,
				DatabaseSqlServer.class,
				DatabaseMongoDb.class,
				DatabaseRocksDb.class,
				DatabaseRedis.class,
				DatabaseDynamoDb.class,
				DatabaseTikv.class,
				DatabaseMemory.class
		);

		var checkedWithDropped = new HashSet<String>();
		for (var dbClass : databaseClasses) {
			for (var inner : dbClass.getDeclaredClasses()) {
				if (!Database.Table.class.isAssignableFrom(inner))
					continue;
				try {
					var droppedField = inner.getDeclaredField("dropped");
					checkedWithDropped.add(inner.getSimpleName());
					Assertions.assertTrue(
							Modifier.isVolatile(droppedField.getModifiers()),
							inner.getName() + ".dropped 必须声明为 volatile");
				} catch (NoSuchFieldException ignored) {
					// 该后端表实现不维护 dropped 标志位
				}
			}
		}

		var expected = Set.of(
				"TableSqlServer",
				"TableMongoDb",
				"TableMysql",
				"TableMysqlRelational",
				"TablePostgreSQL",
				"TablePostgreSQLRelational"
		);
		Assertions.assertTrue(checkedWithDropped.containsAll(expected),
				"预期校验的表实现集合缺失，actual=" + checkedWithDropped);
	}
}
