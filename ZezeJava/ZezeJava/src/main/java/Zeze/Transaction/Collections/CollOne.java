package Zeze.Transaction.Collections;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Log;
import Zeze.Transaction.Record;
import Zeze.Transaction.Transaction;

public class CollOne<V extends Bean> extends Collection {
	V _Value;

	public CollOne(V init, Class<V> valueClass) {
		_Value = init;
	}

	public V getValue() {
		if (!isManaged())
			return _Value;

		var txn = Transaction.getCurrent();
		if (null == txn)
			return _Value;

		@SuppressWarnings("unchecked")
		var log = (LogOne<V>)txn.getLog(parent().objectId() + variableId());
		if (null == log)
			return _Value;

		return log.value;
	}

	public void setValue(V value) {
		if (null == value)
			throw new IllegalArgumentException("value");

		if (isManaged()) {
			value.initRootInfoWithRedo(rootInfo, this);
			@SuppressWarnings("unchecked")
			var log = (LogOne<V>)Transaction.getCurrentVerifyWrite(this)
					.logGetOrAdd(parent().objectId() + variableId(), this::createLogBean);
			log.setValue(value);
		} else {
			_Value = value;
		}
	}

	@SuppressWarnings("unchecked")
	public void assign(CollOne<V> other) {
		setValue((V)other.getValue().copy());
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (obj instanceof CollOne)
			return getValue().equals(((CollOne<V>)obj).getValue());
		return false;
	}

	@Override
	public int hashCode() {
		return getValue().hashCode();
	}

	@Override
	public LogBean createLogBean() {
		var log = new LogOne<V>();
		log.setBelong(parent());
		log.setThis(this);
		log.setVariableId(variableId());
		log.setValue(getValue());
		return log;
	}

	@Override
	public void decode(ByteBuffer bb) {
		var Value = getValue();
		Value.decode(bb);
	}

	@Override
	public void encode(ByteBuffer bb) {
		getValue().encode(bb);
	}

	@Override
	protected void initChildrenRootInfo(Record.RootInfo root) {
		getValue().initRootInfo(root, this);
	}

	@Override
	public void followerApply(Log _log) {
		@SuppressWarnings("unchecked")
		var log = (LogOne<V>)_log;
		if (null != log.value) {
			_Value = log.value;
		} else if (null != log.logBean) {
			_Value.followerApply(log.logBean);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public CollOne<V> copy() {
		return new CollOne<>((V)getValue().copy(), null);
	}

	@Override
	public String toString() {
		return getValue().toString();
	}
}
