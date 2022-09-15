package Zeze.Raft.RocksRaft.Log1;

import Zeze.Raft.RocksRaft.Bean;
import Zeze.Raft.RocksRaft.Log;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.Vector2;

public class LogVector2 extends Log {
	private static final int TYPE_ID = Zeze.Transaction.Bean.hash32("Zeze.Raft.RocksRaft.Log<vector2>");

	public Vector2 Value;

	public LogVector2() {
		super(TYPE_ID);
	}

	public LogVector2(Bean belong, int varId, Vector2 value) {
		this();
		setBelong(belong);
		setVariableId(varId);
		Value = value;
	}

	@Override
	public void Encode(ByteBuffer bb) {
		bb.WriteVector2(Value);
	}

	@Override
	public void Decode(ByteBuffer bb) {
		Value = bb.ReadVector2();
	}

	@Override
	public String toString() {
		return "Value=" + Value;
	}
}
