package Zeze.Raft.RocksRaft.Log1;

import Zeze.Raft.RocksRaft.Bean;
import Zeze.Raft.RocksRaft.Log;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.Vector3;

public class LogVector3 extends Log {
	private static final int TYPE_ID = Zeze.Transaction.Bean.hash32("Zeze.Raft.RocksRaft.Log<vector3>");

	public Vector3 Value;

	public LogVector3() {
		super(TYPE_ID);
	}

	public LogVector3(Bean belong, int varId, Vector3 value) {
		this();
		setBelong(belong);
		setVariableId(varId);
		Value = value;
	}

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteVector3(Value);
	}

	@Override
	public void decode(ByteBuffer bb) {
		Value = bb.ReadVector3();
	}

	@Override
	public String toString() {
		return "Value=" + Value;
	}
}
