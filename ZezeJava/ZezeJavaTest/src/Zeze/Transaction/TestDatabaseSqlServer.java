package Zeze.Transaction;

import Zeze.Config;
import harness.Fast;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@NullMarked
public class TestDatabaseSqlServer extends JdbcDatabaseContract {
	@Override
	protected Config.DbType backend() {
		return Config.DbType.SqlServer;
	}

	@Fast
	@Test
	public void testSaveDataWithSameVersionCallPlaceholders() throws Exception {
		// 不需要真实数据库；占位符必须与存储过程的四个参数保持一致。
		var field = DatabaseSqlServer.class.getDeclaredField("saveDataWithSameVersionCall");
		field.setAccessible(true);
		var call = (String)field.get(null);
		assertEquals(4, call.split("\\?", -1).length - 1, call);
	}
}
