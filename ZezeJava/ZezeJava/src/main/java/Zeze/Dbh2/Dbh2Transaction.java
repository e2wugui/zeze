package Zeze.Dbh2;

import java.io.Closeable;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Builtin.Dbh2.BBatch;
import Zeze.Net.Binary;
import Zeze.Util.RocksDatabase;
import com.alibaba.druid.sql.visitor.functions.Bin;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

public class Dbh2Transaction implements Closeable {
	private final HashMap<Lock, Lock> locks;
	private final Bucket bucket;
	private final BBatch.Data batch;
	private final BBatch.Data logs;

	/*
	 * firstCreate, raftCreate 从正常操作流程上，可以统一。
	 * 但是为了不同的错误处理，分成两个方法。
	 * firstCreate 需要检查是否在桶中，以及锁定失败时，报告错误给PrepareBatch请求放。
	 * raftCreate 不检查是否在桶中，锁定必须成功。出现错误，raft强制退出。
	 */
	public static Dbh2Transaction firstCreate(Bucket bucket, BBatch.Data batch) throws RocksDBException {
		var locks = new HashMap<Lock, Lock>();
		try {
			var logs = new BBatch.Data();
			var result = new Dbh2Transaction(bucket, batch, logs, locks);
			for (var put : batch.getPuts().entrySet()) {
				var key = put.getKey();
				if (!bucket.inBucket(key))
					return null;

				var lock = Lock.get(key);
				if (null == locks.putIfAbsent(lock, lock))
					lock.lock();

				var exist = bucket.get(key);
				if (exist != null)
					logs.getPuts().put(key, exist);
				else
					logs.getDeletes().add(key);
			}

			for (var del : batch.getDeletes()) {
				var lock = Lock.get(del);
				if (null == locks.putIfAbsent(lock, lock))
					lock.lock();

				var exist = bucket.get(del);
				if (exist != null)
					logs.getPuts().put(del, exist);
				// else delete not exist record. skip.
			}

			locks = null; // 释放拥有，finally就不会unlock。
			return result;

		} finally {
			// 失败的时候释放锁。
			if (null != locks) {
				for (var lock : locks.keySet())
					lock.unlock();
			}
		}
	}

	public static Dbh2Transaction raftCreate(Bucket bucket, BBatch.Data batch, BBatch.Data logs) {
		var locks = new HashMap<Lock, Lock>();
		try {
			var result = new Dbh2Transaction(bucket, batch, logs, locks);
			for (var put : batch.getPuts().entrySet()) {
				var key = put.getKey();
				var lock = Lock.get(key);
				if (null == locks.putIfAbsent(lock, lock))
					lock.lock();
			}

			for (var del : batch.getDeletes()) {
				var lock = Lock.get(del);
				if (null == locks.putIfAbsent(lock, lock))
					lock.lock();
			}

			locks = null; // 释放拥有，finally就不会unlock。
			return result;

		} finally {
			// 失败的时候释放锁。
			if (null != locks) {
				for (var lock : locks.keySet())
					lock.unlock();
			}
		}
	}

	private Dbh2Transaction(Bucket bucket, BBatch.Data batch, BBatch.Data logs, HashMap<Lock, Lock> locks) {
		this.bucket = bucket;
		this.batch = batch;
		this.logs = logs;
		this.locks = locks;
	}

	@Override
	public void close() {
		for (var lock : locks.values())
			lock.unlock();
		locks.clear();
	}
}
