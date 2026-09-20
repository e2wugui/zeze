package UnitTest.Zeze.Transaction;

import static org.junit.jupiter.api.Assertions.assertEquals;

import Zeze.Config;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.DatabaseMemory;
import harness.Fast;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * T1-F3 回归：walk/walkKey 的统一契约——"被回调且返回 false 的中断记录计入返回值"
 * （Database.AbstractKVTable.walk 契约注释）。原先 DynamoDb（及 Redis）先判后自增，
 * 中断时计数比其他后端少 1。本测试在 Memory 后端上固化该契约；
 * DynamoDb/Redis 后端需外部服务，无法在本测试环境验证（见台账）。
 */
@Fast
public class TestWalkInterruptCountContract {
	private DatabaseMemory db;

	@BeforeEach
	public void setUp() throws Exception {
		var conf = new Config.DatabaseConf();
		conf.setDatabaseType(Config.DbType.Memory);
		conf.setDatabaseUrl("t1_f3_walk_interrupt_count");
		db = new DatabaseMemory(null, conf);
		var table = (DatabaseMemory.TableMemory)db.openTable("t1", 0);
		// 插入 5 条：key 1..5（TreeMap 按 ByteBuffer 字典序，单字节即数值序）。
		var txn = db.beginTransaction();
		for (var k = 1; k <= 5; k++)
			table.replace(txn, ByteBuffer.Wrap(new byte[] {(byte)k}), ByteBuffer.Wrap(new byte[] {(byte)k}));
		txn.commit();
		txn.close();
	}

	@AfterEach
	public void tearDown() {
		db.close();
		// 不调 DatabaseMemory.clear()：类并行下它会清掉其他类正在使用的静态表，
		// 固定 key 的 replace 语义幂等，无需全局清理。
	}

	@Test
	public void testWalkInterruptItemCounted() throws Exception {
		var table = (DatabaseMemory.TableMemory)db.openTable("t1", 0);
		// 第 3 条（key=3）回调返回 false 中断：中断项计入，共遍历 3 条。
		var count = table.walk((key, value) -> key[0] != 3);
		assertEquals(3, count, "walk 中断项必须计入返回值");
	}

	@Test
	public void testWalkKeyInterruptItemCounted() throws Exception {
		var table = (DatabaseMemory.TableMemory)db.openTable("t1", 0);
		var count = table.walkKey(key -> key[0] != 4);
		assertEquals(4, count, "walkKey 中断项必须计入返回值");
	}

	@Test
	public void testWalkNoInterrupt() throws Exception {
		var table = (DatabaseMemory.TableMemory)db.openTable("t1", 0);
		assertEquals(5, table.walk((key, value) -> true));
		assertEquals(5, table.walkKey(key -> true));
	}
}
