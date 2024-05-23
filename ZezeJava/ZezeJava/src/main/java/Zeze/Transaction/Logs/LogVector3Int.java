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

public class LogVector3Int extends Log {
	private static final int TYPE_ID = Bean.hash32("Zeze.Transaction.Log<vector3int>");

	public Vector3Int value;

	public LogVector3Int(Bean belong, int varId, Vector3Int value) {
		setBelong(belong);
		setVariableId(varId);
		this.value = value;
	}

	public LogVector3Int() {
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
		throw new UnsupportedOperationException();
	}

	@Override
	public void encode(@NotNull ByteBuffer bb) {
		bb.WriteVector3Int(value);
	}

	@Override
	public void decode(@NotNull IByteBuffer bb) {
		value = bb.ReadVector3Int();
	}

	@Override
	public @NotNull String toString() {
		return String.valueOf(value);
	}

	@Override
	public @NotNull Vector3 vector2Value() {
		return new Vector3(value);
	}

	@Override
	public @NotNull Vector3Int vector2IntValue() {
		return value;
	}

	@Override
	public @NotNull Vector3 vector3Value() {
		return new Vector3(value);
	}

	@Override
	public @NotNull Vector3Int vector3IntValue() {
		return value;
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
