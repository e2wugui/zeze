package Zeze.Raft.RocksRaft.Log1;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

import Zeze.Raft.RocksRaft.Bean;
import Zeze.Raft.RocksRaft.Log;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.Vector2;

/**
 * Vector2 落盘日志：序列化 vector2 字段的新值。
 */
public class LogVector2 extends Log {
	private static final int TYPE_ID = Zeze.Transaction.Bean.hash32("Zeze.Raft.RocksRaft.Log<vector2>");

	public @Nullable Vector2 value;

	public LogVector2() {
		super(TYPE_ID);
	}

	public LogVector2(@Nullable Bean belong, int varId, Vector2 value) {
		this();
		setBelong(belong);
		setVariableId(varId);
		this.value = value;
	}

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteVector2(Objects.requireNonNull(value));
	}

	@Override
	public void decode(IByteBuffer bb) {
		value = bb.ReadVector2();
	}

	@Override
	public String toString() {
		return "Value=" + value;
	}
}
