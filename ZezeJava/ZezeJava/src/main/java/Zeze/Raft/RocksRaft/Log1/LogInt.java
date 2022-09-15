package Zeze.Raft.RocksRaft.Log1;

import Zeze.Raft.RocksRaft.Bean;
import Zeze.Raft.RocksRaft.Log;
import Zeze.Serialize.ByteBuffer;

public class LogInt extends Log {
	private static final int TYPE_ID = Zeze.Transaction.Bean.hash32("Zeze.Raft.RocksRaft.Log<int>");

	public int Value;

	public LogInt() {
		super(TYPE_ID);
	}

	public LogInt(Bean belong, int varId, int value) {
		this();
		setBelong(belong);
		setVariableId(varId);
		Value = value;
	}

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteInt(Value);
	}

	@Override
	public void decode(ByteBuffer bb) {
		Value = bb.ReadInt();
	}

	@Override
	public String toString() {
		return "Value=" + Value;
	}
}
