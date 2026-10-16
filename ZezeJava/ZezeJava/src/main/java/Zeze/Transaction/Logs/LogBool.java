package Zeze.Transaction.Logs;

import java.lang.invoke.VarHandle;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Log;
import Zeze.Serialize.ByteBuffer;
import org.jspecify.annotations.Nullable;

/** bool 字段原子修改日志：携带新值，commit 经 VarHandle 写回，序列化为布尔字节。 */
public class LogBool extends Log {
	private static final int TYPE_ID = Bean.hash32("Zeze.Transaction.Log<bool>");

	private final @Nullable VarHandle vh;
	public boolean value;

	public LogBool(@Nullable Bean belong, int varId, @Nullable VarHandle vh, boolean value) {
		super(belong, varId);
		this.vh = vh;
		this.value = value;
	}

	public LogBool(int varId) {
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
		bb.WriteBool(value);
	}

	@Override
	public void decode(IByteBuffer bb) {
		value = bb.ReadBool();
	}

	@Override
	public String toString() {
		return String.valueOf(value);
	}

	@Override
	public boolean booleanValue() {
		return value;
	}

	@Override
	public long longValue() {
		return value ? 1 : 0;
	}

	@Override
	public double doubleValue() {
		return value ? 1 : 0;
	}
}
