package Zeze.Raft.RocksRaft.Log1;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

import Zeze.Raft.RocksRaft.Bean;
import Zeze.Raft.RocksRaft.Log;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.Vector4;

/**
 * Vector4 落盘日志：序列化 vector4 字段的新值。
 */
public class LogVector4 extends Log {
	private static final int TYPE_ID = Zeze.Transaction.Bean.hash32("Zeze.Raft.RocksRaft.Log<vector4>");

	public @Nullable Vector4 value;

	public LogVector4() {
		super(TYPE_ID);
	}

	public LogVector4(@Nullable Bean belong, int varId, Vector4 value) {
		this();
		setBelong(belong);
		setVariableId(varId);
		this.value = value;
	}

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteVector4(Objects.requireNonNull(value));
	}

	@Override
	public void decode(IByteBuffer bb) {
		value = bb.ReadVector4();
	}

	@Override
	public String toString() {
		return "Value=" + value;
	}
}
