package Zeze.Transaction;

import java.util.Objects;

import java.math.BigDecimal;
import java.util.function.IntFunction;
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
import Zeze.Util.LongConcurrentHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

/**
 * 操作日志。
 * 主要用于 bean.variable 的修改。
 * 用于其他非 bean 的日志时，也需要构造一个 bean，用来包装日志。
 */
public abstract class Log implements Serializable {
	private static final Logger logger = LogManager.getLogger(Log.class);

	public enum Category {
		eHistory, // 只有这个类被会被收集到增量日志里面
		eUser, // 用户自定义
		eSpecial, // zeze内部特殊定义的log，也可能是用户自定义的。
	}

	private static final LongConcurrentHashMap<IntFunction<Log>> factories = new LongConcurrentHashMap<>();

	public static void register(IntFunction<Log> s) {
		var ins = s.apply(0);
		var old = factories.putIfAbsent(ins.getTypeId(), s);
		if (old == null) {
			logger.debug("register log typeId({}): {}", ins.getTypeId(), ins.getTypeName());
		} else {
			var oldIns = old.apply(0);
			if (!oldIns.getTypeName().equals(ins.getTypeName())) {
				logger.error("register duplicated log typeId({}): {} & {}",
						ins.getTypeId(), oldIns.getTypeName(), ins.getTypeName());
			} else {
				// 同名重复注册（典型：Map2Meta.get与create对同一
				// (key,value)类算出同typeId，或同构工厂重复注册）：typeId先到先得，
				// 后注册者的差异（如自定义valueCtor）被静默忽略，debug留痕供排查。
				logger.debug("register duplicated log typeId({}) with same name: {}, first registered wins",
						ins.getTypeId(), ins.getTypeName());
			}
		}
	}

	public static Log create(int typeId, int varId) {
		var factory = factories.get(typeId);
		if (factory != null)
			return factory.apply(varId);
		throw new UnsupportedOperationException("unknown log typeId=" + typeId);
	}

	/** typeId 当前占用的工厂（未占用为 null）。dynamic 家族跨批次决胜
	 * （Zeze.History.Helper）用它确认槽位归属后做定向替换。 */
	public static @Nullable IntFunction<Log> getRegistered(int typeId) {
		return factories.get(typeId);
	}

	/** 条件替换（按对象身份）：仅当 typeId 当前占用者为 expect 时替换为 update。
	 * 供 Helper 跨批次字典序决胜在确认槽位确为自己先前登记的 dynamic 家族工厂后
	 * 定向接管——不会误伤其他子系统的注册（工厂闭包无 equals 语义，身份即相等）。 */
	public static boolean replaceRegistered(int typeId, IntFunction<Log> expect,
											IntFunction<Log> update) {
		return factories.replace(typeId, expect, update);
	}

	private final @Nullable Bean belong;
	private final int varId;

	protected Log(@Nullable Bean belong, int varId) {
		this.belong = belong;
		this.varId = varId;
	}

	public final @Nullable Bean getBelong() {
		return belong;
	}

	public final int getVariableId() {
		return varId;
	}

	public abstract Category category();

	public abstract int getTypeId();

	public String getTypeName() {
		return getClass().getSimpleName();
	}

	public long getLogKey() {
		return Objects.requireNonNull(belong).objectId() + getVariableId();
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

	@Override
	public void encode(ByteBuffer bb) {
		throw new UnsupportedOperationException(getTypeName());
	}

	@Override
	public void decode(IByteBuffer bb) {
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

	public Binary binaryValue() {
		throw new UnsupportedOperationException(getTypeName());
	}

	public String stringValue() {
		throw new UnsupportedOperationException(getTypeName());
	}

	public BigDecimal decimalValue() {
		throw new UnsupportedOperationException(getTypeName());
	}

	public Vector2 vector2Value() {
		return new Vector2(floatValue(), 0);
	}

	public Vector2Int vector2IntValue() {
		return new Vector2Int(intValue(), 0);
	}

	public Vector3 vector3Value() {
		return new Vector3(floatValue(), 0, 0);
	}

	public Vector3Int vector3IntValue() {
		return new Vector3Int(intValue(), 0, 0);
	}

	public Vector4 vector4Value() {
		return new Vector4(floatValue(), 0, 0, 0);
	}

	public Quaternion quaternionValue() {
		return new Quaternion(floatValue(), 0, 0, 0);
	}
}
