package Zeze.Raft.RocksRaft.Log1;

import Zeze.Raft.RocksRaft.Bean;
import Zeze.Raft.RocksRaft.Log;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import org.jetbrains.annotations.NotNull;

public class LogByte extends Log {
	private static final int TYPE_ID = Zeze.Transaction.Bean.hash32("Zeze.Raft.RocksRaft.Log<byte>");

	public byte value;

	public LogByte() {
		super(TYPE_ID);
	}

	public LogByte(Bean belong, int varId, byte value) {
		this();
		setBelong(belong);
		setVariableId(varId);
		this.value = value;
	}

	@Override
	public void encode(@NotNull ByteBuffer bb) {
		bb.WriteLong(value);
	}

	@Override
	public void decode(@NotNull IByteBuffer bb) {
		value = (byte)bb.ReadLong();
	}

	@Override
	public String toString() {
		return "Value=" + value;
	}
}
