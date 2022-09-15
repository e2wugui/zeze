package Zeze.Transaction.Logs;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.Vector3;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Log;

public abstract class LogVector3 extends Log {
	private static final int TYPE_ID = Zeze.Transaction.Bean.hash32("Zeze.Raft.RocksRaft.Log<vector3>");

	public Vector3 Value;

	public LogVector3() {
		super(TYPE_ID);
	}

	public LogVector3(Bean belong, int varId, Vector3 value) {
		super(TYPE_ID);
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
		return String.valueOf(Value);
	}
}
