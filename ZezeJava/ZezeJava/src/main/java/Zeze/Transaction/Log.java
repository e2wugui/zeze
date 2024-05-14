package Zeze.Transaction;

import java.util.function.Supplier;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.Serializable;
import Zeze.Transaction.Collections.LogBean;
import Zeze.Util.LongConcurrentHashMap;
import org.jetbrains.annotations.NotNull;

/**
 * 操作日志。
 * 主要用于 bean.variable 的修改。
 * 用于其他非 bean 的日志时，也需要构造一个 bean，用来包装日志。
 */
public abstract class Log implements Serializable {
	public enum Category {
		eHistory, // 只有这个类被会被收集到增量日志里面
		eUser, // 用户自定义
		eSpecial, // zeze内部特殊定义的log，也可能是用户自定义的。
	}

	private static final LongConcurrentHashMap<Supplier<Log>> factorys = new LongConcurrentHashMap<>();

	public static void register(@NotNull Supplier<Log> s) {
		var ins = s.get();
		var old = factorys.putIfAbsent(ins.getTypeId(), s);
		if (old == null)
			LogBean.logger.debug("register log typeId({}): {}", ins.getTypeId(), ins.getTypeName());
		else {
			var oldIns = old.get();
			if (!oldIns.getTypeName().equals(ins.getTypeName())) {
				LogBean.logger.error("register duplicated log typeId({}): {} & {}",
						ins.getTypeId(), oldIns.getTypeName(), ins.getTypeName());
			}
		}
	}

	public static @NotNull Log create(int typeId) {
		var factory = factorys.get(typeId);
		if (factory != null)
			return factory.get();
		throw new UnsupportedOperationException("unknown log typeId=" + typeId);
	}

	private Bean bean;
	private int variableId;

	public abstract @NotNull Category category();

	public abstract int getTypeId();

	public @NotNull String getTypeName() {
		return getClass().getSimpleName();
	}

	public long getLogKey() {
		return bean.objectId() + getVariableId();
	}

	public final Bean getBean() {
		return bean;
	}

	public final void setBean(Bean value) {
		bean = value;
	}

	public final Bean getBelong() {
		return bean;
	}

	public final void setBelong(Bean value) {
		bean = value;
	}

	public final int getVariableId() {
		return variableId;
	}

	public final void setVariableId(int varId) {
		variableId = varId;
	}

	public void collect(Changes changes, Bean recent, Log vlog) {
		// LogBean LogCollection 需要实现这个方法收集日志.
	}

	public Log beginSavepoint() {
		return this;
	}

	public void endSavepoint(Savepoint currentSp) {
		currentSp.putLog(this);
	}

	public abstract void commit();
	// public void rollback() { } // 一般的操作日志不需要实现，特殊日志可能需要。先不实现，参见Savepoint.

	@Override
	public void encode(ByteBuffer bb) {
		throw new UnsupportedOperationException(getTypeName());
	}

	@Override
	public void decode(IByteBuffer bb) {
		throw new UnsupportedOperationException(getTypeName());
	}
}
