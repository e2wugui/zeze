package Zeze.Raft.RocksRaft;

public interface PessimismLock {
	void lock();

	void unlock();
}
