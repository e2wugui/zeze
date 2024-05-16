package Zeze.Transaction.Collections;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.function.LongFunction;
import java.util.function.ToLongFunction;
import java.util.function.UnaryOperator;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Data;
import Zeze.Transaction.Log;
import Zeze.Transaction.Record;
import Zeze.Transaction.Transaction;
import Zeze.Util.Task;
import org.jetbrains.annotations.NotNull;
import org.pcollections.Empty;

@SuppressWarnings("DataFlowIssue")
public class PList2<V extends Bean> extends PList<V> {
	protected final @NotNull Meta1<V> meta;

	public PList2(@NotNull Class<V> valueClass) {
		meta = Meta1.getList2Meta(valueClass);
	}

	public PList2(@NotNull ToLongFunction<Bean> get, @NotNull LongFunction<Bean> create) { // only for DynamicBean value
		meta = Meta1.createDynamicListMeta(get, create);
	}

	private PList2(@NotNull Meta1<V> meta) {
		this.meta = meta;
	}

	@SuppressWarnings("unchecked")
	public @NotNull V createValue() {
		try {
			return (V)meta.valueFactory.invoke();
		} catch (Throwable e) { // MethodHandle.invoke
			Task.forceThrow(e);
			return null; // never run here
		}
	}

	@Override
	public boolean add(@NotNull V item) {
		//noinspection ConstantValue
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
	public boolean remove(@NotNull Object item) {
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
			list = Empty.vector();
	}

	@Override
	public @NotNull V set(int index, @NotNull V item) {
		//noinspection ConstantValue
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
	public void add(int index, @NotNull V item) {
		//noinspection ConstantValue
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
	public @NotNull V remove(int index) {
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
	public boolean addAll(@NotNull Collection<? extends V> items) {
		if (isManaged()) {
			for (var item : items)
				item.initRootInfoWithRedo(rootInfo, this);
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
	public boolean removeAll(@NotNull Collection<?> c) {
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
	public void replaceAll(@NotNull UnaryOperator<V> operator) {
		if (list.isEmpty())
			return;
		var tmpList = new ArrayList<V>(size());
		if (isManaged()) {
			for (V v : this) {
				var newV = operator.apply(v);
				if (newV != v)
					newV.initRootInfoWithRedo(rootInfo, this);
				tmpList.add(newV);
			}
			@SuppressWarnings("unchecked")
			var listLog = (LogList2<V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(
					parent().objectId() + variableId(), this::createLogBean);
			listLog.clear();
			listLog.addAll(tmpList);
			return;
		}
		for (V v : this)
			tmpList.add(operator.apply(v));
		list = Empty.vector();
		list = list.plusAll(tmpList);
	}

	@Override
	public void sort(@NotNull Comparator<? super V> c) {
		if (list.isEmpty())
			return;
		var tmpList = new ArrayList<>(this);
		tmpList.sort(c);
		if (isManaged()) {
			@SuppressWarnings("unchecked")
			var listLog = (LogList2<V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(
					parent().objectId() + variableId(), this::createLogBean);
			listLog.clear();
			listLog.addAll(tmpList);
			return;
		}
		list = Empty.vector();
		list = list.plusAll(tmpList);
	}

	@Override
	public @NotNull LogBean createLogBean() {
		var log = new LogList2<>(meta);
		log.setBelong(parent());
		log.setThis(this);
		log.setVariableId(variableId());
		log.setValue(list);
		return log;
	}

	@Override
	public void followerApply(@NotNull Log _log) {
		@SuppressWarnings("unchecked")
		var log = (LogList2<V>)_log;
		var tmp = list;
		for (var opLog : log.getOpLogs()) {
			switch (opLog.op) {
			case LogList1.OpLog.OP_MODIFY:
				opLog.value.initRootInfo(rootInfo, this);
				tmp = tmp.with(opLog.index, opLog.value);
				break;
			case LogList1.OpLog.OP_ADD:
				opLog.value.initRootInfo(rootInfo, this);
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

		// apply changed
		for (var e : log.getChanged().entrySet()) {
			var item = list.get(e.getValue().value);
			item.followerApply(e.getKey());
		}
	}

	@Override
	protected void initChildrenRootInfo(@NotNull Record.RootInfo root) {
		for (var v : list)
			v.initRootInfo(root, this);
	}

	@Override
	protected void initChildrenRootInfoWithRedo(@NotNull Record.RootInfo root) {
		for (var v : list)
			v.initRootInfoWithRedo(root, this);
	}

	@Override
	public @NotNull PList2<V> copy() {
		var copy = new PList2<>(meta);
		copy.list = getList();
		return copy;
	}

	@Override
	public void encode(@NotNull ByteBuffer bb) {
		var tmp = getList();
		bb.WriteUInt(tmp.size());
		for (var e : tmp)
			e.encode(bb);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void decode(@NotNull IByteBuffer bb) {
		clear();
		try {
			for (int i = bb.ReadUInt(); i > 0; i--) {
				V value = (V)meta.valueFactory.invoke();
				value.decode(bb);
				add(value);
			}
		} catch (Throwable e) { // MethodHandle.invoke
			Task.forceThrow(e);
		}
	}

	public <D extends Data> void addAllData(@NotNull Collection<D> dataList) {
		Bean.toBeanList(dataList, this);
	}

	public <D extends Data> void toDataList(@NotNull Collection<D> dataList) {
		Bean.toDataList(getList(), dataList);
	}

	public <D extends Data> @NotNull ArrayList<D> toDataList() {
		var beanList = getList();
		var dataList = new ArrayList<D>(beanList.size());
		Bean.toDataList(beanList, dataList);
		return dataList;
	}
}
