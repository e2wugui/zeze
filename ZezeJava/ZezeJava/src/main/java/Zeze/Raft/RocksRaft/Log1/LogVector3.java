package Zeze.Raft.RocksRaft.Log1;

import Zeze.Raft.RocksRaft.Bean;
import Zeze.Raft.RocksRaft.Log;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.Vector3;

public class LogVector3 extends Log {
	private static final int TYPE_ID = Zeze.Transaction.Bean.hash32("Zeze.Raft.RocksRaft.Log<vector3>");

	public Vector3 value;

	public LogVector3() {
		super(TYPE_ID);
	}

	public LogVector3(Bean belong, int varId, Vector3 value) {
		this();
		setBelong(belong);
		setVariableId(varId);
		this.value = value;
	}

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteVector3(value);
	}

	@Override
	public void decode(IByteBuffer bb) {
		value = bb.ReadVector3();
	}

	@Override
	public String toString() {
		return "Value=" + value;
	}
}
