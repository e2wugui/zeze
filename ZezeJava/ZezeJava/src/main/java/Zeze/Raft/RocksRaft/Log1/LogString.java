package Zeze.Raft.RocksRaft.Log1;

import Zeze.Raft.RocksRaft.Bean;
import Zeze.Raft.RocksRaft.Log;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

public class LogString extends Log {
	private static final int TYPE_ID = Zeze.Transaction.Bean.hash32("Zeze.Raft.RocksRaft.Log<string>");

	public String value;

	public LogString() {
		super(TYPE_ID);
	}

	public LogString(Bean belong, int varId, String value) {
		this();
		setBelong(belong);
		setVariableId(varId);
		this.value = value;
	}

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteString(value);
	}

	@Override
	public void decode(IByteBuffer bb) {
		value = bb.ReadString();
	}

	@Override
	public String toString() {
		return "Value=" + value;
	}
}
