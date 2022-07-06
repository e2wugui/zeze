package Zeze.Raft.RocksRaft;

import java.util.function.Supplier;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.Serializable;
import Zeze.Util.LongConcurrentHashMap;

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

	// 事务运行时属性，不会被序列化。
	// 当 Decode，Bean为null。
	// Apply通过参数得到日志应用需要的Bean。
	private Bean Belong;
	private final int _TypeId; // 会被序列化，实际上由LogBean管理。
	private int VariableId;

	public Log(int typeId) {
		_TypeId = typeId;
	}

	public Log(String typeName) {
		_TypeId = Zeze.Transaction.Bean.Hash32(typeName);
	}

	public final Bean getBelong() {
		return Belong;
	}

	public final void setBelong(Bean value) {
		Belong = value;
	}

	public final long getLogKey() {
		return Belong.getObjectId() + getVariableId();
	}

	public void Collect(Changes changes, Bean recent, Log vlog) {
		// LogBean LogCollection 需要实现这个方法收集日志.
	}

	public void EndSavepoint(Savepoint currentSp) {
		currentSp.PutLog(this);
	}

	public Log BeginSavepoint() {
		return this;
	}

	public int getTypeId() {
		return _TypeId;
	}

	public final int getVariableId() {
		return VariableId;
	}

	public final void setVariableId(int value) {
		VariableId = value;
	}

	@Override
	public abstract void Encode(ByteBuffer bb);

	@Override
	public abstract void Decode(ByteBuffer bb);
}
