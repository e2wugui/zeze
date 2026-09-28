package Zeze.History;

import Zeze.Net.Binary;
import Zeze.Transaction.TableWalkHandleRaw;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 回放库中一张表的原始 KV 访问接口：get/put/remove/walk/isEmpty。
 */
public interface IApplyTable {
	@NotNull String getTableName();

	@Nullable Binary get(byte @NotNull [] key, int offset, int length);

	void put(byte @NotNull [] key, int keyOffset, int keyLength,
			 byte @NotNull [] value, int valueOffset, int valueLength) throws Exception;

	void remove(byte @NotNull [] key, int offset, int length) throws Exception;

	/**
	 * @return true if empty.
	 */
	boolean isEmpty() throws Exception;

	void walk(@NotNull TableWalkHandleRaw walker) throws Exception;
}
