package Zeze.Raft.RocksRaft;

import java.lang.invoke.MethodHandle;
import java.util.HashMap;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.SerializeHelper;
import Zeze.Util.OutInt;
import Zeze.Util.Reflect;
import Zeze.Util.Task;

public class LogList2<V extends Bean> extends LogList1<V> {
	private static final long logTypeIdHead = Zeze.Transaction.Bean.hash64("Zeze.Raft.RocksRaft.LogList2<");

	private final HashMap<LogBean, OutInt> changed = new HashMap<>(); // changed V logs. using in collect.
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
	public void encode(ByteBuffer bb) {
		var curList = getValue();
		if (curList != null) {
			for (var it = changed.entrySet().iterator(); it.hasNext(); ) {
				var e = it.next();
				var logBean = e.getKey();
				//noinspection SuspiciousMethodCalls
				var idxExist = curList.indexOf(logBean.getThis());
				if (idxExist < 0)
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
					Task.forceThrow(e);
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
