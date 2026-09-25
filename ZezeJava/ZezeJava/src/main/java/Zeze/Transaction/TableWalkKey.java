package Zeze.Transaction;

import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface TableWalkKey<K> {
	boolean handle(@NotNull K key) throws Exception;
	/**
	 * walk 结束时回调（含 handle 返回 false 中断后的提前结束），返回值即 walk 方法的返回值。
	 * count 口径：全量 walkKey 传实际遍历条数，中断时为中断前条数；带游标分页形态
	 * （exclusiveStartKey+proposeLimit）固定传 proposeLimit 上限，末页不传实数——
	 * 分页调用方以返回的 lastKey 是否为 null 判断遍历结尾。详见 TableWalkHandle.endWalk。
	 */
	default long endWalk(long count) {
		return count;
	}
}
