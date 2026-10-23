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

/** Complete field change value, independent of any local transaction target. */
public class LogVector2Value extends Log {
	public static final int TYPE_ID = Bean.hash32("Zeze.Transaction.Log<vector2>");

	public Vector2 value;

	protected LogVector2Value(int varId, Vector2 value) {
		super(varId);
		this.value = value;
	}

	@Override
	public int getTypeId() {
		return TYPE_ID;
	}

	@Override
	public String getTypeName() {
		return "LogVector2";
	}

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteVector2(value);
	}

	@Override
	public String toString() {
		return String.valueOf(value);
	}

	@Override
	public Vector2 vector2Value() {
		return value;
	}

	@Override
	public Vector2Int vector2IntValue() {
		return new Vector2Int(value);
	}

	@Override
	public Vector3 vector3Value() {
		return new Vector3(value);
	}

	@Override
	public Vector3Int vector3IntValue() {
		return new Vector3Int(value);
	}

	@Override
	public Vector4 vector4Value() {
		return new Vector4(value);
	}

	@Override
	public Quaternion quaternionValue() {
		return new Quaternion(value);
	}

	public static LogVector2Value decode(int varId, IByteBuffer bb) {
		return new LogVector2Value(varId, bb.ReadVector2());
	}
}
