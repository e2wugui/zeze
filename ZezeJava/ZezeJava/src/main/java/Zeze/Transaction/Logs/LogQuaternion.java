package Zeze.Transaction.Logs;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.Quaternion;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Log;

public abstract class LogQuaternion extends Log {
	private static final int TYPE_ID = Zeze.Transaction.Bean.hash32("Zeze.Raft.RocksRaft.Log<quaternion>");

	public Quaternion Value;

	public LogQuaternion() {
		super(TYPE_ID);
	}

	public LogQuaternion(Bean belong, int varId, Quaternion value) {
		super(TYPE_ID);
		setBelong(belong);
		setVariableId(varId);
		Value = value;
	}

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteQuaternion(Value);
	}

	@Override
	public void decode(ByteBuffer bb) {
		Value = bb.ReadQuaternion();
	}

	@Override
	public String toString() {
		return String.valueOf(Value);
	}
}
