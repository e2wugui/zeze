package Zeze.Raft.RocksRaft;

import java.lang.invoke.MethodHandle;
import java.util.Collection;
import java.util.HashMap;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.SerializeHelper;
import Zeze.Util.IdentityHashSet;
import Zeze.Util.OutInt;
import Zeze.Util.Reflect;
import Zeze.Util.Task;

public class LogList2<V extends Bean> extends LogList1<V> {
	private static final long logTypeIdHead = Zeze.Transaction.Bean.hash64("Zeze.Raft.RocksRaft.LogList2<");

	private final HashMap<LogBean, OutInt> changed = new HashMap<>(); // changed V logs. using in collect.
	// 【FND3-19】本日志记录过的结构op携带的bean身份（对齐经典 Transaction.Collections.LogList2.addSet）：
	// 这些bean的最终状态由opLogs携带的value编码（提交时刻才encode，含后续编辑），其changed条目
	// 是冗余的——follower侧全量应用changed，冗余条目会叠加应用两次（内部非幂等op双重执行）。
	private IdentityHashSet<V> addSet;
	private final MethodHandle valueFactory;

	public LogList2(Class<V> valueClass) {
		super(Zeze.Transaction.Bean.hashLog(logTypeIdHead, valueClass), SerializeHelper.createCodec(valueClass));
		valueFactory = Reflect.getDefaultConstructor(valueClass);
	}

	public LogList2(int typeId, MethodHandle valueFactory) {
		super(typeId, null);
		this.valueFactory = valueFactory;
	}

	public final HashMap<LogBean, OutInt> getChanged() {
		return changed;
	}

	private IdentityHashSet<V> getAddSet() {
		var set = addSet;
		if (set == null)
			addSet = set = new IdentityHashSet<>();
		return set;
	}

	@Override
	public void add(V item) {
		super.add(item);
		getAddSet().add(item);
	}

	@Override
	public boolean addAll(Collection<? extends V> items) {
		if (!super.addAll(items))
			return false;
		getAddSet().addAll(items);
		return true;
	}

	@Override
	public void clear() {
		super.clear();
		if (addSet != null)
			addSet.clear();
	}

	@Override
	public void add(int index, V item) {
		super.add(index, item);
		getAddSet().add(item);
	}

	@Override
	public V Set(int index, V item) {
		var old = super.Set(index, item);
		getAddSet().remove(old);
		getAddSet().add(item);
		return old;
	}

	@Override
	public V remove(int index) {
		var old = super.remove(index);
		getAddSet().remove(old);
		return old;
	}

	@Override
	public Log beginSavepoint() {
		var dup = new LogList2<V>(getTypeId(), valueFactory);
		dup.setThis(getThis());
		dup.setBelong(getBelong());
		dup.setVariableId(getVariableId());
		dup.setValue(getValue());
		return dup;
	}

	@Override
	public void endSavepoint(Savepoint currentSp) {
		var log = currentSp.getLog(getLogKey());
		if (log != null) {
			@SuppressWarnings("unchecked")
			var currentLog = (LogList2<V>)log;
			currentLog.setValue(this.getValue());
			currentLog.merge(this);
		} else
			currentSp.putLog(this);
	}

	// savepoint合并时opLogs与addSet一起传递：encode侧身份过滤依赖完整的addSet
	// （本事务所有层级savepoint内结构op携带过的bean）。
	private void merge(LogList2<V> from) {
		if (!from.opLogs.isEmpty()) {
			if (from.opLogs.getFirst().op == OpLog.OP_CLEAR)
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
	public void encode(ByteBuffer bb) {
		var curList = getValue();
		if (curList != null) {
			for (var it = changed.entrySet().iterator(); it.hasNext(); ) {
				var e = it.next();
				var logBean = e.getKey();
				//noinspection SuspiciousMethodCalls
				var idxExist = curList.indexOf(logBean.getThis());
				// 【FND3-19】不在最终列表（已被结构op移除）或∈addSet（由结构op携带最终状态）的
				// 条目剔除：follower侧changed按最终index全量应用，冗余条目会双重应用。
				if (idxExist < 0 || addSet != null && addSet.contains(logBean.getThis()))
					it.remove();
				else
					e.getValue().value = idxExist;
			}
		}
		bb.WriteUInt(changed.size());
		for (var e : changed.entrySet()) {
			e.getKey().encode(bb);
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
	public void decode(IByteBuffer bb) {
		changed.clear();
		for (int i = bb.ReadUInt(); i > 0; i--) {
			var value = new LogBean();
			value.decode(bb);
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
					value = (V)valueFactory.invoke();
				} catch (Throwable e) { // MethodHandle.invoke
					throw Task.forceThrow(e);
				}
				value.decode(bb);
			}
			opLogs.add(new OpLog<>(op, index, value));
		}
	}

	@Override
	public void collect(Changes changes, Bean recent, Log vlog) {
		if (changed.put((LogBean)vlog, new OutInt()) == null)
			changes.collect(recent, this);
	}

	@Override
	public String toString() {
		var sb = new StringBuilder();
		sb.append(" opLogs:");
		ByteBuffer.BuildSortedString(sb, getOpLogs());
		sb.append(" Changed:");
		ByteBuffer.BuildSortedString(sb, changed);
		return sb.toString();
	}
}
