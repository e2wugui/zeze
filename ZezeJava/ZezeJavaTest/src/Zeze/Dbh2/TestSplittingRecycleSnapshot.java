package Zeze.Dbh2;

import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import Zeze.Builtin.Dbh2.BBucketMeta;
import Zeze.Builtin.Dbh2.Master.EndMove;
import Zeze.Config;
import Zeze.Dbh2.Master.Master;
import Zeze.Dbh2.Master.MasterDatabase;
import Zeze.Dbh2.Master.MasterTable;
import Zeze.Raft.RaftConfig;
import harness.Fast;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestSplittingRecycleSnapshot {

	private static final class ObservedMaster extends Master {
		int destroyLookups;
		Runnable onLookup = () -> { };

		ObservedMaster(Path home) throws Exception {
			super(home.toString(), new Config());
		}

		@Override
		public @Nullable Manager findManagerByIdentity(String identity) {
			// 截断销毁 RPC 的边界：若被调用，回收器已经选择了该目标来销毁。
			++destroyLookups;
			onLookup.run();
			return null;
		}
	}

	private static BBucketMeta.Data bucket(int port) {
		var meta = new BBucketMeta.Data();
		meta.setDatabaseName("db");
		meta.setTableName("table");
		meta.setRaftConfig("<raft Name=\"RaftName\"><node Host=\"127.0.0.1\" Port=\"" + port + "\"/></raft>");
		return meta;
	}

	@SuppressWarnings("unchecked")
	private static MasterTable.Data splitting(MasterDatabase db) throws Exception {
		var field = MasterDatabase.class.getDeclaredField("splitting");
		field.setAccessible(true);
		return ((ConcurrentHashMap<String, MasterTable.Data>)field.get(db))
				.computeIfAbsent("table", __ -> new MasterTable.Data());
	}

	private static Object scannedEntry(BBucketMeta.Data target, BBucketMeta.Data source) throws Exception {
		var type = Class.forName("Zeze.Dbh2.Master.MasterDatabase$AgedEntry");
		var ctor = type.getDeclaredConstructor(BBucketMeta.Data.class, long.class, String.class);
		ctor.setAccessible(true);
		return ctor.newInstance(target.copy(), 600_000L,
				RaftConfig.loadFromString(source.getRaftConfig()).getSortedNames());
	}

	private static void recycle(MasterDatabase db, Object snapshot) throws Exception {
		var method = MasterDatabase.class.getDeclaredMethod("recycleDeadSplitting", String.class, snapshot.getClass());
		method.setAccessible(true);
		method.invoke(db, "table", snapshot);
	}

	@Test
	void settledBucketIsNotDestroyedByEarlierScan(@TempDir Path home) throws Exception {
		var master = new ObservedMaster(home);
		var db = new MasterDatabase(master, "db");
		try {
			var source = bucket(19521);
			var target = bucket(19522);
			target.getHost2Raft().put("manager", "127.0.0.1_19522");
			var main = new MasterTable.Data();
			db.getTables().put("table", main);
			main.getBuckets().put(source.getKeyFirst(), source);
			splitting(db).getBuckets().put(target.getKeyFirst(), target);
			var scanned = scannedEntry(target, source);

			// scanSplittingAge 已拷出 aged，回收尚未取主表锁时，正常结算发布目标。
			var move = new EndMove();
			move.Argument.setTo(target);
			assertEquals(0, db.endMove(move));
			assertSame(target, main.getBuckets().get(target.getKeyFirst()));
			assertTrue(splitting(db).getBuckets().isEmpty());
			recycle(db, scanned);
			assertEquals(0, master.destroyLookups, "过期扫描不得进入已发布主表桶的销毁路径");
		} finally {
			db.close();
			master.close();
		}
	}

	@Test
	void replacedGenerationKeepsItsMetadata(@TempDir Path home) throws Exception {
		var master = new ObservedMaster(home);
		var db = new MasterDatabase(master, "db");
		try {
			var source = bucket(19521);
			var oldTarget = bucket(19522);
			var newSource = bucket(19523);
			var newTarget = bucket(19524);
			var main = new MasterTable.Data();
			db.getTables().put("table", main);
			main.getBuckets().put(newSource.getKeyFirst(), newSource);
			// 扫描后同区间条目被重建为另一套 raft；四元组相同不代表同一代。
			splitting(db).getBuckets().put(newTarget.getKeyFirst(), newTarget);
			recycle(db, scannedEntry(oldTarget, source));
			assertSame(newTarget, splitting(db).getBuckets().get(newTarget.getKeyFirst()),
					"旧扫描只能消费原目标，不能摘除同边界的新世代");
		} finally {
			db.close();
			master.close();
		}
	}

	@Test
	void unchangedDeadGenerationCanBeRecycled(@TempDir Path home) throws Exception {
		var master = new ObservedMaster(home);
		var db = new MasterDatabase(master, "db");
		try {
			var source = bucket(19521);
			var target = bucket(19522);
			var main = new MasterTable.Data();
			db.getTables().put("table", main);
			main.getBuckets().put(source.getKeyFirst(), bucket(19523));
			splitting(db).getBuckets().put(target.getKeyFirst(), target);
			// 无 host2Raft 的既有测试形态：销毁空集成功，不使用网络。
			recycle(db, scannedEntry(target, source));
			assertTrue(splitting(db).getBuckets().isEmpty());
		} finally {
			db.close();
			master.close();
		}
	}

	@Test
	void settleWaitsForRecycleAttemptToFinish(@TempDir Path home) throws Exception {
		var master = new ObservedMaster(home);
		var db = new MasterDatabase(master, "db");
		var workers = Executors.newFixedThreadPool(2);
		var lookupEntered = new CountDownLatch(1);
		var releaseLookup = new CountDownLatch(1);
		try {
			var source = bucket(19521);
			var target = bucket(19522);
			target.getHost2Raft().put("manager", "127.0.0.1_19522");
			var main = new MasterTable.Data();
			db.getTables().put("table", main);
			main.getBuckets().put(source.getKeyFirst(), bucket(19523));
			splitting(db).getBuckets().put(target.getKeyFirst(), target);
			var scanned = scannedEntry(target, source);
			master.onLookup = () -> {
				lookupEntered.countDown();
				try {
					assertTrue(releaseLookup.await(5, TimeUnit.SECONDS));
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					throw new RuntimeException(e);
				}
			};
			var recycling = workers.submit(() -> {
				recycle(db, scanned);
				return null;
			});
			assertTrue(lookupEntered.await(5, TimeUnit.SECONDS));
			var settleEntered = new CountDownLatch(1);
			var settling = workers.submit(() -> {
				var move = new EndMove();
				move.Argument.setTo(target);
				settleEntered.countDown();
				return db.endMove(move);
			});
			assertTrue(settleEntered.await(5, TimeUnit.SECONDS));
			assertThrows(TimeoutException.class, () -> settling.get(200, TimeUnit.MILLISECONDS),
					"回收校验后的销毁窗口内不得发布目标桶");
			releaseLookup.countDown();
			recycling.get(5, TimeUnit.SECONDS);
			// 本测试 manager 不在线，未发送任何销毁，失败退出后允许后续元数据操作。
			assertEquals(0L, settling.get(5, TimeUnit.SECONDS));
			assertSame(target, main.getBuckets().get(target.getKeyFirst()));
		} finally {
			releaseLookup.countDown();
			workers.shutdown();
			assertTrue(workers.awaitTermination(10, TimeUnit.SECONDS));
			db.close();
			master.close();
		}
	}
}
