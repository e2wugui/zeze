package Zeze.Transaction.Collections;

import java.util.*;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;
import org.jspecify.annotations.Nullable;

/** Encodable Map1 change payload, independent from transaction state. */
public class LogMap1<K, V> extends LogBean {
	protected final Meta2<K, V> meta;
	private final HashMap<K, V> replaced;
	private final Set<K> removed;

	public LogMap1(int varId, @Nullable Bean source, Meta2<K, V> meta,
				   @Nullable HashMap<K, V> replaced, @Nullable Set<K> removed) {
		super(varId, source);
		this.meta = meta;
		this.replaced = replaced != null ? replaced : new HashMap<>();
		this.removed = removed != null ? removed : new HashSet<>();
	}

	@Override
	public int getTypeId() {
		return meta.logTypeId;
	}

	@Override
	public String getTypeName() {
		return meta.name;
	}

	public final HashMap<K, V> getReplaced() {
		return replaced;
	}

	public final Set<K> getRemoved() {
		return removed;
	}

	@Override
	public void encode(ByteBuffer bb) {
		var keyEncoder = meta.keyEncoder;
		var valueEncoder = meta.valueEncoder;
		bb.WriteUInt(replaced.size());
		for (var e : replaced.entrySet()) {
			keyEncoder.accept(bb, e.getKey());
			//noinspection DataFlowIssue
			valueEncoder.accept(bb, e.getValue());
		}

		bb.WriteUInt(removed.size());
		for (K k : removed)
			keyEncoder.accept(bb, k);
	}

	private void read(IByteBuffer bb) {
		replaced.clear();
		var keyDecoder = meta.keyDecoder;
		var valueDecoder = meta.valueDecoder;
		for (int i = bb.ReadUInt(); i > 0; --i) {
			K k = keyDecoder.apply(bb);
			//noinspection DataFlowIssue
			V v = valueDecoder.apply(bb);
			replaced.put(k, v);
		}

		removed.clear();
		for (int i = bb.ReadUInt(); i > 0; --i)
			removed.add(keyDecoder.apply(bb));
	}

	public static <K, V> LogMap1<K, V> decode(int varId, Meta2<K, V> meta, IByteBuffer bb) {
		var log = new LogMap1<>(varId, null, meta, null, null);
		log.read(bb);
		return log;
	}
}
