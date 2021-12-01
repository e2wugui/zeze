package Zeze.Transaction;

import Zeze.Serialize.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ConcurrentHashMap;

public final class Storage1<K extends Comparable<K>, V extends Bean> extends Storage {

	private static final Logger logger = LogManager.getLogger(Storage1.class);
	private final Table Table;
	public Table getTable() {
		return Table;
	}

	public Storage1(TableX<K, V> table, Database database, String tableName) {
		Table = table;
		setDatabaseTable(database.OpenTable(tableName));
	}

	private final ConcurrentHashMap<K, Record1<K, V>> changed = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<K, Record1<K, V>> encoded = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<K, Record1<K, V>> snapshot = new ConcurrentHashMap<>();

	public void OnRecordChanged(Record1<K, V> r) {
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
	 
	 @return encoded record count
	*/
	@Override
	public int EncodeN() {
		int c = 0;
		for (var e : changed.entrySet()) {
			if (e.getValue().TryEncodeN(changed, encoded)) {
				++c;
			}
		}
		return c;
	}

	/** 
	 仅在 Checkpoint 中调用，在 flushWriteLock 下执行。
	 
	 @return encoded record count
	*/
	@Override
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
	 仅在 Checkpoint 中调用，在 flushWriteLock 下执行。
	 
	 @return snapshot record count
	*/
	@Override
	public int Snapshot() {
		snapshot.putAll(encoded);
		encoded.clear();
		int cc = snapshot.size();
		for (var e : snapshot.entrySet()) {
			e.getValue().setSavedTimestampForCheckpointPeriod(e.getValue().getTimestamp());
		}
		return cc;
	}

	/** 
	 仅在 Checkpoint 中调用。
	 没有拥有任何锁。
	 
	 @return flush record count
	*/
	@Override
	public int Flush(Database.Transaction t) {
		for (var e : snapshot.entrySet()) {
			e.getValue().Flush(t);
		}
		return snapshot.size();
	}

	/** 
	 仅在 Checkpoint 中调用。
	 没有拥有任何锁。
	*/
	@Override
	public void Cleanup() {
		for (var e : snapshot.entrySet()) {
			e.getValue().Cleanup();
		}
		snapshot.clear();
	}

	public V Find(K key, TableX<K, V> table) {
		ByteBuffer value = getDatabaseTable().Find(table.EncodeKey(key));
		return null != value ? table.DecodeValue(value) : null;
	}

	@Override
	public void Close() {
		getDatabaseTable().Close();
	}
}