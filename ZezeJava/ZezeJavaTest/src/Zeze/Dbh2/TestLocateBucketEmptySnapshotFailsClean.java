package Zeze.Dbh2;

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.List;
import Zeze.Builtin.Dbh2.BBucketMeta;
import Zeze.Config;
import Zeze.Dbh2.Dbh2AgentManager;
import Zeze.Dbh2.Master.MasterAgent;
import Zeze.Dbh2.Master.MasterTable;
import Zeze.Net.Binary;
import Zeze.Util.Task;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * locateBucket/locateBucketIterator对"建表进行中"空表快照的语义：
 * master侧createTable先把created=false的空表放入映射（computeIfAbsent先入、桶后放），
 * GetBuckets对该表返回rc=0空快照；MasterTable.locate的空契约此时返回null——
 * locateBucket直解引用以NPE面目失败，且空表入本地路由缓存（putIfAbsent）后该表
 * 后续定位持续NPE直到本实例建表流程完成覆盖。
 * 钉三个契约：空快照(1)以明确的"未就绪"错误失败（非NPE）；(2)不落本地缓存
 * （下次定位重新拉取，建表完成后立即恢复）；(3)非空快照照常缓存，locateIterator
 * 对空快照同样明确失败而非静默空迭代（walk零行是错果）。
 * 形态：纯桩直构——MasterAgent.getBuckets脚本化返回快照序列，不触网。
 */
@Fast
public class TestLocateBucketEmptySnapshotFailsClean {

	// getBuckets脚本化的master：按序返回MasterTable.Data快照，耗尽即失败。
	private static final class ScriptedMasterAgent extends MasterAgent {
		private final ArrayDeque<MasterTable.Data> script = new ArrayDeque<>();
		final int[] attempts = {0};

		ScriptedMasterAgent(MasterTable.Data... snapshots) {
			super(new Config());
			script.addAll(List.of(snapshots));
		}

		@Override
		public MasterTable.Data getBuckets(String database, String table) {
			++attempts[0];
			var snapshot = script.poll();
			Assertions.assertNotNull(snapshot, "script exhausted");
			return snapshot;
		}
	}

	// 建表进行中窗口的空表快照：created=false、0桶（GetBuckets对已入映射未建成的表
	// 返回的形态）。
	private static MasterTable.Data emptyNotReady() {
		return new MasterTable.Data();
	}

	// 就绪表：单桶覆盖全键域（keyFirst=Empty是Binary最小值，任何键locate必命中）。
	private static MasterTable.Data ready(String raftConfig) {
		var table = new MasterTable.Data();
		var meta = new BBucketMeta.Data();
		meta.setDatabaseName("db1");
		meta.setTableName("t1");
		meta.setRaftConfig(raftConfig);
		meta.setKeyFirst(Binary.Empty);
		meta.setKeyLast(Binary.Empty);
		table.getBuckets().put(meta.getKeyFirst(), meta);
		return table;
	}

	private static Dbh2AgentManager newManager(Path tempDir) throws Exception {
		return new Dbh2AgentManager(new Fnd19GADStubSupport.NullServiceAgent(),
				Config.load(Fnd19GADStubSupport.writeRemoteCommitConfig(tempDir).toString()));
	}

	@Test
	public void testEmptySnapshotFailsNotReadyNotCached(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		var master = new ScriptedMasterAgent(emptyNotReady(), emptyNotReady());
		var manager = newManager(tempDir);
		try {
			var key = new Binary(new byte[]{1});
			for (int i = 1; i <= 2; ++i) {
				var ex = Assertions.assertThrows(RuntimeException.class,
						() -> manager.locateBucket(master, "m1", "db1", "t1", key));
				Assertions.assertFalse(ex instanceof NullPointerException,
						"empty snapshot must fail as explicit not-ready error, not NPE");
				Assertions.assertTrue(ex.getMessage().contains("not ready"),
						"error must carry not-ready semantics: " + ex.getMessage());
				Assertions.assertEquals(i, master.attempts[0],
						"empty snapshot must not be cached: every locate re-fetches");
			}
		} finally {
			manager.stop();
		}
	}

	@Test
	public void testReadySnapshotCachedAndLocates(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		var master = new ScriptedMasterAgent(ready("raftA"));
		var manager = newManager(tempDir);
		try {
			var key = new Binary(new byte[]{1});
			Assertions.assertEquals("raftA", manager.locateBucket(master, "m1", "db1", "t1", key));
			Assertions.assertEquals("raftA", manager.locateBucket(master, "m1", "db1", "t1", key));
			Assertions.assertEquals(1, master.attempts[0], "ready snapshot must be cached after first fetch");
		} finally {
			manager.stop();
		}
	}

	@Test
	public void testEmptyThenReadyRecoversImmediately(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		var master = new ScriptedMasterAgent(emptyNotReady(), ready("raftB"));
		var manager = newManager(tempDir);
		try {
			var key = new Binary(new byte[]{1});
			Assertions.assertThrows(RuntimeException.class,
					() -> manager.locateBucket(master, "m1", "db1", "t1", key));
			// 空快照未被缓存（毒化形态：此处持续NPE直到建表流程覆盖），建表完成后
			// 下次定位立即恢复路由。
			Assertions.assertEquals("raftB", manager.locateBucket(master, "m1", "db1", "t1", key));
			Assertions.assertEquals(2, master.attempts[0]);
		} finally {
			manager.stop();
		}
	}

	@Test
	public void testIteratorEmptySnapshotFailsNotSilentlyEmpty(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		var master = new ScriptedMasterAgent(emptyNotReady());
		var manager = newManager(tempDir);
		try {
			var key = new Binary(new byte[]{1});
			var ex = Assertions.assertThrows(RuntimeException.class,
					() -> manager.locateBucketIterator(master, "m1", "db1", "t1", key, false));
			Assertions.assertFalse(ex instanceof NullPointerException, "must fail explicit, not NPE");
			Assertions.assertTrue(ex.getMessage().contains("not ready"),
					"error must carry not-ready semantics: " + ex.getMessage());
		} finally {
			manager.stop();
		}
	}
}
