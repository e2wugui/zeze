package Zeze.Transaction.Logs;

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
import org.jetbrains.annotations.NotNull;

public class LogVector2 extends Log {
	private static final int TYPE_ID = Bean.hash32("Zeze.Transaction.Log<vector2>");

	private final VarHandle vh;
	public Vector2 value;

	public LogVector2(Bean belong, int varId, VarHandle vh, Vector2 value) {
		super(belong, varId);
		this.vh = vh;
		this.value = value;
	}

	public LogVector2(int varId) {
		super(null, varId);
		vh = null;
	}

	@Override
	public @NotNull Category category() {
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
	public void encode(@NotNull ByteBuffer bb) {
		bb.WriteVector2(value);
	}

	@Override
	public void decode(@NotNull IByteBuffer bb) {
		value = bb.ReadVector2();
	}

	@Override
	public @NotNull String toString() {
		return String.valueOf(value);
	}

	@Override
	public @NotNull Vector2 vector2Value() {
		return value;
	}

	@Override
	public @NotNull Vector2Int vector2IntValue() {
		return new Vector2Int(value);
	}

	@Override
	public @NotNull Vector3 vector3Value() {
		return new Vector3(value);
	}

	@Override
	public @NotNull Vector3Int vector3IntValue() {
		return new Vector3Int(value);
	}

	@Override
	public @NotNull Vector4 vector4Value() {
		return new Vector4(value);
	}

	@Override
	public @NotNull Quaternion quaternionValue() {
		return new Quaternion(value);
	}
}
