package Zeze.History;

import Zeze.Net.Binary;
import Zeze.Transaction.TableWalkHandleRaw;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface IApplyTable {
	@NotNull
	String getTableName();

	@Nullable
	Binary get(byte @NotNull [] key, int offset, int length);

	void put(byte @NotNull [] key, int keyOffset, int keyLength,
			 byte @NotNull [] value, int valueOffset, int valueLength) throws Exception;

	void remove(byte @NotNull [] key, int offset, int length) throws Exception;

	/**
	 * 注意这个操作可能很慢，谨慎使用。
	 * @return true if empty.
	 */
	boolean isEmpty() throws Exception;

	void walk(@NotNull TableWalkHandleRaw walker) throws Exception;
}
