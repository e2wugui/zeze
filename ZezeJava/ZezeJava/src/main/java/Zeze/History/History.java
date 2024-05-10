package Zeze.History;

import java.util.HashMap;
import Zeze.Builtin.HistoryModule.BLogChanges;
import Zeze.Builtin.HistoryModule.BTableKey;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Changes;
import Zeze.Transaction.Database;
import Zeze.Transaction.TableKey;
import Zeze.Util.LongConcurrentHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class History {
	// private static final Logger logger = LogManager.getLogger(History.class);

	// 为了节约内存，在确实需要的时候才分配。
	// 为了在锁外并发。使用并发Map，否则ArrayList或者自己实现的支持splice的连接表效率更高。
	private volatile LongConcurrentHashMap<LogChangesRaw> logChanges;

	// 这里为了并发接收数据，不能优化为可null？需要确认。
	private final LongConcurrentHashMap<Binary> encoded = new LongConcurrentHashMap<>();

	public History() {
	}

	public History(LogChangesRaw firstData) {
		addLogChanges(firstData);
	}

	public void addLogChanges(LogChangesRaw _logChanges) {
		if (null == logChanges)
			logChanges = new LongConcurrentHashMap<>();
		logChanges.put(_logChanges.getGlobalSerialId(), _logChanges);
	}

	public void encodeN() {
		// rrs 锁外
		var changes = logChanges; // volatile
		if (null != changes) {
			changes.forEach((v) -> {
				// logChanges只要系列号一样，表示内容一样，所以，只要key存在，不需要再encode一次。
				encoded.computeIfAbsent(v.getGlobalSerialId(), (key) -> {
					var bb = ByteBuffer.Allocate();
					v.encode(bb);
					return new Binary(bb);
				});
				// encodeN跟merge并发，它本身的执行不会并发，由Checkpoint调度。
				// 所以remove前后无所谓。
				changes.remove(v.getGlobalSerialId());
			});
		}
	}

	public void encode0() {
		// 锁内
		var changes = logChanges;
		if (null != changes) {
			changes.forEach((v) -> {
				// logChanges只要系列号一样，表示内容一样，所以，只要key存在，不需要再encode一次。
				encoded.computeIfAbsent(v.getGlobalSerialId(), (key) -> {
					var bb = ByteBuffer.Allocate();
					v.encode(bb);
					return new Binary(bb);
				});
			});
			changes.clear();
		}
	}

	public void flush(Database.Table table, Database.Transaction txn) {
		// 但仅仅Checkpoint访问，不需要加锁。现实也在锁内。
		//logger.debug("flush: {}", encoded.size());
		for (var it = encoded.entryIterator(); it.moveToNext(); /**/) {
			var key = ByteBuffer.Allocate();
			key.WriteLong(it.key());
			var value = ByteBuffer.Wrap(it.value());
			table.replace(txn, key, value);
		}
		encoded.clear();
	}

	public static void putLogChangesAll(@NotNull LongConcurrentHashMap<LogChangesRaw> to,
										@NotNull LongConcurrentHashMap<LogChangesRaw> other) {
		other.forEach((v) -> to.putIfAbsent(v.getGlobalSerialId(), v));
	}

	public static void putEncodedAll(@NotNull LongConcurrentHashMap<Binary> to,
									 @NotNull LongConcurrentHashMap<Binary> other) {
		for (var it = other.entryIterator(); it.moveToNext(); /**/)
			to.putIfAbsent(it.key(), it.value());
	}

	// merge 辅助方法，完整的判断to,from及里面的logChanges的null状况。
	public static History merge(History to, History from) {
		// rrs 锁内
		// 整体查看
		if (null == to)
			return from; // still maybe null. 直接全部接管。需要确认from不会再被修改。

		// 合并encoded
		if (null != from)
			putEncodedAll(to.encoded, from.encoded);

		// 合并logChanges
		if (null == to.logChanges) {
			if (null != from)
				to.logChanges = from.logChanges; // still maybe null

			return to;
		}

		if (null != from && null != from.logChanges)
			putLogChangesAll(to.logChanges, from.logChanges);

		return to;
	}

	public static final class LogChangesRaw extends BLogChanges.Data {
		private final HashMap<TableKey, Changes.Record> records;

		public LogChangesRaw(HashMap<TableKey, Changes.Record> records) {
			this.records = records;
		}

		@Override
		public void encode(ByteBuffer bb) {
			for (var e : records.entrySet()) {
				var tableKey = new BTableKey(
						e.getKey().getId(),
						new Binary(e.getValue().getTable().encodeKey(e.getKey().getKey())));
				var bbValue = ByteBuffer.Allocate();
				e.getValue().encode(bbValue);
				getChanges().put(tableKey, new Binary(bbValue));
			}
			records.clear();
			super.encode(bb);
		}
	}

	public static LogChangesRaw buildLogChanges(long globalSerialId,
												   Changes changes,
												   @Nullable String protocolClassName,
												   @Nullable Binary protocolArgument) {
		var logChanges = new LogChangesRaw(changes.getRecords());
		logChanges.setTimestamp(System.currentTimeMillis());
		logChanges.setGlobalSerialId(globalSerialId);
		if (null != protocolClassName)
			logChanges.setProtocolClassName(protocolClassName);
		if (null != protocolArgument)
			logChanges.setProtocolArgument(protocolArgument);
		return logChanges;
	}
}
