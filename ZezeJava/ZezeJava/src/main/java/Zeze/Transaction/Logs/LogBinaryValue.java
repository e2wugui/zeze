package Zeze.Transaction.Logs;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import Zeze.Net.Binary;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Log;
import Zeze.Serialize.ByteBuffer;

/** Complete field change value, independent of any local transaction target. */
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
		// 不用DECIMAL128：静默截断>34位有效数字（LogDecimal.decode同款已修），字符串构造本就精确
		return new BigDecimal(stringValue());
	}

	public static LogBinaryValue decode(int varId, IByteBuffer bb) {
		return new LogBinaryValue(varId, bb.ReadBinary());
	}
}
