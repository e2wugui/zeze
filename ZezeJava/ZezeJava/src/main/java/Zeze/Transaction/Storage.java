package Zeze.Transaction;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Serialize.ByteBuffer;

public final class Storage<K extends Comparable<K>, V extends Bean> {
	private final Table Table;
	private final Database.Table DatabaseTable;
	private final ConcurrentHashMap<K, Record1<K, V>> changed = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<K, Record1<K, V>> encoded = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<K, Record1<K, V>> snapshot = new ConcurrentHashMap<>();

	public Storage(TableX<K, V> table, Database database, String tableName) {
		Table = table;
		DatabaseTable = database.OpenTable(tableName);
	}

	public Table getTable() {
		return Table;
	}

	public Database.Table getDatabaseTable() {
		return DatabaseTable;
	}

	public V Find(K key, TableX<K, V> table) {
		ByteBuffer value = DatabaseTable.Find(table.EncodeKey(key));
		return value != null ? table.DecodeValue(value) : null;
	}

	public void OnRecordChanged(Record1<K, V> r) {
		changed.put(r.getObjectKey(), r);
	}

	/*
	 * Not Need Now. See Record.Dirty
	internal bool IsRecordChanged(K key)
	{
	    if (changed.TryGetValue(key, out var _))
	        return true;
	    if (encoded.TryGetValue(key, out var _))
	        return true;
	    return false;
	}
	*/

	/**
	 * 仅在 Checkpoint 中调用，同时只有一个线程执行。
	 * 没有得到任何锁。
	 *
	 * @return encoded record count
	 */
	public int EncodeN() {
		int c = 0;
		for (var v : changed.values()) {
			if (v.TryEncodeN(changed, encoded)) {
				++c;
			}
		}
		return c;
	}

	/**
	 * 仅在 Checkpoint 中调用，在 flushWriteLock 下执行。
	 *
	 * @return encoded record count
	 */
	public int Encode0() {
		for (var e : changed.entrySet()) {
			e.getValue().Encode0();
			encoded.put(e.getKey(), e.getValue());
		}
		int cc = changed.size();
		changed.clear();
		return cc;
	}

	/**
	 * 仅在 Checkpoint 中调用，在 flushWriteLock 下执行。
	 *
	 * @return snapshot record count
	 */
	public int Snapshot() {
		snapshot.putAll(encoded);
		encoded.clear();
		int cc = snapshot.size();
		for (var v : snapshot.values()) {
			v.setSavedTimestampForCheckpointPeriod(v.getTimestamp());
		}
		return cc;
	}

	/**
	 * 仅在 Checkpoint 中调用。
	 * 没有拥有任何锁。
	 *
	 * @return flush record count
	 */
	public int Flush(Database.Transaction t, HashMap<Database, Database.Transaction> tss, Database.Transaction lct) {
		for (var v : snapshot.values()) {
			v.Flush(t, tss, lct);
		}
		return snapshot.size();
	}

	/**
	 * 仅在 Checkpoint 中调用。
	 * 没有拥有任何锁。
	 */
	public void Cleanup() {
		for (var v : snapshot.values()) {
			v.Cleanup();
		}
		snapshot.clear();
	}

	public void Close() {
		DatabaseTable.Close();
	}
}
