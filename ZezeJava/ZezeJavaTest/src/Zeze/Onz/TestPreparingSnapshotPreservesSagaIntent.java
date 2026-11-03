package Zeze.Onz;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import Zeze.Builtin.Onz.BSavedCommits;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Service;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.EmptyBean;
import Zeze.Util.RocksDatabase;
import Zeze.Util.Task;
import harness.Fast;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Fast
public class TestPreparingSnapshotPreservesSagaIntent {
	private static final class Transaction extends OnzTransaction<EmptyBean.Data, EmptyBean.Data> {
		@Override protected long perform() { setPendingAsync(true); return 0; }
		@Override void commit(byte[] key, BSavedCommits.Data state) { }
		@Override void waitFlushDone() { }
	}

	private static void set(Object target, String name, Object value) throws Exception {
		var field = OnzServer.class.getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}

	@Test
	void asynchronousSagaCannotBeOverwrittenByEarlierPreparingSnapshot(@TempDir Path home) throws Exception {
		Task.tryInitThreadPool();
		try (var database = new RocksDatabase(home.resolve("coordinator").toString())) {
			var server = mock(OnzServer.class, CALLS_REAL_METHODS);
			set(server, "database", database);
			set(server, "commitPoint", database.getOrAddTable("CommitPoint"));
			set(server, "commitIndex", database.getOrAddTable("CommitIndex"));
			set(server, "dbLock", new ReentrantLock());
			set(server, "writeOptions", RocksDatabase.getSyncWriteOptions());
			set(server, "onzAgent", new OnzAgent());
			set(server, "hangWarnedTids", ConcurrentHashMap.newKeySet());
			doReturn(1L).when(server).nextOnzTid();
			var socket = mock(AsyncSocket.class);
			doReturn(new Service("preparing-snapshot")).when(socket).getService();
			doReturn(socket).when(server).getZezeInstance(anyString());
			var transaction = new Transaction();
			transaction.setOnzServer(server);
			var saving = new CountDownLatch(1);
			var releaseSave = new CountDownLatch(1);
			var snapshotLockOwned = new java.util.concurrent.atomic.AtomicBoolean();
			doAnswer(call -> {
				if (Thread.currentThread().getName().equals("preparing-performer")) {
					snapshotLockOwned.set(transaction.isHeldByCurrentThread());
					saving.countDown();
					assertTrue(releaseSave.await(10, TimeUnit.SECONDS));
				}
				return call.callRealMethod();
			}).when(server).saveCommitPoint(any(), any(), eq(AbstractOnz.ePreparing));
			var performed = new CompletableFuture<Long>();
			var performer = new Thread(() -> {
				try { performed.complete(server.perform(transaction)); }
				catch (Throwable failure) { performed.completeExceptionally(failure); }
			}, "preparing-performer");
			performer.start();
			CompletableFuture<Void> registration = null;
			try {
				assertTrue(saving.await(10, TimeUnit.SECONDS));
				var registering = new CountDownLatch(1);
				registration = CompletableFuture.runAsync(() -> {
					registering.countDown();
					transaction.callSagaAsync("late", "step", EmptyBean.Data.instance, EmptyBean.Data.instance);
				});
				assertTrue(registering.await(10, TimeUnit.SECONDS));
				// 初始快照落盘时必须持有登记锁；旧代码允许新意图先落盘、再被旧快照覆写。
				assertTrue(snapshotLockOwned.get());
				releaseSave.countDown();
				registration.get(10, TimeUnit.SECONDS);
				var key = new byte[8];
				ByteBuffer.longBeHandler.set(key, 0, transaction.getOnzTid());
				var state = new BSavedCommits.Data();
				state.decode(ByteBuffer.Wrap(database.getOrAddTable("CommitPoint").get(key)));
				assertTrue(state.getOnzs().contains(OnzTransaction.encodeSagaParticipant("late")));
			} finally {
				releaseSave.countDown();
				if (registration != null)
					registration.get(10, TimeUnit.SECONDS);
				transaction.setPendingAsync(false);
				assertEquals(0, performed.get(10, TimeUnit.SECONDS));
				performer.join(10_000);
			}
		}
	}
}
