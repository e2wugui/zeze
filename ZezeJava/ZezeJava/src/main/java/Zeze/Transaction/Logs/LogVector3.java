package Zeze.Transaction.Logs;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.Vector3;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Log;

public class LogVector3 extends Log {
	private static final int TYPE_ID = Bean.hash32("Zeze.Transaction.Log<vector3>");

	public Vector3 value;

	public LogVector3(Bean belong, int varId, Vector3 value) {
		setBelong(belong);
		setVariableId(varId);
		this.value = value;
	}

	public LogVector3() {

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
		bb.WriteVector3(value);
	}

	@Override
	public void decode(IByteBuffer bb) {
		value = bb.ReadVector3();
	}

	@Override
	public String toString() {
		return String.valueOf(value);
	}
}
