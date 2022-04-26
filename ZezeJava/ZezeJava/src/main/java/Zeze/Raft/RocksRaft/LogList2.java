package Zeze.Raft.RocksRaft;

import java.lang.invoke.MethodHandle;
import java.util.HashSet;
import java.util.Set;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.Reflect;

public class LogList2<V extends Bean> extends LogList1<V> {
	private final Set<LogBean> Changed = new HashSet<>(); // changed V logs. using in collect.
	private final MethodHandle valueFactory;

	public LogList2(Class<V> valueClass) {
		super("Zeze.Raft.RocksRaft.LogList2<" + Reflect.GetStableName(valueClass) + '>', valueClass);
		valueFactory = Reflect.getDefaultConstructor(valueClass);
	}

	LogList2(int typeId, MethodHandle valueFactory) {
		super(typeId, null);
		this.valueFactory = valueFactory;
	}

	public final Set<LogBean> getChanged() {
		return Changed;
	}

	@Override
	public void Encode(ByteBuffer bb) {
		for (var it = Changed.iterator(); it.hasNext(); ) {
			var bean = it.next().getThis();
			for (var opLog : opLogs) {
				if (opLog.value == bean) {
					it.remove();
					break;
				}
			}
		}
		bb.WriteUInt(Changed.size());
		for (var e : Changed)
			e.Encode(bb);

		// super.Encode(bb);
		var logSize = opLogs.size();
		bb.WriteUInt(cleared ? -1 - logSize : logSize);
		for (var opLog : opLogs) {
			bb.WriteUInt(opLog.op);
			bb.WriteUInt(opLog.index);
			if (opLog.op != OpLog.OP_REMOVE)
				opLog.value.Encode(bb);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void Decode(ByteBuffer bb) {
		Changed.clear();
		for (int i = bb.ReadUInt(); i > 0; i--) {
			var value = new LogBean();
			value.Decode(bb);
			Changed.add(value);
		}

		// super.Decode(bb);
		var logSize = bb.ReadUInt();
		if (logSize < 0) {
			cleared = true;
			logSize = -1 - logSize;
		}
		opLogs.clear();
		while (--logSize >= 0) {
			int op = bb.ReadUInt();
			int index = bb.ReadUInt();
			V value = null;
			if (op != OpLog.OP_REMOVE) {
				try {
					value = (V)valueFactory.invoke();
				} catch (Throwable e) {
					throw new RuntimeException(e);
				}
				value.Decode(bb);
			}
			opLogs.add(new OpLog<>(op, index, value));
		}
	}

	@Override
	public void Collect(Changes changes, Bean recent, Log vlog) {
		if (Changed.add((LogBean)vlog))
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
