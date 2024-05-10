package Zeze.Transaction;

@FunctionalInterface
public interface TableWalkHandleRaw {
	boolean handle(byte[] key, byte[] value) throws Exception;
}
