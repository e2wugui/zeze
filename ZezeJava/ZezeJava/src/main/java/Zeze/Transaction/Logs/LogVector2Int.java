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

/** vector2int 字段原子修改日志：携带新值，commit 经 VarHandle 写回，序列化为两个 int。 */
public class LogVector2Int extends Log {
	private static final int TYPE_ID = Bean.hash32("Zeze.Transaction.Log<vector2int>");

	private final @Nullable VarHandle vh;
	public @Nullable Vector2Int value;

	public LogVector2Int(@Nullable Bean belong, int varId, @Nullable VarHandle vh, Vector2Int value) {
		super(belong, varId);
		this.vh = vh;
		this.value = value;
	}

	public LogVector2Int(int varId) {
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
		//noinspection DataFlowIssue
		bb.WriteVector2Int(value);
	}

	@Override
	public void decode(IByteBuffer bb) {
		value = bb.ReadVector2Int();
	}

	@Override
	public String toString() {
		return String.valueOf(value);
	}

	@Override
	public Vector2 vector2Value() {
		//noinspection DataFlowIssue
		return new Vector2(value);
	}

	@Override
	public Vector2Int vector2IntValue() {
		return Objects.requireNonNull(value);
	}

	@Override
	public Vector3 vector3Value() {
		//noinspection DataFlowIssue
		return new Vector3(value);
	}

	@Override
	public Vector3Int vector3IntValue() {
		//noinspection DataFlowIssue
		return new Vector3Int(value);
	}

	@Override
	public Vector4 vector4Value() {
		//noinspection DataFlowIssue
		return new Vector4(value);
	}

	@Override
	public Quaternion quaternionValue() {
		//noinspection DataFlowIssue
		return new Quaternion(value);
	}
}
