package Zeze.Raft.RocksRaft.Log1;

import Zeze.Raft.RocksRaft.Bean;
import Zeze.Raft.RocksRaft.Log;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import org.jetbrains.annotations.NotNull;

public class LogFloat extends Log {
	private static final int TYPE_ID = Zeze.Transaction.Bean.hash32("Zeze.Raft.RocksRaft.Log<float>");

	public float value;

	public LogFloat() {
		super(TYPE_ID);
	}

	public LogFloat(Bean belong, int varId, float value) {
		this();
		setBelong(belong);
		setVariableId(varId);
		this.value = value;
	}

	@Override
	public void encode(@NotNull ByteBuffer bb) {
		bb.WriteFloat(value);
	}

	@Override
	public void decode(@NotNull IByteBuffer bb) {
		value = bb.ReadFloat();
	}

	@Override
	public String toString() {
		return "Value=" + value;
	}
}
