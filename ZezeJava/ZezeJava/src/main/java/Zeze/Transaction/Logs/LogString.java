package Zeze.Transaction.Logs;

import java.lang.invoke.VarHandle;
import java.math.BigDecimal;
import Zeze.Net.Binary;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Log;
import Zeze.Serialize.ByteBuffer;
import org.jspecify.annotations.Nullable;

/** string 字段原子修改日志：携带新值（内部以 Object 持有），commit 经 VarHandle 写回，序列化为字符串。 */
public class LogString extends Log {
	private static final int TYPE_ID = Bean.hash32("Zeze.Transaction.Log<string>");

	private final @Nullable VarHandle vh;
	private @Nullable Object value;

	public LogString(@Nullable Bean belong, int varId, @Nullable VarHandle vh, Object value) {
		super(belong, varId);
		this.vh = vh;
		this.value = value;
	}

	public LogString(int varId) {
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
		vh.set(getBelong(), value.toString());
	}

	@Override
	public void encode(ByteBuffer bb) {
		//noinspection DataFlowIssue
		bb.WriteString(value.toString());
	}

	@Override
	public void decode(IByteBuffer bb) {
		value = bb.ReadString();
	}

	@Override
	public String toString() {
		return String.valueOf(value);
	}

	@Override
	public Binary binaryValue() {
		//noinspection DataFlowIssue
		return new Binary(value.toString());
	}

	@Override
	public String stringValue() {
		//noinspection DataFlowIssue
		return value.toString();
	}

	public @Nullable Object getValue() {
		return value;
	}

	@Override
	public BigDecimal decimalValue() {
		// 不用DECIMAL128：静默截断>34位有效数字（LogDecimal.decode同款已修），字符串构造本就精确
		//noinspection DataFlowIssue
		return new BigDecimal(value.toString());
	}
}
