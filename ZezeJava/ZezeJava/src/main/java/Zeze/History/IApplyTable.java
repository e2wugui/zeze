package Zeze.History;

import Zeze.Net.Binary;
import Zeze.Transaction.TableWalkHandleRaw;

public interface IApplyTable {
	String getTableName();
	Binary get(byte[] key, int offset, int length);
	void put(byte[] key, int keyOffset, int keyLength, byte[] value, int valueOffset, int valueLength);
	void remove(byte[] key, int offset, int length);
	boolean isEmpty();
	void walk(TableWalkHandleRaw walker);
}
