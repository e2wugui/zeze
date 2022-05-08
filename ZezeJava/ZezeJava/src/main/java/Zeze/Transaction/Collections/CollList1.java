package Zeze.Transaction.Collections;

import java.util.Collection;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Log;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.SerializeHelper;
import Zeze.Transaction.Record;
import Zeze.Transaction.Transaction;
import Zeze.Util.Reflect;
import org.pcollections.Empty;

public class CollList1<V> extends CollList<V> {
	protected final SerializeHelper.CodecFuncs<V> valueCodecFuncs;
	private final int logTypeId;

	public CollList1(Class<V> valueClass) {
		valueCodecFuncs = SerializeHelper.createCodec(valueClass);
		logTypeId = Zeze.Transaction.Bean.Hash32("Zeze.Transaction.LogList1<" + Reflect.GetStableName(valueClass) + '>');
	}

	private CollList1(int logTypeId, SerializeHelper.CodecFuncs<V> valueCodecFuncs) {
		this.valueCodecFuncs = valueCodecFuncs;
		this.logTypeId = logTypeId;
	}

	@Override
	public boolean add(V item) {
		if (item == null) {
			throw new NullPointerException();
		}

		if (isManaged()) {
			var txn = Transaction.getCurrent();
			assert txn != null;
			txn.VerifyRecordAccessed(this);
			@SuppressWarnings("unchecked")
			var listLog = (LogList1<V>)txn.LogGetOrAdd(
					getParent().getObjectId() + getVariableId(), this::CreateLogBean);
			return listLog.Add(item);
		}
		var newList = _list.plus(item);
		if (newList == _list)
			return false;
		_list = newList;
		return true;
	}

	@Override
	public boolean remove(Object item) {
		if (isManaged()) {
			var txn = Transaction.getCurrent();
			assert txn != null;
			txn.VerifyRecordAccessed(this);
			@SuppressWarnings("unchecked")
			var listLog = (LogList1<V>)txn.LogGetOrAdd(
					getParent().getObjectId() + getVariableId(), this::CreateLogBean);
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
			var txn = Transaction.getCurrent();
			assert txn != null;
			txn.VerifyRecordAccessed(this);
			@SuppressWarnings("unchecked")
			var listLog = (LogList1<V>)txn.LogGetOrAdd(
					getParent().getObjectId() + getVariableId(), this::CreateLogBean);
			listLog.Clear();
		} else
			_list = org.pcollections.Empty.vector();
	}

	@Override
	public V set(int index, V item) {
		if (item == null) {
			throw new NullPointerException();
		}

		if (isManaged()) {
			var txn = Transaction.getCurrent();
			assert txn != null;
			txn.VerifyRecordAccessed(this);
			@SuppressWarnings("unchecked")
			var listLog = (LogList1<V>)txn.LogGetOrAdd(
					getParent().getObjectId() + getVariableId(), this::CreateLogBean);
			return listLog.Set(index, item);
		}
		var old = _list.get(index);
		_list = _list.with(index, item);
		return old;
	}

	@Override
	public void add(int index, V item) {
		if (item == null) {
			throw new NullPointerException();
		}

		if (isManaged()) {
			var txn = Transaction.getCurrent();
			assert txn != null;
			txn.VerifyRecordAccessed(this);
			@SuppressWarnings("unchecked")
			var listLog = (LogList1<V>)txn.LogGetOrAdd(
					getParent().getObjectId() + getVariableId(), this::CreateLogBean);
			listLog.Add(index, item);
		} else
			_list = _list.plus(index, item);
	}

	@Override
	public V remove(int index) {
		if (isManaged()) {
			var txn = Transaction.getCurrent();
			assert txn != null;
			txn.VerifyRecordAccessed(this);
			@SuppressWarnings("unchecked")
			var listLog = (LogList1<V>)txn.LogGetOrAdd(
					getParent().getObjectId() + getVariableId(), this::CreateLogBean);
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
			if (null == v) {
				throw new NullPointerException();
			}
		}

		if (this.isManaged()) {
			var txn = Transaction.getCurrent();
			assert txn != null;
			txn.VerifyRecordAccessed(this);
			@SuppressWarnings("unchecked")
			var listLog = (LogList1<V>)txn.LogGetOrAdd(
					getParent().getObjectId() + getVariableId(), this::CreateLogBean);
			return listLog.AddAll(items);
		}
		else {
			_list = _list.plusAll(items);
		}
		return true;
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		if (this.isManaged()) {
			var txn = Transaction.getCurrent();
			assert txn != null;
			txn.VerifyRecordAccessed(this);
			@SuppressWarnings("unchecked")
			var listLog = (LogList1<V>)txn.LogGetOrAdd(
					getParent().getObjectId() + getVariableId(), this::CreateLogBean);
			return listLog.RemoveAll(c);
		}
		else {
			var oldList = _list;
			_list = _list.minusAll(c);
			return oldList != _list;
		}
	}

	@Override
	public LogBean CreateLogBean() {
		var log = new LogList1<>(logTypeId, valueCodecFuncs);
		log.setBelong(getParent());
		log.setThis(this);
		log.setVariableId(getVariableId());
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
	public Bean CopyBean() {
		var copy = new CollList1<>(logTypeId, valueCodecFuncs);
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
