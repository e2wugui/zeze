package Zeze.Transaction.Logs;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.Quaternion;
import Zeze.Serialize.Vector3;
import Zeze.Serialize.Vector3Int;
import Zeze.Serialize.Vector4;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Log;

/** Complete field change value, independent of any local transaction target. */
public class LogVector3IntValue extends Log {
	public static final int TYPE_ID = Bean.hash32("Zeze.Transaction.Log<vector3int>");

	public Vector3Int value;

	protected LogVector3IntValue(int varId, Vector3Int value) {
		super(varId);
		this.value = value;
	}

	@Override
	public int getTypeId() {
		return TYPE_ID;
	}

	@Override
	public String getTypeName() {
		return "LogVector3Int";
	}

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteVector3Int(value);
	}

	@Override
	public String toString() {
		return String.valueOf(value);
	}

	@Override
	public Vector3 vector2Value() {
		return new Vector3(value);
	}

	@Override
	public Vector3Int vector2IntValue() {
		return value;
	}

	@Override
	public Vector3 vector3Value() {
		return new Vector3(value);
	}

	@Override
	public Vector3Int vector3IntValue() {
		return value;
	}

	@Override
	public Vector4 vector4Value() {
		return new Vector4(value);
	}

	@Override
	public Quaternion quaternionValue() {
		return new Quaternion(value);
	}

	public static LogVector3IntValue decode(int varId, IByteBuffer bb) {
		return new LogVector3IntValue(varId, bb.ReadVector3Int());
	}
}
