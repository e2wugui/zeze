package Zeze.Transaction.Logs;

import java.lang.invoke.VarHandle;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Log;
import Zeze.Serialize.ByteBuffer;
import org.jetbrains.annotations.NotNull;

public class LogShort extends Log {
	private static final int TYPE_ID = Bean.hash32("Zeze.Transaction.Log<short>");

	private final VarHandle vh;
	public short value;

	public LogShort(Bean belong, int varId, VarHandle vh, short value) {
		super(belong, varId);
		this.vh = vh;
		this.value = value;
	}

	public LogShort(int varId) {
		super(null, varId);
		vh = null;
	}

	@Override
	public @NotNull Category category() {
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
	public void encode(@NotNull ByteBuffer bb) {
		bb.WriteInt(value);
	}

	@Override
	public void decode(@NotNull IByteBuffer bb) {
		value = (short)bb.ReadInt();
	}

	@Override
	public @NotNull String toString() {
		return String.valueOf(value);
	}

	@Override
	public short shortValue() {
		return value;
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
