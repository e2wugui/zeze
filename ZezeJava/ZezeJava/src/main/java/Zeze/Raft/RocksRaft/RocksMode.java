package Zeze.Raft.RocksRaft;

/**
 * RocksRaft 并发模式：悲观（显式悲观锁）或乐观。
 */
public enum RocksMode {
	Pessimism,
	Optimistic,
}
