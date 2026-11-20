package Zeze.Transaction;

import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import Zeze.Application;
import Zeze.Builtin.RedoQueue.BTaskId;
import Zeze.Config;
import Zeze.Dbh2.Dbh2Agent;
import Zeze.Dbh2.Dbh2AgentManager;
import Zeze.Dbh2.Master.MasterAgent;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.Action2;
import Zeze.Util.FuncLong;
import Zeze.Util.KV;
import harness.Fast;
import harness.FastServerIds;
import org.junit.jupiter.api.Test;
import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 超长键契约（Database.eMaxKeyLength 注释）在 dbh2+本地镜像形态下的钉子：
 * 存量 901..2712 键读入口快速失败（不装载不回填）；删除管线（load→回填镜像→tombstone→
 * flush 删主库+镜像）全程零检查，walk 能看到存量键；后端 replace 的 900 检查退化为
 * flush 兜底断言（绕过 typed 入口的脏记录被拒且保持 dirty）。
 */
@Fast
public class TestRocksMirrorLegacyKeys {
	private static final String TABLE = "TestRocksMirrorLegacyKeys_records";

	@Test
	void legacyKeyReadFailsFastAndWalkDeletePipelinePasses() throws Exception {
		var manager = mock(Dbh2AgentManager.class);
		var master = mock(MasterAgent.class);
		var bucket = mock(Dbh2Agent.class);
		when(manager.openDatabase(anyString(), anyString())).thenReturn(master);
		doAnswer(invocation -> {
			Action2<Integer, Boolean> callback = invocation.getArgument(4);
			callback.run(0, true);
			return null;
		}).when(manager).createTableAsync(eq(master), anyString(), anyString(), anyString(), any());
		when(manager.locateBucket(eq(master), anyString(), anyString(), anyString(), any()))
				.thenReturn("bucket");
		when(manager.openBucket("bucket")).thenReturn(bucket);

		var config = new Config();
		config.setServiceManager("disable");
		config.setServerId(FastServerIds.TEST_ROCKS_MIRROR_LEGACY_KEYS);
		config.setDefaultTableConf(new Config.TableConf());
		config.setCheckpointMode(CheckpointMode.Table);
		var defaultDb = new Config.DatabaseConf();
		defaultDb.setDatabaseUrl("rocks_mirror_legacy_keys");
		config.getDatabaseConfMap().put("", defaultDb);
		var tableConf = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument()
				.createElement("TableConf");
		tableConf.setAttribute("Name", TABLE);
		tableConf.setAttribute("DatabaseName", "dbh2");
		new Config.TableConf(config, tableConf);
		var app = new Application("TestRocksMirrorLegacyKeys", config);
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseUrl("dbh2://127.0.0.1:1/mirrorLegacy");
		dbConf.setDisableOperates(true);
		app.getDatabases().put("dbh2", new Zeze.Dbh2.Database(app, manager, dbConf));
		try {
			app.start();
			var table = new TableDynamic<>(app, TABLE, ByteBuffer::Wrap, Binary::new,
					BTaskId::new, false, null, Binary.class, BTaskId.class);
			var checkpoint = requireNonNull(app.getCheckpoint());
			var mirror = table.getLocalRocksCacheTable();

			var legacy = new Binary("x".repeat(Database.eMaxKeyLength + 1));
			var stored = new BTaskId();
			stored.setTaskId(10);
			when(bucket.get("mirrorLegacy", TABLE, legacy)).thenReturn(KV.create(true, ByteBuffer.encode(stored)));

			// 存量超长键读入口快速失败（事务成功之前拒绝），不再装载回填镜像。
			var readFailure = assertThrows(IllegalArgumentException.class, () -> table.selectDirty(legacy));
			assertTrue(readFailure.getMessage().contains(TABLE), readFailure.getMessage());
			assertTrue(readFailure.getMessage().contains(String.valueOf(Database.eMaxKeyLength + 1)),
					readFailure.getMessage());
			assertNull(mirror.find(table, legacy));

			// 删除管线全程零检查：load 旧值（stub）→ 回填镜像（backfillMirror 不检查）→ tombstone。
			assertEquals(0L, app.newProcedure((FuncLong)() -> {
				table.remove(legacy);
				return 0L;
			}, "TestRocksMirrorLegacyKeys.remove").call());

			// remove 的 load 已把存量回填进镜像：walkDatabase（纯镜像迭代）看得到它。
			// 注意应用层 walk 在此时点反而看不到：缓存合并层把"已删除未 flush"的 tombstone 记录
			// 正确过滤（invokeCallback 的 StateShare/StateModify 分支）——镜像真相与缓存视图分离。
			var seen = new ArrayList<Binary>();
			mirror.walkDatabase(table, (k, v) -> {
				seen.add(k);
				return false;
			});
			assertEquals(List.of(legacy), seen);
			assertEquals(10L, requireNonNull(mirror.find(table, legacy)).getTaskId());

			// flush tombstone：主库 remove + 镜像 remove 均无检查，记录干净、镜像清空。
			var record = requireNonNull(requireNonNull(table.getCache()).get(legacy));
			assertTrue(record.getDirty());
			checkpoint.flush(List.of(record), null, null);
			verify(manager, atLeastOnce()).commit(any());
			assertFalse(record.getDirty());
			assertNull(mirror.find(table, legacy));

			// 兜底断言：手工绕过 typed 入口的超长脏记录在 flush 的后端 replace 处被拒，保持 dirty。
			clearInvocations(manager);
			var tooLong = new Binary(new byte[Database.eMaxKeyLength + 1]);
			var rejectedRecord = dirtyRecord(table, tooLong);
			var failure = assertThrows(RuntimeException.class,
					() -> checkpoint.flush(List.of(rejectedRecord), null, null));
			assertInstanceOf(IllegalArgumentException.class, failure.getCause());
			assertTrue(rejectedRecord.getDirty());
			verify(manager, never()).commit(any());
			assertNull(mirror.find(table, tooLong));

			var maxKey = new Binary(new byte[Database.eMaxKeyLength]);
			var acceptedRecord = dirtyRecord(table, maxKey);
			checkpoint.flush(List.of(acceptedRecord), null, null);
			verify(manager).commit(any());
			assertFalse(acceptedRecord.getDirty());
			assertEquals(20L, requireNonNull(mirror.find(table, maxKey)).getTaskId());
		} finally {
			app.stop();
		}
	}

	private static Record1<Binary, BTaskId> dirtyRecord(TableX<Binary, BTaskId> table, Binary key) {
		var value = new BTaskId();
		value.setTaskId(20);
		var record = new Record1<>(table, key, value);
		record.setDirty(true);
		return record;
	}
}
