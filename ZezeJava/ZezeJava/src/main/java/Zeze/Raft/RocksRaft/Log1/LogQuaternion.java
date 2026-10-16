package Zeze.Raft.RocksRaft.Log1;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

import Zeze.Raft.RocksRaft.Bean;
import Zeze.Raft.RocksRaft.Log;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.Quaternion;

/**
 * Quaternion 落盘日志：序列化 quaternion 字段的新值。
 */
public class LogQuaternion extends Log {
	private static final int TYPE_ID = Zeze.Transaction.Bean.hash32("Zeze.Raft.RocksRaft.Log<quaternion>");

	public @Nullable Quaternion value;

	public LogQuaternion() {
		super(TYPE_ID);
	}

	public LogQuaternion(@Nullable Bean belong, int varId, Quaternion value) {
		this();
		setBelong(belong);
		setVariableId(varId);
		this.value = value;
	}

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteQuaternion(Objects.requireNonNull(value));
	}

	@Override
	public void decode(IByteBuffer bb) {
		value = bb.ReadQuaternion();
	}

	@Override
	public String toString() {
		return "Value=" + value;
	}
}
