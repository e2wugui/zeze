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

	/** 实现可以保留输入数组；调用方此后不得修改键和值的数组内容。 */
	void put(byte [] key, int keyOffset, int keyLength,
			 byte [] value, int valueOffset, int valueLength) throws Exception;

	/** 实现可以暂存输入数组；记录级事务结束前调用方不得修改键的数组内容。 */
	void remove(byte [] key, int offset, int length) throws Exception;

	/**
	 * @return true if empty.
	 */
	boolean isEmpty() throws Exception;

	/** 回调中的数组只供读取；需要在回调后使用时须复制。 */
	void walk(TableWalkHandleRaw walker) throws Exception;
}
