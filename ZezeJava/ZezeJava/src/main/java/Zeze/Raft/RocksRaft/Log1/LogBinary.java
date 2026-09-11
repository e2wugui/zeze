package Zeze.Raft.RocksRaft.Log1;

import Zeze.Net.Binary;
import Zeze.Raft.RocksRaft.Bean;
import Zeze.Raft.RocksRaft.Log;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import org.jetbrains.annotations.NotNull;

public class LogBinary extends Log {
	private static final int TYPE_ID = Zeze.Transaction.Bean.hash32("Zeze.Raft.RocksRaft.Log<binary>");

	public Binary value;

	public LogBinary() {
		super(TYPE_ID);
	}

	public LogBinary(Bean belong, int varId, Binary value) {
		this();
		setBelong(belong);
		setVariableId(varId);
		this.value = value;
	}

	@Override
	public void encode(@NotNull ByteBuffer bb) {
		bb.WriteBinary(value);
	}

	@Override
	public void decode(@NotNull IByteBuffer bb) {
		value = bb.ReadBinary();
	}

	@Override
	public String toString() {
		return "Value=" + value;
	}
}
