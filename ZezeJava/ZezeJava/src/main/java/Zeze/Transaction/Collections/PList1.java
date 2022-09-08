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
		logTypeId = Zeze.Transaction.Bean.Hash32("Zeze.Transaction.LogList1<" + Reflect.GetStableName(valueClass) + '>');
	}

	@SuppressWarnings("unchecked")
	public PList1(ToLongFunction<Bean> get, LongFunction<Bean> create) { // only for DynamicBean value
		valueCodecFuncs = (SerializeHelper.CodecFuncs<V>)SerializeHelper.createCodec(get, create);
		logTypeId = Zeze.Transaction.Bean.Hash32("Zeze.Transaction.LogList1<Zeze.Transaction.DynamicBean>");
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
			var listLog = (LogList1<V>)Transaction.getCurrentVerifyWrite(this).LogGetOrAdd(
					parent().objectId() + variableId(), this::CreateLogBean);
			return listLog.Add(item);
		}
		var newList = _list.plus(item);
		if (newList == _list)
			return false;
		_list = newList;
		return true;
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean remove(Object item) {
		if (isManaged()) {
			var listLog = (LogList1<V>)Transaction.getCurrentVerifyWrite(this).LogGetOrAdd(
					parent().objectId() + variableId(), this::CreateLogBean);
			return listLog.Remove((V)item);
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
			var listLog = (LogList1<V>)Transaction.getCurrentVerifyWrite(this).LogGetOrAdd(
					parent().objectId() + variableId(), this::CreateLogBean);
			listLog.Clear();
		} else
			_list = org.pcollections.Empty.vector();
	}

	@Override
	public V set(int index, V item) {
		if (item == null)
			throw new IllegalArgumentException("null item");

		if (isManaged()) {
			@SuppressWarnings("unchecked")
			var listLog = (LogList1<V>)Transaction.getCurrentVerifyWrite(this).LogGetOrAdd(
					parent().objectId() + variableId(), this::CreateLogBean);
			return listLog.Set(index, item);
		}
		var old = _list.get(index);
		_list = _list.with(index, item);
		return old;
	}

	@Override
	public void add(int index, V item) {
		if (item == null)
			throw new IllegalArgumentException("null item");

		if (isManaged()) {
			@SuppressWarnings("unchecked")
			var listLog = (LogList1<V>)Transaction.getCurrentVerifyWrite(this).LogGetOrAdd(
					parent().objectId() + variableId(), this::CreateLogBean);
			listLog.Add(index, item);
		} else
			_list = _list.plus(index, item);
	}

	@Override
	public V remove(int index) {
		if (isManaged()) {
			@SuppressWarnings("unchecked")
			var listLog = (LogList1<V>)Transaction.getCurrentVerifyWrite(this).LogGetOrAdd(
					parent().objectId() + variableId(), this::CreateLogBean);
			return listLog.Remove(index);
		}
		var old = _list.get(index);
		_list = _list.minus(index);
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
			var listLog = (LogList1<V>)Transaction.getCurrentVerifyWrite(this).LogGetOrAdd(
					parent().objectId() + variableId(), this::CreateLogBean);
			return listLog.AddAll(items);
		}
		_list = _list.plusAll(items);
		return true;
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean removeAll(Collection<?> c) {
		if (isManaged()) {
			var listLog = (LogList1<V>)Transaction.getCurrentVerifyWrite(this).LogGetOrAdd(
					parent().objectId() + variableId(), this::CreateLogBean);
			return listLog.RemoveAll((Collection<V>)c);
		}
		var oldList = _list;
		_list = _list.minusAll(c);
		return oldList != _list;
	}

	@Override
	public LogBean CreateLogBean() {
		var log = new LogList1<>(logTypeId, valueCodecFuncs);
		log.setBelong(parent());
		log.setThis(this);
		log.setVariableId(variableId());
		log.setValue(_list);
		return log;
	}

	@Override
	public void FollowerApply(Log _log) {
		@SuppressWarnings("unchecked")
		var log = (LogList1<V>)_log;
		for (var opLog : log.getOpLogs()) {
			switch (opLog.op) {
			case LogList1.OpLog.OP_MODIFY:
				_list = _list.with(opLog.index, opLog.value);
				break;
			case LogList1.OpLog.OP_ADD:
				_list = _list.plus(opLog.index, opLog.value);
				break;
			case LogList1.OpLog.OP_REMOVE:
				_list = _list.minus(opLog.index);
				break;
			case LogList1.OpLog.OP_CLEAR:
				_list = Empty.vector();
				break;
			}
		}
	}

	@Override
	protected void InitChildrenRootInfo(Record.RootInfo root) {
	}

	@Override
	protected void ResetChildrenRootInfo() {
	}

	@Override
	public PList1<V> CopyBean() {
		var copy = new PList1<>(logTypeId, valueCodecFuncs);
		copy._list = _list;
		return copy;
	}

	@Override
	public void Encode(ByteBuffer bb) {
		var tmp = getList();
		bb.WriteUInt(tmp.size());
		var encoder = valueCodecFuncs.encoder;
		for (var e : tmp)
			encoder.accept(bb, e);
	}

	@Override
	public void Decode(ByteBuffer bb) {
		clear();
		var decoder = valueCodecFuncs.decoder;
		for (int i = bb.ReadUInt(); i > 0; i--)
			add(decoder.apply(bb));
	}
}
