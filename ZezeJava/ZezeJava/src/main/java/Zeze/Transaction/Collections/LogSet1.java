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

	private final Set<V> Added = new HashSet<>();
	private final Set<V> Removed = new HashSet<>();

	public LogSet1(Class<V> valueClass) {
		super("Zeze.Raft.RocksRaft.LogSet1<" + Reflect.GetStableName(valueClass) + '>');
		valueCodecFuncs = SerializeHelper.createCodec(valueClass);
	}

	LogSet1(int typeId, SerializeHelper.CodecFuncs<V> valueCodecFuncs) {
		super(typeId);
		this.valueCodecFuncs = valueCodecFuncs;
	}

	public final Set<V> getAdded() {
		return Added;
	}

	public final Set<V> getRemoved() {
		return Removed;
	}

	@Override
	public void Collect(Changes changes, Zeze.Transaction.Bean recent, Log vlog) {
		throw new UnsupportedOperationException("Collect Not Implement.");
	}

	public final boolean Add(V item) {
		var newSet = getValue().plus(item);
		if (newSet != getValue()) {
			Added.add(item);
			Removed.remove(item);
			setValue(newSet);
			return true;
		}
		return false;
	}

	public final boolean AddAll(Collection<? extends V> c) {
		var newSet = getValue().plusAll(c);
		if (newSet != getValue()) {
			for (var item : c) {
				Added.add(item);
				Removed.remove(item);
			}
			setValue(newSet);
			return true;
		}
		return false;
	}

	public final boolean Remove(V item) {
		var newSet = getValue().minus(item);
		if (newSet != getValue()) {
			Removed.add(item);
			Added.remove(item);
			setValue(newSet);
			return true;
		}
		return false;
	}

	public final boolean RemoveAll(Collection<? extends V> c) {
		var newSet = getValue().minusAll(c);
		if (newSet != getValue()) {
			for (var i : c) {
				Removed.add(i);
				Added.remove(i);
			}
			setValue(newSet);
			return true;
		}
		return false;
	}

	public final void Clear() {
		for (var e : getValue())
			Remove(e);
		setValue(Empty.set());
	}

	@Override
	public void Encode(ByteBuffer bb) {
		var encoder = valueCodecFuncs.encoder;

		bb.WriteUInt(Added.size());
		for (var e : Added)
			encoder.accept(bb, e);

		bb.WriteUInt(Removed.size());
		for (var e : Removed)
			encoder.accept(bb, e);
	}

	@Override
	public void Decode(ByteBuffer bb) {
		var decoder = valueCodecFuncs.decoder;

		Added.clear();
		for (int i = bb.ReadUInt(); i > 0; i--)
			Added.add(decoder.apply(bb));

		Removed.clear();
		for (int i = bb.ReadUInt(); i > 0; i--)
			Removed.add(decoder.apply(bb));
	}

	@Override
	public void EndSavepoint(Savepoint currentSp) {
		var log = currentSp.getLogs().get(getLogKey());
		if (log != null) {
			@SuppressWarnings("unchecked")
			var currentLog = (LogSet1<V>)log;
			currentLog.setValue(getValue());
			currentLog.Merge(this);
		} else
			currentSp.getLogs().put(getLogKey(), this);
	}

	public final void Merge(LogSet1<V> from) {
		// Put,Remove 需要确认有没有顺序问题
		// this: add 1,3 remove 2,4 nest: add 2 remove 1
		for (var e : from.Added)
			Add(e); // replace 1,2,3 remove 4
		for (var e : from.Removed)
			Remove(e); // replace 2,3 remove 1,4
	}

	@Override
	public Log BeginSavepoint() {
		var dup = new LogSet1<>(getTypeId(), valueCodecFuncs);
		dup.setBelong(getBelong());
		dup.setVariableId(getVariableId());
		dup.setValue(getValue());
		return dup;
	}

	@Override
	public String toString() {
		var sb = new StringBuilder();
		sb.append(" Added:");
		ByteBuffer.BuildSortedString(sb, Added);
		sb.append(" Removed:");
		ByteBuffer.BuildSortedString(sb, Removed);
		return sb.toString();
	}
}
