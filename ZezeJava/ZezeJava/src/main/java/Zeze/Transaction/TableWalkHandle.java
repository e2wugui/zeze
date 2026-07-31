package Zeze.Transaction;

import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface TableWalkHandle<K, V> {
	boolean handle(@NotNull K key, @NotNull V value) throws Exception;

	default long endWalk(long count) throws Exception {
		// walk调用完成。
		return count; // 加count参数是为了简化return。
	}
}
