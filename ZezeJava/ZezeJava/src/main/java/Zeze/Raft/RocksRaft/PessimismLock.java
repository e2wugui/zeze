package Zeze.Raft.RocksRaft;

/**
 * 悲观锁接口：事务期间持锁串行化同 key 访问（如 GCM 的记录锁）。
 */
public interface PessimismLock {
	void lock();

	void unlock();
}
