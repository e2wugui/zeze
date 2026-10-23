package Zeze.Transaction.Logs;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.Quaternion;
import Zeze.Serialize.Vector3Int;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Log;

/** Complete field change value, independent of any local transaction target. */
public class LogQuaternionValue extends Log {
	public static final int TYPE_ID = Bean.hash32("Zeze.Transaction.Log<quaternion>");

	public Quaternion value;

	protected LogQuaternionValue(int varId, Quaternion value) {
		super(varId);
		this.value = value;
	}

	@Override
	public int getTypeId() {
		return TYPE_ID;
	}

	@Override
	public String getTypeName() {
		return "LogQuaternion";
	}

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteQuaternion(value);
	}

	@Override
	public String toString() {
		return String.valueOf(value);
	}

	@Override
	public Quaternion vector2Value() {
		return value;
	}

	@Override
	public Vector3Int vector2IntValue() {
		return new Vector3Int(value);
	}

	@Override
	public Quaternion vector3Value() {
		return value;
	}

	@Override
	public Vector3Int vector3IntValue() {
		return new Vector3Int(value);
	}

	@Override
	public Quaternion vector4Value() {
		return value;
	}

	@Override
	public Quaternion quaternionValue() {
		return value;
	}

	public static LogQuaternionValue decode(int varId, IByteBuffer bb) {
		return new LogQuaternionValue(varId, bb.ReadQuaternion());
	}
}
