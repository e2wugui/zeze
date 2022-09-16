package Zeze.Transaction.Collections;

import java.util.Collection;
import java.util.function.LongFunction;
import java.util.function.ToLongFunction;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Log;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.SerializeHelper;
import Zeze.Transaction.Record;
import Zeze.Transaction.Transaction;
import Zeze.Util.Reflect;
import org.pcollections.Empty;

public class PList1<V> extends PList<V> {
	protected final SerializeHelper.CodecFuncs<V> valueCodecFuncs;
	private final int logTypeId;

	public PList1(Class<V> valueClass) {
		valueCodecFuncs = SerializeHelper.createCodec(valueClass);
		logTypeId = Zeze.Transaction.Bean.hash32("Zeze.Transaction.LogList1<" + Reflect.getStableName(valueClass) + '>');
	}

	@SuppressWarnings("unchecked")
	public PList1(ToLongFunction<Bean> get, LongFunction<Bean> create) { // only for DynamicBean value
		valueCodecFuncs = (SerializeHelper.CodecFuncs<V>)SerializeHelper.createCodec(get, create);
		logTypeId = Zeze.Transaction.Bean.hash32("Zeze.Transaction.LogList1<Zeze.Transaction.DynamicBean>");
	}

	private PList1(int logTypeId, SerializeHelper.CodecFuncs<V> valueCodecFuncs) {
		this.valueCodecFuncs = valueCodecFuncs;
		this.logTypeId = logTypeId;
	}

	public SerializeHelper.CodecFuncs<V> getValueCodecFuncs() {
		return valueCodecFuncs;
	}

	@Override
	public boolean add(V item) {
		if (item == null)
			throw new IllegalArgumentException("null item");

		if (isManaged()) {
			@SuppressWarnings("unchecked")
			var listLog = (LogList1<V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(
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
			var listLog = (LogList1<V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(
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
			var listLog = (LogList1<V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(
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
			@SuppressWarnings("unchecked")
			var listLog = (LogList1<V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(
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
			@SuppressWarnings("unchecked")
			var listLog = (LogList1<V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(
					parent().objectId() + variableId(), this::createLogBean);
			listLog.add(index, item);
		} else
			list = list.plus(index, item);
	}

	@Override
	public V remove(int index) {
		if (isManaged()) {
			@SuppressWarnings("unchecked")
			var listLog = (LogList1<V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(
					parent().objectId() + variableId(), this::createLogBean);
			return listLog.remove(index);
		}
		var old = list.get(index);
		list = list.minus(index);
		return old;
	}

	@Override
	public boolean addAll(Collection<? extends V> items) {
		// XXX
		for (var v : items) {
			if (v == null)
				throw new IllegalArgumentException("null in items");
		}

		if (isManaged()) {
			@SuppressWarnings("unchecked")
			var listLog = (LogList1<V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(
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
			var listLog = (LogList1<V>)Transaction.getCurrentVerifyWrite(this).logGetOrAdd(
					parent().objectId() + variableId(), this::createLogBean);
			return listLog.removeAll((Collection<V>)c);
		}
		var oldList = list;
		list = list.minusAll(c);
		return oldList != list;
	}

	@Override
	public LogBean createLogBean() {
		var log = new LogList1<>(logTypeId, valueCodecFuncs);
		log.setBelong(parent());
		log.setThis(this);
		log.setVariableId(variableId());
		log.setValue(list);
		return log;
	}

	@Override
	public void followerApply(Log _log) {
		@SuppressWarnings("unchecked")
		var log = (LogList1<V>)_log;
		for (var opLog : log.getOpLogs()) {
			switch (opLog.op) {
			case LogList1.OpLog.OP_MODIFY:
				list = list.with(opLog.index, opLog.value);
				break;
			case LogList1.OpLog.OP_ADD:
				list = list.plus(opLog.index, opLog.value);
				break;
			case LogList1.OpLog.OP_REMOVE:
				list = list.minus(opLog.index);
				break;
			case LogList1.OpLog.OP_CLEAR:
				list = Empty.vector();
				break;
			}
		}
	}

	@Override
	protected void initChildrenRootInfo(Record.RootInfo root) {
	}

	@Override
	protected void resetChildrenRootInfo() {
	}

	@Override
	public PList1<V> copyBean() {
		var copy = new PList1<>(logTypeId, valueCodecFuncs);
		copy.list = list;
		return copy;
	}

	@Override
	public void encode(ByteBuffer bb) {
		var tmp = getList();
		bb.WriteUInt(tmp.size());
		var encoder = valueCodecFuncs.encoder;
		for (var e : tmp)
			encoder.accept(bb, e);
	}

	@Override
	public void decode(ByteBuffer bb) {
		clear();
		var decoder = valueCodecFuncs.decoder;
		for (int i = bb.ReadUInt(); i > 0; i--)
			add(decoder.apply(bb));
	}
}
