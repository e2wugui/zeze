package Zeze.Raft.RocksRaft;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.SerializeHelper;

import Zeze.Collections.Persistents;

/**
 * 基础类型 List 容器的增量日志：按结构操作（add/set/remove/clear）记录 OpLog 序列。
 */
public class LogList1<V> extends LogList<V> {
	private static final long logTypeIdHead = Zeze.Transaction.Bean.hash64("Zeze.Raft.RocksRaft.LogList1<");

	protected final SerializeHelper.@Nullable CodecFuncs<V> valueCodecFuncs;
	protected final ArrayList<OpLog<V>> opLogs = new ArrayList<>();

	protected static final class OpLog<V> {
		static final int OP_MODIFY = 0;
		static final int OP_ADD = 1;
		static final int OP_REMOVE = 2;
		static final int OP_CLEAR = 3;

		final int op;
		final int index;
		final @Nullable V value;

		OpLog(int op, int index, @Nullable V value) {
			this.op = op;
			this.index = index;
			this.value = value;
		}

		@Override
		public String toString() {
			return "{" + op + ',' + index + ',' + value + '}';
		}
	}

	public LogList1(Class<V> valueClass) {
		super(Zeze.Transaction.Bean.hashLog(logTypeIdHead, valueClass));
		valueCodecFuncs = SerializeHelper.createCodec(valueClass);
	}

	LogList1(int typeId, SerializeHelper.@Nullable CodecFuncs<V> valueCodecFuncs) {
		super(typeId);
		this.valueCodecFuncs = valueCodecFuncs;
	}

	protected final ArrayList<OpLog<V>> getOpLogs() {
		return opLogs;
	}

	// 可变方法非final：LogList2覆写它们同步维护addSet（encode侧身份过滤依赖，
	// 对齐经典 Transaction.Collections.LogList1/LogList2 的结构）。
	@SuppressWarnings("ConstantConditions")
	public void add(V item) {
		if (item == null)
			throw new IllegalArgumentException("null item");
		var list = Objects.requireNonNull(getValue());
		setValue(list.plus(item));
		opLogs.add(new OpLog<>(OpLog.OP_ADD, list.size(), item));
	}

	public boolean addAll(Collection<? extends V> items) {
		int addIndex = Objects.requireNonNull(getValue()).size();
		var list = Objects.requireNonNull(getValue()).plusAll(items);
		if (list == getValue())
			return false;
		setValue(list);
		for (var item : items) {
			opLogs.add(new OpLog<>(OpLog.OP_ADD, addIndex++, item));
		}
		return true;
	}

	public final boolean remove(V item) {
		var index = Objects.requireNonNull(getValue()).indexOf(item);
		if (index < 0)
			return false;
		remove(index);
		return true;
	}

	public void clear() {
		setValue(Persistents.vector());
		opLogs.clear();
		opLogs.add(new OpLog<>(OpLog.OP_CLEAR, 0, null));
	}

	public void add(int index, V item) {
		setValue(Objects.requireNonNull(getValue()).plus(index, item));
		opLogs.add(new OpLog<>(OpLog.OP_ADD, index, item));
	}

	public V set(int index, V item) {
		var list = Objects.requireNonNull(getValue());
		var old = list.get(index);
		setValue(list.with(index, item));
		opLogs.add(new OpLog<>(OpLog.OP_MODIFY, index, item));
		return old;
	}

	public V remove(int index) {
		var list = Objects.requireNonNull(getValue());
		var old = list.get(index);
		setValue(list.minus(index));
		opLogs.add(new OpLog<>(OpLog.OP_REMOVE, index, old));
		return old;
	}

	@Override
	public void encode(ByteBuffer bb) {
		var encoder = Objects.requireNonNull(valueCodecFuncs).encoder;
		bb.WriteUInt(opLogs.size());
		for (var opLog : opLogs) {
			bb.WriteUInt(opLog.op);
			if (opLog.op < OpLog.OP_CLEAR) {
				bb.WriteUInt(opLog.index);
				if (opLog.op < OpLog.OP_REMOVE)
					encoder.accept(bb, Objects.requireNonNull(opLog.value));
			}
		}
	}

	@Override
	public void decode(IByteBuffer bb) {
		var decoder = Objects.requireNonNull(valueCodecFuncs).decoder;
		opLogs.clear();
		for (var logSize = bb.ReadUInt(); --logSize >= 0; ) {
			int op = bb.ReadUInt();
			int index = op < OpLog.OP_CLEAR ? bb.ReadUInt() : 0;
			opLogs.add(new OpLog<V>(op, index, op < OpLog.OP_REMOVE ? decoder.apply(bb) : null));
		}
	}

	@Override
	public void endSavepoint(Savepoint currentSp) {
		var log = currentSp.getLog(getLogKey());
		if (log != null) {
			@SuppressWarnings("unchecked")
			var currentLog = (LogList1<V>)log;
			currentLog.setValue(this.getValue());
			currentLog.merge(this);
		} else
			currentSp.putLog(this);
	}

	private void merge(LogList1<V> from) {
		if (!from.opLogs.isEmpty()) {
			if (from.opLogs.getFirst().op == OpLog.OP_CLEAR)
				opLogs.clear();
			opLogs.addAll(from.opLogs);
		}
	}

	@Override
	public Log beginSavepoint() {
		var dup = new LogList1<>(getTypeId(), valueCodecFuncs);
		dup.setThis(getThis());
		dup.setBelong(getBelong());
		dup.setVariableId(getVariableId());
		dup.setValue(getValue());
		return dup;
	}

	@Override
	public String toString() {
		var sb = new StringBuilder();
		sb.append(" opLogs:");
		ByteBuffer.BuildSortedString(sb, opLogs);
		return sb.toString();
	}
}
