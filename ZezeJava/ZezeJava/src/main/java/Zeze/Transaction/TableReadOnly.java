package Zeze.Transaction;

import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;

/**
 * Table 只读接口。
 * 模块返回这个类型的表格定义接口给其他模块读取用。
 */
public interface TableReadOnly<K extends Comparable<K>, V extends Bean, VReadOnly> {
	public Binary encodeGlobalKey(K key);

	public VReadOnly getReadOnly(K key);

	public boolean contains(K key);

	public ByteBuffer encodeKey(K key);

	public ByteBuffer encodeKey(Object key);

	public K decodeKey(ByteBuffer bb);

	public V newValue();

	public V decodeValue(ByteBuffer bb);

	public long walk(TableWalkHandle<K, V> callback);

	public long walk(TableWalkHandle<K, V> callback, Runnable afterLock);

	public long walkCacheKey(TableWalkKey<K> callback);

	public long walkDatabaseKey(TableWalkKey<K> callback);

	public long walkDatabase(TableWalkHandleRaw callback);

	public long walkDatabase(TableWalkHandle<K, V> callback);

	public long walkCache(TableWalkHandle<K, V> callback);

	public long walkCache(TableWalkHandle<K, V> callback, Runnable afterLock);

	public V selectCopy(K key);

	public V selectDirty(K key);

	public ByteBuffer encodeChangeListenerWithSpecialName(String specialName, Object key, Changes.Record r);
}
