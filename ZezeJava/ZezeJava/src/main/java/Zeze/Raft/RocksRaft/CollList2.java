package Zeze.Raft.RocksRaft;

import java.lang.invoke.MethodHandle;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Util.Reflect;
import Zeze.Util.Task;
import org.pcollections.Empty;

public class CollList2<V extends Bean> extends CollList<V> {
	private final MethodHandle valueFactory;
	private final int logTypeId;

	public CollList2(Class<V> valueClass) {
		valueFactory = Reflect.getDefaultConstructor(valueClass);
		logTypeId = Zeze.Transaction.Bean.hash32("Zeze.Raft.RocksRaft.LogList2<" + Reflect.getStableName(valueClass) + '>');
	}

	private CollList2(int logTypeId, MethodHandle valueFactory) {
		this.valueFactory = valueFactory;
		this.logTypeId = logTypeId;
	}

	@Override
	public boolean add(V item) {
		if (isManaged()) {
			item.initRootInfo(rootInfo(), this);
			@SuppressWarnings("unchecked")
			var listLog = (LogList2<V>)Transaction.getCurrent().logGetOrAdd(
					parent().objectId() + variableId(), this::createLogBean);
			listLog.add(item);
		} else
			list = list.plus(item);
		return true;
	}

	@Override
	public boolean remove(V item) {
		if (isManaged()) {
			@SuppressWarnings("unchecked")
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
			@SuppressWarnings("unchecked")
			var listLog = (LogList2<V>)Transaction.getCurrent().logGetOrAdd(
					parent().objectId() + variableId(), this::createLogBean);
			listLog.clear();
		} else
			list = org.pcollections.Empty.vector();
	}

	@Override
	public V set(int index, V item) {
		if (isManaged()) {
			item.initRootInfo(rootInfo(), this);
			@SuppressWarnings("unchecked")
			var listLog = (LogList2<V>)Transaction.getCurrent().logGetOrAdd(
					parent().objectId() + variableId(), this::createLogBean);
			return listLog.Set(index, item);
		}
		var old = list.get(index);
		list = list.with(index, item);
		return old;
	}

	@Override
	public void add(int index, V item) {
		if (isManaged()) {
			item.initRootInfo(rootInfo(), this);
			@SuppressWarnings("unchecked")
			var listLog = (LogList2<V>)Transaction.getCurrent().logGetOrAdd(
					parent().objectId() + variableId(), this::createLogBean);
			listLog.add(index, item);
		} else
			list = list.plus(index, item);
	}

	@Override
	public V remove(int index) {
		if (isManaged()) {
			@SuppressWarnings("unchecked")
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
		var log = new LogList2<V>(logTypeId, valueFactory);
		log.setBelong(parent());
		log.setThis(this);
		log.setVariableId(variableId());
		log.setValue(list);
		return log;
	}

	@Override
	public void followerApply(Log _log) {
		@SuppressWarnings("unchecked")
		var log = (LogList2<V>)_log;
		var tmp = list;
		for (var opLog : log.getOpLogs()) {
			switch (opLog.op) {
			case LogList1.OpLog.OP_MODIFY:
				opLog.value.initRootInfo(rootInfo(), this);
				tmp = tmp.with(opLog.index, opLog.value);
				break;
			case LogList1.OpLog.OP_ADD:
				opLog.value.initRootInfo(rootInfo(), this);
				tmp = tmp.plus(opLog.index, opLog.value);
				break;
			case LogList1.OpLog.OP_REMOVE:
				tmp = tmp.minus(opLog.index);
				break;
			case LogList1.OpLog.OP_CLEAR:
				tmp = Empty.vector();
			}
		}
		list = tmp;

		// apply changed：encode侧已按addSet身份过滤结构op携带bean的冗余条目（【FND3-19】对齐
		// 经典LogList2：op的value编码于提交时刻、已含最终状态，冗余增量再叠加会双重应用；
		// 旧的op时index启发式newest与changed最终坐标系错位，位移/加删相消时误跳过丢编辑）。
		// 【FND2-R2-2】changed 携带的 index 越过当前 list 边界（任何来源的先行分歧）时直接get
		// 抛IndexOutOfBoundsException：正常重放下index必在界内（LogList2.encode只保留最终
		// 列表中存在的bean并按最终列表计算index），越界即先行分歧。异常由Rocks.followerApply
		// 统一catch并fatalKill（宁死不糊；也兜住FND2-R2-2的"不catch则apply重试同条目反复抛出、
		// lastApplied楔死"教训），容器层不再内联防御。
		for (var e : log.getChanged().entrySet())
			list.get(e.getValue().value).followerApply(e.getKey());
	}

	@SuppressWarnings("unchecked")
	@Override
	public void leaderApplyNoRecursive(Log _log) {
		list = ((LogList2<V>)_log).getValue();
	}

	@Override
	protected void initChildrenRootInfo(Record.RootInfo root) {
		for (var v : list)
			v.initRootInfo(root, this);
	}

	@Override
	public CollList2<V> copy() {
		var copy = new CollList2<V>(logTypeId, valueFactory);
		copy.list = list;
		return copy;
	}

	@Override
	public void encode(ByteBuffer bb) {
		var tmp = getList();
		bb.WriteUInt(tmp.size());
		for (var e : tmp)
			e.encode(bb);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void decode(IByteBuffer bb) {
		clear();
		try {
			for (int i = bb.ReadUInt(); i > 0; i--) {
				V value = (V)valueFactory.invoke();
				value.decode(bb);
				add(value);
			}
		} catch (Throwable e) { // MethodHandle.invoke
			throw Task.forceThrow(e);
		}
	}
}
