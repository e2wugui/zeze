package Zeze.Component;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.nio.file.Path;
import Zeze.Builtin.RedoQueue.RunTask;
import Zeze.Config;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.RocksDatabase;
import Zeze.Util.Task;
import harness.Fast;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.rocksdb.ColumnFamilyHandle;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.WriteOptions;

/**
 * 服务端水位回退（换库/清数据/同名队列接新实例）时，回包携带的服务端进度小于本地水位。
 * 泵必须停摆等运维且不得有任何持久化动作：本地水位不可安全回写（deleteDoneTasks会删错
 * 区间），同值重写则是无退避热循环的写放大（每轮put+deleteRange+重发）。
 * 测试以"RocksDB写入一律失败"的native边界替换判别：回退应答处理必须零RocksDB写、
 * 水位与在途任务行原样保留、不安排重试（停摆而非循环）。
 */
@Fast
public class TestRedoQueueServerWatermarkRegressed {
	@TempDir
	Path directory;

	/** 只在本测试持锁的回包处理期间替换native调用边界，其他资源仍由真实RocksDatabase持有。 */
	private static final class FailingRocks extends RocksDB {
		final RocksDB delegate;

		FailingRocks(RocksDB delegate) {
			super(delegate.getNativeHandle());
			this.delegate = delegate;
		}

		void releaseBorrowedHandle() {
			disOwnNativeHandle();
		}

		@Override
		public void put(ColumnFamilyHandle cf, WriteOptions options, byte[] key, int keyOff, int keyLen,
				byte[] value, int valueOff, int valueLen) throws RocksDBException {
			throw new RocksDBException("watermark put must not run on regressed reply");
		}

		@Override
		public void deleteRange(ColumnFamilyHandle cf, WriteOptions options, byte[] begin, byte[] end)
				throws RocksDBException {
			throw new RocksDBException("deleteRange must not run on regressed reply");
		}
	}

	@Test
	void regressedReplyHaltsPumpWithoutPersistence() throws Exception {
		Task.tryInitThreadPool();
		var queue = new RedoQueue(directory.resolve("queue").toString(), new Config());
		try {
			queue.start();
			// 构造"本地水位=2、任务3在途"的形态：连加三个任务得id 1..3，
			// 删除已完成的1、2行并落水位2（模拟正常推进后的落盘状态）。
			queue.add(1, Zeze.Transaction.EmptyBean.instance);
			queue.add(1, Zeze.Transaction.EmptyBean.instance);
			queue.add(1, Zeze.Transaction.EmptyBean.instance);
			deleteTaskKey(queue, 1L);
			deleteTaskKey(queue, 2L);
			writeWatermark(queue, 2L);
			field("lastDoneTaskId").setLong(queue, 2L);

			// 服务端状态回退后对任务3（prevTaskId=2）的拒答：ErrorRequestId携带服务端进度0。
			var rpc = new RunTask();
			rpc.Argument.setTaskId(3);
			rpc.Result.setTaskId(0);
			field("pending").set(queue, rpc);

			var db = (RocksDatabase)field("db").get(queue);
			var nativeField = RocksDatabase.class.getDeclaredField("rocksDb");
			nativeField.setAccessible(true);
			var original = db.getRocksDb();
			var failing = new FailingRocks(original);
			var callback = RedoQueue.class.getDeclaredMethod("processRunTaskResult", Zeze.Net.Rpc.class);
			callback.setAccessible(true);
			nativeField.set(db, failing);
			try {
				// RocksDB写入被替换为失败：回退应答若触发任何持久化（旧钳制续泵行为的
				// 同值persistCompletion）这里会以RocksDBException翻红。
				assertEquals(0L, callback.invoke(queue, rpc));
			} finally {
				nativeField.set(db, original);
				failing.releaseBorrowedHandle();
			}

			// 停摆语义：水位不动、在途任务行保留、无重试驱动、pending已清。
			assertEquals(2L, field("lastDoneTaskId").getLong(queue), "回退应答不得改动本地水位");
			assertNotNull(taskRow(queue, 3L), "在途任务行必须原样保留（等运维处置）");
			assertNull(field("retryTask").get(queue), "停摆不得安排重试驱动");
			assertNull(field("pending").get(queue));

			// 停摆是稳态：重复回退应答同样零副作用。
			field("pending").set(queue, rpc);
			nativeField.set(db, failing);
			try {
				assertEquals(0L, callback.invoke(queue, rpc));
			} finally {
				nativeField.set(db, original);
			}
			assertEquals(2L, field("lastDoneTaskId").getLong(queue));
		} finally {
			queue.stop();
		}
	}

	private static Field field(String name) throws Exception {
		var field = RedoQueue.class.getDeclaredField(name);
		field.setAccessible(true);
		return field;
	}

	/** 写水位表：模拟processRunTaskResult成功路径的tableLastDoneTaskId.put。 */
	private static void writeWatermark(RedoQueue queue, long taskId) throws Exception {
		var table = (RocksDatabase.Table)field("tableLastDoneTaskId").get(queue);
		var key = (byte[])field("lastDoneTaskIdKey").get(queue);
		var value = ByteBuffer.Allocate(9);
		value.WriteLong(taskId);
		table.put(key, 0, key.length, value.Bytes, 0, value.WriteIndex);
	}

	private static void deleteTaskKey(RedoQueue queue, long taskId) throws Exception {
		var table = (RocksDatabase.Table)field("tableTaskQueue").get(queue);
		var key = ByteBuffer.Allocate(9);
		key.WriteLong(taskId);
		table.delete(key.Bytes, 0, key.WriteIndex);
	}

	private static byte[] taskRow(RedoQueue queue, long taskId) throws Exception {
		var table = (RocksDatabase.Table)field("tableTaskQueue").get(queue);
		var key = ByteBuffer.Allocate(9);
		key.WriteLong(taskId);
		return table.get(key.Bytes, 0, key.WriteIndex);
	}
}
