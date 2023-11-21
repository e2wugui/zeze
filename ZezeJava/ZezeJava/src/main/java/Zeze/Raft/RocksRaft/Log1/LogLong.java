package Zeze.Raft.RocksRaft.Log1;

import Zeze.Raft.RocksRaft.Bean;
import Zeze.Raft.RocksRaft.Log;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

public class LogLong extends Log {
	private static final int TYPE_ID = Zeze.Transaction.Bean.hash32("Zeze.Raft.RocksRaft.Log<long>");

	public long value;

	public LogLong() {
		super(TYPE_ID);
	}

	public LogLong(Bean belong, int varId, long value) {
		this();
		setBelong(belong);
		setVariableId(varId);
		this.value = value;
	}

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteLong(value);
	}

	@Override
	public void decode(IByteBuffer bb) {
		value = bb.ReadLong();
	}

	@Override
	public String toString() {
		return "Value=" + value;
	}
}
