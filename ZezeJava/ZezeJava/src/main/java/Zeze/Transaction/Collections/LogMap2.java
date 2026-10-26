package Zeze.Transaction.Collections;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;
import org.jspecify.annotations.Nullable;

/** PMap2 的消费日志：结构变化与最终仍在容器中的值 Bean 原位修改。 */
public class LogMap2<K, V extends Bean> extends LogMap2Base<K, V> {
	/** Borrow final notes without retaining the transaction state. decode always creates a fresh payload. */
	public LogMap2(int varId, @Nullable Bean self, Meta2<K, V> meta,
			@Nullable HashMap<K, V> replaced, @Nullable Set<K> removed) {
		super(varId, self, meta, replaced, removed);
	}

	@Override
	<T> HashMap<K, T> newMap() {
		return new HashMap<>();
	}

	@Override
	Set<K> newKeySet() {
		return new HashSet<>();
	}

	@Override
	boolean isCurrentValue(Bean source, K key, Bean value) {
		return ((PMap2<?, ?>)source).map.get(key) == value;
	}

	@Override
	public final HashMap<K, V> getReplaced() {
		return (HashMap<K, V>)super.getReplaced();
	}

	@Override
	public final HashMap<K, LogBean> getChangedWithKey() {
		return (HashMap<K, LogBean>)super.getChangedWithKey();
	}

	public static <K, V extends Bean> LogMap2<K, V> decode(int varId, Meta2<K, V> meta, IByteBuffer bb) {
		var log = new LogMap2<>(varId, null, meta, null, null);
		log.read(bb);
		return log;
	}
}
