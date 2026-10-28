package Zeze.Component;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import Zeze.Builtin.RedoQueue.RunTask;
import Zeze.Config;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.EmptyBean;
import Zeze.Util.RocksDatabase;
import Zeze.Util.Task;
import harness.Fast;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.rocksdb.ColumnFamilyHandle;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.WriteOptions;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestRedoQueueCompletionPersistence {
	@TempDir
	Path directory;

	// 只在本测试持锁的回包处理期间替换native调用边界，其他资源仍由真实RocksDatabase持有。
	private static final class FailingRocks extends RocksDB {
		final RocksDB delegate;
		final boolean failPut;

		FailingRocks(RocksDB delegate, boolean failPut) {
			super(delegate.getNativeHandle());
			this.delegate = delegate;
			this.failPut = failPut;
		}

		void releaseBorrowedHandle() {
			disOwnNativeHandle();
		}

		@Override
		public void put(ColumnFamilyHandle cf, WriteOptions options, byte[] key, int keyOff, int keyLen,
				byte[] value, int valueOff, int valueLen) throws RocksDBException {
			if (failPut)
				throw new RocksDBException("completion put failed");
			delegate.put(cf, options, key, keyOff, keyLen, value, valueOff, valueLen);
		}

		@Override
		public void deleteRange(ColumnFamilyHandle cf, WriteOptions options, byte[] begin, byte[] end)
				throws RocksDBException {
			throw new RocksDBException("completion delete failed");
		}
	}

	@Test
	void failedWatermarkWriteRetainsProgressAndRetry() throws Exception {
		checkFailure(true);
	}

	@Test
	void failedTaskDeletionRetainsProgressAndRetry() throws Exception {
		checkFailure(false);
	}

	private void checkFailure(boolean failPut) throws Exception {
		Task.tryInitThreadPool();
		var queue = new RedoQueue(directory.resolve("queue").toString(), new Config());
		try {
			queue.start();
			queue.add(1, EmptyBean.instance);
			var rpc = new RunTask();
			rpc.Argument.setTaskId(1);
			rpc.Result.setTaskId(1);
			field("pending").set(queue, rpc);
			var db = (RocksDatabase)field("db").get(queue);
			var nativeField = RocksDatabase.class.getDeclaredField("rocksDb");
			nativeField.setAccessible(true);
			var original = db.getRocksDb();
			var failing = new FailingRocks(original, failPut);
			var callback = RedoQueue.class.getDeclaredMethod("processRunTaskResult", Zeze.Net.Rpc.class);
			callback.setAccessible(true);
			nativeField.set(db, failing);
			try {
				var thrown = assertThrows(InvocationTargetException.class, () -> callback.invoke(queue, rpc));
				assertInstanceOf(RocksDBException.class, thrown.getCause());
			} finally {
				nativeField.set(db, original);
				failing.releaseBorrowedHandle();
			}
			assertEquals(0L, field("lastDoneTaskId").getLong(queue), "失败不能发布未完整持久化的完成状态");
			assertNotNull(field("retryTask").get(queue), "pending清除后必须留有推进驱动");
			// 恢复I/O后同一任务的幂等成功应答推进水位、清除队列正文。
			field("pending").set(queue, rpc);
			assertEquals(0L, callback.invoke(queue, rpc));
			assertEquals(1L, field("lastDoneTaskId").getLong(queue));
			var key = ByteBuffer.Allocate();
			key.WriteLong(1);
			assertNull(((RocksDatabase.Table)field("tableTaskQueue").get(queue))
					.get(key.Bytes, 0, key.WriteIndex));
		} finally {
			queue.stop();
		}
	}

	private static Field field(String name) throws Exception {
		var field = RedoQueue.class.getDeclaredField(name);
		field.setAccessible(true);
		return field;
	}
}
