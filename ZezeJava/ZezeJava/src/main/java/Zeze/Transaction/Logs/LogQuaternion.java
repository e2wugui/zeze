package Zeze.Transaction.Logs;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.Quaternion;
import Zeze.Serialize.Vector3Int;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Log;
import org.jetbrains.annotations.NotNull;

public class LogQuaternion extends Log {
	private static final int TYPE_ID = Bean.hash32("Zeze.Transaction.Log<quaternion>");

	public Quaternion value;

	public LogQuaternion(Bean belong, int varId, Quaternion value) {
		setBelong(belong);
		setVariableId(varId);
		this.value = value;
	}

	public LogQuaternion() {
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
		bb.WriteQuaternion(value);
	}

	@Override
	public void decode(@NotNull IByteBuffer bb) {
		value = bb.ReadQuaternion();
	}

	@Override
	public @NotNull String toString() {
		return String.valueOf(value);
	}

	@Override
	public @NotNull Quaternion vector2Value() {
		return value;
	}

	@Override
	public @NotNull Vector3Int vector2IntValue() {
		return new Vector3Int(value);
	}

	@Override
	public @NotNull Quaternion vector3Value() {
		return value;
	}

	@Override
	public @NotNull Vector3Int vector3IntValue() {
		return new Vector3Int(value);
	}

	@Override
	public @NotNull Quaternion vector4Value() {
		return value;
	}

	@Override
	public @NotNull Quaternion quaternionValue() {
		return value;
	}
}
