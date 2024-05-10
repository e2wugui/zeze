package Zeze.Transaction;

import java.sql.ResultSet;
import java.sql.SQLException;
import Zeze.Application;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.SQLStatement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Table 只读接口。
 * 模块返回这个类型的表格定义接口给其他模块读取用。
 */
public interface TableReadOnly<K extends Comparable<K>, V extends Bean, VReadOnly> {
	int getOriginalId();

	int getId();

	@NotNull String getOriginalName();

	@NotNull String getName();

	@NotNull String getSuffix();

	Application getZeze();

	boolean isMemory();

	boolean isAutoKey();

	boolean isUseRelationalMapping();

	int getCacheSize();

	@NotNull Binary encodeGlobalKey(@NotNull K key);

	@Nullable VReadOnly getReadOnly(@NotNull K key);

	boolean contains(@NotNull K key);

	@NotNull ByteBuffer encodeKey(@NotNull K key);

	@NotNull ByteBuffer encodeKey(@NotNull Object key);

	@NotNull K decodeKey(@NotNull ByteBuffer bb);

	@NotNull K decodeKey(byte @NotNull [] bytes);

	@NotNull K decodeKeyResultSet(@NotNull ResultSet rs) throws SQLException;

	void encodeKeySQLStatement(@NotNull SQLStatement st, @NotNull K _v_);

	void encodeKeySQLStatement(@NotNull SQLStatement st, @NotNull Object _v_);

	@NotNull V newValue();

	@NotNull V decodeValue(@NotNull ByteBuffer bb);

	@NotNull V decodeValue(byte @NotNull [] bytes);

	K walk(@Nullable K exclusiveStartKey, int proposeLimit, @NotNull TableWalkHandle<K, V> callback) throws Exception;

	K walkDesc(@Nullable K exclusiveStartKey, int proposeLimit, @NotNull TableWalkHandle<K, V> callback) throws Exception;

	K walkKey(@Nullable K exclusiveStartKey, int proposeLimit, @NotNull TableWalkKey<K> callback) throws Exception;

	K walkKeyDesc(@Nullable K exclusiveStartKey, int proposeLimit, @NotNull TableWalkKey<K> callback) throws Exception;

	long walk(@NotNull TableWalkHandle<K, V> callback) throws Exception;

	long walkDesc(@NotNull TableWalkHandle<K, V> callback) throws Exception;

	long walkKey(@NotNull TableWalkKey<K> callback) throws Exception;

	long walkKeyDesc(@NotNull TableWalkKey<K> callback) throws Exception;

	long walkCacheKey(@NotNull TableWalkKey<K> callback) throws Exception;

	long walkDatabaseRaw(@NotNull TableWalkHandleRaw callback) throws Exception;

	long walkDatabaseRawDesc(@NotNull TableWalkHandleRaw callback) throws Exception;

	long walkDatabaseRawKey(@NotNull TableWalkKeyRaw callback) throws Exception;

	long walkDatabaseRawKeyDesc(@NotNull TableWalkKeyRaw callback) throws Exception;

	ByteBuffer walkDatabaseRaw(@Nullable ByteBuffer exclusiveStartKey, int proposeLimit,
							   @NotNull TableWalkHandleRaw callback) throws Exception;

	ByteBuffer walkDatabaseRawDesc(@Nullable ByteBuffer exclusiveStartKey, int proposeLimit,
								   @NotNull TableWalkHandleRaw callback) throws Exception;

	ByteBuffer walkDatabaseRawKey(@Nullable ByteBuffer exclusiveStartKey, int proposeLimit,
								  @NotNull TableWalkKeyRaw callback) throws Exception;

	ByteBuffer walkDatabaseRawKeyDesc(@Nullable ByteBuffer exclusiveStartKey, int proposeLimit,
									  @NotNull TableWalkKeyRaw callback) throws Exception;

	long walkDatabase(@NotNull TableWalkHandle<K, V> callback) throws Exception;

	long walkDatabaseDesc(@NotNull TableWalkHandle<K, V> callback) throws Exception;

	long walkDatabaseKey(@NotNull TableWalkKey<K> callback) throws Exception;

	long walkDatabaseKeyDesc(@NotNull TableWalkKey<K> callback) throws Exception;

	K walkDatabase(@Nullable K exclusiveStartKey, int proposeLimit, @NotNull TableWalkHandle<K, V> callback) throws Exception;

	K walkDatabaseDesc(@Nullable K exclusiveStartKey, int proposeLimit, @NotNull TableWalkHandle<K, V> callback) throws Exception;

	K walkDatabaseKey(@Nullable K exclusiveStartKey, int proposeLimit, @NotNull TableWalkKey<K> callback) throws Exception;

	K walkDatabaseKeyDesc(@Nullable K exclusiveStartKey, int proposeLimit, @NotNull TableWalkKey<K> callback) throws Exception;

	long walkMemoryAny(TableWalkHandle<Object, Bean> handle) throws Exception;

	long walkMemory(@NotNull TableWalkHandle<K, V> callback) throws Exception;

	@Nullable V selectCopy(@NotNull K key);

	@Nullable V selectDirty(@NotNull K key);

	@Nullable V selectDirty(@NotNull K key, int ttl);

	boolean isNew();

	@NotNull ByteBuffer encodeChangeListenerWithSpecialName(@Nullable String specialName, @NotNull Object key,
															@NotNull Changes.Record r);
}
