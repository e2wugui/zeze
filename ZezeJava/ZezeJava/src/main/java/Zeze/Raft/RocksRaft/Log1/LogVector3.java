package Zeze.Raft.RocksRaft.Log1;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

import Zeze.Raft.RocksRaft.Bean;
import Zeze.Raft.RocksRaft.Log;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.Vector3;

/**
 * Vector3 落盘日志：序列化 vector3 字段的新值。
 */
public class LogVector3 extends Log {
	private static final int TYPE_ID = Zeze.Transaction.Bean.hash32("Zeze.Raft.RocksRaft.Log<vector3>");

	public @Nullable Vector3 value;

	public LogVector3() {
		super(TYPE_ID);
	}

	public LogVector3(@Nullable Bean belong, int varId, Vector3 value) {
		this();
		setBelong(belong);
		setVariableId(varId);
		this.value = value;
	}

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteVector3(Objects.requireNonNull(value));
	}

	@Override
	public void decode(IByteBuffer bb) {
		value = bb.ReadVector3();
	}

	@Override
	public String toString() {
		return "Value=" + value;
	}
}
