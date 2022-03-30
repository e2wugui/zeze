package Zeze.Raft.RocksRaft.Log1;

import Zeze.Raft.RocksRaft.Bean;
import Zeze.Raft.RocksRaft.Log;
import Zeze.Serialize.ByteBuffer;

public class LogDouble extends Log {
	public double Value;

	public LogDouble() {
		super("Zeze.Raft.RocksRaft.Log<double>");
	}

	public LogDouble(Bean belong, int varId, double value) {
		this();
		setBelong(belong);
		setVariableId(varId);
		Value = value;
	}

	@Override
	public void Encode(ByteBuffer bb) {
		bb.WriteDouble(Value);
	}

	@Override
	public void Decode(ByteBuffer bb) {
		Value = bb.ReadDouble();
	}

	@Override
	public String toString() {
		return "Value=" + Value;
	}
}
