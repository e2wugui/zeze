package Zeze.Transaction.Logs;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.Vector2Int;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Log;

public class LogVector2Int extends Log {
	private static final int TYPE_ID = Bean.hash32("Zeze.Transaction.Log<vector2int>");

	public Vector2Int value;

	public LogVector2Int(Bean belong, int varId, Vector2Int value) {
		setBelong(belong);
		setVariableId(varId);
		this.value = value;
	}

	public LogVector2Int() {

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
		bb.WriteVector2Int(value);
	}

	@Override
	public void decode(IByteBuffer bb) {
		value = bb.ReadVector2Int();
	}

	@Override
	public String toString() {
		return String.valueOf(value);
	}
}
