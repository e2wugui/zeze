package Zeze.Transaction.Logs;

import java.util.Objects;

import java.lang.invoke.VarHandle;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.Quaternion;
import Zeze.Serialize.Vector3Int;
import Zeze.Serialize.Vector4;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Log;
import org.jspecify.annotations.Nullable;

/** vector4 字段原子修改日志：携带新值，commit 经 VarHandle 写回，序列化为四个 float。 */
public class LogVector4 extends Log {
	private static final int TYPE_ID = Bean.hash32("Zeze.Transaction.Log<vector4>");

	private final @Nullable VarHandle vh;
	public @Nullable Vector4 value;

	public LogVector4(@Nullable Bean belong, int varId, @Nullable VarHandle vh, Vector4 value) {
		super(belong, varId);
		this.vh = vh;
		this.value = value;
	}

	public LogVector4(int varId) {
		super(null, varId);
		vh = null;
	}

	@Override
	public Category category() {
		return Category.eHistory;
	}

	@Override
	public int getTypeId() {
		return TYPE_ID;
	}

	@Override
	public void commit() {
		//noinspection DataFlowIssue
		vh.set(getBelong(), value);
	}

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteVector4(Objects.requireNonNull(value));
	}

	@Override
	public void decode(IByteBuffer bb) {
		value = bb.ReadVector4();
	}

	@Override
	public String toString() {
		return String.valueOf(value);
	}

	@Override
	public Vector4 vector2Value() {
		return Objects.requireNonNull(value);
	}

	@Override
	public Vector3Int vector2IntValue() {
		return new Vector3Int(Objects.requireNonNull(value));
	}

	@Override
	public Vector4 vector3Value() {
		return Objects.requireNonNull(value);
	}

	@Override
	public Vector3Int vector3IntValue() {
		return new Vector3Int(Objects.requireNonNull(value));
	}

	@Override
	public Vector4 vector4Value() {
		return Objects.requireNonNull(value);
	}

	@Override
	public Quaternion quaternionValue() {
		return new Quaternion(Objects.requireNonNull(value));
	}
}
