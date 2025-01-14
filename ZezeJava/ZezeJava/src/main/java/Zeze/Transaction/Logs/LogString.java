package Zeze.Transaction.Logs;

import java.lang.invoke.VarHandle;
import java.math.BigDecimal;
import java.math.MathContext;
import Zeze.Net.Binary;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Log;
import Zeze.Serialize.ByteBuffer;
import org.jetbrains.annotations.NotNull;

public class LogString extends Log {
	private static final int TYPE_ID = Bean.hash32("Zeze.Transaction.Log<string>");

	private final VarHandle vh;
	private Object value;

	public LogString(Bean belong, int varId, VarHandle vh, Object value) {
		super(belong, varId);
		this.vh = vh;
		this.value = value;
	}

	public LogString(int varId) {
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
		vh.set(getBelong(), value.toString());
	}

	@Override
	public void encode(@NotNull ByteBuffer bb) {
		bb.WriteString(value.toString());
	}

	@Override
	public void decode(@NotNull IByteBuffer bb) {
		value = bb.ReadString();
	}

	@Override
	public @NotNull String toString() {
		return String.valueOf(value);
	}

	@Override
	public @NotNull Binary binaryValue() {
		return new Binary(value.toString());
	}

	@Override
	public @NotNull String stringValue() {
		return value.toString();
	}

	@Override
	public @NotNull BigDecimal decimalValue() {
		return new BigDecimal(value.toString(), MathContext.DECIMAL128);
	}
}
