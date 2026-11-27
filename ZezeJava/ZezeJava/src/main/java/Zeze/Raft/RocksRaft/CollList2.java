package Zeze.Raft.RocksRaft;

import org.jspecify.annotations.Nullable;

import java.util.Objects;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Collections.Persistents;
import Zeze.Transaction.HasManagedException;

/**
 * 管理 Bean 的 List 容器（值类型可变 bean）：结构操作记 LogList2 增量日志，值 bean 的字段修改记 changed。
 */
public class CollList2<V extends Bean> extends CollList<V> {
	private final BeanValueFactory<V> valueFactory;
	private final int logTypeId;

	public CollList2(Class<V> valueClass) {
		valueFactory = BeanValueFactory.of(valueClass);
		logTypeId = LogList2.typeId(valueClass);
	}

	public static CollList2<DynamicBean> dynamic(DynamicField field) {
		return new CollList2<>(LogList2.typeId(DynamicBean.class),
				field.values());
	}

	private CollList2(int logTypeId, BeanValueFactory<V> valueFactory) {
		this.valueFactory = valueFactory;
		this.logTypeId = logTypeId;
	}

	@Override
	public boolean add(V item) {
		valueFactory.checkValue(item);
		if (isManaged()) {
			item.initRootInfo(rootInfo(), this);
			//noinspection unchecked,DataFlowIssue
			var listLog = (LogList2<V>)Transaction.getCurrent().logGetOrAdd(
					parent().objectId() + variableId(), this::createLogBean);
			listLog.add(item);
		} else {
			if (item.isManaged())
				throw new HasManagedException();
			list = list.plus(item);
		}
		return true;
	}

	@Override
	public boolean remove(V item) {
		if (isManaged()) {
			//noinspection unchecked,DataFlowIssue
			var listLog = (LogList2<V>)Transaction.getCurrent().logGetOrAdd(
					parent().objectId() + variableId(), this::createLogBean);
			return listLog.remove(item);
		}
		var newList = list.minus(item);
		if (newList == list)
			return false;
		list = newList;
		return true;
	}

	@Override
	public void clear() {
		if (isManaged()) {
			//noinspection unchecked,DataFlowIssue
			var listLog = (LogList2<V>)Transaction.getCurrent().logGetOrAdd(
					parent().objectId() + variableId(), this::createLogBean);
			listLog.clear();
		} else
			list = Zeze.Collections.Persistents.vector();
	}

	@Override
	public V set(int index, V item) {
		valueFactory.checkValue(item);
		if (isManaged()) {
			// 先验界再挂接，避免越界抛异常后 item 残留脏归属。
			var cur = getList();
			if (index < 0 || index >= cur.size())
				throw new IndexOutOfBoundsException("index: " + index + ", size: " + cur.size());
			item.initRootInfo(rootInfo(), this);
			//noinspection unchecked,DataFlowIssue
			var listLog = (LogList2<V>)Transaction.getCurrent().logGetOrAdd(
					parent().objectId() + variableId(), this::createLogBean);
			return listLog.set(index, item);
		}
		if (item.isManaged())
			throw new HasManagedException();
		var old = list.get(index);
		list = list.with(index, item);
		return old;
	}

	@Override
	public void add(int index, V item) {
		valueFactory.checkValue(item);
		if (isManaged()) {
			// 先验界再挂接，避免越界抛异常后 item 残留脏归属。
			var cur = getList();
			if (index < 0 || index > cur.size())
				throw new IndexOutOfBoundsException("index: " + index + ", size: " + cur.size());
			item.initRootInfo(rootInfo(), this);
			//noinspection unchecked,DataFlowIssue
			var listLog = (LogList2<V>)Transaction.getCurrent().logGetOrAdd(
					parent().objectId() + variableId(), this::createLogBean);
			listLog.add(index, item);
		} else {
			if (item.isManaged())
				throw new HasManagedException();
			list = list.plus(index, item);
		}
	}

	@Override
	public V remove(int index) {
		if (isManaged()) {
			//noinspection unchecked,DataFlowIssue
			var listLog = (LogList2<V>)Transaction.getCurrent().logGetOrAdd(
					parent().objectId() + variableId(), this::createLogBean);
			return listLog.remove(index);
		}
		var old = list.get(index);
		list = list.minus(index);
		return old;
	}

	@Override
	public LogBean createLogBean() {
		var log = new LogList2<>(logTypeId, valueFactory);
		log.setBelong(parent());
		log.setThis(this);
		log.setVariableId(variableId());
		log.setValue(list);
		return log;
	}

	@Override
	public void followerApply(Log _log) {
		valueFactory.checkLog(_log);
		@SuppressWarnings("unchecked")
		var log = (LogList2<V>)_log;
		var tmp = list;
		for (var opLog : log.getOpLogs()) {
			tmp = switch (opLog.op) {
				case LogList1.OpLog.OP_MODIFY -> {
					//noinspection DataFlowIssue
					opLog.value.initRootInfo(rootInfo(), this);
					yield tmp.with(opLog.index, Objects.requireNonNull(opLog.value));
				}
				case LogList1.OpLog.OP_ADD -> {
					//noinspection DataFlowIssue
					opLog.value.initRootInfo(rootInfo(), this);
					yield tmp.plus(opLog.index, Objects.requireNonNull(opLog.value));
				}
				case LogList1.OpLog.OP_REMOVE -> tmp.minus(opLog.index);
				case LogList1.OpLog.OP_CLEAR -> Persistents.vector();
				default -> tmp;
			};
		}
		list = tmp;

		// encode 侧已按最终列表过滤并重算下标，此处越界即状态机分歧，直接抛出由 Rocks.followerApply 统一 fatalKill。
		for (var e : log.getChanged().entrySet())
			list.get(e.getValue().value).followerApply(e.getKey());
	}

	@SuppressWarnings("unchecked")
	@Override
	public void leaderApplyNoRecursive(Log _log) {
		list = Objects.requireNonNull(((LogList2<V>)_log).getValue());
	}

	@Override
	protected void initChildrenRootInfo(Record.@Nullable RootInfo root) {
		for (var v : list)
			v.initRootInfo(root, this);
	}

	@Override
	public CollList2<V> copy() {
		var copy = new CollList2<>(logTypeId, valueFactory);
		copy.list = getList();
		return copy;
	}

	@Override
	public void encode(ByteBuffer bb) {
		var tmp = getList();
		bb.WriteUInt(tmp.size());
		for (var e : tmp)
			e.encode(bb);
	}

	@Override
	public void decode(IByteBuffer bb) {
		clear();
		for (int i = bb.ReadUInt(); i > 0; i--)
			add(valueFactory.read(bb));
	}
}
