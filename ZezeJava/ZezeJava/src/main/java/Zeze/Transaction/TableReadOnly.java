package Zeze.Transaction;

import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;

/**
 * Table 只读接口。
 * 模块返回这个类型的表格定义接口给其他模块读取用。
 */
public interface TableReadOnly<K extends Comparable<K>, V extends Bean, VReadOnly> {
	Binary encodeGlobalKey(K key);

	VReadOnly getReadOnly(K key);

	boolean contains(K key);

	ByteBuffer encodeKey(K key);

	ByteBuffer encodeKey(Object key);

	K decodeKey(ByteBuffer bb);

	V newValue();

	V decodeValue(ByteBuffer bb);

	long walk(TableWalkHandle<K, V> callback);

	long walk(TableWalkHandle<K, V> callback, Runnable afterLock);

	long walkCacheKey(TableWalkKey<K> callback);

	long walkDatabaseKey(TableWalkKey<K> callback);

	long walkDatabase(TableWalkHandleRaw callback);

	long walkDatabase(TableWalkHandle<K, V> callback);

	long walkMemory(TableWalkHandle<K, V> callback);

	long walkMemory(TableWalkHandle<K, V> callback, Runnable afterLock);

	V selectCopy(K key);

	V selectDirty(K key);

	ByteBuffer encodeChangeListenerWithSpecialName(String specialName, Object key, Changes.Record r);
}
