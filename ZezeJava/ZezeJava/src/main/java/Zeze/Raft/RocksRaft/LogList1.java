package Zeze.Raft.RocksRaft;

import java.util.ArrayList;
import java.util.Collection;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.SerializeHelper;
import org.pcollections.Empty;

public class LogList1<V> extends LogList<V> {
	private static final long logTypeIdHead = Zeze.Transaction.Bean.hash64("Zeze.Raft.RocksRaft.LogList1<");

	protected final SerializeHelper.CodecFuncs<V> valueCodecFuncs;
	protected final ArrayList<OpLog<V>> opLogs = new ArrayList<>();

	protected static final class OpLog<V> {
		static final int OP_MODIFY = 0;
		static final int OP_ADD = 1;
		static final int OP_REMOVE = 2;
		static final int OP_CLEAR = 3;

		final int op;
		final int index;
		final V value;

		OpLog(int op, int index, V value) {
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

	LogList1(int typeId, SerializeHelper.CodecFuncs<V> valueCodecFuncs) {
		super(typeId);
		this.valueCodecFuncs = valueCodecFuncs;
	}

	protected final ArrayList<OpLog<V>> getOpLogs() {
		return opLogs;
	}

	public final void add(V item) {
		if (item == null)
			throw new IllegalArgumentException("null item");
		var list = getValue();
		setValue(list.plus(item));
		opLogs.add(new OpLog<>(OpLog.OP_ADD, list.size(), item));
	}

	public final boolean addAll(Collection<? extends V> items) {
		int addIndex = getValue().size();
		var list = getValue().plusAll(items);
		if (list == getValue())
			return false;
		setValue(list);
		for (var item : items) {
			opLogs.add(new OpLog<>(OpLog.OP_ADD, addIndex++, item));
		}
		return true;
	}

	public final boolean remove(V item) {
		var index = getValue().indexOf(item);
		if (index < 0)
			return false;
		remove(index);
		return true;
	}

	public final void clear() {
		setValue(Empty.vector());
		opLogs.clear();
		opLogs.add(new OpLog<>(OpLog.OP_CLEAR, 0, null));
	}

	public final void add(int index, V item) {
		setValue(getValue().plus(index, item));
		opLogs.add(new OpLog<>(OpLog.OP_ADD, index, item));
	}

	public final V Set(int index, V item) {
		var list = getValue();
		var old = list.get(index);
		setValue(list.with(index, item));
		opLogs.add(new OpLog<>(OpLog.OP_MODIFY, index, item));
		return old;
	}

	public final V remove(int index) {
		var list = getValue();
		var old = list.get(index);
		setValue(list.minus(index));
		opLogs.add(new OpLog<>(OpLog.OP_REMOVE, index, old));
		return old;
	}

	@Override
	public void encode(ByteBuffer bb) {
		var encoder = valueCodecFuncs.encoder;
		bb.WriteUInt(opLogs.size());
		for (var opLog : opLogs) {
			bb.WriteUInt(opLog.op);
			if (opLog.op < OpLog.OP_CLEAR) {
				bb.WriteUInt(opLog.index);
				if (opLog.op < OpLog.OP_REMOVE)
					encoder.accept(bb, opLog.value);
			}
		}
	}

	@Override
	public void decode(IByteBuffer bb) {
		var decoder = valueCodecFuncs.decoder;
		opLogs.clear();
		for (var logSize = bb.ReadUInt(); --logSize >= 0; ) {
			int op = bb.ReadUInt();
			int index = op < OpLog.OP_CLEAR ? bb.ReadUInt() : 0;
			opLogs.add(new OpLog<>(op, index, op < OpLog.OP_REMOVE ? decoder.apply(bb) : null));
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

	public final void merge(LogList1<V> from) {
		if (!from.opLogs.isEmpty()) {
			if (from.opLogs.get(0).op == OpLog.OP_CLEAR)
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
