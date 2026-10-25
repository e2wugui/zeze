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
import Zeze.Transaction.TransactionLog;
import Zeze.Transaction.ChangeCaptureMode;
import org.jspecify.annotations.Nullable;

/** 单值 Bean 容器：普通 bean 字段的宿主，受管后修改经 LogOne 记账。 */
@SuppressWarnings({"unchecked", "DataFlowIssue"})
public final class CollOne<V extends Bean> extends Collection {
	V value;
	// 按声明类预建的meta，与读端日志工厂注册对称。
	final LogOneMeta<V> meta;

	public CollOne(V value, LogOneMeta<V> meta) {
		this.value = value;
		this.meta = meta;
	}

	public V getValue() {
		if (!isManaged())
			return value;

		var txn = Transaction.getCurrent();
		if (txn == null)
			return value;

		var log = (LogOne<V>)txn.getLog(parent().objectId() + variableId());
		return log != null ? log.value : value;
	}

	public void setValue(V value) {
		//noinspection ConstantValue
		if (value == null)
			throw new NullPointerException("value");

		if (isManaged()) {
			// 先取写权限后挂接（理由同PList2.add）：拒绝路径不得污染输入bean归属。
			var log = (LogOne<V>)Transaction.getCurrentVerifyWrite(this)
				.logGetOrAdd(parent().objectId() + variableId(), () -> createLog(ChangeCaptureMode.DELTA));
			value.initRootInfoWithRedo(rootInfo, this);
			log.setValue(value);
		} else
			this.value = value;
	}

	public void assign(CollOne<V> other) {
		setValue((V)other.getValue().copy());
	}

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
	public LogBean createLogBean(@Nullable TransactionLog finalLog) {
		return finalLog != null ? (LogOne<?>)finalLog : new LogOne<>(parent(), variableId(), this, value);
	}

	@Override
	public void decode(IByteBuffer bb) {
		getValue().decode(bb);
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
	protected void initChildrenRootInfoWithRedo(Record.RootInfo root) {
		getValue().initRootInfoWithRedo(root, this);
	}

	@Override
	public void followerApply(Log _log) {
		var log = (LogOneValue<V>)_log;
		if (log.value != null) {
			log.value.initRootInfo(rootInfo, this); // 与PList2/PMap2等全部同类实现保持一致
			value = log.value;
		} else if (log.logBean != null)
			value.followerApply(log.logBean);
	}

	@Override
	public CollOne<V> copy() {
		return new CollOne<>((V)getValue().copy(), meta); // 声明类型meta随副本传递
	}

	@Override
	public String toString() {
		return getValue().toString();
	}

	@Override
	public void encodeSQLStatement(ArrayList<String> parents, SQLStatement st) {
		value.encodeSQLStatement(parents, st);
	}

	@Override
	public void decodeResultSet(ArrayList<String> parents, ResultSet rs) throws SQLException {
		value.decodeResultSet(parents, rs);
	}

	@Override
	public TransactionLog createLog(ChangeCaptureMode mode) {
		return new LogOne<>(parent(), variableId(), this, value);
	}
}
