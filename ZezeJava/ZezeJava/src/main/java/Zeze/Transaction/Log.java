package Zeze.Transaction;

import java.util.function.Supplier;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.Serializable;
import Zeze.Util.LongConcurrentHashMap;

/**
 * 操作日志。
 * 主要用于 bean.variable 的修改。
 * 用于其他非 bean 的日志时，也需要构造一个 bean，用来包装日志。
 */
public abstract class Log implements Serializable {
	private static final LongConcurrentHashMap<Supplier<Log>> Factorys = new LongConcurrentHashMap<>();

	public static void Register(Supplier<Log> s) {
		Factorys.put(s.get().getTypeId(), s);
	}

	public static Log Create(int typeId) {
		var factory = Factorys.get(typeId);
		if (factory != null)
			return factory.get();
		throw new UnsupportedOperationException("unknown log typeId=" + typeId);
	}

	private final int _TypeId; // 会被序列化，实际上由LogBean管理。
	private Bean Bean;
	private int VariableId;

	public Log(int typeId) {
		_TypeId = typeId;
	}

	public Log(String typeName) {
		_TypeId = Zeze.Transaction.Bean.Hash32(typeName);
	}

	public int getTypeId() {
		return _TypeId;
	}

	public long getLogKey() {
		return Bean.getObjectId() + getVariableId();
	}

	public final Bean getBean() {
		return Bean;
	}

	public final void setBean(Bean value) {
		Bean = value;
	}

	public final Bean getBelong() {
		return Bean;
	}

	public final void setBelong(Bean value) {
		Bean = value;
	}

	public final int getVariableId() {
		return VariableId;
	}

	public final void setVariableId(int varId) {
		VariableId = varId;
	}

	public void Collect(Changes changes, Bean recent, Log vlog) {
		// LogBean LogCollection 需要实现这个方法收集日志.
	}

	public Log BeginSavepoint() {
		return this;
	}

	public void EndSavepoint(Savepoint currentSp) {
		currentSp.getLogs().put(getLogKey(), this);
	}

	public abstract void Commit();
	// public void Rollback() { } // 一般的操作日志不需要实现，特殊日志可能需要。先不实现，参见Savepoint.

	@Override
	public void Encode(ByteBuffer bb) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void Decode(ByteBuffer bb) {
		throw new UnsupportedOperationException();
	}
}
