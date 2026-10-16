package Zeze.Raft.RocksRaft.Log1;


import org.jspecify.annotations.Nullable;

import Zeze.Raft.RocksRaft.Bean;
import Zeze.Raft.RocksRaft.Log;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

/**
 * String 落盘日志：序列化 string 字段的新值。
 */
public class LogString extends Log {
	private static final int TYPE_ID = Zeze.Transaction.Bean.hash32("Zeze.Raft.RocksRaft.Log<string>");

	public @Nullable String value;

	public LogString() {
		super(TYPE_ID);
	}

	public LogString(@Nullable Bean belong, int varId, @Nullable String value) {
		this();
		setBelong(belong);
		setVariableId(varId);
		this.value = value;
	}

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteString(value);
	}

	@Override
	public void decode(IByteBuffer bb) {
		value = bb.ReadString();
	}

	@Override
	public String toString() {
		return "Value=" + value;
	}
}
