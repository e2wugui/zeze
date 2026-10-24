package Zeze.Transaction;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import Zeze.History.Helper;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Collections.Collection;
import Zeze.Transaction.Collections.LogBean;
import Zeze.Transaction.Collections.LogCollection;
import demo.Module1.BSimple;
import demo.Module1.BValue;
import demo.Module1.tflush;
import harness.Fast;
import org.junit.jupiter.api.Test;

/** Consumer selection must preserve state, rollback, and complete replay across retries. */
@Fast
public class TestCollectionChangeCapture {
	private static List<Collection> collections(BValue root) {
		return List.of(root.getMap15(), root.getMap41(), root.getSortedmap1(), root.getSortedmap2(),
			root.getList30(), root.getList9(), root.getSet10());
	}

	private static void write(BValue root, int value) {
		root.setInt_1(value);
		root.getMap15().clear();
		root.getMap15().put(1L, (long)value);
		var mapItem = new BSimple();
		mapItem.setInt_1(value);
		root.getMap41().clear();
		root.getMap41().put(1L, mapItem);
		root.getSortedmap1().clear();
		root.getSortedmap1().put(1, value);
		var sortedItem = new BSimple();
		sortedItem.setInt_1(value);
		root.getSortedmap2().clear();
		root.getSortedmap2().put(1, sortedItem);
		root.getList30().clear();
		root.getList30().add(value);
		var listItem = new demo.Bean1();
		listItem.setV1(value);
		root.getList9().clear();
		root.getList9().add(listItem);
		root.getSet10().clear();
		root.getSet10().add(value);
	}

	private static void verify(BValue root, int value) {
		assertEquals(value, root.getInt_1());
		assertEquals(value, root.getMap15().get(1L));
		var mapItem = root.getMap41().get(1L);
		assertNotNull(mapItem);
		assertEquals(value, mapItem.getInt_1());
		assertEquals(value, root.getSortedmap1().get(1));
		var sortedItem = root.getSortedmap2().get(1);
		assertNotNull(sortedItem);
		assertEquals(value, sortedItem.getInt_1());
		assertEquals(List.of(value), root.getList30());
		assertEquals(value, root.getList9().get(0).getV1());
		assertEquals(java.util.Set.of(value), root.getSet10());
	}

	private static void verifyCapture(BValue root, boolean capture) {
		var txn = transaction();
		for (var source : collections(root)) {
			var log = (LogCollection)txn.getLog(source.changeLogKey());
			assertNotNull(log);
			assertEquals(capture, log.hasDelta(), source.getClass().getSimpleName());
			if (!capture)
				assertThrows(IllegalStateException.class, () -> source.createLogBean(log));
		}
	}

	private static Transaction transaction() {
		var current = Transaction.getCurrent();
		assertNotNull(current);
		return current;
	}

	private static tflush table(Zeze.Application app) {
		var table = (tflush)app.getTable("demo_Module1_tflush");
		assertNotNull(table);
		return table;
	}

	private static BValue existing(tflush table) {
		var root = table.get(1L);
		assertNotNull(root);
		return root;
	}

	private static void registerReplay() {
		Helper.registerLogs();
		Helper.registerLogMap1(Long.class, Long.class);
		Helper.registerLogMap2(Long.class, BSimple.class);
		Helper.registerLogSortedMap1(Integer.class, Integer.class);
		Helper.registerLogSortedMap2(Integer.class, BSimple.class);
		Helper.registerLogList1(Integer.class);
		Helper.registerLogList2(demo.Bean1.class);
		Helper.registerLogSet1(Integer.class);
	}

	private static demo.Bean1 item(int value) {
		var bean = new demo.Bean1();
		bean.setV1(value);
		return bean;
	}

	@Test
	public void listAndSetBulkOperationsSavepointsAndReplayAgreeInBothCaptureModes() throws Exception {
		registerReplay();
		for (boolean capture : new boolean[]{false, true}) {
			var app = Map2TestSupport.newApp("collectionBulk" + capture);
			try {
				var table = table(app);
				assertEquals(Procedure.Success, app.newProcedure(() -> {
					table.getOrAdd(1L);
					return Procedure.Success;
				}, "seed").call());
				var follower = new BValue();
				var callbacks = new AtomicInteger();
				if (capture) table.getChangeListenerMap().addListener((key, record) -> {
					var bb = ByteBuffer.Allocate();
					var log = record.getLogBean();
				assertNotNull(log);
				log.encode(bb);
					follower.followerApply(LogBean.decode(0, bb));
					var root = (BValue)record.getValue();
				assertNotNull(root);
					assertEquals(root.getList30(), follower.getList30());
					assertEquals(root.getList9(), follower.getList9());
					assertEquals(root.getSet10(), follower.getSet10());
					assertTrue(bb.isEmpty());
					callbacks.incrementAndGet();
				});
				assertEquals(Procedure.Success, app.newProcedure(() -> {
					var root = table.getOrAdd(1L);
					var list = root.getList30();
					var beans = root.getList9();
					var set = root.getSet10();
					list.addAll(List.of(1, 2, 1, 3));
					assertTrue(list.removeAll(List.of(1, 9)));
					assertFalse(list.removeAll(List.of(9)));
					list.add(1, 5);
					assertEquals(2, list.set(0, 7));
					assertTrue(list.remove((Integer)3));
					beans.addAll(List.of(item(1), item(2), item(3)));
					beans.set(0, item(7));
					beans.get(1).setV1(9);
					beans.remove(2);
					set.addAll(List.of(1, 2, 3));
					assertTrue(set.addAll(List.of(2, 4)));
					assertFalse(set.addAll(List.of(2, 4)));
					assertTrue(set.removeAll(List.of(1, 99)));
					assertFalse(set.removeAll(List.of(99)));
					assertEquals(1L, app.newProcedure(() -> {
						list.clear();
						beans.clear();
						set.clear();
						return 1L;
					}, "bulkRollback").call());
					assertEquals(List.of(7, 5), list);
					assertEquals(2, beans.size());
					assertEquals(java.util.Set.of(2, 3, 4), set);
					assertEquals(Procedure.Success, app.newProcedure(() -> {
						list.addAll(List.of(5, 11));
						list.removeAll(List.of(5));
						beans.add(1, item(11));
						set.remove(3);
						set.add(11);
						return Procedure.Success;
					}, "bulkNestedCommit").call());
					assertEquals(List.of(7, 11), list);
					assertEquals(List.of(item(7), item(11), item(9)), beans);
					assertEquals(java.util.Set.of(2, 4, 11), set);
					return Procedure.Success;
				}, "bulk").call());
				assertEquals(Procedure.Success, app.newProcedure(() -> {
					var root = existing(table);
					root.getList9().get(0).setV1(13);
					root.getList30().clear();
					root.getList30().add(13);
					root.getSet10().clear();
					return Procedure.Success;
				}, "bulkNextCommit").call());
				assertEquals(capture ? 2 : 0, callbacks.get());
			} finally {
				app.stop();
			}
		}
	}

	@Test
	public void userLogCommitsWithoutEnteringChangeCollection() throws Exception {
		var app = Map2TestSupport.newApp("collectionUserLog");
		try {
			var table = table(app);
			var commits = new AtomicInteger();
			table.getChangeListenerMap().addListener((key, record) -> {
			});
			assertEquals(Procedure.Success, app.newProcedure(() -> {
				var root = table.getOrAdd(1L);
				root.setInt_1(7);
				transaction().putLog(new TransactionLog() {
					@Override
					public Bean getBelong() {
						return root;
					}

					@Override
					public int getVariableId() {
						return Bean.OBJECT_ID_STEP - 1;
					}

					@Override
					public Category category() {
						return Category.eUser;
					}

					@Override
					public void commit() {
						commits.incrementAndGet();
					}

					@Override
					public void collectChanges(Changes changes) {
						fail("user log entered change collection");
					}
				});
				return Procedure.Success;
			}, "userLog").call());
			assertEquals(1, commits.get());
		} finally {
			app.stop();
		}
	}

	@Test
	public void untrackedCollectionsPreserveNestedRollbackAndCommit() throws Exception {
		var app = Map2TestSupport.newApp("collectionState");
		try {
			var table = table(app);
			assertNull(app.getHistory());
			assertEquals(Procedure.Success, app.newProcedure(() -> {
				var root = table.getOrAdd(1L);
				write(root, 7);
				verifyCapture(root, false);
				assertEquals(1L, app.newProcedure(() -> {
					write(root, 9);
					verifyCapture(root, false);
					return 1L;
				}, "rollback").call());
				verify(root, 7);
				assertEquals(Procedure.Success, app.newProcedure(() -> {
					write(root, 11);
					verifyCapture(root, false);
					return Procedure.Success;
				}, "nestedCommit").call());
				verify(root, 11);
				verifyCapture(root, false);
				return Procedure.Success;
			}, "untracked").call());
			assertEquals(Procedure.Success, app.newProcedure(() -> {
				verify(existing(table), 11);
				return Procedure.Success;
			}, "read").call());
		} finally {
			app.stop();
		}
	}

	@Test
	public void listenerRegisteredWhileCommitStartsWithNextTransaction() throws Exception {
		registerReplay();
		var app = Map2TestSupport.newApp("collectionWhileCommitListener");
		try {
			var table = table(app);
			assertEquals(Procedure.Success, app.newProcedure(() -> {
				table.getOrAdd(1L);
				return Procedure.Success;
			}, "seed").call());
			var rounds = new AtomicInteger();
			var callbacks = new AtomicInteger();
			ChangeListener listener = (key, record) -> {
				var bb = ByteBuffer.Allocate();
				var log = record.getLogBean();
				assertNotNull(log);
				log.encode(bb);
				var follower = new BValue();
				follower.followerApply(LogBean.decode(0, bb));
				verify(follower, 7);
				assertTrue(bb.isEmpty());
				callbacks.incrementAndGet();
			};
			assertEquals(Procedure.Success, app.newProcedure(() -> {
				rounds.incrementAndGet();
				var root = table.getOrAdd(1L);
				write(root, 7);
				verifyCapture(root, false);
				Transaction.whileCommit(() -> table.getChangeListenerMap().addListener(listener));
				return Procedure.Success;
			}, "registerListener").call());
			assertEquals(1, rounds.get());
			assertEquals(0, callbacks.get());
			assertEquals(Procedure.Success, app.newProcedure(() -> {
				var root = existing(table);
				write(root, 7);
				verifyCapture(root, true);
				return Procedure.Success;
			}, "afterListener").call());
			assertEquals(1, callbacks.get());
		} finally {
			app.stop();
		}
	}

	@Test
	public void rolledBackUntrackedLogsDoNotNotifyWhileCommitListener() throws Exception {
		var app = Map2TestSupport.newApp("collectionRolledBackCapture");
		try {
			var table = table(app);
			var rounds = new AtomicInteger();
			var callbacks = new AtomicInteger();
			assertEquals(Procedure.Success, app.newProcedure(() -> {
				rounds.incrementAndGet();
				var root = table.getOrAdd(1L);
				assertEquals(1L, app.newProcedure(() -> {
					write(root, 7);
					return 1L;
				}, "rollback").call());
				root.setInt_1(11);
				Transaction.whileCommit(() -> table.getChangeListenerMap()
					.addListener((key, record) -> callbacks.incrementAndGet()));
				return Procedure.Success;
			}, "rolledBack").call());
			assertEquals(1, rounds.get());
			assertEquals(0, callbacks.get());
		} finally {
			app.stop();
		}
	}

	@Test
	public void existingBeanFieldEditDoesNotCreateCollectionTransactionState() throws Exception {
		var app = Map2TestSupport.newApp("collectionFieldOnly");
		try {
			var table = table(app);
			assertEquals(Procedure.Success, app.newProcedure(() -> {
				write(table.getOrAdd(1L), 7);
				return Procedure.Success;
			}, "seed").call());
			assertEquals(Procedure.Success, app.newProcedure(() -> {
				var root = existing(table);
				var item = root.getMap41().get(1L);
				assertNotNull(item);
				item.setInt_1(9);
				assertNull(transaction().getLog(root.getMap41().changeLogKey()));
				return Procedure.Success;
			}, "fieldOnly").call());
		} finally {
			app.stop();
		}
	}

	@Test
	public void earlyOnzBindingSelectsDeltaWithoutRetry() throws Exception {
		var app = Map2TestSupport.newApp("collectionEarlyOnzCapture");
		try {
			var table = table(app);
			var rounds = new AtomicInteger();
			var ready = new AtomicInteger();
			var onz = new Zeze.Onz.OnzProcedure(null, null, null, null, null) {
				@Override
				public void sendReadyAndWait() {
					ready.incrementAndGet();
				}

				@Override
				public int getFlushMode() {
					return Zeze.Onz.Onz.eFlushAsync;
				}
			};
			assertEquals(Procedure.Success, app.newProcedure(() -> {
				rounds.incrementAndGet();
				transaction().setOnzProcedure(onz);
				var root = table.getOrAdd(1L);
				write(root, 7);
				verifyCapture(root, true);
				assertEquals(0, ready.get());
				return Procedure.Success;
			}, "earlyOnzCapture").call());
			assertEquals(1, rounds.get());
			assertEquals(1, ready.get());
		} finally {
			app.stop();
		}
	}

	@Test
	public void lateOnzBindingIsRejectedAfterUntrackedWrites() throws Exception {
		var app = Map2TestSupport.newApp("collectionLateOnzCapture");
		try {
			var table = table(app);
			var ready = new AtomicInteger();
			var onz = new Zeze.Onz.OnzProcedure(null, null, null, null, null) {
				@Override
				public void sendReadyAndWait() {
					ready.incrementAndGet();
				}

				@Override
				public int getFlushMode() {
					return Zeze.Onz.Onz.eFlushAsync;
				}
			};
			assertEquals(Procedure.Success, app.newProcedure(() -> {
				var root = table.getOrAdd(1L);
				write(root, 7);
				verifyCapture(root, false);
				assertThrows(IllegalStateException.class, () -> transaction().setOnzProcedure(onz));
				return Procedure.Success;
			}, "lateOnzCapture").call());
			assertEquals(0, ready.get());
		} finally {
			app.stop();
		}
	}
}
