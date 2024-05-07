package Zeze.Transaction.Collections;

import java.util.ArrayList;
import java.util.Collection;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Log;
import Zeze.Transaction.Savepoint;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.pcollections.Empty;

public class LogList1<V> extends LogList<V> {
	public static final class OpLog<V> {
		public static final int OP_MODIFY = 0;
		public static final int OP_ADD = 1;
		public static final int OP_REMOVE = 2;
		public static final int OP_CLEAR = 3;

		public final int op;
		public final int index;
		public final V value;

		OpLog(int op, int index, V value) {
			this.op = op;
			this.index = index;
			this.value = value;
		}

		@Override
		public @NotNull String toString() {
			return "{" + op + ',' + index + ',' + value + '}';
		}
	}

	protected final @NotNull Meta1<V> meta;
	protected final ArrayList<OpLog<V>> opLogs = new ArrayList<>();

	public LogList1(@NotNull Meta1<V> meta) {
		this.meta = meta;
	}

	public LogList1(@NotNull Class<V> valueClass) {
		meta = Meta1.getList1Meta(valueClass);
	}

	@Override
	public int getTypeId() {
		return meta.logTypeId;
	}

	public final @NotNull ArrayList<OpLog<V>> getOpLogs() {
		return opLogs;
	}

	public final boolean add(@NotNull V item) {
		//noinspection ConstantValue
		if (item == null)
			throw new IllegalArgumentException("null item");
		var list = getValue();
		setValue(list.plus(item));
		opLogs.add(new OpLog<>(OpLog.OP_ADD, list.size(), item));
		return true;
	}

	public final boolean addAll(@NotNull Collection<? extends V> items) {
		var addindex = getValue().size();
		var list = getValue().plusAll(items);
		if (list == getValue())
			return false;
		setValue(list);
		for (var item : items) {
			opLogs.add(new OpLog<>(OpLog.OP_ADD, addindex++, item));
		}
		return true;
	}

	public final boolean removeAll(@NotNull Collection<? extends V> c) {
		var result = false;
		for (var o : c) {
			if (remove(o))
				result = true; // 只要有一个删除成功，就认为成功：removeAll的定义是发生了改变就返回true。
		}
		return result;
	}

	public final boolean remove(@NotNull V item) {
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

	public final void add(int index, @NotNull V item) {
		setValue(getValue().plus(index, item));
		opLogs.add(new OpLog<>(OpLog.OP_ADD, index, item));
	}

	public final @Nullable V set(int index, @NotNull V item) {
		var list = getValue();
		var old = list.get(index);
		setValue(list.with(index, item));
		opLogs.add(new OpLog<>(OpLog.OP_MODIFY, index, item));
		return old;
	}

	public final @Nullable V remove(int index) {
		var list = getValue();
		var old = list.get(index);
		setValue(list.minus(index));
		opLogs.add(new OpLog<>(OpLog.OP_REMOVE, index, old));
		return old;
	}

	@Override
	public void encode(@NotNull ByteBuffer bb) {
		var encoder = meta.valueEncoder;
		bb.WriteUInt(opLogs.size());
		for (var opLog : opLogs) {
			bb.WriteUInt(opLog.op);
			if (opLog.op < OpLog.OP_CLEAR) {
				bb.WriteUInt(opLog.index);
				if (opLog.op < OpLog.OP_REMOVE) {
					encoder.accept(bb, opLog.value);
				}
			}
		}
	}

	@Override
	public void decode(@NotNull IByteBuffer bb) {
		var decoder = meta.valueDecoder;
		opLogs.clear();
		for (var logSize = bb.ReadUInt(); --logSize >= 0; ) {
			int op = bb.ReadUInt();
			int index = op < OpLog.OP_CLEAR ? bb.ReadUInt() : 0;
			opLogs.add(new OpLog<>(op, index, op < OpLog.OP_REMOVE ? decoder.apply(bb) : null));
		}
	}

	@Override
	public void endSavepoint(@NotNull Savepoint currentSp) {
		var log = currentSp.getLog(getLogKey());
		if (log != null) {
			@SuppressWarnings("unchecked")
			var currentLog = (LogList1<V>)log;
			currentLog.setValue(getValue());
			currentLog.merge(this);
		} else
			currentSp.putLog(this);
	}

	public final void merge(@NotNull LogList1<V> from) {
		if (!from.opLogs.isEmpty()) {
			if (from.opLogs.get(0).op == OpLog.OP_CLEAR)
				opLogs.clear();
			opLogs.addAll(from.opLogs);
		}
	}

	@Override
	public @NotNull Log beginSavepoint() {
		var dup = new LogList1<>(meta);
		dup.setThis(getThis());
		dup.setBelong(getBelong());
		dup.setVariableId(getVariableId());
		dup.setValue(getValue());
		return dup;
	}

	@Override
	public @NotNull String toString() {
		var sb = new StringBuilder();
		sb.append(" opLogs:");
		ByteBuffer.BuildSortedString(sb, opLogs);
		return sb.toString();
	}
}
