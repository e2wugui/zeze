package Zeze.Transaction.Logs;

import Zeze.Transaction.Bean;
import Zeze.Transaction.Log;
import Zeze.Serialize.ByteBuffer;

public abstract class LogBool extends Log {
	private static final int TYPE_ID = Zeze.Transaction.Bean.Hash32("Zeze.Raft.RocksRaft.Log<bool>");

	public boolean Value;

	public LogBool() {
		super(TYPE_ID);
	}

	public LogBool(Bean belong, int varId, boolean value) {
		this();
		setBelong(belong);
		setVariableId(varId);
		Value = value;
	}

	@Override
	public void Encode(ByteBuffer bb) {
		bb.WriteBool(Value);
	}

	@Override
	public void Decode(ByteBuffer bb) {
		Value = bb.ReadBool();
	}

	@Override
	public String toString() {
		return String.valueOf(Value);
	}
}
