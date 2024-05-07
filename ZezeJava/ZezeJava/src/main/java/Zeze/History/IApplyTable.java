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
			 byte @NotNull [] value, int valueOffset, int valueLength);

	void remove(byte @NotNull [] key, int offset, int length);

	boolean isEmpty();

	void walk(@NotNull TableWalkHandleRaw walker);
}
