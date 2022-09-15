package Zeze.Transaction.Logs;

import Zeze.Transaction.Bean;
import Zeze.Transaction.Log;
import Zeze.Serialize.ByteBuffer;

public abstract class LogByte extends Log {
	private static final int TYPE_ID = Zeze.Transaction.Bean.hash32("Zeze.Raft.RocksRaft.Log<byte>");

	public byte Value;

	public LogByte() {
		super(TYPE_ID);
	}

	public LogByte(Bean belong, int varId, byte value) {
		this();
		setBelong(belong);
		setVariableId(varId);
		Value = value;
	}

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteLong(Value);
	}

	@Override
	public void decode(ByteBuffer bb) {
		Value = (byte)bb.ReadLong();
	}

	@Override
	public String toString() {
		return String.valueOf(Value);
	}
}
