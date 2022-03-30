package Zeze.Raft.RocksRaft.Log1;

import Zeze.Raft.RocksRaft.Bean;
import Zeze.Raft.RocksRaft.Log;
import Zeze.Serialize.ByteBuffer;

public class LogFloat extends Log {
	public float Value;

	public LogFloat() {
		super("Zeze.Raft.RocksRaft.Log<float>");
	}

	public LogFloat(Bean belong, int varId, float value) {
		this();
		setBelong(belong);
		setVariableId(varId);
		Value = value;
	}

	@Override
	public void Encode(ByteBuffer bb) {
		bb.WriteFloat(Value);
	}

	@Override
	public void Decode(ByteBuffer bb) {
		Value = bb.ReadFloat();
	}

	@Override
	public String toString() {
		return "Value=" + Value;
	}
}
