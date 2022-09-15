package Zeze.Transaction.Logs;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.Vector2;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Log;

public abstract class LogVector2 extends Log {
	private static final int TYPE_ID = Zeze.Transaction.Bean.hash32("Zeze.Raft.RocksRaft.Log<vector2>");

	public Vector2 Value;

	public LogVector2() {
		super(TYPE_ID);
	}

	public LogVector2(Bean belong, int varId, Vector2 value) {
		super(TYPE_ID);
		setBelong(belong);
		setVariableId(varId);
		Value = value;
	}

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteVector2(Value);
	}

	@Override
	public void decode(ByteBuffer bb) {
		Value = bb.ReadVector2();
	}

	@Override
	public String toString() {
		return String.valueOf(Value);
	}
}
