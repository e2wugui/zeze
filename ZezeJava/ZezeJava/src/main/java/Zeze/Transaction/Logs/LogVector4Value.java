package Zeze.Transaction.Logs;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.Quaternion;
import Zeze.Serialize.Vector3Int;
import Zeze.Serialize.Vector4;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Log;

/** Complete field change value, independent of any local transaction target. */
public class LogVector4Value extends Log {
	public static final int TYPE_ID = Bean.hash32("Zeze.Transaction.Log<vector4>");

	public Vector4 value;

	protected LogVector4Value(int varId, Vector4 value) {
		super(varId);
		this.value = value;
	}

	@Override
	public int getTypeId() {
		return TYPE_ID;
	}

	@Override
	public String getTypeName() {
		return "LogVector4";
	}

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteVector4(value);
	}

	@Override
	public String toString() {
		return String.valueOf(value);
	}

	@Override
	public Vector4 vector2Value() {
		return value;
	}

	@Override
	public Vector3Int vector2IntValue() {
		return new Vector3Int(value);
	}

	@Override
	public Vector4 vector3Value() {
		return value;
	}

	@Override
	public Vector3Int vector3IntValue() {
		return new Vector3Int(value);
	}

	@Override
	public Vector4 vector4Value() {
		return value;
	}

	@Override
	public Quaternion quaternionValue() {
		return new Quaternion(value);
	}

	public static LogVector4Value decode(int varId, IByteBuffer bb) {
		return new LogVector4Value(varId, bb.ReadVector4());
	}
}
