package Zeze.Dbh2;

import java.util.concurrent.ConcurrentHashMap;
import org.rocksdb.RocksDBException;

public class Dbh2Transaction {
	private final org.rocksdb.Transaction transaction;
	private final ConcurrentHashMap<Lock, Lock> locks = new ConcurrentHashMap<>();

	public Dbh2Transaction(org.rocksdb.Transaction transaction) {
		this.transaction = transaction;
	}

	private void tryAddAndLock(Lock lock) {
		if (null == locks.putIfAbsent(lock, lock))
			lock.lock();
	}

	private void unlockAll() {
		for (var lock : locks.values())
			lock.unlock();
		locks.clear();
	}

	public void put(byte[] key, byte[] value) throws RocksDBException {
		var lock = Lock.get(key);
		tryAddAndLock(lock);
		transaction.put(key, value);
	}

	public void delete(byte[] key) throws RocksDBException {
		var lock = Lock.get(key);
		tryAddAndLock(lock);
		transaction.delete(key);
	}

	public void rollback() throws RocksDBException {
		transaction.rollback();
	}

	public void commit() throws RocksDBException {
		transaction.commit();
	}

	public void close() {
		transaction.close();
		unlockAll();
	}
}
