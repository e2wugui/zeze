package Zeze.Transaction;


@FunctionalInterface
public interface TableWalkHandle<K, V> {
	boolean handle(K key, V value) throws Exception;

	/**
	 * walk 结束时回调，包括 handle 返回 false 提前结束。
	 * 全量 walk 的 count 包括使遍历停止的记录，以及读取后被缓存过滤的记录，
	 * 本回调的返回值作为 walk 的返回值。正常分页请求的 count 为 proposeLimit，
	 * walk 返回续扫键，返回 null 表示遍历结束。
	 */
	default long endWalk(long count) throws Exception {
		// walk调用完成。
		return count; // 加count参数是为了简化return。
	}
}
