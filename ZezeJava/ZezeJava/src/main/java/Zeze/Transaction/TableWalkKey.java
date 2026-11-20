package Zeze.Transaction;


@FunctionalInterface
public interface TableWalkKey<K> {
	boolean handle(K key) throws Exception;
	/**
	 * 遍历结束时回调，包括 handle 返回 false 提前结束。全量遍历的 count 包括
	 * 使遍历停止的记录和被缓存过滤的记录。正常分页请求的 count 为 proposeLimit，
	 * 遍历方法返回续扫键，返回 null 表示遍历结束。详见 {@link TableWalkHandle#endWalk(long)}。
	 */
	default long endWalk(long count) {
		return count;
	}
}
