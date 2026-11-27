package Zeze.Transaction.Logs;

import java.math.BigDecimal;

import Zeze.Net.Binary;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Log;
import Zeze.Serialize.ByteBuffer;

/** String 字段变更值日志，独立于本地事务目标。 */
public class LogStringValue extends Log {
	public static final int TYPE_ID = Bean.hash32("Zeze.Transaction.Log<string>");

	private Object value;

	protected LogStringValue(int varId, Object value) {
		super(varId);
		this.value = value;
	}

	@Override
	public int getTypeId() {
		return TYPE_ID;
	}

	@Override
	public String getTypeName() {
		return "LogString";
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

	public Object getValue() {
		return value;
	}

	@Override
	public BigDecimal decimalValue() {
		// 不指定 MathContext，避免 DECIMAL128 静默截断超过 34 位的有效数字。
		return new BigDecimal(value.toString());
	}

	public static LogStringValue decode(int varId, IByteBuffer bb) {
		return new LogStringValue(varId, bb.ReadString());
	}
}
