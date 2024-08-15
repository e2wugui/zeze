package Zeze.Raft.RocksRaft;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.SerializeHelper;
import Zeze.Util.Reflect;
import org.pcollections.Empty;

public class CollList1<V> extends CollList<V> {
	protected final SerializeHelper.CodecFuncs<V> valueCodecFuncs;
	private final int logTypeId;

	public CollList1(Class<V> valueClass) {
		valueCodecFuncs = SerializeHelper.createCodec(valueClass);
		logTypeId = Zeze.Transaction.Bean.hash32("Zeze.Raft.RocksRaft.LogList1<" + Reflect.getStableName(valueClass) + '>');
	}

	private CollList1(int logTypeId, SerializeHelper.CodecFuncs<V> valueCodecFuncs) {
		this.valueCodecFuncs = valueCodecFuncs;
		this.logTypeId = logTypeId;
	}

	@Override
	public boolean add(V item) {
		if (isManaged()) {
			@SuppressWarnings("unchecked")
			var listLog = (LogList1<V>)Transaction.getCurrent().logGetOrAdd(
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
			var listLog = (LogList1<V>)Transaction.getCurrent().logGetOrAdd(
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
			var listLog = (LogList1<V>)Transaction.getCurrent().logGetOrAdd(
					parent().objectId() + variableId(), this::createLogBean);
			listLog.clear();
		} else
			list = org.pcollections.Empty.vector();
	}

	@Override
	public V set(int index, V item) {
		if (isManaged()) {
			@SuppressWarnings("unchecked")
			var listLog = (LogList1<V>)Transaction.getCurrent().logGetOrAdd(
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
			@SuppressWarnings("unchecked")
			var listLog = (LogList1<V>)Transaction.getCurrent().logGetOrAdd(
					parent().objectId() + variableId(), this::createLogBean);
			listLog.add(index, item);
		} else
			list = list.plus(index, item);
	}

	@Override
	public V remove(int index) {
		if (isManaged()) {
			@SuppressWarnings("unchecked")
			var listLog = (LogList1<V>)Transaction.getCurrent().logGetOrAdd(
					parent().objectId() + variableId(), this::createLogBean);
			return listLog.remove(index);
		}
		var old = list.get(index);
		list = list.minus(index);
		return old;
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

	@SuppressWarnings("unchecked")
	@Override
	public void leaderApplyNoRecursive(Log _log) {
		list = ((LogList1<V>)_log).getValue();
	}

	@Override
	protected void initChildrenRootInfo(Record.RootInfo root) {
	}

	@Override
	public CollList1<V> copy() {
		var copy = new CollList1<>(logTypeId, valueCodecFuncs);
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
	public void decode(IByteBuffer bb) {
		clear();
		var decoder = valueCodecFuncs.decoder;
		for (int i = bb.ReadUInt(); i > 0; i--)
			add(decoder.apply(bb));
	}
}
