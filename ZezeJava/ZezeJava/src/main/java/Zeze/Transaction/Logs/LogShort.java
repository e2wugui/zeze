package Zeze.Transaction.Logs;

import Zeze.Transaction.Bean;
import Zeze.Transaction.Log;
import Zeze.Serialize.ByteBuffer;

public abstract class LogShort extends Log {
	private static final int TYPE_ID = Zeze.Transaction.Bean.hash32("Zeze.Raft.RocksRaft.Log<short>");

	public short Value;

	public LogShort() {
		super(TYPE_ID);
	}

	public LogShort(Bean belong, int varId, short value) {
		this();
		setBelong(belong);
		setVariableId(varId);
		Value = value;
	}

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteInt(Value);
	}

	@Override
	public void decode(ByteBuffer bb) {
		Value = (short)bb.ReadInt();
	}

	@Override
	public String toString() {
		return String.valueOf(Value);
	}
}
