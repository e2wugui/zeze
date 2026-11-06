package Zeze.Transaction.Collections;

import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;
import org.jspecify.annotations.Nullable;

/** Encodable SortedMap1 change payload, independent from transaction state. */
public class LogSortedMap1<K extends Comparable<K>, V> extends LogBean {
	protected final Meta2<K, V> meta;
	private final @Nullable Comparator<? super K> comparator;
	/**
	 * 结构增量单容器：value 非 null 为替换项，null 为删除标记。
	 * 使用源容器比较器：比较器等价键（equals不同但compare==0）折叠进同一槽位，
	 * 键等价语义与双容器时代的TreeMap+TreeSet一致。
	 */
	private @Nullable Map<K, V> modified;

	public LogSortedMap1(int varId, @Nullable Bean source, @Nullable Comparator<? super K> comparator,
						 Meta2<K, V> meta, @Nullable Map<K, V> modified) {
		super(varId, source);
		this.meta = meta;
		this.comparator = comparator;
		this.modified = modified;
	}

	@Override
	public int getTypeId() {
		return meta.logTypeId;
	}

	@Override
	public String getTypeName() {
		return meta.name;
	}

	public final Map<K, V> getModified() {
		return modified != null ? modified : (modified = new TreeMap<>(comparator));
	}

	@Override
	public void encode(ByteBuffer bb) {
		Map<K, V> m = modified != null ? modified : Collections.emptyMap();
		var keyEncoder = meta.keyEncoder;
		var valueEncoder = meta.valueEncoder;
		// 两遍split编码（同LogMap1）：线格式与双容器时代逐字节一致。
		int replacedCount = 0;
		for (var e : m.entrySet())
			if (e.getValue() != null)
				replacedCount++;
		bb.WriteUInt(replacedCount);
		for (var e : m.entrySet()) {
			V v = e.getValue();
			if (v != null) {
				keyEncoder.accept(bb, e.getKey());
				valueEncoder.accept(bb, v);
			}
		}

		bb.WriteUInt(m.size() - replacedCount);
		for (var e : m.entrySet())
			if (e.getValue() == null)
				keyEncoder.accept(bb, e.getKey());
	}

	private void read(IByteBuffer bb) {
		var keyDecoder = meta.keyDecoder;
		var valueDecoder = meta.valueDecoder;
		var m = getModified();
		for (int i = bb.ReadUIntPositive(); i > 0; --i)
			m.put(keyDecoder.apply(bb), valueDecoder.apply(bb));

		for (int i = bb.ReadUIntPositive(); i > 0; --i)
			m.put(keyDecoder.apply(bb), null);
	}

	public static <K extends Comparable<K>, V> LogSortedMap1<K, V> decode(int varId, Meta2<K, V> meta, IByteBuffer bb) {
		return decode(varId, meta, null, bb);
	}

	public static <K extends Comparable<K>, V> LogSortedMap1<K, V> decode(
			int varId, Meta2<K, V> meta, @Nullable Comparator<? super K> comparator, IByteBuffer bb) {
		var log = new LogSortedMap1<>(varId, null, comparator, meta, null);
		log.read(bb);
		return log;
	}
}
