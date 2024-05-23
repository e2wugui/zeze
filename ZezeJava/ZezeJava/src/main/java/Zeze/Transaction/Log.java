package Zeze.Transaction;

import java.math.BigDecimal;
import java.util.function.Supplier;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.Quaternion;
import Zeze.Serialize.Serializable;
import Zeze.Serialize.Vector2;
import Zeze.Serialize.Vector2Int;
import Zeze.Serialize.Vector3;
import Zeze.Serialize.Vector3Int;
import Zeze.Serialize.Vector4;
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

	public void collect(@NotNull Changes changes, @NotNull Bean recent, @NotNull Log vlog) {
		// LogBean LogCollection 需要实现这个方法收集日志.
	}

	public @NotNull Log beginSavepoint() {
		return this;
	}

	public void endSavepoint(@NotNull Savepoint currentSp) {
		currentSp.putLog(this);
	}

	public abstract void commit();
	// public void rollback() { } // 一般的操作日志不需要实现，特殊日志可能需要。先不实现，参见Savepoint.

	@Override
	public void encode(@NotNull ByteBuffer bb) {
		throw new UnsupportedOperationException(getTypeName());
	}

	@Override
	public void decode(@NotNull IByteBuffer bb) {
		throw new UnsupportedOperationException(getTypeName());
	}

	public boolean booleanValue() {
		return longValue() != 0;
	}

	public byte byteValue() {
		return (byte)longValue();
	}

	public short shortValue() {
		return (short)longValue();
	}

	public int intValue() {
		return (int)longValue();
	}

	public long longValue() {
		throw new UnsupportedOperationException(getTypeName());
	}

	public float floatValue() {
		return (float)doubleValue();
	}

	public double doubleValue() {
		throw new UnsupportedOperationException(getTypeName());
	}

	public @NotNull Binary binaryValue() {
		throw new UnsupportedOperationException(getTypeName());
	}

	public @NotNull String stringValue() {
		throw new UnsupportedOperationException(getTypeName());
	}

	public @NotNull BigDecimal decimalValue() {
		throw new UnsupportedOperationException(getTypeName());
	}

	public @NotNull Vector2 vector2Value() {
		return new Vector2(floatValue(), 0);
	}

	public @NotNull Vector2Int vector2IntValue() {
		return new Vector2Int(intValue(), 0);
	}

	public @NotNull Vector3 vector3Value() {
		return new Vector3(floatValue(), 0, 0);
	}

	public @NotNull Vector3Int vector3IntValue() {
		return new Vector3Int(intValue(), 0, 0);
	}

	public @NotNull Vector4 vector4Value() {
		return new Vector4(floatValue(), 0, 0, 0);
	}

	public @NotNull Quaternion quaternionValue() {
		return new Quaternion(floatValue(), 0, 0, 0);
	}
}
