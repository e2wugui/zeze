package Zeze.Transaction;

import java.sql.ResultSet;
import java.sql.SQLException;
import Zeze.Application;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.SQLStatement;
import org.jspecify.annotations.Nullable;

/**
 * Table 只读接口。
 * 模块返回这个类型的表格定义接口给其他模块读取用。
 */
public interface TableReadOnly<K extends Comparable<K>, V extends Bean, VReadOnly> {
	int getOriginalId();

	int getId();

	String getOriginalName();

	String getName();

	String getSuffix();

	Application getZeze();

	boolean isMemory();

	boolean isAutoKey();

	int getCacheSize();

	Binary encodeGlobalKey(K key);

	@Nullable VReadOnly getReadOnly(K key);

	boolean contains(K key);

	ByteBuffer encodeKey(K key);

	ByteBuffer encodeKey(Object key);

	K decodeKey(ByteBuffer bb);

	K decodeKey(byte [] bytes);

	K decodeKeyResultSet(ResultSet rs) throws SQLException;

	void encodeKeySQLStatement(SQLStatement st, K _v_);

	void encodeKeySQLStatement(SQLStatement st, Object _v_);

	V newValue();

	V decodeValue(ByteBuffer bb);

	V decodeValue(byte [] bytes);

	@Nullable K walk(@Nullable K exclusiveStartKey, int proposeLimit,
					 TableWalkHandle<K, V> callback) throws Exception;

	@Nullable K walkDesc(@Nullable K exclusiveStartKey, int proposeLimit,
						 TableWalkHandle<K, V> callback) throws Exception;

	@Nullable K walkKey(@Nullable K exclusiveStartKey, int proposeLimit,
						TableWalkKey<K> callback) throws Exception;

	@Nullable K walkKeyDesc(@Nullable K exclusiveStartKey, int proposeLimit,
							TableWalkKey<K> callback) throws Exception;

	long walk(TableWalkHandle<K, V> callback) throws Exception;

	long walkDesc(TableWalkHandle<K, V> callback) throws Exception;

	long walkKey(TableWalkKey<K> callback) throws Exception;

	long walkKeyDesc(TableWalkKey<K> callback) throws Exception;

	long walkCacheKey(TableWalkKey<K> callback) throws Exception;

	long walkDatabaseRaw(TableWalkHandleRaw callback) throws Exception;

	long walkDatabaseRawDesc(TableWalkHandleRaw callback) throws Exception;

	long walkDatabaseRawKey(TableWalkKeyRaw callback) throws Exception;

	long walkDatabaseRawKeyDesc(TableWalkKeyRaw callback) throws Exception;

	@Nullable ByteBuffer walkDatabaseRaw(@Nullable ByteBuffer exclusiveStartKey, int proposeLimit,
										 TableWalkHandleRaw callback) throws Exception;

	@Nullable ByteBuffer walkDatabaseRawDesc(@Nullable ByteBuffer exclusiveStartKey, int proposeLimit,
											 TableWalkHandleRaw callback) throws Exception;

	@Nullable ByteBuffer walkDatabaseRawKey(@Nullable ByteBuffer exclusiveStartKey, int proposeLimit,
											TableWalkKeyRaw callback) throws Exception;

	@Nullable ByteBuffer walkDatabaseRawKeyDesc(@Nullable ByteBuffer exclusiveStartKey, int proposeLimit,
												TableWalkKeyRaw callback) throws Exception;

	long walkDatabase(TableWalkHandle<K, V> callback) throws Exception;

	long walkDatabaseDesc(TableWalkHandle<K, V> callback) throws Exception;

	long walkDatabaseKey(TableWalkKey<K> callback) throws Exception;

	long walkDatabaseKeyDesc(TableWalkKey<K> callback) throws Exception;

	@Nullable K walkDatabase(@Nullable K exclusiveStartKey, int proposeLimit,
							 TableWalkHandle<K, V> callback) throws Exception;

	@Nullable K walkDatabaseDesc(@Nullable K exclusiveStartKey, int proposeLimit,
								 TableWalkHandle<K, V> callback) throws Exception;

	@Nullable K walkDatabaseKey(@Nullable K exclusiveStartKey, int proposeLimit,
								TableWalkKey<K> callback) throws Exception;

	@Nullable K walkDatabaseKeyDesc(@Nullable K exclusiveStartKey, int proposeLimit,
									TableWalkKey<K> callback) throws Exception;

	long walkMemoryAny(TableWalkHandle<Object, Bean> handle) throws Exception;

	long walkMemory(TableWalkHandle<K, V> callback) throws Exception;

	@Nullable V selectCopy(K key);

	@Nullable V selectDirty(K key);

	@Nullable V selectDirty(K key, int ttl);

	boolean isNew();

	ByteBuffer encodeChangeListenerWithSpecialName(@Nullable String specialName, Object key,
															Changes.Record r);
}
