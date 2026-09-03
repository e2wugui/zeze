package UnitTest.Zeze.Raft;

import java.io.File;

import harness.Fast;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import Zeze.Config;
import Zeze.Raft.LogSequence;
import Zeze.Raft.RaftConfig;
import Zeze.Raft.RocksRaft.Rocks;
import Zeze.Raft.RocksRaft.RocksMode;
import Zeze.Util.Task;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Rocks 必须重载 StateMachine.reset() 清空 statemachine RocksDB。
 * 没有快照的时候（首个快照前，SnapshotLogCount 默认 100 万条日志），
 * Raft 重启后 lastApplied 归零、从头全量重放日志，状态机若残留旧数据，
 * list 等按索引增量 apply 的非幂等日志会在旧数据上被双重应用（重复插入/错位删除）。
 * 这里直接构造 Rocks 状态机（不 start server，无网络与选举流量），
 * 用 storage 层表读写验证 reset 的清库机械与无快照重启的构造路径。
 */
@Fast
public class TestRocksReset {
	private static final String raftName = "127.0.0.1:17620";
	private static final String dbHome = "TestRocksReset.raft";
	private static final String tableName = "TestRocksReset.table";
	private static final byte[] key = {1};
	private static final byte[] value = {1, 2, 3};

	// 显式DbHome，避免RaftName改写默认DbHome；3节点仅是Raft构造的配置要求，
	// 本测试不启动server，不占用任何端口。
	private static RaftConfig newRaftConfig() {
		return RaftConfig.loadFromString("""
				<?xml version="1.0" encoding="utf-8"?>
				<raft Name="127.0.0.1:17620" DbHome="TestRocksReset.raft">
					<node Host="127.0.0.1" Port="17620"/>
					<node Host="127.0.0.1" Port="17621"/>
					<node Host="127.0.0.1" Port="17622"/>
				</raft>
				""");
	}

	private static Rocks newRocks() throws Exception {
		return new Rocks(raftName, RocksMode.Pessimism, newRaftConfig(), new Config(), false);
	}

	private static byte[] get(Rocks rocks) throws Exception {
		return rocks.openTable(tableName).get(key);
	}

	private static int count(Rocks rocks) throws Exception {
		try (var it = rocks.openTable(tableName).iterator()) {
			int n = 0;
			for (it.seekToFirst(); it.isValid(); it.next())
				n++;
			return n;
		}
	}

	@BeforeEach
	public void setUp() {
		Task.tryInitThreadPool();
		LogSequence.deletedDirectoryAndCheck(new File(dbHome), 100);
	}

	@AfterEach
	public void tearDown() {
		LogSequence.deleteDirectory(new File(dbHome)); // best-effort
	}

	// 场景1：运行中直接reset()（storage已打开）：清空statemachine库，且重开后可继续使用。
	@Test
	public void testResetClearStatemachine() throws Exception {
		try (var rocks = newRocks()) {
			assertFalse(new File(dbHome, "snapshot.dat").isFile()); // 无快照路径

			rocks.openTable(tableName).put(key, value);
			assertArrayEquals(value, get(rocks));
			assertEquals(1, count(rocks));

			rocks.reset(); // 修复前为空操作，数据残留

			assertEquals(0, count(rocks));
			assertNull(get(rocks));

			// reset后状态机仍可写入（库句柄已重开）
			rocks.openTable(tableName).put(key, value);
			assertArrayEquals(value, get(rocks));
		}
	}

	// 场景2（触发路径）：无快照重启。Raft构造内部调用sm.reset()（此时storage==null），
	// 上次运行残留的statemachine数据必须被清除，否则重启后全量日志重放到未清空的状态机上。
	@Test
	public void testResetOnRestartWithoutSnapshot() throws Exception {
		try (var rocks1 = newRocks()) {
			rocks1.openTable(tableName).put(key, value);
			assertArrayEquals(value, get(rocks1));
			// 此时无snapshot.dat，raft日志库<dbHome>/db非空（至少含index=0的HeartbeatLog），
			// 重启后LogSequence会把lastApplied归零到firstIndex。
		}

		// 模拟进程重启：同一DbHome重新构造。Raft构造发现无快照 → sm.reset()。
		try (var rocks2 = newRocks()) {
			assertFalse(new File(dbHome, "snapshot.dat").isFile());
			assertNull(get(rocks2)); // 修复前：上次运行的数据残留
			assertEquals(0, count(rocks2));
		}
	}
}
