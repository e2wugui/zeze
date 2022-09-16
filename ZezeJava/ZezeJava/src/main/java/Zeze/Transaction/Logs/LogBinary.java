package Zeze.Transaction.Logs;

import Zeze.Net.Binary;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Log;
import Zeze.Serialize.ByteBuffer;

public abstract class LogBinary extends Log {
	private static final int TYPE_ID = Zeze.Transaction.Bean.hash32("Zeze.Raft.RocksRaft.Log<binary>");

	public Binary value;

	public LogBinary() {
		super(TYPE_ID);
	}

	public LogBinary(Bean belong, int varId, Binary value) {
		this();
		setBelong(belong);
		setVariableId(varId);
		this.value = value;
	}

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteBinary(value);
	}

	@Override
	public void decode(ByteBuffer bb) {
		value = bb.ReadBinary();
	}

	@Override
	public String toString() {
		return value.toString();
	}
}
