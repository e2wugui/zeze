package Zeze.Transaction.Collections;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import Zeze.Transaction.Collections.LogList1.OpLog;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Log;
import Zeze.Util.IdentityHashSet;
import Zeze.Util.OutInt;
import Zeze.Util.Task;
import org.jspecify.annotations.Nullable;

/** PList2 的变更日志：在 LogList1 的 OpLog 序列之外，另记 changed（元素 Bean 的原位修改日志及最终下标）。 */
public class LogList2<V extends Bean> extends LogBean {
	protected final Meta1<V> meta;
	private @Nullable ArrayList<OpLog<V>> opLogs;

	public final ArrayList<OpLog<V>> getOpLogs() {
		var operations = opLogs;
		return operations != null ? operations : (opLogs = new ArrayList<>());
	}

	final List<OpLog<V>> operationsForRead() {
		return opLogs != null ? opLogs : Collections.emptyList();
	}

	final Map<LogBean, OutInt> changesForRead() {
		return changed != null ? changed : Collections.emptyMap();
	}

	@Override
	public int getTypeId() {
		return meta.logTypeId;
	}

	@Override
	public String getTypeName() {
		return meta.name;
	}

	private @Nullable HashMap<LogBean, OutInt> changed;
	private @Nullable IdentityHashSet<V> addSet;
	private @Nullable IdentityHashMap<Bean, Integer> indices;

	public LogList2(int varId, @Nullable Bean source, Meta1<V> meta, @Nullable ArrayList<OpLog<V>> operations, @Nullable IdentityHashSet<V> added) {
		super(varId, source);
		this.meta = meta;
		opLogs = operations;
		addSet = added;
	}

	public final HashMap<LogBean, OutInt> getChanged() {
		var edits = changed;
		return edits != null ? edits : (changed = new HashMap<>());
	}

	@Override
	public void encode(ByteBuffer bb) {
		var edits = changesForRead();
		bb.WriteUInt(edits.size());
		for (var e : edits.entrySet()) {
			LogMap2.encodeLogBean(bb, e.getKey());
			bb.WriteUInt(e.getValue().value);
		}

		var operations = operationsForRead();
		bb.WriteUInt(operations.size());
		for (var opLog : operations) {
			bb.WriteUInt(opLog.op);
			if (opLog.op < OpLog.OP_CLEAR) {
				bb.WriteUInt(opLog.index);
				if (opLog.op < OpLog.OP_REMOVE)
					//noinspection DataFlowIssue
					opLog.value.encode(bb);
			}
		}
	}

	private void read(IByteBuffer bb) {
		for (int i = bb.ReadUIntPositive(); i > 0; i--) {
			var logBean = LogMap2.decodeLogBean(bb);
			int index = bb.ReadUIntPositive();
			getChanged().put(logBean, new OutInt(index));
		}

		int logSize = bb.ReadUIntPositive();
		if (logSize > 0) {
			Function<IByteBuffer, V> decoder = this::readValue;
			for (; logSize > 0; logSize--)
				getOpLogs().add(OpLog.decode(bb, decoder));
		}
	}

	@SuppressWarnings("unchecked")
	private V readValue(IByteBuffer bb) {
		try {
			//noinspection DataFlowIssue
			var value = (V)meta.valueFactory.invoke();
			value.decode(bb);
			return value;
		} catch (Throwable e) { // MethodHandle.invoke
			throw Task.forceThrow(e);
		}
	}

	@Override
	public boolean addChild(Bean source, Log vlog) {
		var child = (LogBean)vlog;
		var item = child.getThis();
		if (item == null || addSet != null && addSet.contains(item)) return true;
		if (changed != null && changed.containsKey(child)) return false;
		if (indices == null) {
			var list = ((PList2<?>)source).list;
			indices = new IdentityHashMap<>(list.size());
			for (int i = 0; i < list.size(); i++) indices.putIfAbsent(list.get(i), i);
		}
		var index = indices.get(item);
		if (index == null) return true;
		getChanged().put(child, new OutInt(index));
		return true;
	}

	@Override
	public String toString() {
		var sb = new StringBuilder();
		sb.append(" opLogs:");
		ByteBuffer.BuildSortedString(sb, operationsForRead());
		sb.append(" changed:");
		ByteBuffer.BuildSortedString(sb, changesForRead());
		return sb.toString();
	}

	public static <V extends Bean> LogList2<V> decode(int varId, Meta1<V> meta, IByteBuffer bb) {
		var log = new LogList2<>(varId, null, meta, null, null);
		log.read(bb);
		return log;
	}

	@Override
	public void finishCollect() {
		indices = null;
		addSet = null;
	}
}
