package Zeze.Raft.RocksRaft.Log1;

import Zeze.Raft.RocksRaft.Bean;
import Zeze.Raft.RocksRaft.Log;
import Zeze.Serialize.ByteBuffer;

public class LogLong extends Log {
	private static final int TYPE_ID = Zeze.Transaction.Bean.hash32("Zeze.Raft.RocksRaft.Log<long>");

	public long Value;

	public LogLong() {
		super(TYPE_ID);
	}

	public LogLong(Bean belong, int varId, long value) {
		this();
		setBelong(belong);
		setVariableId(varId);
		Value = value;
	}

	@Override
	public void Encode(ByteBuffer bb) {
		bb.WriteLong(Value);
	}

	@Override
	public void Decode(ByteBuffer bb) {
		Value = bb.ReadLong();
	}

	@Override
	public String toString() {
		return "Value=" + Value;
	}
}
