package Zeze.Raft.RocksRaft;

import java.util.HashSet;
import java.util.Set;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.SerializeHelper;
import org.pcollections.Empty;

public class LogSet1<V> extends LogSet<V> {
	private static final long logTypeIdHead = Zeze.Transaction.Bean.hash64("Zeze.Raft.RocksRaft.LogSet1<");

	protected final SerializeHelper.CodecFuncs<V> valueCodecFuncs;

	private final Set<V> added = new HashSet<>();
	private final Set<V> removed = new HashSet<>();

	public LogSet1(Class<V> valueClass) {
		super(Zeze.Transaction.Bean.hashLog(logTypeIdHead, valueClass));
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

	public final boolean add(V item) {
		var newSet = getValue().plus(item);
		if (newSet != getValue()) {
			added.add(item);
			removed.remove(item);
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
	public void decode(IByteBuffer bb) {
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
			currentLog.setValue(this.getValue());
			currentLog.merge(this);
		} else
			currentSp.putLog(this);
	}

	public final void merge(LogSet1<V> from) {
		// Put,Remove 需要确认有没有顺序问题
		// this: add 1,3 remove 2,4 nest: add 2 remove 1
		for (var e : from.added)
			add(e); // replace 1,2,3 remove 4
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
