package Zeze.History;

import java.util.regex.Pattern;
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
import org.jspecify.annotations.Nullable;

/**
 * 回放侧的业务表封装：带 LRU 缓存读写回放库表，按 tHistory 记录中的 Put/Remove/Edit 变更应用。
 */
public class ApplyTable<K extends Comparable<K>, V extends Bean> {
	private static final Logger logger = LogManager.getLogger(ApplyTable.class);
	private static final Pattern diffPat = Pattern.compile("\r?\n");
	private final Lru<K, V> lru = new Lru<>(4096);
	private final IApplyTable table;
	private final TableX<K, V> originTable;

	public static ApplyTable<?, ?> create(Table table, IApplyDatabase db) {
		if (!(table instanceof TableX<?, ?> origin))
			throw new IllegalArgumentException("history replay requires TableX: " + table.getName());
		return new ApplyTable<>(origin, db);
	}

	public ApplyTable(TableX<K, V> originTable, IApplyDatabase db) {
		this.originTable = originTable;
		var tableName = originTable.getName();
		table = db.open(tableName);
	}

	public Table getOriginTable() {
		return originTable;
	}

	public @Nullable V get(K key) {
		var value = lru.get(key);
		if (value != null)
			return value;
		var bbKey = originTable.encodeKey(key);
		var valueRaw = table.get(bbKey.Bytes, bbKey.ReadIndex, bbKey.size());
		if (valueRaw == null)
			return null;
		value = originTable.newValue();
		value.decode(ByteBuffer.Wrap(valueRaw));
		lru.put(key, value);
		return value;
	}

	public K apply(BTableKey tableKey, Binary changes) throws Exception {
		var logRecord = new Changes.Record(originTable);
		logRecord.decode(ByteBuffer.Wrap(changes));
		var key = originTable.decodeKey(ByteBuffer.Wrap(tableKey.getKeyEncoded()));

		switch (logRecord.getState()) {
		case Changes.Record.Remove:
			remove(key);
			break;

		case Changes.Record.Put:
			//noinspection DataFlowIssue,unchecked
			put(key, (V)logRecord.getValue());
			break;

		case Changes.Record.Edit:
			var value = get(key);
			// 目标行缺失时由后续调用直接抛 NPE 中断回放批，不静默新建或跳过；log 为 null 是合法空编辑。
			var log = logRecord.getLogBean();
			if (log != null)
				//noinspection DataFlowIssue
				value.followerApply(log);
			//noinspection DataFlowIssue
			put(key, value);
			break;

		default:
			throw new RuntimeException("unknown Changes.Record state=" + logRecord.getState()
					+ ", table=" + originTable.getName());
		}
		return key;
	}

	public void put(K key, V value) throws Exception {
		var bbKey = originTable.encodeKey(key);
		var bbValue = ByteBuffer.Allocate();
		value.encode(bbValue);
		lru.put(key, value);
		table.put(bbKey.Bytes, bbKey.ReadIndex, bbKey.size(), bbValue.Bytes, bbValue.ReadIndex, bbValue.size());
	}

	public void remove(K key) throws Exception {
		lru.remove(key);
		var bbKey = originTable.encodeKey(key);
		table.remove(bbKey.Bytes, bbKey.ReadIndex, bbKey.size());
	}

	/** 失效指定键的 LRU 缓存，供记录级事务回滚时清理已写入或被原地修改的脏缓存。 */
	public void invalidate(BTableKey tableKey) {
		lru.remove(originTable.decodeKey(ByteBuffer.Wrap(tableKey.getKeyEncoded())));
	}

	public static String diff(String strA, String strB, @Nullable String skipIfContains) {
		var a = diffPat.split(strA);
		var b = diffPat.split(strB);

		var i = 0;
		// skip same line at head.
		for (; i < a.length && i < b.length; ++i) {
			if (!a[i].equals(b[i]) && (skipIfContains == null || !a[i].contains(skipIfContains)))
				break;
		}
		var aEnd = a.length - 1;
		var bEnd = b.length - 1;
		for (; aEnd > i && bEnd > i; aEnd--, bEnd--) {
			if (!a[aEnd].equals(b[bEnd]) && (skipIfContains == null || !a[aEnd].contains(skipIfContains)))
				break;
		}

		var sb = new StringBuilder();
		for (; i <= aEnd && i <= bEnd; ++i)
			sb.append(a[i]).append(Str.indent(Math.max(0, 50 - a[i].length()))).append(b[i]).append('\n');
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
			if (applyValue == null)
				throw new RuntimeException("record miss. key=" + key + ", table=" + originTable.getName());
			if (!applyValue.equals(value)) {
				var originText = value.toString();
				var applyText = applyValue.toString();
				throw new RuntimeException("record not equals. key=" + key + "\n"
						+ diff(originText, applyText, applyValue.versionVarName() + "="));
			}
			remove(key);
			return true;
		});

		if (!table.isEmpty()) {
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
