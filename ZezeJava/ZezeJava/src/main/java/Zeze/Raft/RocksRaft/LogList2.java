package Zeze.Raft.RocksRaft;

import java.util.Objects;

import java.lang.invoke.MethodHandle;
import java.util.Collection;
import java.util.HashMap;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.SerializeHelper;
import Zeze.Util.IdentityHashSet;
import Zeze.Util.OutInt;
import org.jspecify.annotations.Nullable;

/**
 * 管理 Bean 的 List 容器增量日志：结构操作记 OpLog，值 bean 的字段修改记 changed，
 * encode 侧按 addSet 身份过滤冗余条目。
 */
public class LogList2<V extends Bean> extends LogList1<V> {
	private static final long logTypeIdHead = Zeze.Transaction.Bean.hash64("Zeze.Raft.RocksRaft.LogList2<");

	private final HashMap<LogBean, OutInt> changed = new HashMap<>(); // 收集阶段记录值 bean 的字段修改日志
	// 本日志结构操作携带的 bean 身份集合：这些 bean 已由 opLogs 编码最终状态，encode 时据此剔除冗余 changed。
	private @Nullable IdentityHashSet<V> addSet;
	private final BeanValueFactory<V> valueFactory;
	private boolean decoded; // 解码后的下标来自日志流，不再按内存对象身份重算。

	static int typeId(Class<?> valueClass) {
		return Zeze.Transaction.Bean.hashLog(logTypeIdHead, valueClass);
	}

	public LogList2(Class<V> valueClass) {
		super(typeId(valueClass), SerializeHelper.createCodec(valueClass));
		valueFactory = BeanValueFactory.of(valueClass);
	}

	public LogList2(int typeId, MethodHandle valueFactory) {
		this(typeId, BeanValueFactory.of(valueFactory));
	}

	LogList2(int typeId, BeanValueFactory<V> valueFactory) {
		super(typeId, null);
		this.valueFactory = valueFactory;
	}

	@Override
	@Nullable DynamicField dynamicField() {
		return valueFactory.field();
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
	public V set(int index, V item) {
		var old = super.set(index, item);
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
		var dup = new LogList2<>(getTypeId(), valueFactory);
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

	// savepoint 合并时同步传递 addSet，保证 encode 侧身份过滤完整。
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
		if (!decoded && curList != null) {
			for (var it = changed.entrySet().iterator(); it.hasNext(); ) {
				var e = it.next();
				var logBean = e.getKey();
				// 按对象身份（v == bean）查找下标，避免自定义 equals 造成错位或误留已删 bean。
				var bean = logBean.getThis();
				int idxExist = 0;
				for (var v : curList) {
					if (v == bean)
						break;
					idxExist++;
				}
				// 剔除已移出列表或已在 opLogs 中携带最终状态的条目，防止 follower 侧双重应用。
				if (idxExist >= curList.size() || addSet != null && addSet.contains(Objects.requireNonNull(bean)))
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

		// opLogs 手工编码（不走 super.encode）：value 是 Bean，用 bean.encode 而非 codec。
		bb.WriteUInt(opLogs.size());
		for (var opLog : opLogs) {
			bb.WriteUInt(opLog.op);
			if (opLog.op < OpLog.OP_CLEAR) {
				bb.WriteUInt(opLog.index);
				if (opLog.op < OpLog.OP_REMOVE)
					//noinspection DataFlowIssue
					opLog.value.encode(bb);
			}
		}
	}

	@Override
	public void decode(IByteBuffer bb) {
		addSet = null;
		changed.clear();
		for (int i = bb.ReadUInt(); i > 0; i--) {
			var value = valueFactory.readEdit(bb);
			var index = bb.ReadUInt();
			changed.put(value, new OutInt(index));
		}

		// opLogs 手工解码（不走 super.decode）：value 是 Bean，经 valueFactory 构造后 decode。
		opLogs.clear();
		for (var logSize = bb.ReadUInt(); --logSize >= 0; ) {
			int op = bb.ReadUInt();
			int index = op < OpLog.OP_CLEAR ? bb.ReadUInt() : 0;
			V value = null;
			if (op < OpLog.OP_REMOVE)
				value = valueFactory.read(bb);
			opLogs.add(new OpLog<>(op, index, value));
		}
		decoded = true;
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
