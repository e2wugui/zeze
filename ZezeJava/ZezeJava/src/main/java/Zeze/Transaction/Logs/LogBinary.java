package Zeze.Transaction.Logs;

import java.lang.invoke.VarHandle;
import java.math.BigDecimal;
import java.math.MathContext;
import java.nio.charset.StandardCharsets;
import Zeze.Net.Binary;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Log;
import Zeze.Serialize.ByteBuffer;
import org.jetbrains.annotations.NotNull;

public class LogBinary extends Log {
	private static final int TYPE_ID = Bean.hash32("Zeze.Transaction.Log<binary>");

	private final VarHandle vh;
	public Binary value;

	public LogBinary(Bean belong, int varId, VarHandle vh, Binary value) {
		setBelong(belong);
		setVariableId(varId);
		this.vh = vh;
		this.value = value;
	}

	public LogBinary() {
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
		vh.set(getBelong(), value);
	}

	@Override
	public void encode(@NotNull ByteBuffer bb) {
		bb.WriteBinary(value);
	}

	@Override
	public void decode(@NotNull IByteBuffer bb) {
		value = bb.ReadBinary();
	}

	@Override
	public @NotNull String toString() {
		return value.toString();
	}

	@Override
	public @NotNull Binary binaryValue() {
		return value;
	}

	@Override
	public @NotNull String stringValue() {
		return value.toString(StandardCharsets.UTF_8);
	}

	@Override
	public @NotNull BigDecimal decimalValue() {
		return new BigDecimal(stringValue(), MathContext.DECIMAL128);
	}
}
