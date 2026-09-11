package Zeze.Raft.RocksRaft.Log1;

import Zeze.Raft.RocksRaft.Bean;
import Zeze.Raft.RocksRaft.Log;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import org.jetbrains.annotations.NotNull;

public class LogDouble extends Log {
	private static final int TYPE_ID = Zeze.Transaction.Bean.hash32("Zeze.Raft.RocksRaft.Log<double>");

	public double value;

	public LogDouble() {
		super(TYPE_ID);
	}

	public LogDouble(Bean belong, int varId, double value) {
		this();
		setBelong(belong);
		setVariableId(varId);
		this.value = value;
	}

	@Override
	public void encode(@NotNull ByteBuffer bb) {
		bb.WriteDouble(value);
	}

	@Override
	public void decode(@NotNull IByteBuffer bb) {
		value = bb.ReadDouble();
	}

	@Override
	public String toString() {
		return "Value=" + value;
	}
}
