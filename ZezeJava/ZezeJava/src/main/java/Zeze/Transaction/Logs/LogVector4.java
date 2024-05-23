package Zeze.Transaction.Logs;

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

public class LogVector4 extends Log {
	private static final int TYPE_ID = Bean.hash32("Zeze.Transaction.Log<vector4>");

	public Vector4 value;

	public LogVector4(Bean belong, int varId, Vector4 value) {
		setBelong(belong);
		setVariableId(varId);
		this.value = value;
	}

	public LogVector4() {
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
		throw new UnsupportedOperationException();
	}

	@Override
	public void encode(@NotNull ByteBuffer bb) {
		bb.WriteVector4(value);
	}

	@Override
	public void decode(@NotNull IByteBuffer bb) {
		value = bb.ReadVector4();
	}

	@Override
	public @NotNull String toString() {
		return String.valueOf(value);
	}

	@Override
	public @NotNull Vector4 vector2Value() {
		return value;
	}

	@Override
	public @NotNull Vector3Int vector2IntValue() {
		return new Vector3Int(value);
	}

	@Override
	public @NotNull Vector4 vector3Value() {
		return value;
	}

	@Override
	public @NotNull Vector3Int vector3IntValue() {
		return new Vector3Int(value);
	}

	@Override
	public @NotNull Vector4 vector4Value() {
		return value;
	}

	@Override
	public @NotNull Quaternion quaternionValue() {
		return new Quaternion(value);
	}
}
