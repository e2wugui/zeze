package Zeze.Transaction.Logs;

import java.util.Objects;

import java.lang.invoke.VarHandle;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.Quaternion;
import Zeze.Serialize.Vector2;
import Zeze.Serialize.Vector2Int;
import Zeze.Serialize.Vector3;
import Zeze.Serialize.Vector3Int;
import Zeze.Serialize.Vector4;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Log;
import org.jspecify.annotations.Nullable;

/** vector2 字段原子修改日志：携带新值，commit 经 VarHandle 写回，序列化为两个 float。 */
public class LogVector2 extends Log {
	private static final int TYPE_ID = Bean.hash32("Zeze.Transaction.Log<vector2>");

	private final @Nullable VarHandle vh;
	public @Nullable Vector2 value;

	public LogVector2(@Nullable Bean belong, int varId, @Nullable VarHandle vh, Vector2 value) {
		super(belong, varId);
		this.vh = vh;
		this.value = value;
	}

	public LogVector2(int varId) {
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
		bb.WriteVector2(Objects.requireNonNull(value));
	}

	@Override
	public void decode(IByteBuffer bb) {
		value = bb.ReadVector2();
	}

	@Override
	public String toString() {
		return String.valueOf(value);
	}

	@Override
	public Vector2 vector2Value() {
		return Objects.requireNonNull(value);
	}

	@Override
	public Vector2Int vector2IntValue() {
		return new Vector2Int(Objects.requireNonNull(value));
	}

	@Override
	public Vector3 vector3Value() {
		return new Vector3(Objects.requireNonNull(value));
	}

	@Override
	public Vector3Int vector3IntValue() {
		return new Vector3Int(Objects.requireNonNull(value));
	}

	@Override
	public Vector4 vector4Value() {
		return new Vector4(Objects.requireNonNull(value));
	}

	@Override
	public Quaternion quaternionValue() {
		return new Quaternion(Objects.requireNonNull(value));
	}
}
