package Zeze.History;

import Zeze.Builtin.HistoryModule.BLogChanges;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.LongConcurrentHashMap;
import org.jetbrains.annotations.NotNull;

public class History {
	// 为了节约内存，在确实需要的时候才分配。
	// 为了在锁外并发。使用并发Map，否则ArrayList或者自己实现的支持splice的连接表效率更高。
	private LongConcurrentHashMap<BLogChanges> logChanges;

	// 这里为了并发接收数据，不能优化为可null？需要确认。
	private final LongConcurrentHashMap<BLogChanges> encoded = new LongConcurrentHashMap<>();

	public void encodeN() {
		// rrs 锁外,XXX 不好实现啊！！！
		/*
		var changes = logChanges; // volatile
		if (null != changes) {

		}
		*/
	}

	public void encode0() {
		var changes = logChanges;
		if (null != changes) {
			changes.forEach((v) -> {
				// logChanges只要系列号一样，表示内容一样，所以，只要key存在，不需要再encode一次。
				encoded.computeIfAbsent(v.getGlobalSerialId(), (key) -> {
					var bb = ByteBuffer.Allocate();
					v.encode(bb);
					v.setEncoded(new Binary(bb));
					return v;
				});
			});
		}
	}

	public void flush() {
		// 锁外，但仅仅Checkpoint访问，不需要加锁。
	}

	public static void putAll(@NotNull LongConcurrentHashMap<BLogChanges> to,
							  @NotNull LongConcurrentHashMap<BLogChanges> other) {
		other.forEach((v) -> to.putIfAbsent(v.getGlobalSerialId(), v));
	}

	// merge 辅助方法，完整的判断to,from及里面的logChanges的null状况。
	public static History merge(History to, History from) {
		// 整体查看
		if (null == to)
			return from; // still maybe null. 直接全部接管。需要确认from不会再被修改。

		// 合并encoded
		if (null != from)
			putAll(to.encoded, from.encoded);

		// 合并logChanges
		if (null == to.logChanges) {
			if (null != from)
				to.logChanges = from.logChanges; // still maybe null

			return to;
		}

		if (null != from && null != from.logChanges)
			putAll(to.logChanges, from.logChanges);

		return to;
	}
}
