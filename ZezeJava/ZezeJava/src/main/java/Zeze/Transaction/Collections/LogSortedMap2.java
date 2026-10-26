package Zeze.Transaction.Collections;

import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;
import org.jspecify.annotations.Nullable;

/** PSortedMap2 的消费日志：结构变化与最终仍在容器中的值 Bean 原位修改。 */
public class LogSortedMap2<K extends Comparable<K>, V extends Bean> extends LogMap2Base<K, V> {
	private final @Nullable Comparator<? super K> comparator;

	public LogSortedMap2(int varId, @Nullable Bean self,
			@Nullable Comparator<? super K> comparator, Meta2<K, V> meta,
			@Nullable Map<K, V> replaced, @Nullable Set<K> removed) {
		super(varId, self, meta, replaced, removed);
		this.comparator = comparator;
	}

	public final @Nullable Comparator<? super K> comparator() {
		return comparator;
	}

	@Override
	<T> Map<K, T> newMap() {
		return new TreeMap<>(comparator);
	}

	@Override
	Set<K> newKeySet() {
		return new TreeSet<>(comparator);
	}

	@Override
	boolean isCurrentValue(Bean source, K key, Bean value) {
		// 读取最终提交后的 map，保留源容器比较器的键等价关系。
		return ((PSortedMap2<?, ?>)source).map.get(key) == value;
	}

	public static <K extends Comparable<K>, V extends Bean> LogSortedMap2<K, V> decode(
			int varId, Meta2<K, V> meta, IByteBuffer bb) {
		return decode(varId, meta, null, bb);
	}

	public static <K extends Comparable<K>, V extends Bean> LogSortedMap2<K, V> decode(
			int varId, Meta2<K, V> meta, @Nullable Comparator<? super K> comparator, IByteBuffer bb) {
		var log = new LogSortedMap2<>(varId, null, comparator, meta, null, null);
		log.read(bb);
		return log;
	}
}
