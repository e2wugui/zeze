package Zeze.Transaction.Collections;

import java.util.HashMap;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Changes;
import Zeze.Transaction.Log;
import Zeze.Util.IdentityHashSet;
import Zeze.Util.OutInt;
import org.jetbrains.annotations.NotNull;

public class LogList2<V extends Bean> extends LogList1<V> implements IZeroLogBean {
	private final HashMap<LogBean, OutInt> changed = new HashMap<>(); // changed V logs. using in collect.

	public LogList2(@NotNull Meta1<V> meta) {
		super(meta);
		addSet = new IdentityHashSet<>();
	}

	public LogList2(@NotNull Class<V> valueClass) {
		super(Meta1.getList2Meta(valueClass));
		// addSet not need.
	}

	public final @NotNull HashMap<LogBean, OutInt> getChanged() {
		return changed;
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
				var logBean = e.getKey();
				//noinspection SuspiciousMethodCalls
				var idxExist = curList.indexOf(logBean.getThis());
				if (idxExist < 0 || addSet.contains(curList.get(idxExist)))
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
			LogMap2.encodeLogBeanWithTag(bb, e.getKey());
			bb.WriteUInt(e.getValue().value);
		}

		// super.encode(bb);
		bb.WriteUInt(opLogs.size());
		for (var opLog : opLogs) {
			bb.WriteUInt(opLog.op);
			if (opLog.op < OpLog.OP_CLEAR) {
				bb.WriteUInt(opLog.index);
				if (opLog.op < OpLog.OP_REMOVE)
					encodeLogBeanWithTag(bb, opLog.valueLogBean); //旧版协议：opLog.value.encode(bb);
			}
		}
	}

	@Override
	public void decode(@NotNull IByteBuffer bb) {
		changed.clear();
		for (int i = bb.ReadUInt(); i > 0; i--) {
			var value = LogMap2.decodeLogBeanByTag(bb);
			var index = bb.ReadUInt();
			changed.put(value, new OutInt(index));
		}

		// super.decode(bb);
		opLogs.clear();
		for (var logSize = bb.ReadUInt(); --logSize >= 0; ) {
			int op = bb.ReadUInt();
			int index = op < OpLog.OP_CLEAR ? bb.ReadUInt() : 0;
			LogBean valueLogBean = null;
			if (op < OpLog.OP_REMOVE) {
				valueLogBean = decodeLogBeanByTag(bb);
			}
			opLogs.add(new OpLog<>(op, index, valueLogBean));
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
		sb.append(" Changed:");
		ByteBuffer.BuildSortedString(sb, changed);
		return sb.toString();
	}

	@Override
	public void zeroLogBean(Changes changes) {
		var container = getThis();
		var tableKey = container.tableKey();
		var record = changes.getRecords().get(tableKey);
		for (var opLog : opLogs) {
			// 等价于 (opLog.value != null)? 这里继续使用encode/decode中的判断，保持一致。
			if ((opLog.op < OpLog.OP_REMOVE)) {
				opLog.valueLogBean = record.getLogBeans().get(opLog.value);
				if (null == opLog.valueLogBean)
					opLog.valueLogBean = LogBean.Zero; // encode需要一个，即使没有。
			}
		}
	}
}
