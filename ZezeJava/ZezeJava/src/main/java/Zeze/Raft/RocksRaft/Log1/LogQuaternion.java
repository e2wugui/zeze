package Zeze.Raft.RocksRaft.Log1;

import Zeze.Raft.RocksRaft.Bean;
import Zeze.Raft.RocksRaft.Log;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.Quaternion;

public class LogQuaternion extends Log {
	private static final int TYPE_ID = Zeze.Transaction.Bean.hash32("Zeze.Raft.RocksRaft.Log<quaternion>");

	public Quaternion Value;

	public LogQuaternion() {
		super(TYPE_ID);
	}

	public LogQuaternion(Bean belong, int varId, Quaternion value) {
		this();
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
		return "Value=" + Value;
	}
}
