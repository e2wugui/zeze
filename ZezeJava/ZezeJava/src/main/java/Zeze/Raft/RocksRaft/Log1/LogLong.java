package Zeze.Raft.RocksRaft.Log1;

import Zeze.Raft.RocksRaft.Bean;
import Zeze.Raft.RocksRaft.Log;
import Zeze.Serialize.ByteBuffer;

public class LogLong extends Log {
	public long Value;

	public LogLong() {
		super("Zeze.Raft.RocksRaft.Log<long>");
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
