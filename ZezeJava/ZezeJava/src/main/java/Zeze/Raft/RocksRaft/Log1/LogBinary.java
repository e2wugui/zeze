package Zeze.Raft.RocksRaft.Log1;

import Zeze.Net.Binary;
import Zeze.Raft.RocksRaft.Bean;
import Zeze.Raft.RocksRaft.Log;
import Zeze.Serialize.ByteBuffer;

public class LogBinary extends Log {
	private static final int TYPE_ID = Zeze.Transaction.Bean.hash32("Zeze.Raft.RocksRaft.Log<binary>");

	public Binary Value;

	public LogBinary() {
		super(TYPE_ID);
	}

	public LogBinary(Bean belong, int varId, Binary value) {
		this();
		setBelong(belong);
		setVariableId(varId);
		Value = value;
	}

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteBinary(Value);
	}

	@Override
	public void decode(ByteBuffer bb) {
		Value = bb.ReadBinary();
	}

	@Override
	public String toString() {
		return "Value=" + Value;
	}
}
