package Zeze.Transaction;

import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface TableWalkHandle<K, V> {
	boolean handle(@NotNull K key, @NotNull V value) throws Exception;

	/**
	 * walk 结束时回调（含 handle 返回 false 中断后的提前结束），返回值即 walk 方法的返回值。
	 * count 口径：全量 walk（walk/walkDesc）传实际遍历条数，中断时为中断前条数；
	 * 带游标分页 walk（exclusiveStartKey+proposeLimit 形态）固定传 proposeLimit 上限，
	 * 末页实际条数少于上限时不传实数——分页调用方以返回的 lastKey 是否为 null 判断遍历结尾。
	 */
	default long endWalk(long count) throws Exception {
		// walk调用完成。
		return count; // 加count参数是为了简化return。
	}
}
