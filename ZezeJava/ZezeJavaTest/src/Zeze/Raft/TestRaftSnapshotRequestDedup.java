package Zeze.Raft;

import java.nio.file.Path;
import java.util.Date;
import java.util.List;

import Zeze.Builtin.ServiceManagerWithRaft.Login;
import Zeze.Config;
import Zeze.Net.Binary;
import Zeze.Raft.RocksRaft.Changes;
import Zeze.Raft.RocksRaft.Rocks;
import Zeze.Raft.RocksRaft.RocksMode;
import Zeze.Raft.RocksRaft.TestFlushRetryApply.BListBean;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.Task;
import harness.Fast;
import harness.FastServerIds;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 实际生成和恢复RocksDB快照，验证跨节点及旧版表名的请求去重；不启动网络服务。 */
@Fast
public class TestRaftSnapshotRequestDedup {
	// 号段growth=2：source与target各占一个相邻raftName（跨节点语义需要两个不同名字）。
	private static final int SOURCE_ID = FastServerIds.TEST_RAFT_SNAPSHOT_REQUEST_DEDUP;
	private static final String templateName = "tSnapshotRequestDedup";
	private static final String clientId = "test.snapshotRequestDedup";
	private static final Binary rpcResult = new Binary(new byte[]{1, 2, 3});

	@TempDir
	Path dbHome;

	@BeforeEach
	public void setUp() {
		Task.tryInitThreadPool();
		Rocks.registerLog(() -> new Zeze.Raft.RocksRaft.LogList1<>(Integer.class));
	}

	private Rocks newRocks(int serverId, Path home) throws Exception {
		var rocks = new Rocks(RaftHeadlessSupport.raftName(serverId), RocksMode.Pessimism,
				RaftHeadlessSupport.newRaftConfig(serverId, home.toString()), new Config(), false);
		rocks.registerTableTemplate(templateName, Integer.class, BListBean.class);
		return rocks;
	}

	private Changes applyUniqueEntry(Rocks rocks) throws Exception {
		var table = rocks.<Integer, BListBean>getTableTemplate(templateName).openTable(0);
		RaftHeadlessSupport.seedStorage(table, 1, 10, 20);
		var changes = RaftHeadlessSupport.captureUniqueChanges(rocks, table, clientId, rpcResult);
		var seq = rocks.getRaft().getLogSequence();
		seq.saveLog(new RaftLog(1, 1, changes));
		seq.tryApply(seq.readLog(1), 1); // 解码后的follower路径也必须持久化终态。
		assertEquals(1, seq.getLastApplied());
		assertApplied(rocks, RaftHeadlessSupport.newRetriedRpc(changes, clientId), rpcResult);
		return changes;
	}

	private static void assertApplied(Rocks rocks, Login rpc, Binary result) throws Exception {
		var state = rocks.getRaft().getLogSequence().tryGetRequestState(rpc);
		assertNotNull(state);
		assertTrue(state.isApplied(), "重复请求必须命中已应用的终态");
		assertEquals(result, state.getRpcResult(), "重试必须回放原应答");
	}

	private static byte[] requestKey(Login rpc) {
		var key = ByteBuffer.Allocate();
		rpc.getUnique().encode(key);
		return key.CopyIf();
	}

	private static String legacyTableName(String name, Changes changes) {
		return LogSequence.makeUniqueRequestTableName(name,
				LogSequence.toUniqueRequestKey(new Date(changes.getCreateTime())));
	}

	@Test
	public void testCrossNodeSnapshotKeepsAppliedRequests() throws Exception {
		try (var source = newRocks(SOURCE_ID, dbHome.resolve("source"));
			 var target = newRocks(SOURCE_ID + 1, dbHome.resolve("target"))) {
			var changes = applyUniqueEntry(source);
			applyUniqueEntry(target); // 接收节点原有终态也会被物理restore替换。
			assertTrue(source.snapshot(dbHome.resolve("snapshot.zip").toString()).success);
			target.loadSnapshot(source.getRaft().getLogSequence().getCommittedSnapshotFile());
			assertApplied(target, RaftHeadlessSupport.newRetriedRpc(changes, clientId), rpcResult);
			var table = target.<Integer, BListBean>getTableTemplate(templateName).openTable(0);
			assertEquals(List.of(10, 20, 30), RaftHeadlessSupport.readStorage(table, 1));
			assertEquals(1, target.readAppliedWatermark().index());
		}
	}

	@Test
	public void testLegacySnapshotMigratesAllRequestsAndPreservesCurrentResult() throws Exception {
		try (var source = newRocks(SOURCE_ID, dbHome.resolve("source"));
			 var target = newRocks(SOURCE_ID + 1, dbHome.resolve("target"))) {
			var changes = applyUniqueEntry(source);
			// 已退役节点的旧前缀也需识别，不能仅迁移当前配置内的节点名。
			var oldName = legacyTableName("retired.node.test", changes);
			var oldTable = source.openTable(oldName);
			var oldResult = new Binary(new byte[]{4, 5, 6});
			changes.setRpcResult(oldResult);
			var value = ByteBuffer.Allocate();
			new UniqueRequestState(new RaftLog(1, 1, changes), true).encode(value);
			changes.setRpcResult(rpcResult);
			var rpc = RaftHeadlessSupport.newRetriedRpc(changes, clientId);
			// 超过单批上限，覆盖多批迁移及相同key不覆盖已有新格式结果。
			for (int id = 1; id <= 1026; id++) {
				rpc.getUnique().setRequestId(id);
				oldTable.put(requestKey(rpc), value.CopyIf());
			}
			assertTrue(source.snapshot(dbHome.resolve("legacy.snapshot.zip").toString()).success);
			target.loadSnapshot(source.getRaft().getLogSequence().getCommittedSnapshotFile());
			rpc.getUnique().setRequestId(1);
			assertApplied(target, rpc, rpcResult);
			for (int id = 2; id <= 1026; id++) {
				rpc.getUnique().setRequestId(id);
				assertApplied(target, rpc, oldResult);
			}
			assertFalse(target.getStorage().getTableMap().containsKey(oldName), "迁移完成后删除旧表");
		}
	}

	@Test
	public void testLocalLegacyTableMigratesDuringIncrementalRecoveryAndExpires() throws Exception {
		var home = dbHome.resolve("local");
		Changes changes;
		String oldName;
		try (var rocks = newRocks(SOURCE_ID, home)) {
			changes = applyUniqueEntry(rocks);
			oldName = legacyTableName(rocks.getRaft().getName(), changes);
			var rpc = RaftHeadlessSupport.newRetriedRpc(changes, clientId);
			var key = requestKey(rpc);
			var oldTable = rocks.openTable(oldName);
			var value = ByteBuffer.Allocate();
			new UniqueRequestState(new RaftLog(1, 1, changes), true).encode(value);
			oldTable.put(key, value.CopyIf());
			// 模拟升级前仅有节点前缀存根的状态库。
			for (var table : rocks.getStorage().getTableMap().values()) {
				if (table.getName().contains(".unique.") && !table.getName().equals(oldName))
					table.delete(key);
			}
			rocks.openTable(oldName + ".suffix").put(key, value.CopyIf()); // 非法日期表不得误删。
			assertTrue(rocks.snapshot(dbHome.resolve("local.snapshot.zip").toString()).success);
		}
		try (var rocks = newRocks(SOURCE_ID, home)) {
			assertEquals(1, rocks.getRaft().getLogSequence().getLastApplied(), "有效水位走增量恢复");
			var rpc = RaftHeadlessSupport.newRetriedRpc(changes, clientId);
			assertApplied(rocks, rpc, rpcResult);
			assertFalse(rocks.getStorage().getTableMap().containsKey(oldName), "开库时迁移本地旧表");
			rocks.removeExpiredUniqueStubTables(changes.getCreateTime() + 86_400_000L);
			assertNull(rocks.getUniqueAppliedStub(rpc), "新格式表也按日期过期");
			assertTrue(rocks.getStorage().getTableMap().containsKey(oldName + ".suffix"));
			assertNotNull(rocks.readAppliedWatermark());
			var table = rocks.<Integer, BListBean>getTableTemplate(templateName).openTable(0);
			assertEquals(List.of(10, 20, 30), RaftHeadlessSupport.readStorage(table, 1));
		}
	}
}
