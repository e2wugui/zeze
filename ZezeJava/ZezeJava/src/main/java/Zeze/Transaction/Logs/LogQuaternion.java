package Zeze.Transaction.Logs;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.Quaternion;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Log;

public abstract class LogQuaternion extends Log {
	private static final int TYPE_ID = Zeze.Transaction.Bean.Hash32("Zeze.Raft.RocksRaft.Log<vector2>");

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
	public void Encode(ByteBuffer bb) {
		bb.WriteQuaternion(Value);
	}

	@Override
	public void Decode(ByteBuffer bb) {
		Value = bb.ReadQuaternion();
	}

	@Override
	public String toString() {
		return String.valueOf(Value);
	}
}
