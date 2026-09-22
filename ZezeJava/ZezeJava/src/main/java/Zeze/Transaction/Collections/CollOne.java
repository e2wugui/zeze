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

/** 单值 Bean 容器：普通 bean 字段的宿主，受管后修改经 LogOne 记账。 */
@SuppressWarnings({"unchecked", "DataFlowIssue"})
public final class CollOne<V extends Bean> extends Collection {
	@NotNull V value;
	// FND7-80：按声明类预建的meta（生成端meta1_静态字段，手写Class构造器现建）；
	// null时LogOne退回运行时类。读端工厂按声明类注册（Helper.dependsBean→registerLogOne）。
	final @Nullable LogOneMeta<V> meta;

	public CollOne(@NotNull V value, @Nullable Class<V> valueClass) {
		this(value, valueClass != null ? LogOneMeta.get(valueClass) : null);
	}

	public CollOne(@NotNull V value, @Nullable LogOneMeta<V> meta) {
		this.value = value;
		this.meta = meta;
	}

	public @NotNull V getValue() {
		if (!isManaged())
			return value;

		var txn = Transaction.getCurrent();
		if (txn == null)
			return value;

		var log = (LogOne<V>)txn.getLog(parent().objectId() + variableId());
		return log != null ? log.value : value;
	}

	public void setValue(@NotNull V value) {
		//noinspection ConstantValue
		if (value == null)
			throw new NullPointerException("value");

		if (isManaged()) {
			value.initRootInfoWithRedo(rootInfo, this);
			var log = (LogOne<V>)Transaction.getCurrentVerifyWrite(this)
					.logGetOrAdd(parent().objectId() + variableId(), this::createLogBean);
			log.setValue(value);
		} else
			this.value = value;
	}

	public void assign(@NotNull CollOne<V> other) {
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
	public @NotNull LogBean createLogBean() {
		return new LogOne<>(parent(), variableId(), this, getValue());
	}

	@Override
	public void decode(@NotNull IByteBuffer bb) {
		getValue().decode(bb);
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
		var log = (LogOne<V>)_log;
		if (log.value != null) { // value是否真的可以为null,目前没看到哪里可以让它为null
			log.value.initRootInfo(rootInfo, this); // 与PList2/PMap2等全部同类实现保持一致
			value = log.value;
		} else if (log.logBean != null)
			value.followerApply(log.logBean);
	}

	@Override
	public @NotNull CollOne<V> copy() {
		return new CollOne<>((V)getValue().copy(), meta); // 声明类型meta随副本传递
	}

	@Override
	public @NotNull String toString() {
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
}
