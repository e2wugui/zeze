package Zeze.Transaction.Logs;

import Zeze.Transaction.Bean;
import Zeze.Transaction.Log;
import Zeze.Serialize.ByteBuffer;

public abstract class LogInt extends Log {
	private static final int TYPE_ID = Zeze.Transaction.Bean.hash32("Zeze.Raft.RocksRaft.Log<int>");

	public int value;

	public LogInt() {
		super(TYPE_ID);
	}

	public LogInt(Bean belong, int varId, int value) {
		this();
		setBelong(belong);
		setVariableId(varId);
		this.value = value;
	}

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteInt(value);
	}

	@Override
	public void decode(ByteBuffer bb) {
		value = bb.ReadInt();
	}

	@Override
	public String toString() {
		return String.valueOf(value);
	}
}
