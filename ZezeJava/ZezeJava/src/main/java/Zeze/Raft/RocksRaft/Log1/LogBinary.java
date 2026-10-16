package Zeze.Raft.RocksRaft.Log1;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

import Zeze.Net.Binary;
import Zeze.Raft.RocksRaft.Bean;
import Zeze.Raft.RocksRaft.Log;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

/**
 * Binary 落盘日志：序列化 Binary 字段的新值。
 */
public class LogBinary extends Log {
	private static final int TYPE_ID = Zeze.Transaction.Bean.hash32("Zeze.Raft.RocksRaft.Log<binary>");

	public @Nullable Binary value;

	public LogBinary() {
		super(TYPE_ID);
	}

	public LogBinary(@Nullable Bean belong, int varId, Binary value) {
		this();
		setBelong(belong);
		setVariableId(varId);
		this.value = value;
	}

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteBinary(Objects.requireNonNull(value));
	}

	@Override
	public void decode(IByteBuffer bb) {
		value = bb.ReadBinary();
	}

	@Override
	public String toString() {
		return "Value=" + value;
	}
}
