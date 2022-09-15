package Zeze.Raft.RocksRaft;

import java.lang.invoke.MethodHandle;
import java.util.HashMap;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.OutInt;
import Zeze.Util.Reflect;

public class LogList2<V extends Bean> extends LogList1<V> {
	private final HashMap<LogBean, OutInt> Changed = new HashMap<>(); // changed V logs. using in collect.
	private final MethodHandle valueFactory;

	public LogList2(Class<V> valueClass) {
		super("Zeze.Raft.RocksRaft.LogList2<" + Reflect.GetStableName(valueClass) + '>', valueClass);
		valueFactory = Reflect.getDefaultConstructor(valueClass);
	}

	public LogList2(int typeId, MethodHandle valueFactory) {
		super(typeId, null);
		this.valueFactory = valueFactory;
	}

	public final HashMap<LogBean, OutInt> getChanged() {
		return Changed;
	}

	@Override
	public Log BeginSavepoint() {
		var dup = new LogList2<V>(getTypeId(), valueFactory);
		dup.setBelong(getBelong());
		dup.setVariableId(getVariableId());
		dup.setValue(getValue());
		return dup;
	}

	@Override
	public void encode(ByteBuffer bb) {
		var curList = getValue();
		if (curList != null) {
			for (var it = Changed.entrySet().iterator(); it.hasNext(); ) {
				var e = it.next();
				var logBean = e.getKey();
				//noinspection SuspiciousMethodCalls
				var idxExist = curList.indexOf(logBean.getThis());
				if (idxExist < 0)
					it.remove();
				else
					e.getValue().Value = idxExist;
			}
		}
		bb.WriteUInt(Changed.size());
		for (var e : Changed.entrySet()) {
			e.getKey().encode(bb);
			bb.WriteUInt(e.getValue().Value);
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
	public void decode(ByteBuffer bb) {
		Changed.clear();
		for (int i = bb.ReadUInt(); i > 0; i--) {
			var value = new LogBean();
			value.decode(bb);
			var index = bb.ReadUInt();
			Changed.put(value, new OutInt(index));
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
				} catch (Throwable e) {
					throw new RuntimeException(e);
				}
				value.decode(bb);
			}
			opLogs.add(new OpLog<>(op, index, value));
		}
	}

	@Override
	public void Collect(Changes changes, Bean recent, Log vlog) {
		if (Changed.put((LogBean)vlog, new OutInt()) == null)
			changes.Collect(recent, this);
	}

	@Override
	public String toString() {
		var sb = new StringBuilder();
		sb.append(" opLogs:");
		ByteBuffer.BuildSortedString(sb, getOpLogs());
		sb.append(" Changed:");
		ByteBuffer.BuildSortedString(sb, Changed);
		return sb.toString();
	}
}
