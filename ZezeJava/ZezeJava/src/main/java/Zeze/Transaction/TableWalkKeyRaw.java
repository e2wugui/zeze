package Zeze.Transaction;

@FunctionalInterface
public interface TableWalkKeyRaw {
	boolean handle(byte[] key) throws Exception;
}
