package Zeze.Raft.RocksRaft.Log1;

import Zeze.Raft.RocksRaft.Bean;
import Zeze.Raft.RocksRaft.Log;
import Zeze.Serialize.ByteBuffer;

public class LogByte extends Log {
	private static final int TYPE_ID = Zeze.Transaction.Bean.Hash32("Zeze.Raft.RocksRaft.Log<byte>");

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
	public void Encode(ByteBuffer bb) {
		bb.WriteLong(Value);
	}

	@Override
	public void Decode(ByteBuffer bb) {
		Value = (byte)bb.ReadLong();
	}

	@Override
	public String toString() {
		return "Value=" + Value;
	}
}
