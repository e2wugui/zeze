package Zeze.Transaction;

import Zeze.Serialize.ByteBuffer;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Database.checkKvKeyLength 的边界回归：eMaxKeyLength 是全后端统一的 KV key 预算，
 * 超限在入口抛含表名与长度的 IllegalArgumentException，等长放行。
 * 检查在 Database 基类内完成，不依赖真实数据库。
 */
@Fast
public class TestDatabaseCheckKvKeyLength {
	@Test
	public void testBoundary() {
		Database.checkKvKeyLength("t1", ByteBuffer.Wrap(new byte[Database.eMaxKeyLength]));

		var ex = Assertions.assertThrows(IllegalArgumentException.class,
				() -> Database.checkKvKeyLength("t1", ByteBuffer.Wrap(new byte[Database.eMaxKeyLength + 1])));
		var msg = ex.getMessage();
		Assertions.assertTrue(msg.contains("t1"), msg);
		Assertions.assertTrue(msg.contains(String.valueOf(Database.eMaxKeyLength + 1)), msg);
		Assertions.assertTrue(msg.contains(String.valueOf(Database.eMaxKeyLength)), msg);
	}
}
