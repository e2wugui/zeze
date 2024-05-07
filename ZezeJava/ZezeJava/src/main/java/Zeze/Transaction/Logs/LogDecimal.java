package Zeze.Transaction.Logs;

import java.math.BigDecimal;
import java.math.MathContext;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Log;

public class LogDecimal extends Log {
	private static final int TYPE_ID = Bean.hash32("Zeze.Transaction.Log<decimal>");

	public BigDecimal value;

	public LogDecimal(Bean belong, int varId, BigDecimal value) {
		setBelong(belong);
		setVariableId(varId);
		this.value = value;
	}

	public LogDecimal() {

	}

	@Override
	public int getTypeId() {
		return TYPE_ID;
	}

	@Override
	public void commit() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteString(value.toString());
	}

	@Override
	public void decode(IByteBuffer bb) {
		value = new BigDecimal(bb.ReadString(), MathContext.DECIMAL128);
	}

	@Override
	public String toString() {
		return String.valueOf(value);
	}
}
