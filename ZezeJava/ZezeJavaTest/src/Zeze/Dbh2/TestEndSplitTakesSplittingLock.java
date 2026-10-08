package Zeze.Dbh2;

import harness.Extra;
import java.io.File;
import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import Zeze.Builtin.Dbh2.BBucketMeta;
import Zeze.Builtin.Dbh2.Master.EndMove;
import Zeze.Builtin.Dbh2.Master.EndSplit;
import Zeze.Config;
import Zeze.Dbh2.Master.Master;
import Zeze.Dbh2.Master.MasterDatabase;
import Zeze.Dbh2.Master.MasterTable;
import Zeze.Net.Binary;
import Zeze.Raft.LogSequence;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND19 GA-C05回归：endSplit/endMove访问splitting表的TreeMap必须持splitting表
 * 自己的锁（与createSplitBucket互斥）。bug时只持主表锁直接get/remove/encode，
 * 与持splitting锁的createSplitBucket并发，TreeMap并发读写可CME/红黑树中间态死循环。
 * 钉住锁契约：外部持有splitting锁时endSplit/endMove必须阻塞等待
 * （bug时不取该锁，立即完成）。锁序为单向主表→splitting，无死锁环。
 */
@Fast
@Extra
public class TestEndSplitTakesSplittingLock {

	private static BBucketMeta.Data newBucketMeta() {
		var bucket = new BBucketMeta.Data();
		bucket.setDatabaseName("db1");
		bucket.setTableName("t1");
		bucket.setRaftConfig("");
		bucket.setKeyFirst(new Binary(new byte[]{5}));
		bucket.setKeyLast(Binary.Empty);
		return bucket;
	}

	// Master构造时扫描home目录自动注册db1（预建目录），反射取回实例。
	@SuppressWarnings("unchecked")
	private static MasterDatabase getDatabase(Master master) throws Exception {
		Field field = Master.class.getDeclaredField("databases");
		field.setAccessible(true);
		return ((ConcurrentHashMap<String, MasterDatabase>)field.get(master)).get("db1");
	}

	@SuppressWarnings("unchecked")
	private static ConcurrentHashMap<String, MasterTable.Data> getSplitting(MasterDatabase db) throws Exception {
		Field field = MasterDatabase.class.getDeclaredField("splitting");
		field.setAccessible(true);
		return (ConcurrentHashMap<String, MasterTable.Data>)field.get(db);
	}

	// 调用方（当前线程）已持有splitting锁：call在另一线程执行，若不取splitting锁
	// 则立即完成（done=true，红灯）。
	private static void assertBlocksOnSplittingLock(MasterTable.Data splitting, Runnable call) throws Exception {
		var done = new AtomicBoolean(false);
		var error = new AtomicBoolean(false);
		var thread = Thread.ofPlatform().start(() -> {
			try {
				call.run();
			} catch (Throwable e) {
				error.set(true);
			}
			done.set(true);
		});
		try {
			//noinspection BusyWait
			for (int i = 0; i < 30 && !done.get(); ++i)
				Thread.sleep(10); // 等待线程进入endSplit/endMove
			Assertions.assertFalse(done.get(), "end call must block while splitting lock is held");
		} finally {
			splitting.unlock(); // 锁由调用方线程获取，必须由本线程释放
		}
		thread.join(10_000);
		Assertions.assertTrue(done.get());
		Assertions.assertFalse(error.get(), "end call must complete without error after lock released");
	}

	@Test
	public void testEndSplitWaitsSplittingLock() throws Exception {
		var home = "testEndSplit";
		LogSequence.deleteDirectory(new File(home));
		new File(home, "db1").mkdirs();
		var master = new Master(home, new Config());
		try {
			var db = getDatabase(master);
			db.getTables().put("t1", new MasterTable.Data());
			var splitting = getSplitting(db).computeIfAbsent("t1", __ -> new MasterTable.Data());

			var r = new EndSplit();
			r.Argument.setFrom(newBucketMeta());
			r.Argument.setTo(newBucketMeta());

			splitting.lock();
			assertBlocksOnSplittingLock(splitting, () -> {
				try {
					Assertions.assertEquals(master.errorCode(Master.eSplittingBucketNotFound), db.endSplit(r));
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			});
		} finally {
			master.close();
			LogSequence.deleteDirectory(new File(home));
		}
	}

	@Test
	public void testEndMoveWaitsSplittingLock() throws Exception {
		var home = "testEndSplitEndMove";
		LogSequence.deleteDirectory(new File(home));
		new File(home, "db1").mkdirs();
		var master = new Master(home, new Config());
		try {
			var db = getDatabase(master);
			db.getTables().put("t1", new MasterTable.Data());
			var splitting = getSplitting(db).computeIfAbsent("t1", __ -> new MasterTable.Data());

			var r = new EndMove();
			r.Argument.setTo(newBucketMeta());

			splitting.lock();
			assertBlocksOnSplittingLock(splitting, () -> {
				try {
					Assertions.assertEquals(master.errorCode(Master.eSplittingBucketNotFound), db.endMove(r));
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			});
		} finally {
			master.close();
			LogSequence.deleteDirectory(new File(home));
		}
	}
}
