package Zeze.Transaction.Collections;

import java.lang.invoke.MethodHandle;
import java.util.Collection;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Log;
import Zeze.Transaction.Record;
import Zeze.Transaction.Transaction;
import Zeze.Util.IntHashSet;
import Zeze.Util.Reflect;
import org.pcollections.Empty;

public class PList2<V extends Bean> extends PList<V> {
	private final MethodHandle valueFactory;
	private final int logTypeId;

	public PList2(Class<V> valueClass) {
		valueFactory = Reflect.getDefaultConstructor(valueClass);
		logTypeId = Zeze.Transaction.Bean.hash32("Zeze.Raft.RocksRaft.LogList2<" + Reflect.getStableName(valueClass) + '>');
	}

	private PList2(int logTypeId, MethodHandle valueFactory) {
		this.valueFactory = valueFactory;
		this.logTypeId = logTypeId;
	}

	@Override
	public boolean add(V item) {
		if (item == null)
			throw new IllegalArgumentException("null item");

		if (isManaged()) {
			item.initRootInfoWithRedo(rootInfo, this);
			@SuppressWarnings("unchecked")
			var listLog = (LogList2<V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(
					parent().objectId() + variableId(), this::createLogBean);
			return listLog.add(item);
		}
		var newList = list.plus(item);
		if (newList == list)
			return false;
		list = newList;
		return true;
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean remove(Object item) {
		if (isManaged()) {
			var listLog = (LogList2<V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(
					parent().objectId() + variableId(), this::createLogBean);
			return listLog.remove((V)item);
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
			var listLog = (LogList2<V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(
					parent().objectId() + variableId(), this::createLogBean);
			listLog.clear();
		} else
			list = org.pcollections.Empty.vector();
	}

	@Override
	public V set(int index, V item) {
		if (item == null)
			throw new IllegalArgumentException("null item");

		if (isManaged()) {
			item.initRootInfoWithRedo(rootInfo, this);
			@SuppressWarnings("unchecked")
			var listLog = (LogList2<V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(
					parent().objectId() + variableId(), this::createLogBean);
			return listLog.set(index, item);
		}
		var old = list.get(index);
		list = list.with(index, item);
		return old;
	}

	@Override
	public void add(int index, V item) {
		if (item == null)
			throw new IllegalArgumentException("null item");

		if (isManaged()) {
			item.initRootInfoWithRedo(rootInfo, this);
			@SuppressWarnings("unchecked")
			var listLog = (LogList2<V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(
					parent().objectId() + variableId(), this::createLogBean);
			listLog.add(index, item);
		} else
			list = list.plus(index, item);
	}

	@Override
	public V remove(int index) {
		if (isManaged()) {
			@SuppressWarnings("unchecked")
			var listLog = (LogList2<V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(
					parent().objectId() + variableId(), this::createLogBean);
			return listLog.remove(index);
		}
		var old = list.get(index);
		list = list.minus(index);
		return old;
	}

	@Override
	public boolean addAll(Collection<? extends V> items) {
		if (isManaged()) {
			for (var item : items) {
				item.initRootInfoWithRedo(rootInfo, this);
			}
			@SuppressWarnings("unchecked")
			var listLog = (LogList2<V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(
					parent().objectId() + variableId(), this::createLogBean);
			return listLog.addAll(items);
		}
		list = list.plusAll(items);
		return true;
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean removeAll(Collection<?> c) {
		if (isManaged()) {
			var listLog = (LogList2<V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(
					parent().objectId() + variableId(), this::createLogBean);
			return listLog.removeAll((Collection<? extends V>)c);
		}
		var oldV = list;
		list = list.minusAll(c);
		return oldV != list;
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
		var newest = new IntHashSet();
		for (var opLog : log.getOpLogs()) {
			switch (opLog.op) {
			case LogList1.OpLog.OP_MODIFY:
				opLog.value.initRootInfo(rootInfo, this);
				tmp = tmp.with(opLog.index, opLog.value);
				newest.add(opLog.index);
				break;
			case LogList1.OpLog.OP_ADD:
				opLog.value.initRootInfo(rootInfo, this);
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
		list = tmp;

		// apply changed
		for (var e : log.getChanged().entrySet()) {
			if (newest.contains(e.getValue().value))
				continue;
			list.get(e.getValue().value).followerApply(e.getKey());
		}
	}

	@Override
	protected void initChildrenRootInfo(Record.RootInfo root) {
		for (var v : list)
			v.initRootInfo(root, this);
	}

	@Override
	protected void resetChildrenRootInfo() {
		for (var v : list)
			v.resetRootInfo();
	}

	@Override
	public PList2<V> copyBean() {
		var copy = new PList2<V>(logTypeId, valueFactory);
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
