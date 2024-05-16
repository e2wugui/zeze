package Zeze.History;

import Zeze.Builtin.HistoryModule.BTableKey;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Changes;
import Zeze.Transaction.Table;
import Zeze.Transaction.TableX;
import Zeze.Util.Lru;
import Zeze.Util.Str;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ApplyTable<K extends Comparable<K>, V extends Bean> {
	private static final @NotNull Logger logger = LogManager.getLogger();
	private final Lru<K, V> lru = new Lru<>(4096);
	private final @NotNull IApplyTable table;
	private final @NotNull TableX<K, V> originTable;

	public ApplyTable(@NotNull TableX<K, V> originTable, @NotNull IApplyDatabase db) {
		this.originTable = originTable;
		var tableName = originTable.getName();
		table = db.open(tableName);
	}

	public @NotNull Table getOriginTable() {
		return originTable;
	}

	public @Nullable V get(@NotNull K key) {
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
	public K apply(@NotNull BTableKey tableKey, @NotNull Binary changes) throws Exception {
		var logRecord = new Changes.Record(originTable);
		logRecord.decode(ByteBuffer.Wrap(changes));
		var key = originTable.decodeKey(ByteBuffer.Wrap(tableKey.getKeyEncoded()));

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
		return key;
	}

	public void put(@NotNull K key, @NotNull V value) throws Exception {
		var bbKey = originTable.encodeKey(key);
		var bbValue = ByteBuffer.Allocate();
		value.encode(bbValue);
		lru.put(key, value);
		table.put(bbKey.Bytes, bbKey.ReadIndex, bbKey.size(), bbValue.Bytes, bbValue.ReadIndex, bbValue.size());
	}

	public void remove(@NotNull K key) throws Exception {
		lru.remove(key);
		var bbKey = originTable.encodeKey(key);
		table.remove(bbKey.Bytes, bbKey.ReadIndex, bbKey.size());
	}

	public static String diff(String strA, String strB, String skipIfContains) {
		var a = strA.split("\r?\n");
		var b = strB.split("\r?\n");

		var i = 0;
		// skip same line at head.
		for (; i < a.length && i < b.length; ++i) {
			if (!a[i].equals(b[i]) && (null == skipIfContains || !a[i].contains(skipIfContains)))
				break;
		}
		var aEnd = a.length - 1;
		var bEnd = b.length - 1;
		for (; aEnd > i && bEnd > i; aEnd--, bEnd--) {
			if (!a[aEnd].equals(b[bEnd]) && (null == skipIfContains || !a[aEnd].contains(skipIfContains)))
				break;
		}

		var sb = new StringBuilder();
		for (; i <= aEnd && i <= bEnd; ++i)
			sb.append(a[i]).append(Str.indent(50 - a[i].length())).append(b[i]).append('\n');
		if (aEnd > bEnd) {
			for (; i <= aEnd; ++i)
				sb.append(a[i]).append('\n');
		} else {
			for (; i <= bEnd; ++i)
				sb.append(Str.indent(50)).append(b[i]).append('\n');
		}
		return sb.toString();
	}

	public void verifyAndClear() throws Exception {
		originTable.walk((key, value) -> {
			var applyValue = get(key);
			if (null == applyValue)
				throw new RuntimeException("record miss. key=" + key);
			if (!applyValue.equals(value)) {
				var originText = value.toString();
				var applyText = applyValue.toString();

				throw new RuntimeException("record not equals. key=" + key + "\n"
						+ diff(originText, applyText, applyValue.versionVarName() + "="));

				//throw new RuntimeException("record not equals. key=" + key + "\n"
				//		+ originText + "\n" + applyText);
			}
			remove(key);
			return true;
		});

		if (!table.isEmpty()) {
			// dump
			logger.info("remain records in apply table:");
			table.walk((key, value) -> {
				logger.info("{}->{}", originTable.decodeKey(key), originTable.decodeValue(value));
				return true;
			});
			throw new RuntimeException("apply remain record.");
		}
	}

	public static void main(String[] args) {
		System.out.println("---");
		System.out.println(diff("abc\r\ndef\n123\nversion=1", "abc\nde\nghi\r\n123\nversion=2", "version="));
		System.out.println("---");
	}
}
