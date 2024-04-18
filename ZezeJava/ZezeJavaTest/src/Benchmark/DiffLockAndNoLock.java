package Benchmark;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.RocksDatabase;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.rocksdb.RocksDBException;

public class DiffLockAndNoLock {
	Zeze.Util.RocksDatabase db;
	RocksDatabase.Table table;
	final AtomicLong atomicKey = new AtomicLong();
	final ReentrantLock lock = new ReentrantLock();

	@Before
	public void before() throws RocksDBException {
		db = new Zeze.Util.RocksDatabase("DiffLockAndNoLock");
		table = db.getOrAddTable("testDiff");
	}

	@After
	public void after() {
		db.close();
	}

	@Test
	public void testDiff() throws RocksDBException, InterruptedException {
		var threads = new ArrayList<Thread>();
		var threadCount = 8;
		for (int i = 0; i < threadCount; ++i)
			threads.add(new Thread(this::lockPut));
		var bMulti = new Zeze.Util.Benchmark();
		for (var thread : threads)
			thread.start();
		for (var thread : threads)
			thread.join();
		bMulti.report("MultiThreadWithLock", taskPerThreadCount * threadCount);
		var bSingle = new Zeze.Util.Benchmark();
		for (int i = 0; i < taskPerThreadCount * threadCount; ++i)
			put();
		bSingle.report("SingleThreadWithoutLock", taskPerThreadCount * threadCount);
	}

	public final static int taskPerThreadCount = 10_000;

	public void put() throws RocksDBException {
		var key = ByteBuffer.Allocate();
		key.WriteLong(atomicKey.incrementAndGet());
		var value = Zeze.Util.Random.nextBytes(100);
		table.put(key.Bytes, key.ReadIndex, key.size(), value, 0, value.length);
	}

	public void lockPut() {
		for (int i = 0; i < taskPerThreadCount; ++i) {
			lock.lock();
			try {
				put();
			} catch (RocksDBException e) {
				throw new RuntimeException(e);
			} finally {
				lock.unlock();
			}
		}
	}
}
