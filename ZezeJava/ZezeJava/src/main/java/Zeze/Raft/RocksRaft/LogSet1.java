package Zeze.Raft.RocksRaft;

import java.util.HashMap;
import java.util.Objects;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.SerializeHelper;

import Zeze.Collections.Persistents;

/**
 * 基础类型 Set 容器的增量日志：单容器 modified 记差量，
 * value 为 PRESENT 表示加入、null 表示删除（同 HashSet 底层 HashMap 惯例）。
 */
public class LogSet1<V> extends LogSet<V> {
	private static final long logTypeIdHead = Zeze.Transaction.Bean.hash64("Zeze.Raft.RocksRaft.LogSet1<");

	/** 加入项的哑值（同 HashSet 底层 HashMap 的 PRESENT 惯例）：set 无值语义，value 槽只作判别位。 */
	public static final Object PRESENT = new Object();

	protected final SerializeHelper.CodecFuncs<V> valueCodecFuncs;

	private final HashMap<V, Object> modified = new HashMap<>();

	public LogSet1(Class<V> valueClass) {
		super(Zeze.Transaction.Bean.hashLog(logTypeIdHead, valueClass));
		valueCodecFuncs = SerializeHelper.createCodec(valueClass);
	}

	LogSet1(int typeId, SerializeHelper.CodecFuncs<V> valueCodecFuncs) {
		super(typeId);
		this.valueCodecFuncs = valueCodecFuncs;
	}

	public final HashMap<V, Object> getModified() {
		return modified;
	}

	public final boolean add(V item) {
		//noinspection DataFlowIssue
		var newSet = getValue().plus(item);
		if (newSet != getValue()) {
			modified.put(item, PRESENT);
			setValue(newSet);
			return true;
		}
		return false;
	}

	public final boolean remove(V item) {
		//noinspection DataFlowIssue
		var newSet = getValue().minus(item);
		if (newSet != getValue()) {
			modified.put(item, null);
			setValue(newSet);
			return true;
		}
		return false;
	}

	public final void clear() {
		for (var e : Objects.requireNonNull(getValue()))
			remove(e);
		setValue(Persistents.set());
	}

	@Override
	public void encode(ByteBuffer bb) {
		var encoder = valueCodecFuncs.encoder;
		// 两遍split编码：线格式与added/removed双容器时代逐字节一致。
		int addedCount = 0;
		for (var e : modified.entrySet())
			if (e.getValue() != null)
				addedCount++;
		bb.WriteUInt(addedCount);
		for (var e : modified.entrySet())
			if (e.getValue() != null)
				encoder.accept(bb, e.getKey());

		bb.WriteUInt(modified.size() - addedCount);
		for (var e : modified.entrySet())
			if (e.getValue() == null)
				encoder.accept(bb, e.getKey());
	}

	@Override
	public void decode(IByteBuffer bb) {
		var decoder = valueCodecFuncs.decoder;

		modified.clear();
		for (int i = bb.ReadUInt(); i > 0; i--)
			modified.put(decoder.apply(bb), PRESENT);

		for (int i = bb.ReadUInt(); i > 0; i--)
			modified.put(decoder.apply(bb), null);
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

	private void merge(LogSet1<V> from) {
		// 单容器putAll即last-writer-wins（add覆盖remove、remove覆盖add）：
		// 例：this: add 1,3 remove 2,4; nest: add 2 remove 1 → 合并后 2,3=PRESENT、1,4=null。
		modified.putAll(from.modified);
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
		sb.append(" Modified:");
		ByteBuffer.BuildSortedString(sb, modified);
		return sb.toString();
	}
}
