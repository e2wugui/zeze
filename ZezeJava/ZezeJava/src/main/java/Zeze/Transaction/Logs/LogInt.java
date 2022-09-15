package Zeze.Transaction.Logs;

import Zeze.Transaction.Bean;
import Zeze.Transaction.Log;
import Zeze.Serialize.ByteBuffer;

public abstract class LogInt extends Log {
	private static final int TYPE_ID = Zeze.Transaction.Bean.hash32("Zeze.Raft.RocksRaft.Log<int>");

	public int Value;

	public LogInt() {
		super(TYPE_ID);
	}

	public LogInt(Bean belong, int varId, int value) {
		this();
		setBelong(belong);
		setVariableId(varId);
		Value = value;
	}

	@Override
	public void Encode(ByteBuffer bb) {
		bb.WriteInt(Value);
	}

	@Override
	public void Decode(ByteBuffer bb) {
		Value = bb.ReadInt();
	}

	@Override
	public String toString() {
		return String.valueOf(Value);
	}
}
