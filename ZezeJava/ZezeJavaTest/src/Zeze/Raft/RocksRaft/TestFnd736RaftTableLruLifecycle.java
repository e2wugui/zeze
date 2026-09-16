package Zeze.Raft.RocksRaft;

import java.io.File;
import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentLinkedQueue;

import harness.Fast;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import Zeze.Config;
import Zeze.Raft.LogSequence;
import Zeze.Raft.RaftConfig;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Util.ConcurrentLruLike;
import Zeze.Util.Task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * FND7-36 的 Raft 侧闭环：ConcurrentLruLike 构造即注册两个常驻周期任务（热点轮转+
 * cleanNow），U1 桶已为其提供 close()（取消任务）；Raft 侧必须在重建/关闭处调用——
 * 否则 follower 的 restore/reset（InstallSnapshot 恢复、状态机重置）反复 Table.open
 * 重建 lruCache、Rocks 关闭退出时，旧实例的任务永续执行并强引用 dataMap（每表最多
 * 容量条 Record/Bean），任务与内存随重开次数无界泄漏。
 * 修复：Table.open() 重建前 close 旧实例；Table.close()（由 Rocks.close() 级联，
 * 在 storage.close() 之前，阻止关闭路径上的 lazy load 触碰已关存储句柄）。
 * 直接构造 Rocks（不start server）用真实 reset()/close() 驱动：通过 lruQueue 节点数
 * 观察周期任务是否仍在驱动轮转（close 前必须增长、close 后必须停止），有区分度。
 */
@Fast
public class TestFnd736RaftTableLruLifecycle {
	private static final String raftName = "127.0.0.1:17720";
	private static final String dbHome = "TestFnd736RaftTableLruLifecycle.raft";
	private static final String templateName = "tFnd736Lru";

	// 最小bean：无变量，仅作缓存载体。
	public static final class BEmptyBean extends Bean {
		@Override
		public Bean copy() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void encode(ByteBuffer bb) {
		}

		@Override
		public void decode(IByteBuffer bb) {
		}

		@Override
		protected void initChildrenRootInfo(Record.RootInfo root) {
		}

		@Override
		public void followerApply(Log log) {
		}

		@Override
		public void leaderApplyNoRecursive(Log vlog) {
		}
	}

	// 显式DbHome；3节点仅是Raft构造的配置要求，本测试不启动server，不占用任何端口。
	private static RaftConfig newRaftConfig() {
		return RaftConfig.loadFromString("""
				<?xml version="1.0" encoding="utf-8"?>
				<raft Name="127.0.0.1:17720" DbHome="TestFnd736RaftTableLruLifecycle.raft">
					<node Host="127.0.0.1" Port="17720"/>
					<node Host="127.0.0.1" Port="17721"/>
					<node Host="127.0.0.1" Port="17722"/>
				</raft>
				""");
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

	@SuppressWarnings("unchecked")
	private static ConcurrentLinkedQueue<Object> lruQueueOf(ConcurrentLruLike<Integer, Record<Integer>> lru)
			throws Exception {
		Field field = ConcurrentLruLike.class.getDeclaredField("lruQueue");
		field.setAccessible(true);
		return (ConcurrentLinkedQueue<Object>)field.get(lru);
	}

	// 持续访问>半满热点（lruInitialCapacity=1024/5=204，需>102个key）驱动轮转任务
	// 连续建新节点，证明周期任务在跑；返回观察到的节点数。
	private static int driveRotation(Rocks rocks, Table<Integer, BEmptyBean> table,
									 ConcurrentLruLike<Integer, Record<Integer>> lru) throws Exception {
		var queue = lruQueueOf(lru);
		var deadline = System.currentTimeMillis() + 5000;
		while (queue.size() < 3 && System.currentTimeMillis() < deadline) {
			rocks.newProcedure(() -> {
				for (var i = 0; i < 120; i++)
					table.get(i); // miss也缓存Record，驱动热点轮转
				return 0L;
			}).call();
			//noinspection BusyWait
			Thread.sleep(50);
		}
		assertTrue(queue.size() >= 3, "rotation timer must drive lruQueue growth before close, nodes=" + queue.size());
		return queue.size();
	}

	@Test
	public void testResetAndCloseCancelOldLruTimers() throws Exception {
		try (var rocks = new Rocks(raftName, RocksMode.Pessimism, newRaftConfig(), new Config(), false)) {
			rocks.registerTableTemplate(templateName, Integer.class, BEmptyBean.class);
			var table = rocks.<Integer, BEmptyBean>getTableTemplate(templateName).openTable(0);

			// 1. 初代缓存：轮转任务运行中。
			var lru1 = table.getLruCache();
			assertNotNull(lru1);
			var queue1 = lruQueueOf(lru1);
			int nodes1 = driveRotation(rocks, table, lru1);

			// 2. reset（状态机重置路径）→ Table.open 重建：旧实例必须被close，
			//    新实例替代。
			rocks.reset();
			var lru2 = table.getLruCache();
			assertNotNull(lru2);
			assertNotSame(lru1, lru2, "reset must rebuild the record cache");

			// 旧实例的周期任务已取消：不再访问旧实例，节点数在多个轮转周期后保持不变。
			//noinspection BusyWait
			Thread.sleep(700); // >3个轮转周期（200ms）
			assertEquals(nodes1, queue1.size(), "old lru timers must be cancelled after Table.open rebuild");

			// 3. 新代缓存正常服务（轮转继续驱动），证明重开后功能完好。
			int nodes2 = driveRotation(rocks, table, lru2);
			var queue2 = lruQueueOf(lru2);
			assertTrue(nodes2 >= 3);

			// 4. Rocks.close()：级联关闭当前表缓存（storage.close()之前）。
			rocks.close();
			assertNull(table.getLruCache(), "Rocks.close must cascade-close table caches");
			//noinspection BusyWait
			Thread.sleep(700);
			assertEquals(nodes2, queue2.size(), "lru timers must be cancelled after Rocks.close");
		}
	}
}
