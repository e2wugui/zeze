package Zeze.Raft.RocksRaft.Log1;

import Zeze.Raft.RocksRaft.Bean;
import Zeze.Raft.RocksRaft.Log;
import Zeze.Serialize.ByteBuffer;

public class LogInt extends Log {
	public int Value;

	public LogInt() {
		super("Zeze.Raft.RocksRaft.Log<int>");
	}

	public LogInt(Bean belong, int varId, int value) {
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
		Value = bb.ReadInt();
	}

	@Override
	public String toString() {
		return "Value=" + Value;
	}
}
