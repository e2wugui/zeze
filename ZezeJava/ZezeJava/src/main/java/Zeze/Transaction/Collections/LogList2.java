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

public class LogList2<V extends Bean> extends LogList1<V> {
	private final HashMap<LogBean, OutInt> changed = new HashMap<>(); // changed V logs. using in collect.
	private @Nullable IdentityHashSet<V> addSet;

	public LogList2(@NotNull Meta1<V> meta) {
		super(meta);
	}

	public LogList2(@NotNull Class<V> valueClass) {
		super(Meta1.getList2Meta(valueClass));
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
		var addIndex = getValue().size();
		var list = getValue().plusAll(items);
		if (list == getValue())
			return false;
		setValue(list);
		if (addSet == null)
			addSet = new IdentityHashSet<>();
		for (var item : items) {
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
	public @Nullable V set(int index, @NotNull V item) {
		V old = super.set(index, item);
		if (addSet == null)
			addSet = new IdentityHashSet<>();
		addSet.remove(old);
		addSet.add(item);
		return old;
	}

	@Override
	public @Nullable V remove(int index) {
		V old = super.remove(index);
		if (addSet == null)
			addSet = new IdentityHashSet<>();
		addSet.remove(old);
		return old;
	}

	@Override
	public void endSavepoint(@NotNull Savepoint currentSp) {
		var log = currentSp.getLog(getLogKey());
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
		var dup = new LogList2<>(meta);
		dup.setThis(getThis());
		dup.setBelong(getBelong());
		dup.setVariableId(getVariableId());
		dup.setValue(getValue());
		return dup;
	}

	@Override
	public void encode(@NotNull ByteBuffer bb) {
		var curList = getValue();
		if (curList != null) {
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
		}
		bb.WriteUInt(changed.size());
		for (var e : changed.entrySet()) {
			/*
			System.out.println(e.getKey().getClass().getName()
					+ " " + e.getKey().getThis().getClass().getName()
					+ " typeId=" + e.getKey().getTypeId());
			// */
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
			var value = LogMap2.decodeLogBean(bb);
			var index = bb.ReadUInt();
			changed.put(value, new OutInt(index));
		}

		// super.decode(bb);
		opLogs.clear();
		for (var logSize = bb.ReadUInt(); --logSize >= 0; ) {
			int op = bb.ReadUInt();
			int index = op < OpLog.OP_CLEAR ? bb.ReadUInt() : 0;
			V value = null;
			if (op < OpLog.OP_REMOVE) {
				try {
					value = (V)meta.valueFactory.invoke();
				} catch (Throwable e) { // MethodHandle.invoke
					Task.forceThrow(e);
				}
				value.decode(bb);
			}
			opLogs.add(new OpLog<>(op, index, value));
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
