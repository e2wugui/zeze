package Zeze.Transaction;

import Zeze.Serialize.*;
import Zeze.*;

//C# TO JAVA CONVERTER TODO TASK: The C# 'new()' constraint has no equivalent in Java:
//ORIGINAL LINE: public sealed class Storage<K, V> : Storage where V : Bean, new()
public final class Storage<K, V extends Bean> extends Storage {
	private Table Table;
	public Table getTable() {
		return Table;
	}

	public Storage(Table<K, V> table, Database database, String tableName) {
		Table = table;
		setDatabaseTable(database.OpenTable(tableName));
	}

	private java.util.concurrent.ConcurrentHashMap<K, Record<K, V>> changed = new java.util.concurrent.ConcurrentHashMap<K, Record<K, V>>();
	private java.util.concurrent.ConcurrentHashMap<K, Record<K, V>> encoded = new java.util.concurrent.ConcurrentHashMap<K, Record<K, V>>();
	private java.util.concurrent.ConcurrentHashMap<K, Record<K, V>> snapshot = new java.util.concurrent.ConcurrentHashMap<K, Record<K, V>>();

	public void OnRecordChanged(Record<K, V> r) {
		changed.put(r.getKey(), r);
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
	 仅在 Checkpoint 中调用，同时只有一个线程执行。
	 没有得到任何锁。
	 
	 @return 
	*/
	@Override
	public int EncodeN() {
		int c = 0;
		for (var e : changed) {
			if (e.Value.TryEncodeN(changed, encoded)) {
				++c;
			}
		}
		return c;
	}

	/** 
	 仅在 Checkpoint 中调用，在 flushWriteLock 下执行。
	 
	 @return 
	*/
	@Override
	public int Encode0() {
		for (var e : changed) {
			e.Value.Encode0();
			encoded.put(e.getKey(), e.Value);
		}
		int cc = changed.size();
		changed.clear();
		return cc;
	}

	/** 
	 仅在 Checkpoint 中调用，在 flushWriteLock 下执行。
	 
	 @return 
	*/
	@Override
	public int Snapshot() {
		var tmp = snapshot;
		snapshot = encoded;
		encoded = tmp;
		int cc = snapshot.size();
		for (var e : snapshot) {
			e.Value.SavedTimestampForCheckpointPeriod = e.Value.Timestamp;
		}
		return cc;
	}

	/** 
	 仅在 Checkpoint 中调用。
	 没有拥有任何锁。
	 
	 @return 
	*/
	@Override
	public int Flush(Database.Transaction t) {
		int count = 0;
		for (var e : snapshot) {
			if (e.Value.Flush(t)) {
				++count;
			}
		}
		return count;
	}

	/** 
	 仅在 Checkpoint 中调用。
	 没有拥有任何锁。
	*/
	@Override
	public void Cleanup() {
		for (var e : snapshot) {
			e.Value.Cleanup();
		}
		snapshot.clear();
	}

	public V Find(K key, Table<K, V> table) {
		ByteBuffer value = getDatabaseTable().Find(table.EncodeKey(key));
		return null != value ? table.DecodeValue(value) : null;
	}

	@Override
	public void Close() {
		getDatabaseTable().Close();
	}
}