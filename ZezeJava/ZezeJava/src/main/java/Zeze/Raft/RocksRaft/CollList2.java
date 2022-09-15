package Zeze.Raft.RocksRaft;

import java.lang.invoke.MethodHandle;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.IntHashSet;
import Zeze.Util.Reflect;
import org.pcollections.Empty;

public class CollList2<V extends Bean> extends CollList<V> {
	private final MethodHandle valueFactory;
	private final int logTypeId;

	public CollList2(Class<V> valueClass) {
		valueFactory = Reflect.getDefaultConstructor(valueClass);
		logTypeId = Zeze.Transaction.Bean.hash32("Zeze.Raft.RocksRaft.LogList2<" + Reflect.GetStableName(valueClass) + '>');
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
			var listLog = (LogList2<V>)Transaction.getCurrent().LogGetOrAdd(
					parent().objectId() + variableId(), this::createLogBean);
			return listLog.Add(item);
		}
		var newList = _list.plus(item);
		if (newList == _list)
			return false;
		_list = newList;
		return true;
	}

	@Override
	public boolean remove(V item) {
		if (isManaged()) {
			@SuppressWarnings("unchecked")
			var listLog = (LogList2<V>)Transaction.getCurrent().LogGetOrAdd(
					parent().objectId() + variableId(), this::createLogBean);
			return listLog.Remove(item);
		}
		var newList = _list.minus(item);
		if (newList == _list)
			return false;
		_list = newList;
		return true;
	}

	@Override
	public void clear() {
		if (isManaged()) {
			@SuppressWarnings("unchecked")
			var listLog = (LogList2<V>)Transaction.getCurrent().LogGetOrAdd(
					parent().objectId() + variableId(), this::createLogBean);
			listLog.Clear();
		} else
			_list = org.pcollections.Empty.vector();
	}

	@Override
	public V set(int index, V item) {
		if (isManaged()) {
			item.initRootInfo(rootInfo(), this);
			@SuppressWarnings("unchecked")
			var listLog = (LogList2<V>)Transaction.getCurrent().LogGetOrAdd(
					parent().objectId() + variableId(), this::createLogBean);
			return listLog.Set(index, item);
		}
		var old = _list.get(index);
		_list = _list.with(index, item);
		return old;
	}

	@Override
	public void add(int index, V item) {
		if (isManaged()) {
			item.initRootInfo(rootInfo(), this);
			@SuppressWarnings("unchecked")
			var listLog = (LogList2<V>)Transaction.getCurrent().LogGetOrAdd(
					parent().objectId() + variableId(), this::createLogBean);
			listLog.Add(index, item);
		} else
			_list = _list.plus(index, item);
	}

	@Override
	public V remove(int index) {
		if (isManaged()) {
			@SuppressWarnings("unchecked")
			var listLog = (LogList2<V>)Transaction.getCurrent().LogGetOrAdd(
					parent().objectId() + variableId(), this::createLogBean);
			return listLog.Remove(index);
		}
		var old = _list.get(index);
		_list = _list.minus(index);
		return old;
	}

	@Override
	public LogBean createLogBean() {
		var log = new LogList2<V>(logTypeId, valueFactory);
		log.setBelong(parent());
		log.setThis(this);
		log.setVariableId(variableId());
		log.setValue(_list);
		return log;
	}

	@Override
	public void followerApply(Log _log) {
		@SuppressWarnings("unchecked")
		var log = (LogList2<V>)_log;
		var tmp = _list;
		var newest = new IntHashSet();
		for (var opLog : log.getOpLogs()) {
			switch (opLog.op) {
			case LogList1.OpLog.OP_MODIFY:
				opLog.value.initRootInfo(rootInfo(), this);
				tmp = tmp.with(opLog.index, opLog.value);
				newest.add(opLog.index);
				break;
			case LogList1.OpLog.OP_ADD:
				opLog.value.initRootInfo(rootInfo(), this);
				tmp = tmp.plus(opLog.index, opLog.value);
				newest.add(opLog.index);
				break;
			case LogList1.OpLog.OP_REMOVE:
				tmp = tmp.minus(opLog.index);
				break;
			case LogList1.OpLog.OP_CLEAR:
				tmp = Empty.vector();
			}
		}
		_list = tmp;

		// apply changed
		for (var e : log.getChanged().entrySet()) {
			if (newest.contains(e.getValue().Value))
				continue;
			_list.get(e.getValue().Value).followerApply(e.getKey());
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void leaderApplyNoRecursive(Log _log) {
		_list = ((LogList2<V>)_log).getValue();
	}

	@Override
	protected void initChildrenRootInfo(Record.RootInfo root) {
		for (var v : _list)
			v.initRootInfo(root, this);
	}

	@Override
	public Bean copyBean() {
		var copy = new CollList2<V>(logTypeId, valueFactory);
		copy._list = _list;
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
	public void decode(ByteBuffer bb) {
		clear();
		for (int i = bb.ReadUInt(); i > 0; i--) {
			V value;
			try {
				value = (V)valueFactory.invoke();
			} catch (Throwable e) {
				throw new RuntimeException(e);
			}
			value.decode(bb);
			add(value);
		}
	}
}
