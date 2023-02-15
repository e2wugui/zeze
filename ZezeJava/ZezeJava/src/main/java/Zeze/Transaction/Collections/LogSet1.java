package Zeze.Transaction.Collections;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.SerializeHelper;
import Zeze.Transaction.Changes;
import Zeze.Transaction.Log;
import Zeze.Transaction.Savepoint;
import Zeze.Util.Reflect;
import org.pcollections.Empty;

public class LogSet1<V> extends LogSet<V> {
	protected final SerializeHelper.CodecFuncs<V> valueCodecFuncs;

	private final Set<V> added = new HashSet<>();
	private final Set<V> removed = new HashSet<>();

	private static final long logTypeIdHead = Zeze.Transaction.Bean.hash64("Zeze.Transaction.Collections.LogSet1<");
	public LogSet1(Class<V> valueClass) {
		super( Zeze.Transaction.Bean.hashLog(logTypeIdHead, valueClass));
		valueCodecFuncs = SerializeHelper.createCodec(valueClass);
	}

	LogSet1(int typeId, SerializeHelper.CodecFuncs<V> valueCodecFuncs) {
		super(typeId);
		this.valueCodecFuncs = valueCodecFuncs;
	}

	public final Set<V> getAdded() {
		return added;
	}

	public final Set<V> getRemoved() {
		return removed;
	}

	@Override
	public void collect(Changes changes, Zeze.Transaction.Bean recent, Log vlog) {
		throw new UnsupportedOperationException("Collect Not Implement.");
	}

	public final boolean Add(V item) {
		var newSet = getValue().plus(item);
		if (newSet != getValue()) {
			added.add(item);
			removed.remove(item);
			setValue(newSet);
			return true;
		}
		return false;
	}

	public final boolean addAll(Collection<? extends V> c) {
		var newSet = getValue().plusAll(c);
		if (newSet != getValue()) {
			for (var item : c) {
				added.add(item);
				removed.remove(item);
			}
			setValue(newSet);
			return true;
		}
		return false;
	}

	public final boolean remove(V item) {
		var newSet = getValue().minus(item);
		if (newSet != getValue()) {
			removed.add(item);
			added.remove(item);
			setValue(newSet);
			return true;
		}
		return false;
	}

	public final boolean removeAll(Collection<? extends V> c) {
		var newSet = getValue().minusAll(c);
		if (newSet != getValue()) {
			for (var i : c) {
				removed.add(i);
				added.remove(i);
			}
			setValue(newSet);
			return true;
		}
		return false;
	}

	public final void clear() {
		for (var e : getValue())
			remove(e);
		setValue(Empty.set());
	}

	@Override
	public void encode(ByteBuffer bb) {
		var encoder = valueCodecFuncs.encoder;

		bb.WriteUInt(added.size());
		for (var e : added)
			encoder.accept(bb, e);

		bb.WriteUInt(removed.size());
		for (var e : removed)
			encoder.accept(bb, e);
	}

	@Override
	public void decode(ByteBuffer bb) {
		var decoder = valueCodecFuncs.decoder;

		added.clear();
		for (int i = bb.ReadUInt(); i > 0; i--)
			added.add(decoder.apply(bb));

		removed.clear();
		for (int i = bb.ReadUInt(); i > 0; i--)
			removed.add(decoder.apply(bb));
	}

	@Override
	public void endSavepoint(Savepoint currentSp) {
		var log = currentSp.getLog(getLogKey());
		if (log != null) {
			@SuppressWarnings("unchecked")
			var currentLog = (LogSet1<V>)log;
			currentLog.setValue(getValue());
			currentLog.merge(this);
		} else
			currentSp.putLog(this);
	}

	public final void merge(LogSet1<V> from) {
		// Put,Remove 需要确认有没有顺序问题
		// this: add 1,3 remove 2,4 nest: add 2 remove 1
		for (var e : from.added)
			Add(e); // replace 1,2,3 remove 4
		for (var e : from.removed)
			remove(e); // replace 2,3 remove 1,4
	}

	@Override
	public Log beginSavepoint() {
		var dup = new LogSet1<>(getTypeId(), valueCodecFuncs);
		dup.setThis(getThis());
		dup.setBelong(getBelong());
		dup.setVariableId(getVariableId());
		dup.setValue(getValue());
		return dup;
	}

	@Override
	public String toString() {
		var sb = new StringBuilder();
		sb.append(" Added:");
		ByteBuffer.BuildSortedString(sb, added);
		sb.append(" Removed:");
		ByteBuffer.BuildSortedString(sb, removed);
		return sb.toString();
	}
}
