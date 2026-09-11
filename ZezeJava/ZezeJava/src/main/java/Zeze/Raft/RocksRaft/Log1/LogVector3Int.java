package Zeze.Raft.RocksRaft.Log1;

import Zeze.Raft.RocksRaft.Bean;
import Zeze.Raft.RocksRaft.Log;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.Vector3Int;
import org.jetbrains.annotations.NotNull;

public class LogVector3Int extends Log {
	private static final int TYPE_ID = Zeze.Transaction.Bean.hash32("Zeze.Raft.RocksRaft.Log<vector3int>");

	public Vector3Int value;

	public LogVector3Int() {
		super(TYPE_ID);
	}

	public LogVector3Int(Bean belong, int varId, Vector3Int value) {
		this();
		setBelong(belong);
		setVariableId(varId);
		this.value = value;
	}

	@Override
	public void encode(@NotNull ByteBuffer bb) {
		bb.WriteVector3Int(value);
	}

	@Override
	public void decode(@NotNull IByteBuffer bb) {
		value = bb.ReadVector3Int();
	}

	@Override
	public String toString() {
		return "Value=" + value;
	}
}
