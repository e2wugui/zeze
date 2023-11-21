package Zeze.Raft.RocksRaft;

import java.util.function.Supplier;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.Serializable;
import Zeze.Util.LongConcurrentHashMap;

public abstract class Log implements Serializable {
	private static final LongConcurrentHashMap<Supplier<Log>> factorys = new LongConcurrentHashMap<>();

	public static void register(Supplier<Log> s) {
		factorys.put(s.get().getTypeId(), s);
	}

	public static Log create(int typeId) {
		var factory = factorys.get(typeId);
		if (factory != null)
			return factory.get();
		throw new UnsupportedOperationException("unknown log typeId=" + typeId);
	}

	// 事务运行时属性，不会被序列化。
	// 当 decode，Bean为null。
	// Apply通过参数得到日志应用需要的Bean。
	private Bean belong;
	private final int typeId; // 会被序列化，实际上由LogBean管理。
	private int variableId;

	public Log(int typeId) {
		this.typeId = typeId;
	}

	public final Bean getBelong() {
		return belong;
	}

	public final void setBelong(Bean value) {
		belong = value;
	}

	public final long getLogKey() {
		return belong.objectId() + variableId;
	}

	public void collect(Changes changes, Bean recent, Log vlog) {
		// LogBean LogCollection 需要实现这个方法收集日志.
	}

	public void endSavepoint(Savepoint currentSp) {
		currentSp.putLog(this);
	}

	public Log beginSavepoint() {
		return this;
	}

	public int getTypeId() {
		return typeId;
	}

	public final int getVariableId() {
		return variableId;
	}

	public final void setVariableId(int value) {
		variableId = value;
	}

	@Override
	public abstract void encode(ByteBuffer bb);

	@Override
	public abstract void decode(IByteBuffer bb);
}
