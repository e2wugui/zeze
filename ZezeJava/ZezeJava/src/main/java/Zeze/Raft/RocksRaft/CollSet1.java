package Zeze.Raft.RocksRaft;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.SerializeHelper;
import Zeze.Util.Reflect;

public class CollSet1<V> extends CollSet<V> {
	protected final SerializeHelper.CodecFuncs<V> valueCodecFuncs;
	private final int logTypeId;

	public CollSet1(Class<V> valueClass) {
		valueCodecFuncs = SerializeHelper.createCodec(valueClass);
		logTypeId = Zeze.Transaction.Bean.hash32("Zeze.Raft.RocksRaft.LogSet1<" + Reflect.GetStableName(valueClass) + '>');
	}

	private CollSet1(int logTypeId, SerializeHelper.CodecFuncs<V> valueCodecFuncs) {
		this.valueCodecFuncs = valueCodecFuncs;
		this.logTypeId = logTypeId;
	}

	@Override
	public boolean add(V item) {
		if (isManaged()) {
			@SuppressWarnings("unchecked")
			var setLog = (LogSet1<V>)Transaction.getCurrent().LogGetOrAdd(
					parent().objectId() + variableId(), this::CreateLogBean);
			return setLog.Add(item);
		}
		var newSet = _set.plus(item);
		if (newSet == _set)
			return false;
		_set = newSet;
		return true;
	}

	@Override
	public boolean remove(V item) {
		if (isManaged()) {
			@SuppressWarnings("unchecked")
			var setLog = (LogSet1<V>)Transaction.getCurrent().LogGetOrAdd(
					parent().objectId() + variableId(), this::CreateLogBean);
			return setLog.Remove(item);
		}
		var newSet = _set.minus(item);
		if (newSet == _set)
			return false;
		_set = newSet;
		return true;
	}

	@Override
	public void clear() {
		if (isManaged()) {
			@SuppressWarnings("unchecked")
			var setLog = (LogSet1<V>)Transaction.getCurrent().LogGetOrAdd(
					parent().objectId() + variableId(), this::CreateLogBean);
			setLog.Clear();
		} else
			_set = org.pcollections.Empty.set();
	}

	@Override
	public LogBean CreateLogBean() {
		var log = new LogSet1<>(logTypeId, valueCodecFuncs);
		log.setBelong(parent());
		log.setThis(this);
		log.setVariableId(variableId());
		log.setValue(_set);
		return log;
	}

	@Override
	public void FollowerApply(Log _log) {
		@SuppressWarnings("unchecked")
		var log = (LogSet1<V>)_log;
		_set = _set.plusAll(log.getAdded()).minusAll(log.getRemoved());
	}

	@SuppressWarnings("unchecked")
	@Override
	public void LeaderApplyNoRecursive(Log _log) {
		_set = ((LogSet1<V>)_log).getValue();
	}

	@Override
	protected void InitChildrenRootInfo(Record.RootInfo root) {
	}

	@Override
	public Bean CopyBean() {
		var copy = new CollSet1<>(logTypeId, valueCodecFuncs);
		copy._set = _set;
		return copy;
	}

	@Override
	public void Encode(ByteBuffer bb) {
		var tmp = getSet();
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
