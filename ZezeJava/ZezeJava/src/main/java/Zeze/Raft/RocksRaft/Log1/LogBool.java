package Zeze.Raft.RocksRaft.Log1;

import Zeze.Raft.RocksRaft.Bean;
import Zeze.Raft.RocksRaft.Log;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import org.jetbrains.annotations.NotNull;

public class LogBool extends Log {
	private static final int TYPE_ID = Zeze.Transaction.Bean.hash32("Zeze.Raft.RocksRaft.Log<bool>");

	public boolean value;

	public LogBool() {
		super(TYPE_ID);
	}

	public LogBool(Bean belong, int varId, boolean value) {
		this();
		setBelong(belong);
		setVariableId(varId);
		this.value = value;
	}

	@Override
	public void encode(@NotNull ByteBuffer bb) {
		bb.WriteBool(value);
	}

	@Override
	public void decode(@NotNull IByteBuffer bb) {
		value = bb.ReadBool();
	}

	@Override
	public String toString() {
		return "Value=" + value;
	}
}
