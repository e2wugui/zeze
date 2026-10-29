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

	/** 写入后调用方可复用输入数组；记录级事务的暂存也必须保留独立的键和值。 */
	void put(byte [] key, int keyOffset, int keyLength,
			 byte [] value, int valueOffset, int valueLength) throws Exception;

	/** 返回后调用方可复用输入数组，即使删除仍在记录级事务中暂存。 */
	void remove(byte [] key, int offset, int length) throws Exception;

	/**
	 * @return true if empty.
	 */
	boolean isEmpty() throws Exception;

	/** 回调中的数组可由调用方持有或修改，不得暴露存储内部的键和值。 */
	void walk(TableWalkHandleRaw walker) throws Exception;
}
