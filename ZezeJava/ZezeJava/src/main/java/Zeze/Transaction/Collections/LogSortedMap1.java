package Zeze.Transaction.Collections;

import java.util.*;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;
import org.jspecify.annotations.Nullable;

/** Encodable SortedMap1 change payload, independent from transaction state. */
public class LogSortedMap1<K extends Comparable<K>, V> extends LogBean {
	protected final Meta2<K, V> meta;
	private final @Nullable Comparator<? super K> comparator;
	private @Nullable Map<K, V> replaced;
	private @Nullable Set<K> removed;

	public LogSortedMap1(int varId, @Nullable Bean source, @Nullable Comparator<? super K> comparator, Meta2<K, V> meta,
						 @Nullable Map<K, V> replaced, @Nullable Set<K> removed) {
		super(varId, source);
		this.meta = meta;
		this.comparator = comparator;
		this.replaced = replaced;
		this.removed = removed;
	}

	@Override
	public int getTypeId() {
		return meta.logTypeId;
	}

	@Override
	public String getTypeName() {
		return meta.name;
	}

	public final Map<K, V> getReplaced() {
		return replaced != null ? replaced : (replaced = new TreeMap<>(comparator));
	}

	public final Set<K> getRemoved() {
		return removed != null ? removed : (removed = new TreeSet<>(comparator));
	}

	@Override
	public void encode(ByteBuffer bb) {
		Map<K, V> replacements = replaced != null ? replaced : Collections.emptyMap();
		Set<K> removals = removed != null ? removed : Collections.emptySet();
		var keyEncoder = meta.keyEncoder;
		var valueEncoder = meta.valueEncoder;
		bb.WriteUInt(replacements.size());
		for (var e : replacements.entrySet()) {
			keyEncoder.accept(bb, e.getKey());
			//noinspection DataFlowIssue
			valueEncoder.accept(bb, e.getValue());
		}

		bb.WriteUInt(removals.size());
		for (K k : removals)
			keyEncoder.accept(bb, k);
	}

	private void read(IByteBuffer bb) {
		var keyDecoder = meta.keyDecoder;
		var valueDecoder = meta.valueDecoder;
		for (int i = bb.ReadUInt(); i > 0; --i) {
			K k = keyDecoder.apply(bb);
			//noinspection DataFlowIssue
			V v = valueDecoder.apply(bb);
			getReplaced().put(k, v);
		}

		for (int i = bb.ReadUInt(); i > 0; --i)
			getRemoved().add(keyDecoder.apply(bb));
	}

	public static <K extends Comparable<K>, V> LogSortedMap1<K, V> decode(int varId, Meta2<K, V> meta, IByteBuffer bb) {
		return decode(varId, meta, null, bb);
	}

	public static <K extends Comparable<K>, V> LogSortedMap1<K, V> decode(
		int varId, Meta2<K, V> meta, @Nullable Comparator<? super K> comparator, IByteBuffer bb) {
		var log = new LogSortedMap1<>(varId, null, comparator, meta, null, null);
		log.read(bb);
		return log;
	}
}
