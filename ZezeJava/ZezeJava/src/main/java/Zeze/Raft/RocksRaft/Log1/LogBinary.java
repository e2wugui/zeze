package Zeze.Raft.RocksRaft.Log1;

import Zeze.Net.Binary;
import Zeze.Raft.RocksRaft.Bean;
import Zeze.Raft.RocksRaft.Log;
import Zeze.Serialize.ByteBuffer;

public class LogBinary extends Log {
	public Binary Value;

	public LogBinary() {
		super("Zeze.Raft.RocksRaft.Log<binary>");
	}

	public LogBinary(Bean belong, int varId, Binary value) {
		this();
		setBelong(belong);
		setVariableId(varId);
		Value = value;
	}

	@Override
	public void Encode(ByteBuffer bb) {
		bb.WriteBinary(Value);
	}

	@Override
	public void Decode(ByteBuffer bb) {
		Value = bb.ReadBinary();
	}

	@Override
	public String toString() {
		return "Value=" + Value;
	}
}
