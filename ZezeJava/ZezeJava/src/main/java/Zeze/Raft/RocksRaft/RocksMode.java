package Zeze.Raft.RocksRaft;

/**
 * RocksRaft 并发模式。
 * <p>
 * 当前仅支持 {@link #Pessimism}：同 key 并发访问由调用方通过显式 {@link PessimismLock}
 * （{@link Transaction#addPessimismLock(PessimismLock)}）或外部串行化机制保证隔离。
 */
public enum RocksMode {
	/** 悲观模式：由调用方显式持 {@link PessimismLock} 或外部串行化保证同 key 并发安全。 */
	Pessimism,
	/**
	 * @deprecated 乐观并发控制未实现（{@link Transaction} 不做版本冲突检测也无 redo 重试循环，
	 * 并发修改同一 key 会静默丢更新）。{@link Rocks} 构造器会在初始化任何 Raft/RocksDB 资源前
	 * 直接抛出 {@link UnsupportedOperationException}；请改用 {@link #Pessimism} 配合显式
	 * {@link PessimismLock}（{@link Transaction#addPessimismLock(PessimismLock)}）。
	 */
	@Deprecated
	Optimistic,
}
