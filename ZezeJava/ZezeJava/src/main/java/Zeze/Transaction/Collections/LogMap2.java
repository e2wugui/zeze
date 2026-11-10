package Zeze.Transaction.Collections;

import java.util.HashMap;

import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;
import org.jspecify.annotations.Nullable;

/** PMap2 的消费日志：结构变化与最终仍在容器中的值 Bean 原位修改。 */
public class LogMap2<K, V extends Bean> extends LogMap2Base<K, V, Map2Meta<K, V>> {
	public LogMap2(int varId, @Nullable Bean self, Map2Meta<K, V> meta, @Nullable HashMap<K, @Nullable V> modified) {
		super(varId, self, meta, modified);
	}

	@Override
	<T extends @Nullable Object> HashMap<K, T> newMap() {
		return new HashMap<>();
	}

	@Override
	boolean isCurrentValue(Bean source, K key, Bean value) {
		return ((PMap2<?, ?>)source).map.get(key) == value;
	}

	@Override
	public final HashMap<K, @Nullable V> getModified() {
		return (HashMap<K, @Nullable V>)super.getModified();
	}

	@Override
	public final HashMap<K, LogBean> getChangedWithKey() {
		return (HashMap<K, LogBean>)super.getChangedWithKey();
	}

	public static <K, V extends Bean> LogMap2<K, V> decode(int varId, Map2Meta<K, V> meta, IByteBuffer bb) {
		var log = new LogMap2<>(varId, null, meta, null);
		log.read(bb);
		return log;
	}
}
