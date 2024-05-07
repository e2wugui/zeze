package Zeze.Transaction.Logs;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.Quaternion;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Log;

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
	public int getTypeId() {
		return TYPE_ID;
	}

	@Override
	public void commit() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteQuaternion(value);
	}

	@Override
	public void decode(IByteBuffer bb) {
		value = bb.ReadQuaternion();
	}

	@Override
	public String toString() {
		return String.valueOf(value);
	}
}
