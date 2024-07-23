package Zeze.Transaction;

import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface TableWalkHandle<K, V> {
	boolean handle(@NotNull K key, @NotNull V value) throws Exception;
}
