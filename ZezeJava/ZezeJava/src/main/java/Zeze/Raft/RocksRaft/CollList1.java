package Zeze.Raft.RocksRaft;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.SerializeHelper;
import Zeze.Util.Reflect;
import org.pcollections.Empty;

public class CollList1<V> extends CollList<V> {
	protected final SerializeHelper.CodecFuncs<V> valueCodecFuncs;
	private final int logTypeId;

	public CollList1(Class<V> valueClass) {
		valueCodecFuncs = SerializeHelper.createCodec(valueClass);
		logTypeId = Zeze.Transaction.Bean.hash32("Zeze.Raft.RocksRaft.LogList1<" + Reflect.GetStableName(valueClass) + '>');
	}

	private CollList1(int logTypeId, SerializeHelper.CodecFuncs<V> valueCodecFuncs) {
		this.valueCodecFuncs = valueCodecFuncs;
		this.logTypeId = logTypeId;
	}

	@Override
	public boolean add(V item) {
		if (isManaged()) {
			@SuppressWarnings("unchecked")
			var listLog = (LogList1<V>)Transaction.getCurrent().LogGetOrAdd(
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
			var listLog = (LogList1<V>)Transaction.getCurrent().LogGetOrAdd(
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
			var listLog = (LogList1<V>)Transaction.getCurrent().LogGetOrAdd(
					parent().objectId() + variableId(), this::createLogBean);
			listLog.Clear();
		} else
			_list = org.pcollections.Empty.vector();
	}

	@Override
	public V set(int index, V item) {
		if (isManaged()) {
			@SuppressWarnings("unchecked")
			var listLog = (LogList1<V>)Transaction.getCurrent().LogGetOrAdd(
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
			@SuppressWarnings("unchecked")
			var listLog = (LogList1<V>)Transaction.getCurrent().LogGetOrAdd(
					parent().objectId() + variableId(), this::createLogBean);
			listLog.Add(index, item);
		} else
			_list = _list.plus(index, item);
	}

	@Override
	public V remove(int index) {
		if (isManaged()) {
			@SuppressWarnings("unchecked")
			var listLog = (LogList1<V>)Transaction.getCurrent().LogGetOrAdd(
					parent().objectId() + variableId(), this::createLogBean);
			return listLog.Remove(index);
		}
		var old = _list.get(index);
		_list = _list.minus(index);
		return old;
	}

	@Override
	public LogBean createLogBean() {
		var log = new LogList1<>(logTypeId, valueCodecFuncs);
		log.setBelong(parent());
		log.setThis(this);
		log.setVariableId(variableId());
		log.setValue(_list);
		return log;
	}

	@Override
	public void followerApply(Log _log) {
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

	@SuppressWarnings("unchecked")
	@Override
	public void leaderApplyNoRecursive(Log _log) {
		_list = ((LogList1<V>)_log).getValue();
	}

	@Override
	protected void initChildrenRootInfo(Record.RootInfo root) {
	}

	@Override
	public Bean copyBean() {
		var copy = new CollList1<>(logTypeId, valueCodecFuncs);
		copy._list = _list;
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
