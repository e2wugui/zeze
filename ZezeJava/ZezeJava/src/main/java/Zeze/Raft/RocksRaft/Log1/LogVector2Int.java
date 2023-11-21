package Zeze.Raft.RocksRaft.Log1;

import Zeze.Raft.RocksRaft.Bean;
import Zeze.Raft.RocksRaft.Log;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.Vector2Int;

public class LogVector2Int extends Log {
	private static final int TYPE_ID = Zeze.Transaction.Bean.hash32("Zeze.Raft.RocksRaft.Log<vector2int>");

	public Vector2Int value;

	public LogVector2Int() {
		super(TYPE_ID);
	}

	public LogVector2Int(Bean belong, int varId, Vector2Int value) {
		this();
		setBelong(belong);
		setVariableId(varId);
		this.value = value;
	}

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteVector2Int(value);
	}

	@Override
	public void decode(IByteBuffer bb) {
		value = bb.ReadVector2Int();
	}

	@Override
	public String toString() {
		return "Value=" + value;
	}
}
