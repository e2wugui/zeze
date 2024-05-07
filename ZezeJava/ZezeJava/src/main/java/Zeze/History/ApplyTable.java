package Zeze.History;

import java.util.LinkedHashMap;
import Zeze.Builtin.DelayRemove.BTableKey;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Changes;
import Zeze.Transaction.Table;
import Zeze.Transaction.TableX;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ApplyTable<K extends Comparable<K>, V extends Bean> {
	private static final Logger logger = LogManager.getLogger();
	private final LinkedHashMap<K, V> lru = new LinkedHashMap<>();
	private final IApplyTable table;
	private final TableX<K, V> originTable;

	public ApplyTable(TableX<K, V> originTable, IApplyDatabase db) {
		this.originTable = originTable;
		var tableName = originTable.getName();
		this.table = db.open(tableName);
	}

	public Table getOriginTable() {
		return originTable;
	}

	public V get(K key) {
		var value = lru.get(key);
		if (null != value)
			return value;
		var bbKey = originTable.encodeKey(key);
		var valueRaw = table.get(bbKey.Bytes, bbKey.ReadIndex, bbKey.size());
		if (null == valueRaw)
			return null;
		value = originTable.newValue();
		value.decode(ByteBuffer.Wrap(valueRaw));
		lru.put(key, value);
		return value;
	}

	@SuppressWarnings("unchecked")
	public void apply(BTableKey tableKey, Binary changes) {
		var logRecord = new Changes.Record(originTable);
		logRecord.decode(ByteBuffer.Wrap(changes));
		var key = originTable.decodeKey(ByteBuffer.Wrap(tableKey.getEncodedKey()));

		switch (logRecord.getState()) {
		case Changes.Record.Remove:
			remove(key);
			break;

		case Changes.Record.Put:
			put(key, (V)logRecord.getValue());
			break;

		case Changes.Record.Edit:
			var value = get(key);
			if (null == value)
				value = originTable.newValue();
			var log = logRecord.getLogBean();
			if (log != null)
				value.followerApply(log);
			put(key, value);
			break;
		}
	}

	public void put(K key, V value) {
		var bbKey = originTable.encodeKey(key);
		var bbValue = ByteBuffer.Allocate();
		value.encode(bbValue);
		lru.put(key, value);
		table.put(bbKey.Bytes, bbKey.ReadIndex, bbKey.size(), bbValue.Bytes, bbValue.ReadIndex, bbValue.size());
	}

	public void remove(K key) {
		lru.remove(key);
		var bbKey = originTable.encodeKey(key);
		table.remove(bbKey.Bytes, bbKey.ReadIndex, bbKey.size());
	}

	public void verifyAndClear() {
		originTable.walk((key, value) -> {
			var applyValue = get(key);
			if (null == applyValue)
				throw new RuntimeException("record miss. key=" + key);
			if (!applyValue.equals(value))
				throw new RuntimeException("record not equals. key=" + key + "\norigin:"
						+ value + "\napply: " + applyValue + "\n");
			remove(key);
			return true;
		});

		if (!table.isEmpty()) {
			// dump
			logger.info("remain records in apply table:");
			table.walk((key, value) -> {
				logger.info(originTable.decodeKey(key) + "->" + originTable.decodeValue(value));
				return true;
			});
			throw new RuntimeException("apply remain record.");
		}
	}
}
