package Zeze.Transaction.Logs;

import java.util.Objects;

import java.lang.invoke.VarHandle;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.Quaternion;
import Zeze.Serialize.Vector3Int;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Log;
import org.jspecify.annotations.Nullable;

/** quaternion 字段原子修改日志：携带新值，commit 经 VarHandle 写回，序列化为四个 float 分量。 */
public class LogQuaternion extends Log {
	private static final int TYPE_ID = Bean.hash32("Zeze.Transaction.Log<quaternion>");

	private final @Nullable VarHandle vh;
	public @Nullable Quaternion value;

	public LogQuaternion(@Nullable Bean belong, int varId, @Nullable VarHandle vh, Quaternion value) {
		super(belong, varId);
		this.vh = vh;
		this.value = value;
	}

	public LogQuaternion(int varId) {
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
		bb.WriteQuaternion(Objects.requireNonNull(value));
	}

	@Override
	public void decode(IByteBuffer bb) {
		value = bb.ReadQuaternion();
	}

	@Override
	public String toString() {
		return String.valueOf(value);
	}

	@Override
	public Quaternion vector2Value() {
		return Objects.requireNonNull(value);
	}

	@Override
	public Vector3Int vector2IntValue() {
		return new Vector3Int(Objects.requireNonNull(value));
	}

	@Override
	public Quaternion vector3Value() {
		return Objects.requireNonNull(value);
	}

	@Override
	public Vector3Int vector3IntValue() {
		return new Vector3Int(Objects.requireNonNull(value));
	}

	@Override
	public Quaternion vector4Value() {
		return Objects.requireNonNull(value);
	}

	@Override
	public Quaternion quaternionValue() {
		return Objects.requireNonNull(value);
	}
}
