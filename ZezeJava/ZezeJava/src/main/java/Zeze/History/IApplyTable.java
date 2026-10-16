package Zeze.History;

import Zeze.Net.Binary;
import Zeze.Transaction.TableWalkHandleRaw;
import org.jspecify.annotations.Nullable;

/**
 * 回放库中一张表的原始 KV 访问接口：get/put/remove/walk/isEmpty。
 */
public interface IApplyTable {
	String getTableName();

	@Nullable Binary get(byte [] key, int offset, int length);

	void put(byte [] key, int keyOffset, int keyLength,
			 byte [] value, int valueOffset, int valueLength) throws Exception;

	void remove(byte [] key, int offset, int length) throws Exception;

	/**
	 * @return true if empty.
	 */
	boolean isEmpty() throws Exception;

	void walk(TableWalkHandleRaw walker) throws Exception;
}
