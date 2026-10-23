package Zeze.Transaction.Logs;

import java.math.BigDecimal;

import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Log;

/** Complete field change value, independent of any local transaction target. */
public class LogDecimalValue extends Log {
	public static final int TYPE_ID = Bean.hash32("Zeze.Transaction.Log<decimal>");

	public BigDecimal value;

	protected LogDecimalValue(int varId, BigDecimal value) {
		super(varId);
		this.value = value;
	}

	@Override
	public int getTypeId() {
		return TYPE_ID;
	}

	@Override
	public String getTypeName() {
		return "LogDecimal";
	}

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteString(value.toString());
	}

	@Override
	public String toString() {
		return String.valueOf(value);
	}

	@Override
	public Binary binaryValue() {
		return new Binary(value.toString());
	}

	@Override
	public String stringValue() {
		return value.toString();
	}

	@Override
	public BigDecimal decimalValue() {
		return value;
	}

	public static LogDecimalValue decode(int varId, IByteBuffer bb) {
		// Preserve every decimal digit written by encode.
		return new LogDecimalValue(varId, new BigDecimal(bb.ReadString()));
	}
}
