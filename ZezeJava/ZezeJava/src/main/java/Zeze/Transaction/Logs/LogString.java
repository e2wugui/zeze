package Zeze.Transaction.Logs;

import Zeze.Transaction.Bean;
import Zeze.Transaction.Log;
import Zeze.Serialize.ByteBuffer;

public abstract class LogString extends Log {
	private static final int TYPE_ID = Zeze.Transaction.Bean.hash32("Zeze.Raft.RocksRaft.Log<string>");

	public String Value;

	public LogString() {
		super(TYPE_ID);
	}

	public LogString(Bean belong, int varId, String value) {
		this();
		setBelong(belong);
		setVariableId(varId);
		Value = value;
	}

	@Override
	public void Encode(ByteBuffer bb) {
		bb.WriteString(Value);
	}

	@Override
	public void Decode(ByteBuffer bb) {
		Value = bb.ReadString();
	}

	@Override
	public String toString() {
		return String.valueOf(Value);
	}
}
