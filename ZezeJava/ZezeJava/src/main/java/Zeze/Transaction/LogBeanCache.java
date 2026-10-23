package Zeze.Transaction;

import Zeze.Transaction.Collections.LogBean;
import Zeze.Util.LongHashMap;
import org.jspecify.annotations.Nullable;

/** 单次 Changes 的节点缓存：两个节点以内直接保存，物化后只以同一张 Map 为准。 */
final class LogBeanCache {
	private long firstId;
	private long secondId;
	private @Nullable LogBean firstLog;
	private @Nullable LogBean secondLog;
	private @Nullable LongHashMap<LogBean> map;

	@Nullable LogBean get(long id) {
		var current = map;
		if (current != null)
			return current.get(id);
		var first = firstLog;
		if (first != null && firstId == id)
			return first;
		var second = secondLog;
		return second != null && secondId == id ? second : null;
	}

	/** 返回最终节点；工厂重入或公开 Map 已发布同一 id 时，保留先发布的节点。 */
	LogBean putIfAbsent(long id, LogBean log) {
		var current = map;
		if (current != null) {
			var existing = current.putIfAbsent(id, log);
			return existing != null ? existing : log;
		}
		var first = firstLog;
		if (first != null && firstId == id)
			return first;
		var second = secondLog;
		if (second != null && secondId == id)
			return second;
		// 节点引用为空才表示空槽；objectId 0 也是有效键。
		if (first == null) {
			firstId = id;
			firstLog = log;
		} else if (second == null) {
			secondId = id;
			secondLog = log;
		} else {
			// cap=4 对应 8 槽、threshold=6，第三节点不会立即触发扩容。
			var existing = materialize(4).putIfAbsent(id, log);
			return existing != null ? existing : log;
		}
		return log;
	}

	LongHashMap<LogBean> asMap() {
		return materialize(2);
	}

	private LongHashMap<LogBean> materialize(int initialCapacity) {
		var current = map;
		if (current == null) {
			current = new LongHashMap<>(initialCapacity);
			var first = firstLog;
			if (first != null)
				current.put(firstId, first);
			var second = secondLog;
			if (second != null)
				current.put(secondId, second);
			map = current;
			firstLog = null;
			secondLog = null;
			firstId = 0;
			secondId = 0;
		}
		return current;
	}
}
