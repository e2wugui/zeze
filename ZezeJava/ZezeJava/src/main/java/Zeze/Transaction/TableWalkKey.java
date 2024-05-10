package Zeze.Transaction;

@FunctionalInterface
public interface TableWalkKey<K> {
	boolean handle(K key) throws Exception;
}
