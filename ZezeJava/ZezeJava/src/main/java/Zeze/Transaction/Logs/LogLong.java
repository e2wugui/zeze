package Zeze.Transaction.Logs;

import java.lang.invoke.VarHandle;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Log;
import Zeze.Serialize.ByteBuffer;
import org.jspecify.annotations.Nullable;

/** long 字段原子修改日志：携带新值，commit 经 VarHandle 写回，序列化为 8 字节整数。 */
public class LogLong extends Log {
	private static final int TYPE_ID = Bean.hash32("Zeze.Transaction.Log<long>");

	private final @Nullable VarHandle vh;
	public long value;

	public LogLong(@Nullable Bean belong, int varId, @Nullable VarHandle vh, long value) {
		super(belong, varId);
		this.vh = vh;
		this.value = value;
	}

	public LogLong(int varId) {
		super(null, varId);
		vh = null;
	}

	@Override
	public Category category() {
		return Category.eHistory;
	}

	@Override
	public int getTypeId() {
		return TYPE_ID;
	}

	@Override
	public void commit() {
		//noinspection DataFlowIssue
		vh.set(getBelong(), value);
	}

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteLong(value);
	}

	@Override
	public void decode(IByteBuffer bb) {
		value = bb.ReadLong();
	}

	@Override
	public String toString() {
		return String.valueOf(value);
	}

	@Override
	public long longValue() {
		return value;
	}

	@Override
	public double doubleValue() {
		return value;
	}
}
