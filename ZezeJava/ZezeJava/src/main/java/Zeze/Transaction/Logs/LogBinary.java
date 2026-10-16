package Zeze.Transaction.Logs;

import java.util.Objects;

import java.lang.invoke.VarHandle;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import Zeze.Net.Binary;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Log;
import Zeze.Serialize.ByteBuffer;
import org.jspecify.annotations.Nullable;

/** binary 字段原子修改日志：携带新 Binary 值，commit 经 VarHandle 写回，序列化为字节缓冲。 */
public class LogBinary extends Log {
	private static final int TYPE_ID = Bean.hash32("Zeze.Transaction.Log<binary>");

	private final @Nullable VarHandle vh;
	public @Nullable Binary value;

	public LogBinary(@Nullable Bean belong, int varId, @Nullable VarHandle vh, Binary value) {
		super(belong, varId);
		this.vh = vh;
		this.value = value;
	}

	public LogBinary(int varId) {
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
		bb.WriteBinary(Objects.requireNonNull(value));
	}

	@Override
	public void decode(IByteBuffer bb) {
		value = bb.ReadBinary();
	}

	@Override
	public String toString() {
		return Objects.requireNonNull(value).toString();
	}

	@Override
	public Binary binaryValue() {
		return Objects.requireNonNull(value);
	}

	@Override
	public String stringValue() {
		return Objects.requireNonNull(value).toString(StandardCharsets.UTF_8);
	}

	@Override
	public BigDecimal decimalValue() {
		// 不用DECIMAL128：静默截断>34位有效数字（LogDecimal.decode同款已修），字符串构造本就精确
		return new BigDecimal(stringValue());
	}
}
