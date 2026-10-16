package Zeze.Raft.RocksRaft.Log1;

import org.jspecify.annotations.Nullable;

import Zeze.Raft.RocksRaft.Bean;
import Zeze.Raft.RocksRaft.Log;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

/**
 * byte 落盘日志：序列化 byte 字段的新值。
 */
public class LogByte extends Log {
	private static final int TYPE_ID = Zeze.Transaction.Bean.hash32("Zeze.Raft.RocksRaft.Log<byte>");

	public byte value;

	public LogByte() {
		super(TYPE_ID);
	}

	public LogByte(@Nullable Bean belong, int varId, byte value) {
		this();
		setBelong(belong);
		setVariableId(varId);
		this.value = value;
	}

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteLong(value);
	}

	@Override
	public void decode(IByteBuffer bb) {
		value = (byte)bb.ReadLong();
	}

	@Override
	public String toString() {
		return "Value=" + value;
	}
}
