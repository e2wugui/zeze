package Zeze.Transaction.Logs;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.Vector2Int;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Log;

public abstract class LogVector2Int extends Log {
	private static final int TYPE_ID = Zeze.Transaction.Bean.hash32("Zeze.Raft.RocksRaft.Log<vector2int>");

	public Vector2Int Value;

	public LogVector2Int() {
		super(TYPE_ID);
	}

	public LogVector2Int(Bean belong, int varId, Vector2Int value) {
		super(TYPE_ID);
		setBelong(belong);
		setVariableId(varId);
		Value = value;
	}

	@Override
	public void Encode(ByteBuffer bb) {
		bb.WriteVector2Int(Value);
	}

	@Override
	public void Decode(ByteBuffer bb) {
		Value = bb.ReadVector2Int();
	}

	@Override
	public String toString() {
		return String.valueOf(Value);
	}
}
