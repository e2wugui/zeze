package Zeze.Transaction.Logs;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.Vector2;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Log;

public abstract class LogVector2 extends Log {
	private static final int TYPE_ID = Zeze.Transaction.Bean.hash32("Zeze.Transaction.Log<vector2>");

	public Vector2 value;

	public LogVector2(Bean belong, int varId, Vector2 value) {
		setBelong(belong);
		setVariableId(varId);
		this.value = value;
	}

	@Override
	public int getTypeId() {
		return TYPE_ID;
	}

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteVector2(value);
	}

	@Override
	public void decode(ByteBuffer bb) {
		value = bb.ReadVector2();
	}

	@Override
	public String toString() {
		return String.valueOf(value);
	}
}
