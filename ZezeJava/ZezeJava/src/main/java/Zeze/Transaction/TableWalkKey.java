package Zeze.Transaction;

import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface TableWalkKey<K> {
	boolean handle(@NotNull K key) throws Exception;
}
