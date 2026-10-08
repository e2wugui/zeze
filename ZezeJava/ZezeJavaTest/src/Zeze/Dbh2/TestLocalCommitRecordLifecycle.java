package Zeze.Dbh2;

import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Dbh2.Commit;
import Zeze.Dbh2.CommitRocks;
import Zeze.Dbh2.Database;
import Zeze.Dbh2.Dbh2AgentManager;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.KV;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND19 GA-C03/GA-C07/GA-C08回归（共享一次Dbh2TestEnv组网启动）：
 *
 * GA-C03：CommitRocks的commitPoint与commitIndex同批写入，事务完结必须同批删除
 *（bug时只删commitIndex，本地提交默认模式下每个客户端事务在协调者rocks中留下
 * 一条永久记录，磁盘无界增长）。钉住：一次客户端事务commit后两个column family均为空。
 *
 * GA-C07：getDataWithVersion仅对eDataNotExists返回null（bug时一切rpc错误都当
 * "无数据"，调用方以默认值+version=0写回可覆写全局数据）。钉住正常save/get往返
 * 与"不存在→null"契约。（错误码→抛异常分支需故障注入，代码级核实见报告。）
 *
 * GA-C08：非prefix路径openTable的实例不入tables共享map，其close()不能删map条目
 *（bug时把prefix路径注册的同名共享实例摘除，引用计数与map错位级联）。
 */
public class TestLocalCommitRecordLifecycle {

	private static int countEntries(CommitRocks rocks, String fieldName) throws Exception {
		Field field = CommitRocks.class.getDeclaredField(fieldName);
		field.setAccessible(true);
		var table = (Zeze.Util.RocksDatabase.Table)field.get(rocks);
		try (var it = table.iterator()) {
			int count = 0;
			for (it.seekToFirst(); it.isValid(); it.next())
				++count;
			return count;
		}
	}

	// Dbh2AgentManager.commit（本地提交模式下的协调者Commit实例）。
	private static Commit getCommit(Dbh2AgentManager manager) throws Exception {
		Field field = Dbh2AgentManager.class.getDeclaredField("commit");
		field.setAccessible(true);
		return (Commit)field.get(manager);
	}

	@SuppressWarnings("unchecked")
	private static ConcurrentHashMap<String, Database.Dbh2Table> getTablesMap(Database database) throws Exception {
		Field field = Database.class.getDeclaredField("tables");
		field.setAccessible(true);
		return (ConcurrentHashMap<String, Database.Dbh2Table>)field.get(database);
	}

	@Test
	public void testLocalCommitRecordsAndLifecycle() throws Exception {
		var env = new Dbh2TestEnv();
		env.prepareNewEnvironment();
		try {
			// === GA-C03：客户端事务完结后，协调者的commitPoint/commitIndex必须都已清空 ===
			var table1 = env.tables.getFirst();
			var key = ByteBuffer.Wrap(new byte[]{1});
			try (var trans = env.database.beginTransaction()) {
				table1.replace(trans, key, env.value);
				trans.commit();
			}
			var rocks = getCommit(env.dbh2AgentManager).getRocks();
			Assertions.assertEquals(0, countEntries(rocks, "commitIndex"),
					"commitIndex must be cleaned after commit");
			Assertions.assertEquals(0, countEntries(rocks, "commitPoint"),
					"commitPoint must be cleaned together with commitIndex (bug: grows unbounded)");

			// === GA-C07：save/get往返与"不存在→null" ===
			var operates = env.database.getDirectOperates();
			var gkey = ByteBuffer.Wrap("localCommitKey".getBytes());
			KV<Long, Boolean> saved = operates.saveDataWithSameVersion(gkey, ByteBuffer.Wrap(new byte[]{1, 2}), 0);
			Assertions.assertTrue(saved.getValue());
			var loaded = operates.getDataWithVersion(gkey);
			Assertions.assertNotNull(loaded);
			Assertions.assertEquals(0, loaded.version); // 首次插入以参数version落盘
			Assertions.assertEquals(2, loaded.data.size());
			Assertions.assertNull(operates.getDataWithVersion(ByteBuffer.Wrap("fnd19ga07missing".getBytes())),
					"only eDataNotExists maps to null");

			// === GA-C08：非prefix实例close()不删共享map条目 ===
			var tablesMap = getTablesMap(env.database);
			var prefix = env.database.openTable("x___table0", 123); // 注册共享实例，ref=1
			Assertions.assertTrue(tablesMap.containsKey("table0"));
			var shared = tablesMap.get("table0");
			var plain = (Database.Dbh2Table)env.database.openTable("table0", 456); // 不入map
			plain.close(); // bug时把shared从map摘除
			Assertions.assertSame(shared, tablesMap.get("table0"),
					"plain (non-prefix) instance close must not remove the shared map entry");
			prefix.close(); // ref 1→0，注册实例正常摘除
			Assertions.assertFalse(tablesMap.containsKey("table0"));
		} finally {
			env.stopAll();
		}
	}
}
