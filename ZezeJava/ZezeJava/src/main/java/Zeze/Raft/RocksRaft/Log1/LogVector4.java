package Zeze.Raft.RocksRaft.Log1;

import Zeze.Raft.RocksRaft.Bean;
import Zeze.Raft.RocksRaft.Log;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.Vector4;

public class LogVector4 extends Log {
	private static final int TYPE_ID = Zeze.Transaction.Bean.hash32("Zeze.Raft.RocksRaft.Log<vector4>");

	public Vector4 Value;

	public LogVector4() {
		super(TYPE_ID);
	}

	public LogVector4(Bean belong, int varId, Vector4 value) {
		this();
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
		return "Value=" + Value;
	}
}
