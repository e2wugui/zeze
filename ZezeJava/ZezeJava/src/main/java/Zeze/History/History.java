package Zeze.History;

import java.util.concurrent.ConcurrentHashMap;
import Zeze.Builtin.HistoryModule.BLogChanges;
import Zeze.Builtin.HistoryModule.BTableKey;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Services.ServiceManager.Id128UdpClient;
import Zeze.Transaction.Changes;
import Zeze.Transaction.Database;
import Zeze.Util.Id128;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class History {
	// 为了节约内存，在确实需要的时候才分配。
	// 为了在锁外并发。使用并发Map，否则ArrayList或者自己实现的支持splice的连接表效率更高。
	private volatile @Nullable ConcurrentHashMap<Id128, BLogChanges.Data> logChanges;

	// 这里为了并发接收数据，不能优化为可null？需要确认。
	private final ConcurrentHashMap<Id128, Binary> encoded = new ConcurrentHashMap<>();

	public History(@NotNull BLogChanges.Data firstData) {
		addLogChanges(firstData);
	}

	public void addLogChanges(@NotNull BLogChanges.Data _logChanges) {
		var logChanges = this.logChanges;
		if (logChanges == null)
			this.logChanges = logChanges = new ConcurrentHashMap<>();
		logChanges.put(_logChanges.getGlobalSerialId(), _logChanges);
	}

	public void encodeN() {
		// rrs 锁外
		var changes = logChanges; // volatile
		if (changes != null) {
			changes.forEach((key, v) -> {
				// logChanges只要系列号一样，表示内容一样，所以，只要key存在，不需要再encode一次。
				encoded.computeIfAbsent(v.getGlobalSerialId(), __ -> {
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
		if (changes != null) {
			changes.forEach((key, v) -> {
				// logChanges只要系列号一样，表示内容一样，所以，只要key存在，不需要再encode一次。
				encoded.computeIfAbsent(v.getGlobalSerialId(), __ -> {
					var bb = ByteBuffer.Allocate();
					v.encode(bb);
					return new Binary(bb);
				});
			});
			changes.clear();
		}
	}

	public void flush(@NotNull Database.Table table, @NotNull Database.Transaction txn) {
		// 但仅仅Checkpoint访问，不需要加锁。现实也在锁内。
		//logger.debug("flush: {}", encoded.size());
		for (var e : encoded.entrySet()) {
			var key = ByteBuffer.Allocate();
			e.getKey().encode(key);
			var value = ByteBuffer.Wrap(e.getValue());
			table.replace(txn, key, value);
		}
		encoded.clear();
	}

	public static void putLogChangesAll(@NotNull ConcurrentHashMap<Id128, BLogChanges.Data> to,
										@NotNull ConcurrentHashMap<Id128, BLogChanges.Data> other) {
		other.forEach((key, v) -> to.putIfAbsent(v.getGlobalSerialId(), v));
	}

	public static void putEncodedAll(@NotNull ConcurrentHashMap<Id128, Binary> to,
									 @NotNull ConcurrentHashMap<Id128, Binary> other) {
		other.forEach(to::putIfAbsent);
	}

	// merge 辅助方法，完整的判断to,from及里面的logChanges的null状况。
	public static @Nullable History merge(@Nullable History to, @Nullable History from) {
		// rrs 锁内
		// 整体查看
		if (to == null)
			return from; // still maybe null. 直接全部接管。需要确认from不会再被修改。

		// 合并encoded
		if (from != null)
			putEncodedAll(to.encoded, from.encoded);

		// 合并logChanges
		var toLogChanges = to.logChanges;
		if (toLogChanges == null) {
			if (from != null)
				to.logChanges = from.logChanges; // still maybe null
			return to;
		}

		if (from != null) {
			var fromLogChanges = from.logChanges;
			if (fromLogChanges != null)
				putLogChangesAll(toLogChanges, fromLogChanges);
		}
		return to;
	}

	public static @NotNull BLogChanges.Data buildLogChanges(@NotNull Id128UdpClient.FutureNode future,
															@NotNull Changes changes,
															@Nullable String protocolClassName,
															@Nullable Binary protocolArgument) {
		var logChanges = new BLogChanges.Data();
		if (protocolClassName != null)
			logChanges.setProtocolClassName(protocolClassName);
		if (protocolArgument != null)
			logChanges.setProtocolArgument(protocolArgument);
		for (var e : changes.getRecords().entrySet()) {
			var value = e.getValue();
			var table = value.getTable();
			if (table != null && !table.isMemory()) { // 内存表的日志变更不需要持久化，直接忽略。
				var key = e.getKey();
				var tableKey = new BTableKey(key.getId(), new Binary(table.encodeKey(key.getKey())));
				var bbValue = ByteBuffer.Allocate();
				value.encode(bbValue);
				logChanges.getChanges().put(tableKey, new Binary(bbValue));
			}
		}
		logChanges.setTimestamp(System.currentTimeMillis());
		logChanges.setGlobalSerialId(future.get().next());
		return logChanges;
	}
}
