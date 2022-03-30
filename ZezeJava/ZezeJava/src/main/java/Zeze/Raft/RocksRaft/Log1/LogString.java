package Zeze.Raft.RocksRaft.Log1;

import Zeze.Raft.RocksRaft.Bean;
import Zeze.Raft.RocksRaft.Log;
import Zeze.Serialize.ByteBuffer;

public class LogString extends Log {
	public String Value;

	public LogString() {
		super("Zeze.Raft.RocksRaft.Log<string>");
	}

	public LogString(Bean belong, int varId, String value) {
		this();
		setBelong(belong);
		setVariableId(varId);
		Value = value;
	}

	@Override
	public void Encode(ByteBuffer bb) {
		bb.WriteString(Value);
	}

	@Override
	public void Decode(ByteBuffer bb) {
		Value = bb.ReadString();
	}

	@Override
	public String toString() {
		return "Value=" + Value;
	}
}
