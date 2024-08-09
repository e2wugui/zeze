package Zeze.Transaction.Collections;

import java.util.Collection;
import java.util.HashMap;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Changes;
import Zeze.Transaction.Log;
import Zeze.Transaction.Savepoint;
import Zeze.Util.IdentityHashSet;
import Zeze.Util.OutInt;
import Zeze.Util.Task;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.pcollections.PVector;

public class LogList2<V extends Bean> extends LogList1<V> {
	private final HashMap<LogBean, OutInt> changed = new HashMap<>(); // changed V logs. using in collect.
	private @Nullable IdentityHashSet<V> addSet;

	public LogList2(Bean belong, int varId, Bean self, @NotNull PVector<V> value, @NotNull Meta1<V> meta) {
		super(belong, varId, self, value, meta);
	}

	public final @NotNull HashMap<LogBean, OutInt> getChanged() {
		return changed;
	}

	@Override
	public boolean add(@NotNull V item) {
		if (!super.add(item))
			return false;
		if (addSet == null)
			addSet = new IdentityHashSet<>();
		addSet.add(item);
		return true;
	}

	@Override
	public boolean addAll(@NotNull Collection<? extends V> items) {
		int addIndex = getValue().size();
		var list = getValue().plusAll(items);
		if (list == getValue())
			return false;
		setValue(list);
		if (addSet == null)
			addSet = new IdentityHashSet<>();
		for (V item : items) {
			opLogs.add(new OpLog<>(OpLog.OP_ADD, addIndex++, item));
			addSet.add(item);
		}
		return true;
	}

	@Override
	public void clear() {
		super.clear();
		if (addSet != null)
			addSet.clear();
	}

	@Override
	public void add(int index, @NotNull V item) {
		super.add(index, item);
		if (addSet == null)
			addSet = new IdentityHashSet<>();
		addSet.add(item);
	}

	@Override
	public @NotNull V set(int index, @NotNull V item) {
		V old = super.set(index, item);
		if (addSet == null)
			addSet = new IdentityHashSet<>();
		addSet.remove(old);
		addSet.add(item);
		return old;
	}

	@Override
	public @NotNull V remove(int index) {
		V old = super.remove(index);
		if (addSet == null)
			addSet = new IdentityHashSet<>();
		addSet.remove(old);
		return old;
	}

	@Override
	public void endSavepoint(@NotNull Savepoint currentSp) {
		Log log = currentSp.getLog(getLogKey());
		if (log != null) {
			@SuppressWarnings("unchecked")
			var currentLog = (LogList2<V>)log;
			currentLog.setValue(getValue());
			currentLog.merge(this);
		} else
			currentSp.putLog(this);
	}

	private void merge(@NotNull LogList2<V> from) {
		if (!from.opLogs.isEmpty()) {
			if (from.opLogs.get(0).op == OpLog.OP_CLEAR)
				opLogs.clear();
			opLogs.addAll(from.opLogs);
			if (from.addSet != null) {
				if (addSet == null)
					addSet = from.addSet;
				else
					addSet.addAll(from.addSet);
			}
		}
	}

	@Override
	public @NotNull Log beginSavepoint() {
		return new LogList2<>(getBelong(), getVariableId(), getThis(), getValue(), meta);
	}

	@Override
	public void encode(@NotNull ByteBuffer bb) {
		var curList = getValue();
		for (var it = changed.entrySet().iterator(); it.hasNext(); ) {
			var e = it.next();
			var bean = e.getKey().getThis();
			int idxExist = 0;
			for (V v : curList) {
				if (v == bean)
					break;
				idxExist++;
			}
			if (idxExist >= curList.size() || addSet != null && addSet.contains(bean))
				it.remove();
			else
				e.getValue().value = idxExist;
		}
		bb.WriteUInt(changed.size());
		for (var e : changed.entrySet()) {
			LogMap2.encodeLogBean(bb, e.getKey());
			bb.WriteUInt(e.getValue().value);
		}

		// super.encode(bb);
		bb.WriteUInt(opLogs.size());
		for (var opLog : opLogs) {
			bb.WriteUInt(opLog.op);
			if (opLog.op < OpLog.OP_CLEAR) {
				bb.WriteUInt(opLog.index);
				if (opLog.op < OpLog.OP_REMOVE)
					opLog.value.encode(bb);
			}
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void decode(@NotNull IByteBuffer bb) {
		changed.clear();
		for (int i = bb.ReadUInt(); i > 0; i--) {
			var logBean = LogMap2.decodeLogBean(bb);
			int index = bb.ReadUInt();
			changed.put(logBean, new OutInt(index));
		}

		// super.decode(bb);
		opLogs.clear();
		try {
			for (int logSize = bb.ReadUInt(); --logSize >= 0; ) {
				int op = bb.ReadUInt();
				int index = op < OpLog.OP_CLEAR ? bb.ReadUInt() : 0;
				V v = null;
				if (op < OpLog.OP_REMOVE) {
					v = (V)meta.valueFactory.invoke();
					v.decode(bb);
				}
				opLogs.add(new OpLog<>(op, index, v));
			}
		} catch (Throwable e) { // MethodHandle.invoke
			Task.forceThrow(e);
		}
	}

	@Override
	public void collect(@NotNull Changes changes, @NotNull Bean recent, @NotNull Log vlog) {
		if (changed.putIfAbsent((LogBean)vlog, new OutInt()) == null)
			changes.collect(recent, this);
	}

	@Override
	public @NotNull String toString() {
		var sb = new StringBuilder();
		sb.append(" opLogs:");
		ByteBuffer.BuildSortedString(sb, getOpLogs());
		sb.append(" changed:");
		ByteBuffer.BuildSortedString(sb, changed);
		return sb.toString();
	}
}
