package Zeze.Transaction;

import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Table 只读接口。
 * 模块返回这个类型的表格定义接口给其他模块读取用。
 */
public interface TableReadOnly<K extends Comparable<K>, V extends Bean, VReadOnly> {
	@NotNull Binary encodeGlobalKey(@NotNull K key);

	@Nullable VReadOnly getReadOnly(@NotNull K key);

	boolean contains(@NotNull K key);

	@NotNull ByteBuffer encodeKey(@NotNull K key);

	@NotNull ByteBuffer encodeKey(@NotNull Object key);

	@NotNull K decodeKey(@NotNull ByteBuffer bb);

	@NotNull V newValue();

	@NotNull V decodeValue(@NotNull ByteBuffer bb);

	long walk(@NotNull TableWalkHandle<K, V> callback);

	long walkCacheKey(@NotNull TableWalkKey<K> callback);

	long walkDatabaseKey(@NotNull TableWalkKey<K> callback);

	long walkDatabaseRaw(@NotNull TableWalkHandleRaw callback);

	long walkDatabase(@NotNull TableWalkHandle<K, V> callback);

	long walkMemory(@NotNull TableWalkHandle<K, V> callback);

	@Nullable V selectCopy(@NotNull K key);

	@Nullable V selectDirty(@NotNull K key);

	@NotNull ByteBuffer encodeChangeListenerWithSpecialName(@Nullable String specialName, @NotNull Object key,
															@NotNull Changes.Record r);
}
