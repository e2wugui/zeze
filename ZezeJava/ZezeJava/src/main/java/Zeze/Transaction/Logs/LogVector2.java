package Zeze.Transaction.Logs;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.Vector2;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Log;

public class LogVector2 extends Log {
	private static final int TYPE_ID = Bean.hash32("Zeze.Transaction.Log<vector2>");

	public Vector2 value;

	public LogVector2(Bean belong, int varId, Vector2 value) {
		setBelong(belong);
		setVariableId(varId);
		this.value = value;
	}

	public LogVector2() {

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
		bb.WriteVector2(value);
	}

	@Override
	public void decode(IByteBuffer bb) {
		value = bb.ReadVector2();
	}

	@Override
	public String toString() {
		return String.valueOf(value);
	}
}
