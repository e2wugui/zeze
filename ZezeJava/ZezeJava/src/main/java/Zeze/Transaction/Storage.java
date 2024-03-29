package Zeze.Transaction;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.NotNull;

public final class Storage<K extends Comparable<K>, V extends Bean> {
	private final Table table;
	private final @NotNull Database.Table databaseTable;
	private final ConcurrentHashMap<K, Record1<K, V>> changed = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<K, Record1<K, V>> encoded = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<K, Record1<K, V>> snapshot = new ConcurrentHashMap<>();

	public Storage(@NotNull TableX<K, V> table, @NotNull Database database, @NotNull String tableName) {
		this.table = table;
		if (table.isRelationalMapping() && database instanceof DatabaseMySql) {
//			if (!(database instanceof DatabaseMySql))
//				throw new IllegalArgumentException("Only DatabaseMySql Support RelationalMapping.");
			var mysql = (DatabaseMySql)database;
			databaseTable = mysql.openRelationalTable(tableName);
			return; // done
		}
		databaseTable = database.openTable(tableName, table.getId());
	}

	public Table getTable() {
		return table;
	}

	public @NotNull Database.Table getDatabaseTable() {
		return databaseTable;
	}

	public void onRecordChanged(Record1<K, V> r) {
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
	public int encodeN() {
		int c = 0;
		for (var v : changed.values()) {
			if (v.tryEncodeN(changed, encoded))
				c++;
		}
		return c;
	}

	/**
	 * 仅在 Checkpoint 中调用，在 flushWriteLock 下执行。
	 *
	 * @return encoded record count
	 */
	public int encode0() {
		for (var e : changed.entrySet()) {
			e.getValue().encode0();
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
	public int snapshot() {
		snapshot.putAll(encoded);
		encoded.clear();
		return snapshot.size();
	}

	/**
	 * 仅在 Checkpoint 中调用。
	 * 没有拥有任何锁。
	 *
	 * @return flush record count
	 */
	public int flush(Database.Transaction t, HashMap<Database, Database.Transaction> tss, Database.Transaction lct) {
		for (var v : snapshot.values())
			v.flush(t, tss, lct);
		return snapshot.size();
	}

	/**
	 * 仅在 Checkpoint 中调用。
	 * 没有拥有任何锁。
	 */
	public void cleanup() {
		for (var v : snapshot.values())
			v.cleanup();
		snapshot.clear();
	}

	public void close() {
		databaseTable.close();
	}
}
