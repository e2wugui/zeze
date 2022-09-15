package Zeze.Raft.RocksRaft.Log1;

import Zeze.Raft.RocksRaft.Bean;
import Zeze.Raft.RocksRaft.Log;
import Zeze.Serialize.ByteBuffer;

public class LogDouble extends Log {
	private static final int TYPE_ID = Zeze.Transaction.Bean.hash32("Zeze.Raft.RocksRaft.Log<double>");

	public double Value;

	public LogDouble() {
		super(TYPE_ID);
	}

	public LogDouble(Bean belong, int varId, double value) {
		this();
		setBelong(belong);
		setVariableId(varId);
		Value = value;
	}

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteDouble(Value);
	}

	@Override
	public void decode(ByteBuffer bb) {
		Value = bb.ReadDouble();
	}

	@Override
	public String toString() {
		return "Value=" + Value;
	}
}
