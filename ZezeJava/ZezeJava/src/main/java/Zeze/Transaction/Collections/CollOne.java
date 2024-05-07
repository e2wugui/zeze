package Zeze.Transaction.Collections;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.SQLStatement;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Log;
import Zeze.Transaction.Record;
import Zeze.Transaction.Transaction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class CollOne<V extends Bean> extends Collection {
	@NotNull V _Value;

	public CollOne(@NotNull V init, @Nullable Class<V> valueClass) {
		_Value = init;
	}

	public @NotNull V getValue() {
		if (!isManaged())
			return _Value;

		var txn = Transaction.getCurrent();
		if (null == txn)
			return _Value;

		//noinspection DataFlowIssue
		@SuppressWarnings("unchecked")
		var log = (LogOne<V>)txn.getLog(parent().objectId() + variableId());
		if (null == log)
			return _Value;

		return log.value;
	}

	public void setValue(@NotNull V value) {
		//noinspection ConstantValue
		if (null == value)
			throw new NullPointerException("value");

		if (isManaged()) {
			//noinspection DataFlowIssue
			value.initRootInfoWithRedo(rootInfo, this);
			//noinspection DataFlowIssue
			@SuppressWarnings("unchecked")
			var log = (LogOne<V>)Transaction.getCurrentVerifyWrite(this)
					.logGetOrAdd(parent().objectId() + variableId(), this::createLogBean);
			log.setValue(value);
		} else {
			_Value = value;
		}
	}

	@SuppressWarnings("unchecked")
	public void assign(@NotNull CollOne<V> other) {
		setValue((V)other.getValue().copy());
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean equals(@Nullable Object obj) {
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
	public @NotNull LogBean createLogBean() {
		var log = new LogOne<>(getValue());
		log.setBelong(parent());
		log.setThis(this);
		log.setVariableId(variableId());
		return log;
	}

	@Override
	public void decode(@NotNull IByteBuffer bb) {
		var Value = getValue();
		Value.decode(bb);
	}

	@Override
	public void encode(@NotNull ByteBuffer bb) {
		getValue().encode(bb);
	}

	@Override
	protected void initChildrenRootInfo(@NotNull Record.RootInfo root) {
		getValue().initRootInfo(root, this);
	}

	@Override
	protected void initChildrenRootInfoWithRedo(@NotNull Record.RootInfo root) {
		getValue().initRootInfoWithRedo(root, this);
	}

	@Override
	public void followerApply(@NotNull Log _log) {
		@SuppressWarnings("unchecked")
		var log = (LogOne<V>)_log;
		if (null != log.value) { // value是否真的可以为null,目前没看到哪里可以让它为null
			_Value = log.value;
		} else if (null != log.logBean) {
			_Value.followerApply(log.logBean);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public @NotNull CollOne<V> copy() {
		return new CollOne<>((V)getValue().copy(), null);
	}

	@Override
	public @NotNull String toString() {
		return getValue().toString();
	}

	@Override
	public void encodeSQLStatement(ArrayList<String> parents, SQLStatement st) {
		_Value.encodeSQLStatement(parents, st);
	}

	@Override
	public void decodeResultSet(ArrayList<String> parents, ResultSet rs) throws SQLException {
		_Value.decodeResultSet(parents, rs);
	}
}
