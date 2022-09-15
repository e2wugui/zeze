package Zeze.Raft.RocksRaft.Log1;

import Zeze.Raft.RocksRaft.Bean;
import Zeze.Raft.RocksRaft.Log;
import Zeze.Serialize.ByteBuffer;

public class LogFloat extends Log {
	private static final int TYPE_ID = Zeze.Transaction.Bean.hash32("Zeze.Raft.RocksRaft.Log<float>");

	public float Value;

	public LogFloat() {
		super(TYPE_ID);
	}

	public LogFloat(Bean belong, int varId, float value) {
		this();
		setBelong(belong);
		setVariableId(varId);
		Value = value;
	}

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteFloat(Value);
	}

	@Override
	public void decode(ByteBuffer bb) {
		Value = bb.ReadFloat();
	}

	@Override
	public String toString() {
		return "Value=" + Value;
	}
}
