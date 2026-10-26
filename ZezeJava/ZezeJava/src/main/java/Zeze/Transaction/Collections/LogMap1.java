package Zeze.Transaction.Collections;

import java.util.*;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;
import org.jspecify.annotations.Nullable;

/** Encodable Map1 change payload, independent from transaction state. */
public class LogMap1<K, V> extends LogBean {
	protected final Meta2<K, V> meta;
	private @Nullable HashMap<K, V> replaced;
	private @Nullable Set<K> removed;

	public LogMap1(int varId, @Nullable Bean source, Meta2<K, V> meta,
				   @Nullable HashMap<K, V> replaced, @Nullable Set<K> removed) {
		super(varId, source);
		this.meta = meta;
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

	public final HashMap<K, V> getReplaced() {
		return replaced != null ? replaced : (replaced = new HashMap<>());
	}

	public final Set<K> getRemoved() {
		return removed != null ? removed : (removed = new HashSet<>());
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

	public static <K, V> LogMap1<K, V> decode(int varId, Meta2<K, V> meta, IByteBuffer bb) {
		var log = new LogMap1<>(varId, null, meta, null, null);
		log.read(bb);
		return log;
	}
}
