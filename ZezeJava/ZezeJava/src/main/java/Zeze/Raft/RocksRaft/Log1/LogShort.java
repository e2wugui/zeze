package Zeze.Raft.RocksRaft.Log1;

import Zeze.Raft.RocksRaft.Bean;
import Zeze.Raft.RocksRaft.Log;
import Zeze.Serialize.ByteBuffer;

public class LogShort extends Log {
	private static final int TYPE_ID = Zeze.Transaction.Bean.Hash32("Zeze.Raft.RocksRaft.Log<short>");

	public short Value;

	public LogShort() {
		super(TYPE_ID);
	}

	public LogShort(Bean belong, int varId, short value) {
		this();
		setBelong(belong);
		setVariableId(varId);
		Value = value;
	}

	@Override
	public void Encode(ByteBuffer bb) {
		bb.WriteInt(Value);
	}

	@Override
	public void Decode(ByteBuffer bb) {
		Value = (short)bb.ReadInt();
	}

	@Override
	public String toString() {
		return "Value=" + Value;
	}
}
