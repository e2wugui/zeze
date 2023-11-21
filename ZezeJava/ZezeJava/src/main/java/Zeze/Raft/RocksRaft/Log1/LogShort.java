package Zeze.Raft.RocksRaft.Log1;

import Zeze.Raft.RocksRaft.Bean;
import Zeze.Raft.RocksRaft.Log;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

public class LogShort extends Log {
	private static final int TYPE_ID = Zeze.Transaction.Bean.hash32("Zeze.Raft.RocksRaft.Log<short>");

	public short value;

	public LogShort() {
		super(TYPE_ID);
	}

	public LogShort(Bean belong, int varId, short value) {
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
	public void decode(IByteBuffer bb) {
		value = (short)bb.ReadInt();
	}

	@Override
	public String toString() {
		return "Value=" + value;
	}
}
