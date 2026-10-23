package Zeze.Transaction.Collections;

import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.Collections;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;

/** Set1 payload: borrows the final added/removed sets for synchronous encoding and replay. */
public class LogSet1<V> extends LogBean {
	protected final Meta1<V> meta;
	private @Nullable Set<V> added;
	private @Nullable Set<V> removed;

	public LogSet1(int varId, @Nullable Bean source, Meta1<V> meta, @Nullable Set<V> added, @Nullable Set<V> removed) {
		super(varId, source);
		this.meta = meta;
		this.added = added;
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

	public final Set<V> getAdded() {
		return added != null ? added : (added = new HashSet<>());
	}

	public final Set<V> getRemoved() {
		return removed != null ? removed : (removed = new HashSet<>());
	}

	@Override
	public void encode(ByteBuffer bb) {
		var encoder = meta.valueEncoder;
		Set<V> added = this.added != null ? this.added : Collections.emptySet();
		Set<V> removed = this.removed != null ? this.removed : Collections.emptySet();

		bb.WriteUInt(added.size());
		for (V v : added) {
			//noinspection DataFlowIssue
			encoder.accept(bb, v);
		}

		bb.WriteUInt(removed.size());
		for (V v : removed) {
			//noinspection DataFlowIssue
			encoder.accept(bb, v);
		}
	}

	private void read(IByteBuffer bb) {
		var decoder = meta.valueDecoder;

		for (int i = bb.ReadUInt(); i > 0; i--) {
			//noinspection DataFlowIssue
			getAdded().add(decoder.apply(bb));
		}

		for (int i = bb.ReadUInt(); i > 0; i--) {
			//noinspection DataFlowIssue
			getRemoved().add(decoder.apply(bb));
		}
	}

	@Override
	public String toString() {
		var sb = new StringBuilder();
		sb.append(" added:");
		ByteBuffer.BuildSortedString(sb, added);
		sb.append(" removed:");
		ByteBuffer.BuildSortedString(sb, removed);
		return sb.toString();
	}

	public static <V> LogSet1<V> decode(int varId, Meta1<V> meta, IByteBuffer bb) {
		var log = new LogSet1<>(varId, null, meta, null, null);
		log.read(bb);
		return log;
	}
}
