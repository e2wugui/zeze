package Zeze.Raft.RocksRaft.Log1;

import org.jspecify.annotations.Nullable;

import Zeze.Raft.RocksRaft.Bean;
import Zeze.Raft.RocksRaft.Log;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.Vector2Int;

/**
 * Vector2Int 落盘日志：序列化 vector2int 字段的新值。
 */
public class LogVector2Int extends Log {
	private static final int TYPE_ID = Zeze.Transaction.Bean.hash32("Zeze.Raft.RocksRaft.Log<vector2int>");

	public @Nullable Vector2Int value;

	public LogVector2Int() {
		super(TYPE_ID);
	}

	public LogVector2Int(@Nullable Bean belong, int varId, Vector2Int value) {
		this();
		setBelong(belong);
		setVariableId(varId);
		this.value = value;
	}

	@Override
	public void encode(ByteBuffer bb) {
		//noinspection DataFlowIssue
		bb.WriteVector2Int(value);
	}

	@Override
	public void decode(IByteBuffer bb) {
		value = bb.ReadVector2Int();
	}

	@Override
	public String toString() {
		return "Value=" + value;
	}
}
