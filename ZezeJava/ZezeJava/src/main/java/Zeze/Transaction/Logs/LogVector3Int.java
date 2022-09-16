package Zeze.Transaction.Logs;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.Vector3Int;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Log;

public abstract class LogVector3Int extends Log {
	private static final int TYPE_ID = Zeze.Transaction.Bean.hash32("Zeze.Raft.RocksRaft.Log<vector3int>");

	public Vector3Int value;

	public LogVector3Int() {
		super(TYPE_ID);
	}

	public LogVector3Int(Bean belong, int varId, Vector3Int value) {
		super(TYPE_ID);
		setBelong(belong);
		setVariableId(varId);
		this.value = value;
	}

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteVector3Int(value);
	}

	@Override
	public void decode(ByteBuffer bb) {
		value = bb.ReadVector3Int();
	}

	@Override
	public String toString() {
		return String.valueOf(value);
	}
}
