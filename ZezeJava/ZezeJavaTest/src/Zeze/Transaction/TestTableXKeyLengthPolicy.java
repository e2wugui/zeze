package Zeze.Transaction;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import Zeze.Application;
import Zeze.Builtin.RedoQueue.BTaskId;
import Zeze.Config;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.FuncLong;
import harness.Fast;
import harness.FastServerIds;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 超长键契约（Database.eMaxKeyLength 注释）在裸 RocksDb 后端的钉子：
 * 写入口（put/tryAdd/getOrAdd）与读冷路径在事务成功之前拒绝 ≥900；
 * remove 管线（含 Immediately 同步 flush 的 tombstone）零检查；
 * remove 后同事务写回仍被写入口顶部拦截；walk 放行；等长 900 全流程正常。
 * 入口在修改发生前抛出——捕获后事务仍可正常提交收尾（无残留副作用）。
 */
@Fast
public class TestTableXKeyLengthPolicy {
	private static final String TABLE = "TestTableXKeyLengthPolicy_records";

	@TempDir
	Path tempDir;

	@Test
	void typedEntriesEnforceBudgetWhileRemoveAndWalkExempt() throws Exception {
		var config = new Config();
		config.setServiceManager("disable");
		config.setServerId(FastServerIds.TEST_TABLEX_KEY_LENGTH_POLICY);
		config.setDefaultTableConf(new Config.TableConf());
		config.setCheckpointMode(CheckpointMode.Immediately); // 提交即落库：remove 的 tombstone 走真实无检查管线
		var defaultDb = new Config.DatabaseConf();
		defaultDb.setDatabaseType(Config.DbType.RocksDb);
		defaultDb.setDatabaseUrl(tempDir.resolve("dbhome").toString());
		config.getDatabaseConfMap().put("", defaultDb);
		var app = new Application("TestTableXKeyLengthPolicy", config);
		try {
			app.start();
			var table = new TableDynamic<>(app, TABLE, ByteBuffer::Wrap, Binary::new,
					BTaskId::new, false, null, Binary.class, BTaskId.class);

			var tooLong = new Binary(new byte[Database.eMaxKeyLength + 1]);
			var maxKey = new Binary(new byte[Database.eMaxKeyLength]);

			// 写入口封死：put/tryAdd/getOrAdd 在任何修改之前拒绝，事务可正常收尾。
			var rejected = new IllegalArgumentException[3];
			assertEquals(0L, app.newProcedure((FuncLong)() -> {
				var v = new BTaskId();
				try {
					table.put(tooLong, v);
				} catch (IllegalArgumentException e) {
					rejected[0] = e;
				}
				try {
					table.tryAdd(tooLong, v);
				} catch (IllegalArgumentException e) {
					rejected[1] = e;
				}
				try {
					table.getOrAdd(tooLong);
				} catch (IllegalArgumentException e) {
					rejected[2] = e;
				}
				return 0L;
			}, "TestTableXKeyLengthPolicy.write").call());
			for (int i = 0; i < rejected.length; i++) {
				assertNotNull(rejected[i], "entry " + i + " should reject");
				assertTrue(rejected[i].getMessage().contains(TABLE), rejected[i].getMessage());
			}

			// 读冷路径快速失败：检查先于装载，键不存在同样拒绝。
			var readRejected = new IllegalArgumentException[1];
			assertEquals(0L, app.newProcedure((FuncLong)() -> {
				try {
					table.get(tooLong);
				} catch (IllegalArgumentException e) {
					readRejected[0] = e;
				}
				return 0L;
			}, "TestTableXKeyLengthPolicy.read").call());
			assertNotNull(readRejected[0]);

			// 等长 900 全流程：写→读→Immediately 落库→select 族直查。
			assertEquals(0L, app.newProcedure((FuncLong)() -> {
				var v = new BTaskId();
				v.setTaskId(7);
				table.put(maxKey, v);
				assertEquals(7L, requireNonNull(table.get(maxKey)).getTaskId());
				return 0L;
			}, "TestTableXKeyLengthPolicy.put").call());
			assertEquals(7L, requireNonNull(table.selectFromDatabase(maxKey)).getTaskId());
			assertEquals(7L, requireNonNull(table.selectDirty(maxKey)).getTaskId());

			// walk 放行：库里只有等长 900 键（超长键从未写入成功）。
			var walked = new ArrayList<Binary>();
			table.walk((k, v) -> {
				walked.add(k);
				return true;
			});
			assertEquals(List.of(maxKey), walked);

			// 删除豁免：remove 超长键（记录不存在）不抛，Immediately 同步 flush tombstone 无检查。
			assertEquals(0L, app.newProcedure((FuncLong)() -> {
				table.remove(tooLong);
				return 0L;
			}, "TestTableXKeyLengthPolicy.remove").call());

			// remove 后同事务写回仍被拦：写入口顶部执法不信任热路径。
			var reWrite = new IllegalArgumentException[1];
			assertEquals(0L, app.newProcedure((FuncLong)() -> {
				table.remove(tooLong);
				try {
					table.put(tooLong, new BTaskId());
				} catch (IllegalArgumentException e) {
					reWrite[0] = e;
				}
				return 0L;
			}, "TestTableXKeyLengthPolicy.rewrite").call());
			assertNotNull(reWrite[0]);

			// 删除 900 键收尾全流程。
			assertEquals(0L, app.newProcedure((FuncLong)() -> {
				table.remove(maxKey);
				return 0L;
			}, "TestTableXKeyLengthPolicy.cleanup").call());
			assertNull(table.selectFromDatabase(maxKey));
		} finally {
			app.stop();
		}
	}
}
