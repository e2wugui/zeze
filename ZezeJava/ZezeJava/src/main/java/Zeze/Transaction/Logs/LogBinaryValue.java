package Zeze.Transaction.Logs;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import Zeze.Net.Binary;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Log;
import Zeze.Serialize.ByteBuffer;

/** Binary 字段变更值日志，独立于本地事务目标。 */
public class LogBinaryValue extends Log {
	public static final int TYPE_ID = Bean.hash32("Zeze.Transaction.Log<binary>");

	public Binary value;

	protected LogBinaryValue(int varId, Binary value) {
		super(varId);
		this.value = value;
	}

	@Override
	public int getTypeId() {
		return TYPE_ID;
	}

	@Override
	public String getTypeName() {
		return "LogBinary";
	}

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteBinary(value);
	}

	@Override
	public String toString() {
		return value.toString();
	}

	@Override
	public Binary binaryValue() {
		return value;
	}

	@Override
	public String stringValue() {
		return value.toString(StandardCharsets.UTF_8);
	}

	@Override
	public BigDecimal decimalValue() {
		// 不指定 MathContext，避免 DECIMAL128 静默截断超过 34 位的有效数字。
		return new BigDecimal(stringValue());
	}

	public static LogBinaryValue decode(int varId, IByteBuffer bb) {
		return new LogBinaryValue(varId, bb.ReadBinary());
	}
}
