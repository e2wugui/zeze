package Zeze.Transaction;

@FunctionalInterface
public interface TableWalkHandle<K, V> {
	boolean handle(K key, V value) throws Exception;
}
