package Zeze.Transaction.Logs;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.Vector4;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Log;

public abstract class LogVector4 extends Log {
	private static final int TYPE_ID = Zeze.Transaction.Bean.Hash32("Zeze.Raft.RocksRaft.Log<vector2>");

	public Vector4 Value;

	public LogVector4() {
		super(TYPE_ID);
	}

	public LogVector4(Bean belong, int varId, Vector4 value) {
		super(TYPE_ID);
		setBelong(belong);
		setVariableId(varId);
		Value = value;
	}

	@Override
	public void Encode(ByteBuffer bb) {
		bb.WriteVector4(Value);
	}

	@Override
	public void Decode(ByteBuffer bb) {
		Value = bb.ReadVector4();
	}

	@Override
	public String toString() {
		return String.valueOf(Value);
	}
}
