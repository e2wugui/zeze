package Zeze.Raft.RocksRaft;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import Zeze.Config;
import Zeze.Raft.RaftConfig;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Util.Task;
import harness.Fast;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestTableLruCallbackProtection {
	public static final class BStub extends Bean {
		@Override
		public Bean copy() {
			return new BStub();
		}

		@Override
		public void encode(ByteBuffer bb) {
		}

		@Override
		public void decode(IByteBuffer bb) {
		}

		@Override
		protected void initChildrenRootInfo(Record.@Nullable RootInfo root) {
		}

		@Override
		public void followerApply(Log log) {
		}

		@Override
		public void leaderApplyNoRecursive(Log vlog) {
		}
	}

	private static RaftConfig newConfig(int portBase, Path home) {
		var name = "127.0.0.1:" + portBase;
		return RaftConfig.loadFromString("""
				<raft Name="%s" DbHome="%s">
				  <node Host="127.0.0.1" Port="%d"/>
				  <node Host="127.0.0.1" Port="%d"/>
				  <node Host="127.0.0.1" Port="%d"/>
				</raft>
				""".formatted(name, home, portBase, portBase + 1, portBase + 2));
	}

	@Test
	void customCallbackRunsUnderRecordMutexAndMarksRemoved(@TempDir Path home) throws Exception {
		Task.tryInitThreadPool();
		try (var rocks = new Rocks("127.0.0.1:19760", RocksMode.Pessimism, newConfig(19760, home), new Config())) {
			rocks.registerTableTemplate("tCallbackGuard", Integer.class, BStub.class);
			var callbackCalls = new AtomicInteger();
			var heldMutexInCallback = new AtomicBoolean();
			var allowRemove = new AtomicBoolean(true);

			var template = rocks.<Integer, BStub>getTableTemplate("tCallbackGuard");
			assertNotNull(template);
			Table<Integer, BStub> table = template.openTable(0, (k, r) -> {
				callbackCalls.incrementAndGet();
				heldMutexInCallback.set(r.mutex.getOwner() == Thread.currentThread());
				if (!allowRemove.get())
					return false;
				// 模拟 GlobalCacheManagerWithRaft.globalLruTryRemove 自行调用单参 remove(key)。
				var lru = tableLru(rocks);
				if (lru != null)
					lru.remove(k);
				return true;
			});

			// 在事务外加载一条缓存记录以便精确驱动 tryRemoveRecord 并发交错。
			rocks.newProcedure(() -> {
				table.get(1);
				return 0L;
			}).call();

			var lru = table.getLruCache();
			assertNotNull(lru);
			var r1 = lru.get(1);
			assertNotNull(r1);

			var tryRemove = Table.class.getDeclaredMethod("tryRemoveRecord", Object.class, Record.class);
			tryRemove.setAccessible(true);

			// 1. 记录在用时，即便注册了自定义回调也绝不得驱逐或回调。
			r1.beginAccess();
			try {
				assertFalse(Boolean.TRUE.equals(tryRemove.invoke(table, 1, r1)));
				assertEquals(0, callbackCalls.get());
				assertFalse(r1.getRemoved());
				assertSame(r1, lru.get(1));
			} finally {
				r1.endAccess();
			}

			// 2. 持 r.mutex 时（模拟并发 getOrLoad 进入临界区），tryRemoveRecord 必须让步。
			r1.mutex.lock();
			try {
				var otherThreadResult = new AtomicBoolean(true);
				var t = new Thread(() -> {
					try {
						otherThreadResult.set(Boolean.TRUE.equals(tryRemove.invoke(table, 1, r1)));
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				});
				t.start();
				t.join();
				assertFalse(otherThreadResult.get(), "持有 r.mutex 期间驱逐必须返回 false");
				assertEquals(0, callbackCalls.get());
				assertFalse(r1.getRemoved());
			} finally {
				r1.mutex.unlock();
			}

			// 3. 回调拒绝时不得标记 removed。
			allowRemove.set(false);
			assertFalse(Boolean.TRUE.equals(tryRemove.invoke(table, 1, r1)));
			assertEquals(1, callbackCalls.get());
			assertTrue(heldMutexInCallback.get(), "自定义回调必须在持 r.mutex 临界区内执行");
			assertFalse(r1.getRemoved());
			assertSame(r1, lru.get(1));

			// 4. 回调同意驱逐后，必须置 removed=true 且缓存不再包含 r1。
			allowRemove.set(true);
			heldMutexInCallback.set(false);
			assertTrue(Boolean.TRUE.equals(tryRemove.invoke(table, 1, r1)));
			assertEquals(2, callbackCalls.get());
			assertTrue(heldMutexInCallback.get());
			assertTrue(r1.getRemoved(), "回调驱逐成功后必须置 removed=true 防止分裂缓存");
			assertNull(lru.get(1));

			// 5. 同 key 重新加载新记录 r2 后，对已移除旧记录 r1 再次调用 tryRemoveRecord 不得误删 r2。
			rocks.newProcedure(() -> {
				table.get(1);
				return 0L;
			}).call();
			var r2 = lru.get(1);
			assertNotNull(r2);
			assertNotSame(r1, r2);
			assertFalse(Boolean.TRUE.equals(tryRemove.invoke(table, 1, r1)));
			assertEquals(2, callbackCalls.get(), "已移除旧记录不得再次触发回调误删新映射");
			assertSame(r2, lru.get(1));
		}
	}

	@Test
	void customCallbackReturningTrueWithoutExplicitRemoveStillEvictsRecord(@TempDir Path home) throws Exception {
		Task.tryInitThreadPool();
		try (var rocks = new Rocks("127.0.0.1:19770", RocksMode.Pessimism, newConfig(19770, home), new Config())) {
			rocks.registerTableTemplate("tCallbackFallback", Integer.class, BStub.class);
			var template = rocks.<Integer, BStub>getTableTemplate("tCallbackFallback");
			assertNotNull(template);
			Table<Integer, BStub> table = template.openTable(0, (k, r) -> true);

			rocks.newProcedure(() -> {
				table.get(2);
				return 0L;
			}).call();
			var lru = table.getLruCache();
			assertNotNull(lru);
			var r = lru.get(2);
			assertNotNull(r);

			var tryRemove = Table.class.getDeclaredMethod("tryRemoveRecord", Object.class, Record.class);
			tryRemove.setAccessible(true);
			assertTrue(Boolean.TRUE.equals(tryRemove.invoke(table, 2, r)));
			assertTrue(r.getRemoved());
			assertNull(lru.get(2), "回调未自行 remove 时，Table.tryRemoveRecord 必须兜底 pair-remove");
		}
	}

	@SuppressWarnings("unchecked")
	private static Zeze.Util.@Nullable ConcurrentLruLike<Integer, Record<Integer>> tableLru(Rocks rocks) {
		var template = rocks.<Integer, BStub>getTableTemplate("tCallbackGuard");
		return template != null ? template.openTable(0).getLruCache() : null;
	}
}
